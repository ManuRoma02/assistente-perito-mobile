package com.example.assistenteperitomobile.data

import android.content.ContentResolver
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
    val wasP7mUnwrapped: Boolean = false,
    val isSignatureVerified: Boolean = false
)

class DocumentProcessor(private val context: Context) {

    private val recognizerDelegate = lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val recognizer
        get() = recognizerDelegate.value

    fun close() {
        if (recognizerDelegate.isInitialized()) {
            runCatching { recognizerDelegate.value.close() }
        }
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
            if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
                throw SecurityException("Schema URI non consentito: ${uri.scheme}")
            }
            if (uri.authority == "${context.packageName}.fileprovider") {
                throw SecurityException("Accesso diretto al FileProvider interno non consentito")
            }
            val safeHint = originalNameHint?.let { sanitizeDisplayName(it) }
            val rawName = safeHint ?: queryDisplayName(uri) ?: "documento_${System.currentTimeMillis()}.pdf"
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

            // 1. Sbustamento .p7m CAdES (solo se estensione .p7m o busta ASN.1 SEQUENCE 0x30)
            val isAsn1Sequence = rawBytes.isNotEmpty() && rawBytes[0] == 0x30.toByte()
            if (lowerName.endsWith(".p7m") || (isAsn1Sequence && containsPdfHeaderInsideEnvelope(rawBytes))) {
                val unwrapped = unwrapP7mBytes(rawBytes, rawName)
                workingBytes = unwrapped.first
                workingName = unwrapped.second
                wasP7m = lowerName.endsWith(".p7m") || isAsn1Sequence
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
                wasP7mUnwrapped = wasP7m,
                isSignatureVerified = false
            )
        }

    suspend fun performOcrOnBitmap(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Sbustamento nativo in puro Kotlin delle buste crittografiche CAdES (.p7m) di PEC e Tribunale.
     * Estrae il payload OCTET STRING da ASN.1 DER/BER ContentInfo -> SignedData -> encapContentInfo,
     * con fallback alla ricerca %PDF-..%%EOF per buste parziali.
     */
    fun unwrapP7mBytes(rawBytes: ByteArray, originalFileName: String): Pair<ByteArray, String> {
        val cleanName = originalFileName.replace(Regex("""(\.p7m)+$""", RegexOption.IGNORE_CASE), "")
            .ifBlank { "atto_estratto.pdf" }

        var candidate = rawBytes
        var extractedFromAsn1 = false
        for (depth in 0 until 3) {
            if (candidate.isEmpty() || candidate[0] != 0x30.toByte()) break
            val inner = extractCmsOctetStringPayload(candidate) ?: break
            if (inner.isEmpty()) break
            candidate = inner
            extractedFromAsn1 = true
        }
        if (extractedFromAsn1) {
            val finalName = if (isPdfBytes(candidate) && !cleanName.lowercase().endsWith(".pdf")) {
                "$cleanName.pdf"
            } else {
                cleanName
            }
            return candidate to finalName
        }

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

    private data class Asn1Header(
        val tag: Int,
        val contentStart: Int,
        val contentEnd: Int,
        val isIndefinite: Boolean
    )

    private fun readAsn1Header(data: ByteArray, offset: Int, limit: Int): Asn1Header? {
        if (offset < 0 || offset >= limit) return null
        var pos = offset
        val firstTag = data[pos++].toInt() and 0xFF
        if ((firstTag and 0x1F) == 0x1F) {
            while (pos < limit) {
                val b = data[pos++].toInt() and 0xFF
                if ((b and 0x80) == 0) break
            }
        }
        if (pos >= limit) return null
        val lenByte = data[pos++].toInt() and 0xFF
        if (lenByte < 0x80) {
            val end = pos + lenByte
            if (end > limit) return null
            return Asn1Header(firstTag, pos, end, false)
        }
        if (lenByte == 0x80) {
            return Asn1Header(firstTag, pos, limit, true)
        }
        val numOctets = lenByte and 0x7F
        if (numOctets !in 1..4 || pos + numOctets > limit) return null
        var len = 0L
        for (i in 0 until numOctets) {
            len = (len shl 8) or (data[pos++].toInt() and 0xFF).toLong()
        }
        if (len < 0L || pos + len > limit) return null
        return Asn1Header(firstTag, pos, (pos + len).toInt(), false)
    }

    private fun skipAsn1Element(data: ByteArray, offset: Int, limit: Int): Int? {
        val hdr = readAsn1Header(data, offset, limit) ?: return null
        if (!hdr.isIndefinite) return hdr.contentEnd
        var pos = hdr.contentStart
        while (pos + 1 < limit) {
            if (data[pos].toInt() == 0 && data[pos + 1].toInt() == 0) {
                return pos + 2
            }
            pos = skipAsn1Element(data, pos, limit) ?: return null
        }
        return null
    }

    private fun collectOctetStringBytes(data: ByteArray, offset: Int, limit: Int, out: ByteArrayOutputStream): Boolean {
        val hdr = readAsn1Header(data, offset, limit) ?: return false
        return when (hdr.tag) {
            0x04 -> {
                if (hdr.isIndefinite) return false
                out.write(data, hdr.contentStart, hdr.contentEnd - hdr.contentStart)
                true
            }
            0x24 -> {
                var pos = hdr.contentStart
                val end = hdr.contentEnd
                while (pos < end) {
                    if (hdr.isIndefinite && pos + 1 < end && data[pos].toInt() == 0 && data[pos + 1].toInt() == 0) {
                        return true
                    }
                    val child = readAsn1Header(data, pos, end) ?: return false
                    if (!collectOctetStringBytes(data, pos, end, out)) return false
                    pos = if (child.isIndefinite) (skipAsn1Element(data, pos, end) ?: return false) else child.contentEnd
                }
                true
            }
            else -> false
        }
    }

    private fun extractCmsOctetStringPayload(der: ByteArray): ByteArray? {
        val contentInfo = readAsn1Header(der, 0, der.size) ?: return null
        if (contentInfo.tag != 0x30) return null
        val afterOid = skipAsn1Element(der, contentInfo.contentStart, contentInfo.contentEnd) ?: return null
        val contentWrapper = readAsn1Header(der, afterOid, contentInfo.contentEnd) ?: return null
        if (contentWrapper.tag != 0xA0) return null

        val signedData = readAsn1Header(der, contentWrapper.contentStart, contentWrapper.contentEnd) ?: return null
        if (signedData.tag != 0x30) return null

        // SignedData: version (0x02), digestAlgorithms (0x31), encapContentInfo (0x30)
        var pos = skipAsn1Element(der, signedData.contentStart, signedData.contentEnd) ?: return null
        pos = skipAsn1Element(der, pos, signedData.contentEnd) ?: return null
        val encapContentInfo = readAsn1Header(der, pos, signedData.contentEnd) ?: return null
        if (encapContentInfo.tag != 0x30) return null

        val afterContentType = skipAsn1Element(der, encapContentInfo.contentStart, encapContentInfo.contentEnd) ?: return null
        if (afterContentType >= encapContentInfo.contentEnd) return null
        if (encapContentInfo.isIndefinite && afterContentType + 1 < encapContentInfo.contentEnd &&
            der[afterContentType].toInt() == 0 && der[afterContentType + 1].toInt() == 0
        ) {
            return null
        }
        val eContentWrapper = readAsn1Header(der, afterContentType, encapContentInfo.contentEnd) ?: return null
        if (eContentWrapper.tag != 0xA0) return null

        val out = ByteArrayOutputStream()
        return if (collectOctetStringBytes(der, eContentWrapper.contentStart, eContentWrapper.contentEnd, out)) {
            out.toByteArray()
        } else null
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
        } catch (e: CancellationException) {
            throw e
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

    private fun sanitizeDisplayName(name: String): String? {
        val base = File(name).name
        val safe = base.replace(Regex("""[^A-Za-z0-9._\-\s()]"""), "_").trim()
        return safe.takeIf { it.isNotBlank() && it != "." && it != ".." }?.take(120)
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx)?.let { sanitizeDisplayName(it) } else null
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
