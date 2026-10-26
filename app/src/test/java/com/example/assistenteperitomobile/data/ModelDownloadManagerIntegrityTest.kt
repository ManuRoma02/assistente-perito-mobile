package com.example.assistenteperitomobile.data

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress

class ModelDownloadManagerIntegrityTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private var server: HttpServer? = null
    private var serverPort: Int = 0

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        serverPort = server!!.address.port
        server!!.start()
    }

    @After
    fun tearDown() {
        server?.stop(0)
    }

    private fun createTestModel(fileName: String, path: String, expectedBytes: Long): DownloadableAiModel {
        return DownloadableAiModel(
            id = "test_model_$fileName",
            roleBadge = "TEST",
            title = "Test Model",
            subtitle = "Test Model Subtitle",
            sizeLabel = "$expectedBytes B",
            expectedBytes = expectedBytes,
            fileName = fileName,
            downloadUrl = "http://127.0.0.1:$serverPort$path"
        )
    }

    @Test
    fun `test complete download verifies length and promotes part to target file`() = runBlocking {
        val testPayload = "Full Complete Model Content 1234567890".toByteArray(Charsets.US_ASCII)
        server!!.createContext("/full") { exchange: HttpExchange ->
            exchange.responseHeaders.set("Content-Length", testPayload.size.toString())
            exchange.sendResponseHeaders(200, testPayload.size.toLong())
            exchange.responseBody.write(testPayload)
            exchange.responseBody.close()
        }

        val modelsDir = tempFolder.newFolder("models_full")
        val fileName = "test_full.gguf"
        val model = createTestModel(fileName, "/full", testPayload.size.toLong())

        val manager = ModelDownloadManager(context = null, overrideModelsDir = modelsDir)
        val downloaded = manager.downloadModel(model) { _, _, _ -> }

        val targetFile = File(modelsDir, fileName)
        val partFile = File(modelsDir, "$fileName.part")

        assertTrue("File finale deve esistere", downloaded.exists())
        assertEquals(targetFile.absolutePath, downloaded.absolutePath)
        assertFalse("File .part deve essere stato promosso", partFile.exists())
        assertEquals(testPayload.size.toLong(), downloaded.length())
        assertEquals(String(testPayload), downloaded.readText())
    }

    @Test
    fun `test interrupted stream does not promote file and preserves part file`() = runBlocking {
        val testPayload = "0123456789ABCDEF".toByteArray(Charsets.US_ASCII)
        server!!.createContext("/interrupted") { exchange: HttpExchange ->
            // Dichiara Content-Length di 100 byte ma ne invia solo 16
            exchange.responseHeaders.set("Content-Length", "100")
            exchange.sendResponseHeaders(200, 100)
            exchange.responseBody.write(testPayload)
            exchange.responseBody.close()
        }

        val modelsDir = tempFolder.newFolder("models_interrupted")
        val fileName = "test_interrupted.gguf"
        val model = createTestModel(fileName, "/interrupted", 100L)

        val manager = ModelDownloadManager(context = null, overrideModelsDir = modelsDir)
        try {
            manager.downloadModel(model) { _, _, _ -> }
            fail("Dovrebbe fallire per download incompleto")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("Download incompleto") == true)
        }

        val targetFile = File(modelsDir, fileName)
        val partFile = File(modelsDir, "$fileName.part")

        assertFalse("Il file finale non deve essere promosso", targetFile.exists())
        assertTrue("Il file .part deve essere preservato per il resume", partFile.exists())
        assertEquals(16L, partFile.length())
    }

    @Test
    fun `test valid resume with content range promotes complete file`() = runBlocking {
        val firstHalf = "AAAAA".toByteArray(Charsets.US_ASCII)
        val secondHalf = "BBBBB".toByteArray(Charsets.US_ASCII)
        val fullSize = (firstHalf.size + secondHalf.size).toLong()

        server!!.createContext("/resumable") { exchange: HttpExchange ->
            val rangeHeader = exchange.requestHeaders.getFirst("Range")
            if (rangeHeader == "bytes=5-") {
                exchange.responseHeaders.set("Content-Range", "bytes 5-9/$fullSize")
                exchange.responseHeaders.set("Content-Length", secondHalf.size.toString())
                exchange.sendResponseHeaders(206, secondHalf.size.toLong())
                exchange.responseBody.write(secondHalf)
            } else {
                exchange.responseHeaders.set("Content-Length", fullSize.toString())
                exchange.sendResponseHeaders(200, fullSize)
                exchange.responseBody.write(firstHalf + secondHalf)
            }
            exchange.responseBody.close()
        }

        val modelsDir = tempFolder.newFolder("models_resumable")
        val fileName = "test_resumable.gguf"
        val partFile = File(modelsDir, "$fileName.part")

        // Pre-popola il file .part con i primi 5 byte
        partFile.writeBytes(firstHalf)
        assertEquals(5L, partFile.length())

        val model = createTestModel(fileName, "/resumable", fullSize)
        val manager = ModelDownloadManager(context = null, overrideModelsDir = modelsDir)
        val downloaded = manager.downloadModel(model) { _, _, _ -> }

        val targetFile = File(modelsDir, fileName)
        assertTrue("Il file finale deve esistere", downloaded.exists())
        assertEquals(targetFile.absolutePath, downloaded.absolutePath)
        assertFalse("Il file .part deve essere stato rimosso dopo promozione", partFile.exists())
        assertEquals(fullSize, downloaded.length())
        assertEquals("AAAAABBBBB", downloaded.readText())
    }

    @Test
    fun `test missing content range on http 206 fails and preserves part file`() = runBlocking {
        val chunk = "AAAAA".toByteArray(Charsets.US_ASCII)
        server!!.createContext("/missing_content_range") { exchange: HttpExchange ->
            // Risponde 206 ma senza header Content-Range
            exchange.responseHeaders.set("Content-Length", chunk.size.toString())
            exchange.sendResponseHeaders(206, chunk.size.toLong())
            exchange.responseBody.write(chunk)
            exchange.responseBody.close()
        }

        val modelsDir = tempFolder.newFolder("models_no_range")
        val fileName = "test_no_range.gguf"
        val partFile = File(modelsDir, "$fileName.part")
        partFile.writeBytes(chunk)

        val model = createTestModel(fileName, "/missing_content_range", 10L)
        val manager = ModelDownloadManager(context = null, overrideModelsDir = modelsDir)

        try {
            manager.downloadModel(model) { _, _, _ -> }
            fail("Dovrebbe fallire per Content-Range assente")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("Content-Range assente") == true)
        }

        val targetFile = File(modelsDir, fileName)
        assertFalse("Il file finale non deve essere promosso", targetFile.exists())
        assertTrue("Il file .part non deve essere cancellato o corrotto", partFile.exists())
        assertEquals(chunk.size.toLong(), partFile.length())
    }

    @Test
    fun `test malformed content range on http 206 fails and preserves part file`() = runBlocking {
        val chunk = "AAAAA".toByteArray(Charsets.US_ASCII)
        server!!.createContext("/malformed_content_range") { exchange: HttpExchange ->
            exchange.responseHeaders.set("Content-Range", "invalid-garbage-range")
            exchange.responseHeaders.set("Content-Length", chunk.size.toString())
            exchange.sendResponseHeaders(206, chunk.size.toLong())
            exchange.responseBody.write(chunk)
            exchange.responseBody.close()
        }

        val modelsDir = tempFolder.newFolder("models_malformed_range")
        val fileName = "test_malformed_range.gguf"
        val partFile = File(modelsDir, "$fileName.part")
        partFile.writeBytes(chunk)

        val model = createTestModel(fileName, "/malformed_content_range", 10L)
        val manager = ModelDownloadManager(context = null, overrideModelsDir = modelsDir)

        try {
            manager.downloadModel(model) { _, _, _ -> }
            fail("Dovrebbe fallire per Content-Range malformato")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("Content-Range malformato") == true)
        }

        val targetFile = File(modelsDir, fileName)
        assertFalse("Il file finale non deve essere promosso", targetFile.exists())
        assertTrue("Il file .part deve rimanere intatto", partFile.exists())
        assertEquals(chunk.size.toLong(), partFile.length())
    }

    @Test
    fun `test wrong offset in content range fails and preserves part file without corrupt append`() = runBlocking {
        val initialPart = "AAAAA".toByteArray(Charsets.US_ASCII)
        val secondPart = "BBBBB".toByteArray(Charsets.US_ASCII)
        server!!.createContext("/wrong_offset") { exchange: HttpExchange ->
            // Risponde con rangeStart = 0 invece dell'atteso 5
            exchange.responseHeaders.set("Content-Range", "bytes 0-4/10")
            exchange.responseHeaders.set("Content-Length", secondPart.size.toString())
            exchange.sendResponseHeaders(206, secondPart.size.toLong())
            exchange.responseBody.write(secondPart)
            exchange.responseBody.close()
        }

        val modelsDir = tempFolder.newFolder("models_wrong_offset")
        val fileName = "test_wrong_offset.gguf"
        val partFile = File(modelsDir, "$fileName.part")
        partFile.writeBytes(initialPart)

        val model = createTestModel(fileName, "/wrong_offset", 10L)
        val manager = ModelDownloadManager(context = null, overrideModelsDir = modelsDir)

        try {
            manager.downloadModel(model) { _, _, _ -> }
            fail("Dovrebbe fallire per offset Content-Range errato")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("Offset Content-Range non corrispondente") == true)
        }

        val targetFile = File(modelsDir, fileName)
        assertFalse("Il file finale non deve essere promosso", targetFile.exists())
        assertTrue("Il file .part deve rimanere intatto", partFile.exists())
        assertEquals("Il file .part non deve contenere append corrotti", 5L, partFile.length())
        assertEquals("AAAAA", partFile.readText())
    }

    @Test
    fun `test server ignoring range with http 200 safely overwrites part from beginning`() = runBlocking {
        val stalePart = "STALEGARBAGE".toByteArray(Charsets.US_ASCII)
        val freshPayload = "0123456789".toByteArray(Charsets.US_ASCII)

        server!!.createContext("/ignore_range") { exchange: HttpExchange ->
            // Ignora Range e risponde 200 OK con l'intero contenuto
            exchange.responseHeaders.set("Content-Length", freshPayload.size.toString())
            exchange.sendResponseHeaders(200, freshPayload.size.toLong())
            exchange.responseBody.write(freshPayload)
            exchange.responseBody.close()
        }

        val modelsDir = tempFolder.newFolder("models_ignore_range")
        val fileName = "test_ignore_range.gguf"
        val partFile = File(modelsDir, "$fileName.part")
        partFile.writeBytes(stalePart)
        assertEquals(stalePart.size.toLong(), partFile.length())

        val model = createTestModel(fileName, "/ignore_range", freshPayload.size.toLong())
        val manager = ModelDownloadManager(context = null, overrideModelsDir = modelsDir)
        val downloaded = manager.downloadModel(model) { _, _, _ -> }

        val targetFile = File(modelsDir, fileName)
        assertTrue(downloaded.exists())
        assertEquals(targetFile.absolutePath, downloaded.absolutePath)
        assertFalse(partFile.exists())
        assertEquals(freshPayload.size.toLong(), downloaded.length())
        assertEquals("0123456789", downloaded.readText())
    }

    @Test
    fun `test wrong final size against expectedBytes fails and does not promote file`() = runBlocking {
        // Il server completa l'invio ma invia solo 8 byte quando expectedBytes è 10
        val shortPayload = "12345678".toByteArray(Charsets.US_ASCII)

        server!!.createContext("/wrong_size") { exchange: HttpExchange ->
            exchange.responseHeaders.set("Content-Length", shortPayload.size.toString())
            exchange.sendResponseHeaders(200, shortPayload.size.toLong())
            exchange.responseBody.write(shortPayload)
            exchange.responseBody.close()
        }

        val modelsDir = tempFolder.newFolder("models_wrong_size")
        val fileName = "test_wrong_size.gguf"
        val model = createTestModel(fileName, "/wrong_size", expectedBytes = 10L)

        val manager = ModelDownloadManager(context = null, overrideModelsDir = modelsDir)

        try {
            manager.downloadModel(model) { _, _, _ -> }
            fail("Dovrebbe fallire perché scaricati 8 byte su 10 attesi")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("Dimensione finale non corrispondente") == true)
        }

        val targetFile = File(modelsDir, fileName)
        val partFile = File(modelsDir, "$fileName.part")

        assertFalse("Il file finale non deve essere promosso con dimensione errata", targetFile.exists())
        assertTrue("Il file .part deve essere preservato", partFile.exists())
        assertEquals(8L, partFile.length())
    }
}
