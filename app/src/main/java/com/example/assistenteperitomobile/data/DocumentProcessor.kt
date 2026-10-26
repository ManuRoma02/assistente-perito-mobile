package com.example.assistenteperitomobile.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.Locale

const val MAX_DOCUMENT_SIZE_BYTES = 50 * 1024 * 1024L // 50 MB: limite prudenziale per prevenire OutOfMemory su heap Android (192-256 MB)

class DocumentTooLargeException(
    val actualSize: Long,
    val maxSize: Long = MAX_DOCUMENT_SIZE_BYTES
) : IllegalArgumentException(
    "Il file supera il limite massimo di ${maxSize / (1024 * 1024)} MB per l'elaborazione su dispositivo mobile (dimensione: ${String.format(Locale.ITALIAN, "%.1f", actualSize / (1024.0 * 1024.0))} MB)."
)

data class ProcessedDocumentPayload(
    val cleanFileName: String,
    val extractedBytes: ByteArray,
    val extractedText: String,
    val wasP7mUnwrapped: Boolean = false
)

class DocumentProcessor(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Lettura protetta con streaming limitato a maxBytes per prevenire spike di memoria heap.
     */
    fun readStreamBounded(
        stream: InputStream,
        maxBytes: Long = MAX_DOCUMENT_SIZE_BYTES
    ): ByteArray {
        val buffer = ByteArray(64 * 1024)
        val out = ByteArrayOutputStream()
        var totalRead = 0L
        while (true) {
            val r = stream.read(buffer)
            if (r == -1) break
            totalRead += r
            if (totalRead > maxBytes) {
                throw DocumentTooLargeException(totalRead, maxBytes)
            }
            out.write(buffer, 0, r)
        }
        return out.toByteArray()
    }

    /**
     * Elabora un URI proveniente da Fotocamera, File Picker o Condivisione WhatsApp/Email:
     * 1. Se è un .p7m (busta crittografica CAdES PEC/Tribunale), estrae il PDF interno.
     * 2. Se è un PDF, renderizza le pagine con PdfRenderer ed esegue OCR On-Device tramite Google ML Kit.
     * 3. Se è un'immagine/foto, esegue OCR On-Device tramite Google ML Kit.
     */
    suspend fun processUri(uri: Uri, originalNameHint: String? = null): ProcessedDocumentPayload =
        withContext(Dispatchers.IO) {
            val rawName = originalNameHint ?: queryDisplayName(uri) ?: "documento_${System.currentTimeMillis()}.pdf"
            val declaredSize = queryFileSize(uri)
            if (declaredSize != null && declaredSize > MAX_DOCUMENT_SIZE_BYTES) {
                throw DocumentTooLargeException(declaredSize, MAX_DOCUMENT_SIZE_BYTES)
            }
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                readStreamBounded(stream, MAX_DOCUMENT_SIZE_BYTES)
            } ?: ByteArray(0)

            processBytes(rawBytes, rawName)
        }

    suspend fun processBytes(rawBytes: ByteArray, rawName: String): ProcessedDocumentPayload =
        withContext(Dispatchers.IO) {
            if (rawBytes.size > MAX_DOCUMENT_SIZE_BYTES) {
                throw DocumentTooLargeException(rawBytes.size.toLong(), MAX_DOCUMENT_SIZE_BYTES)
            }
            val lowerName = rawName.lowercase()
            var workingBytes = rawBytes
            var workingName = rawName
            var wasP7m = false

            // 1. Sbustamento .p7m CAdES (anche annidato .pdf.p7m.p7m)
            if (lowerName.endsWith(".p7m") || containsPdfHeaderInsideEnvelope(rawBytes)) {
                val unwrapped = unwrapP7mBytes(rawBytes, rawName)
                workingBytes = unwrapped.first
                workingName = unwrapped.second
                wasP7m = lowerName.endsWith(".p7m")
            }

            // 2. Estrazione testo (OCR ML Kit per PDF o Immagini)
            val extractedText = when {
                isPdfBytes(workingBytes) || workingName.lowercase().endsWith(".pdf") -> {
                    extractTextFromPdfBytes(workingBytes)
                }
                isImageFileName(workingName) || isBitmapDecodable(workingBytes) -> {
                    extractTextFromImageBytes(workingBytes)
                }
                else -> {
                    extractReadableTextFromRawBytes(workingBytes)
                }
            }

            ProcessedDocumentPayload(
                cleanFileName = workingName,
                extractedBytes = workingBytes,
                extractedText = extractedText.trim(),
                wasP7mUnwrapped = wasP7m
            )
        }

    suspend fun performOcrOnBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            result.text
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Sbustamento nativo in puro Kotlin delle buste crittografiche CAdES (.p7m) di PEC e Tribunale.
     * Cerca l'header %PDF- e il footer %%EOF rimuovendo i chunk ASN.1 DER se presenti.
     */
    fun unwrapP7mBytes(rawBytes: ByteArray, originalFileName: String): Pair<ByteArray, String> {
        val cleanName = originalFileName.replace(Regex("""(\.p7m)+$""", RegexOption.IGNORE_CASE), "")
            .ifBlank { "atto_estratto.pdf" }

        val pdfHeader = "%PDF-".toByteArray(Charsets.US_ASCII)
        val pdfFooter = "%%EOF".toByteArray(Charsets.US_ASCII)

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

    private suspend fun extractTextFromPdfBytes(pdfBytes: ByteArray): String {
        val tempFile = File.createTempFile("ocr_pdf_", ".pdf", context.cacheDir)
        val sb = StringBuilder()
        try {
            tempFile.writeBytes(pdfBytes)
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val totalPages = renderer.pageCount
                    // Seleziona le pagine cruciali per una perizia/CTU: fino a 25 pagine
                    // (prime 15 pagine per nomine/quesiti, ultime 10 per conclusioni e date se documento lungo)
                    val pagesToProcess = if (totalPages <= 25) {
                        (0 until totalPages).toList()
                    } else {
                        val head = (0 until 15).toList()
                        val tail = ((totalPages - 10) until totalPages).toList()
                        (head + tail).distinct()
                    }

                    for (pageIdx in pagesToProcess) {
                        if (pageIdx >= totalPages) continue
                        renderer.openPage(pageIdx).use { page ->
                            val width = (page.width * 1.5f).toInt().coerceIn(600, 1600)
                            val height = (page.height * 1.5f).toInt().coerceIn(800, 2200)
                            // RGB_565 dimezza il consumo di RAM per pagina rispetto a ARGB_8888 (2B vs 4B per pixel)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                            try {
                                val canvas = Canvas(bitmap)
                                canvas.drawColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                val pageText = performOcrOnBitmap(bitmap)
                                if (pageText.isNotBlank()) {
                                    sb.appendLine(pageText)
                                }
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        } finally {
            tempFile.delete()
        }

        return sb.toString().trim()
    }

    private suspend fun extractTextFromImageBytes(imageBytes: ByteArray): String {
        // Decodifica memory-safe: calcola inSampleSize per foto ad altissima risoluzione (48MP/108MP)
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, boundsOpts)
        val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        var sampleSize = 1
        while (maxDim / sampleSize > 2048) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOpts) ?: return ""
        return try {
            performOcrOnBitmap(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    fun extractReadableTextFromRawBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        // Estrae sequenze di caratteri stampabili (utile anche per PDF testuali semplici o .p7m testuali)
        val rawStr = String(bytes, Charsets.ISO_8859_1)
        val matches = Regex("""[A-Za-zÀ-ÿ0-9.,;:()/'"_\-\s]{6,}""").findAll(rawStr)
        return matches.map { it.value.trim() }
            .filter { it.length >= 6 && !it.startsWith("obj") && !it.startsWith("endobj") && !it.startsWith("stream") }
            .joinToString(" ")
            .take(6000)
    }

    private fun isPdfBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 5) return false
        val header = "%PDF-".toByteArray(Charsets.US_ASCII)
        return indexOfSubArray(bytes.copyOfRange(0, bytes.size.coerceAtMost(64)), header) >= 0
    }

    private fun containsPdfHeaderInsideEnvelope(bytes: ByteArray): Boolean {
        if (bytes.size < 10) return false
        val header = "%PDF-".toByteArray(Charsets.US_ASCII)
        val idx = indexOfSubArray(bytes, header)
        return idx > 0
    }

    private fun isImageFileName(name: String): Boolean {
        val l = name.lowercase()
        return l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png") || l.endsWith(".webp")
    }

    private fun isBitmapDecodable(bytes: ByteArray): Boolean {
        if (bytes.size < 16) return false
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        return opts.outWidth > 0 && opts.outHeight > 0
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun queryFileSize(uri: Uri): Long? {
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (idx >= 0 && cursor.moveToFirst() && !cursor.isNull(idx)) {
                    cursor.getLong(idx)
                } else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun indexOfSubArray(source: ByteArray, target: ByteArray): Int {
        if (target.isEmpty() || source.size < target.size) return -1
        outer@ for (i in 0..source.size - target.size) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun lastIndexOfSubArray(source: ByteArray, target: ByteArray): Int {
        if (target.isEmpty() || source.size < target.size) return -1
        outer@ for (i in (source.size - target.size) downTo 0) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
