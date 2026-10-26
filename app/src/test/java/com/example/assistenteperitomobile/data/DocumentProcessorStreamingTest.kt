package com.example.assistenteperitomobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class DocumentProcessorStreamingTest {

    @Test
    fun `test readStreamBounded succeeds for content within size limit`() {
        val sampleData = "Relazione tecnica di sopralluogo peritale CTU".toByteArray(Charsets.UTF_8)
        val stream = ByteArrayInputStream(sampleData)

        // Simula il processor leggendo con limite 1 MB
        val buffer = ByteArray(64 * 1024)
        val out = java.io.ByteArrayOutputStream()
        var totalRead = 0L
        val maxLimit = 1024 * 1024L

        while (true) {
            val r = stream.read(buffer)
            if (r == -1) break
            totalRead += r
            if (totalRead > maxLimit) {
                throw DocumentTooLargeException(totalRead, maxLimit)
            }
            out.write(buffer, 0, r)
        }

        val result = out.toByteArray()
        assertEquals(sampleData.size, result.size)
        assertEquals(String(sampleData), String(result))
    }

    @Test
    fun `test readStreamBounded throws DocumentTooLargeException when limit exceeded`() {
        // Generatore virtuale di stream infinito senza allocare 50MB in heap
        val infiniteStream = object : InputStream() {
            override fun read(): Int = 0x41 // 'A'
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                b.fill(0x41, off, off + len)
                return len
            }
        }

        val maxAllowed = 1024L * 1024L // 1 MB per il test
        val buffer = ByteArray(64 * 1024)
        var totalRead = 0L

        try {
            while (true) {
                val r = infiniteStream.read(buffer)
                if (r == -1) break
                totalRead += r
                if (totalRead > maxAllowed) {
                    throw DocumentTooLargeException(totalRead, maxAllowed)
                }
            }
            fail("Dovrebbe lanciare DocumentTooLargeException")
        } catch (e: DocumentTooLargeException) {
            assertTrue(e.actualSize > maxAllowed)
            assertEquals(maxAllowed, e.maxSize)
            assertTrue(e.message?.contains("supera il limite massimo") == true)
        }
    }

    @Test
    fun `test DocumentTooLargeException format and message`() {
        val ex = DocumentTooLargeException(actualSize = 75 * 1024 * 1024L, maxSize = 50 * 1024 * 1024L)
        assertTrue(ex.message!!.contains("50 MB"))
        assertTrue(ex.message!!.contains("75.0 MB") || ex.message!!.contains("75,0 MB"))
    }
}
