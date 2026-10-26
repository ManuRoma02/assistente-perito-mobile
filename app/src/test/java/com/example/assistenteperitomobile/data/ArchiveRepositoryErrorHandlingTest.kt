package com.example.assistenteperitomobile.data

import com.example.assistenteperitomobile.domain.ArchivedDocument
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ArchiveRepositoryErrorHandlingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private fun loadDocumentsTestHelper(
        indexFile: File,
        cachedDocs: List<ArchivedDocument>?
    ): Pair<Result<List<ArchivedDocument>>, List<ArchivedDocument>?> {
        var currentCache = cachedDocs
        if (!indexFile.exists()) {
            return Result.success(emptyList<ArchivedDocument>()) to emptyList()
        }
        return try {
            val content = indexFile.readText()
            val list = json.decodeFromString<List<ArchivedDocument>>(content)
            val filtered = list.filter { File(it.filePath).exists() }
                .sortedByDescending { it.createdAtMillis }
            currentCache = filtered
            Result.success(filtered) to currentCache
        } catch (e: Throwable) {
            // Non sovrascrive la cache in caso di errore
            Result.failure<List<ArchivedDocument>>(e) to currentCache
        }
    }

    @Test
    fun `test non-existent index file returns empty list safely`() {
        val rootDir = tempFolder.newFolder("archive_1")
        val indexFile = File(rootDir, "archive_index.json")

        val (result, cache) = loadDocumentsTestHelper(indexFile, null)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
        assertEquals(emptyList<ArchivedDocument>(), cache)
    }

    @Test
    fun `test corrupted index file returns failure and preserves existing cache`() {
        val rootDir = tempFolder.newFolder("archive_2")
        val indexFile = File(rootDir, "archive_index.json")
        val corruptedJson = "{ this is invalid json content !!!"
        indexFile.writeText(corruptedJson)

        val previousCache = listOf(
            ArchivedDocument(
                id = "doc-1",
                fileName = "perizia.pdf",
                originalName = "perizia.pdf",
                categoryFolder = "Perizie_Tecniche",
                cliente = "Rossi",
                oggetto = "Danni",
                dateIso = "2026-10-26",
                filePath = "/tmp/dummy",
                sizeBytes = 1000L,
                extractedTextSnippet = "",
                createdAtMillis = 1000L
            )
        )

        val (result, cache) = loadDocumentsTestHelper(indexFile, previousCache)

        assertTrue("Deve segnalare failure quando l'indice è corrotto", result.isFailure)
        assertEquals("La cache non deve essere azzerata se la lettura fallisce", previousCache, cache)
        assertTrue("Il file indice originale non deve essere cancellato", indexFile.exists())
        assertEquals(corruptedJson, indexFile.readText())
    }

    @Test
    fun `test valid index file parses and filters existing files`() {
        val rootDir = tempFolder.newFolder("archive_3")
        val indexFile = File(rootDir, "archive_index.json")

        val realFile = File(rootDir, "real_file.pdf").apply { writeText("dummy content") }
        val missingFile = File(rootDir, "missing_file.pdf")

        val doc1 = ArchivedDocument(
            id = "doc-1",
            fileName = "real_file.pdf",
            originalName = "real_file.pdf",
            categoryFolder = "Perizie_Tecniche",
            cliente = "Bianchi",
            oggetto = "Infiltrazioni",
            dateIso = "2026-10-26",
            filePath = realFile.absolutePath,
            sizeBytes = 1000L,
            extractedTextSnippet = "",
            createdAtMillis = 2000L
        )

        val doc2 = ArchivedDocument(
            id = "doc-2",
            fileName = "missing_file.pdf",
            originalName = "missing_file.pdf",
            categoryFolder = "Atti_Tribunale",
            cliente = "Verdi",
            oggetto = "CTU",
            dateIso = "2026-10-26",
            filePath = missingFile.absolutePath,
            sizeBytes = 1000L,
            extractedTextSnippet = "",
            createdAtMillis = 1000L
        )

        val serialized = json.encodeToString(listOf(doc1, doc2))
        indexFile.writeText(serialized)

        val (result, cache) = loadDocumentsTestHelper(indexFile, null)

        assertTrue(result.isSuccess)
        val list = result.getOrNull()!!
        assertEquals(1, list.size)
        assertEquals("doc-1", list[0].id)
        assertEquals(1, cache?.size)
    }
}
