package com.example.assistenteperitomobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentProcessorTest {

    // Helper standalone function mirroring unwrapP7mBytes logic
    private fun unwrapP7m(rawBytes: ByteArray, originalFileName: String): Pair<ByteArray, String> {
        val cleanName = originalFileName.replace(Regex("""(\.p7m)+$""", RegexOption.IGNORE_CASE), "")
            .ifBlank { "atto_estratto.pdf" }

        val pdfHeader = "%PDF-".toByteArray(Charsets.US_ASCII)
        val pdfFooter = "%%EOF".toByteArray(Charsets.US_ASCII)

        fun indexOfSubArray(source: ByteArray, target: ByteArray): Int {
            if (target.isEmpty() || source.size < target.size) return -1
            for (i in 0..(source.size - target.size)) {
                var match = true
                for (j in target.indices) {
                    if (source[i + j] != target[j]) {
                        match = false
                        break
                    }
                }
                if (match) return i
            }
            return -1
        }

        fun lastIndexOfSubArray(source: ByteArray, target: ByteArray): Int {
            if (target.isEmpty() || source.size < target.size) return -1
            for (i in (source.size - target.size) downTo 0) {
                var match = true
                for (j in target.indices) {
                    if (source[i + j] != target[j]) {
                        match = false
                        break
                    }
                }
                if (match) return i
            }
            return -1
        }

        val startIdx = indexOfSubArray(rawBytes, pdfHeader)
        if (startIdx >= 0) {
            val lastEofIdx = lastIndexOfSubArray(rawBytes, pdfFooter)
            val endIdx = if (lastEofIdx > startIdx) {
                (lastEofIdx + pdfFooter.size).coerceAtMost(rawBytes.size)
            } else {
                rawBytes.size
            }
            val slice = rawBytes.copyOfRange(startIdx, endIdx)
            val finalName = if (cleanName.lowercase().endsWith(".pdf")) cleanName else "$cleanName.pdf"
            return slice to finalName
        }

        return rawBytes to cleanName
    }

    @Test
    fun `test unwrap p7m envelope extracts nested pdf correctly`() {
        val dummyDerHeader = byteArrayOf(0x30, 0x82.toByte(), 0x01, 0x20)
        val dummyPdfContent = "%PDF-1.7\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF".toByteArray(Charsets.US_ASCII)
        val dummyPkcsSignature = byteArrayOf(0x04, 0x09, 0xFF.toByte(), 0xEE.toByte())

        val combinedBytes = dummyDerHeader + dummyPdfContent + dummyPkcsSignature

        val (unwrappedBytes, cleanName) = unwrapP7m(combinedBytes, "Nomina_CTU_Tribunale.pdf.p7m")

        assertEquals("Nomina_CTU_Tribunale.pdf", cleanName)
        assertEquals(String(dummyPdfContent, Charsets.US_ASCII), String(unwrappedBytes, Charsets.US_ASCII))
    }

    @Test
    fun `test nested p7m extensions stripped completely`() {
        val dummyPdf = "%PDF-1.4 %%EOF".toByteArray(Charsets.US_ASCII)
        val (unwrappedBytes, cleanName) = unwrapP7m(dummyPdf, "verbale.pdf.p7m.p7m")
        assertEquals("verbale.pdf", cleanName)
        assertTrue(unwrappedBytes.isNotEmpty())
    }

    @Test
    fun `test non-p7m file preserved without corruption`() {
        val plainText = "Relazione tecnica di sopralluogo".toByteArray(Charsets.UTF_8)
        val (bytes, cleanName) = unwrapP7m(plainText, "nota_sopralluogo.txt")
        assertEquals("nota_sopralluogo.txt", cleanName)
        assertEquals(plainText.size, bytes.size)
    }
}
