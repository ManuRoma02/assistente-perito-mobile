package com.example.assistenteperitomobile.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.assistenteperitomobile.domain.ArchivedDocument
import com.example.assistenteperitomobile.domain.DocumentClassificationResult
import com.example.assistenteperitomobile.domain.PeritoAiEngine
import com.example.assistenteperitomobile.domain.PeritoCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale
import java.util.UUID

class ArchiveRepository(
    private val context: Context,
    private val aiEngine: PeritoAiEngine
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val mutex = Mutex()

    val archiveRootDir: File
        get() = File(context.filesDir, "Perizie_Archivio").also { root ->
            if (!root.exists()) root.mkdirs()
            PeritoCategory.entries.forEach { cat ->
                File(root, cat.folderName).apply { if (!exists()) mkdirs() }
            }
        }

    /**
     * Cartella pubblica visibile da Dropbox/Dropsync, OneDrive/Onesync, Syncthing e FolderSync:
     * /storage/emulated/0/Documents/Perizie_Archivio/<6_Cartelle>
     */
    val publicSyncRootDir: File
        get() = File(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
            "Perizie_Archivio"
        ).also { root ->
            runCatching {
                if (!root.exists()) root.mkdirs()
                PeritoCategory.entries.forEach { cat ->
                    File(root, cat.folderName).apply { if (!exists()) mkdirs() }
                }
            }
        }

    private val indexFile: File
        get() = File(archiveRootDir, "archive_index.json")

    @Volatile
    private var cachedDocuments: List<ArchivedDocument>? = null

    init {
        archiveRootDir // Inizializza le 6 cartelle interne all'avvio
        runCatching { publicSyncRootDir } // Prepara le 6 cartelle pubbliche per Sync PC (Dropbox/OneDrive/Syncthing)
    }

    suspend fun saveAndArchiveDocument(
        bytes: ByteArray,
        originalFileName: String,
        classification: DocumentClassificationResult,
        extractedSnippet: String = "",
        fascicoloId: String? = null
    ): ArchivedDocument = withContext(Dispatchers.IO) {
        val category = PeritoCategory.fromFolderName(classification.categoria)
        val categoryDir = File(archiveRootDir, category.folderName).apply { if (!exists()) mkdirs() }

        val normalizedBase = aiEngine.buildNormalizedFilename(classification, originalFileName)
        var targetFile = File(categoryDir, normalizedBase)
        var counter = 1
        while (targetFile.exists()) {
            val dotIdx = normalizedBase.lastIndexOf('.')
            val candidateName = if (dotIdx > 0) {
                "${normalizedBase.substring(0, dotIdx)}_$counter${normalizedBase.substring(dotIdx)}"
            } else {
                "${normalizedBase}_$counter"
            }
            targetFile = File(categoryDir, candidateName)
            counter++
        }

        targetFile.writeBytes(bytes)

        // Copia speculare automatica nella cartella pubblica Documents/Perizie_Archivio/<Categoria> per Sync PC (compatibile Scoped Storage Android 10-15)
        mirrorToPublicDocuments(category, targetFile.name, bytes)

        val record = ArchivedDocument(
            id = UUID.randomUUID().toString(),
            fileName = targetFile.name,
            originalName = originalFileName,
            categoryFolder = category.folderName,
            cliente = classification.cliente_o_controparte,
            oggetto = classification.oggetto_sintetico,
            dateIso = classification.data_documento,
            filePath = targetFile.absolutePath,
            sizeBytes = targetFile.length(),
            extractedTextSnippet = extractedSnippet.take(600),
            createdAtMillis = System.currentTimeMillis(),
            fascicoloId = fascicoloId
        )

        mutex.withLock {
            val current = loadAllDocuments(forceRefresh = true).toMutableList()
            current.add(0, record)
            saveIndex(current)
        }
        record
    }

    suspend fun moveDocumentCategory(
        documentId: String,
        newCategory: PeritoCategory
    ): ArchivedDocument? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val docs = loadAllDocuments(forceRefresh = true).toMutableList()
            val idx = docs.indexOfFirst { it.id == documentId }
            if (idx < 0) return@withContext null

            val existing = docs[idx]
            val oldFile = File(existing.filePath)
            val newDir = File(archiveRootDir, newCategory.folderName).apply { if (!exists()) mkdirs() }
            val newFile = File(newDir, existing.fileName)

            if (oldFile.exists() && oldFile.absolutePath != newFile.absolutePath) {
                oldFile.copyTo(newFile, overwrite = true)
                oldFile.delete()
            }

            // Aggiorna anche la cartella pubblica sincronizzata col PC (Scoped Storage)
            deleteFromPublicDocuments(existing.categoryFolder, existing.fileName)
            if (newFile.exists()) {
                mirrorToPublicDocuments(newCategory, existing.fileName, newFile.readBytes())
            }

            val updated = existing.copy(
                categoryFolder = newCategory.folderName,
                filePath = newFile.absolutePath
            )
            docs[idx] = updated
            saveIndex(docs)
            updated
        }
    }

    suspend fun deleteDocument(documentId: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val docs = loadAllDocuments(forceRefresh = true).toMutableList()
            val doc = docs.firstOrNull { it.id == documentId } ?: return@withContext false
            File(doc.filePath).delete()
            deleteFromPublicDocuments(doc.categoryFolder, doc.fileName)
            docs.removeAll { it.id == documentId }
            saveIndex(docs)
            true
        }
    }

    private fun mirrorToPublicDocuments(category: PeritoCategory, fileName: String, bytes: ByteArray) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val mimeType = when {
                    fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                    fileName.endsWith(".p7m", ignoreCase = true) -> "application/pkcs7-mime"
                    fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                    fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                    fileName.endsWith(".txt", ignoreCase = true) -> "text/plain"
                    else -> "application/octet-stream"
                }
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOCUMENTS + "/Perizie_Archivio/" + category.folderName
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, contentValues)
                if (itemUri != null) {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        out.write(bytes)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                }
            } else {
                val pubCatDir = File(publicSyncRootDir, category.folderName).apply { if (!exists()) mkdirs() }
                File(pubCatDir, fileName).writeBytes(bytes)
            }
        }
    }

    private fun deleteFromPublicDocuments(categoryFolder: String, fileName: String) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val relPath = Environment.DIRECTORY_DOCUMENTS + "/Perizie_Archivio/" + categoryFolder + "/"
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(fileName, "%$relPath%")
                resolver.delete(collection, selection, selectionArgs)
            } else {
                File(File(publicSyncRootDir, categoryFolder), fileName).delete()
            }
        }
    }

    suspend fun loadAllDocumentsResult(forceRefresh: Boolean = false): Result<List<ArchivedDocument>> = withContext(Dispatchers.IO) {
        val cached = cachedDocuments
        if (!forceRefresh && cached != null) {
            return@withContext Result.success(cached)
        }
        mutex.withLock {
            val lockedCached = cachedDocuments
            if (!forceRefresh && lockedCached != null) {
                return@withContext Result.success(lockedCached)
            }
            if (!indexFile.exists()) {
                cachedDocuments = emptyList()
                return@withContext Result.success(emptyList())
            }
            try {
                val list = json.decodeFromString<List<ArchivedDocument>>(indexFile.readText())
                val filtered = list.filter { File(it.filePath).exists() }
                    .sortedByDescending { it.createdAtMillis }
                cachedDocuments = filtered
                Result.success(filtered)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }
    }

    suspend fun loadAllDocuments(forceRefresh: Boolean = false): List<ArchivedDocument> {
        val result = loadAllDocumentsResult(forceRefresh)
        return result.getOrNull() ?: cachedDocuments ?: emptyList()
    }

    fun invalidateCache() {
        cachedDocuments = null
    }

    /**
     * Ricerca Fuzzy nell'archivio (per nome file, cliente, oggetto, categoria o testo OCR estratto).
     */
    suspend fun searchDocuments(query: String): List<ArchivedDocument> = withContext(Dispatchers.IO) {
        val all = loadAllDocuments()
        val cleaned = query.trim().lowercase(Locale.ITALIAN)
        if (cleaned.isEmpty()) return@withContext all

        val tokens = cleaned.split(Regex("""[\s_]+""")).filter { it.length >= 2 }
        if (tokens.isEmpty()) return@withContext all

        all.mapNotNull { doc ->
            val searchable = "${doc.fileName} ${doc.cliente} ${doc.oggetto} ${doc.category.displayTitle} ${doc.extractedTextSnippet}"
                .lowercase(Locale.ITALIAN)
            var score = 0
            for (token in tokens) {
                if (doc.cliente.lowercase(Locale.ITALIAN).contains(token)) score += 5
                if (doc.oggetto.lowercase(Locale.ITALIAN).contains(token)) score += 4
                if (doc.fileName.lowercase(Locale.ITALIAN).contains(token)) score += 3
                if (searchable.contains(token)) score += 2
            }
            if (score > 0) doc to score else null
        }.sortedByDescending { it.second }.map { it.first }
    }

    /**
     * Trova tutti i documenti nel fascicolo di uno specifico Cliente/Perizia per il Briefing Mattutino.
     */
    suspend fun findClientDossier(clientName: String, location: String = ""): List<ArchivedDocument> = withContext(Dispatchers.IO) {
        val rawTokens = "$clientName $location"
            .replace(Regex("""\b(Ing\.|Arch\.|Avv\.|Geom\.|Dott\.|Sig\.|Cliente|Da definire|Via|Piazza|Corso|Viale)\b""", RegexOption.IGNORE_CASE), " ")
            .lowercase(Locale.ITALIAN)
            .split(Regex("""[\s_,.-]+"""))
            .filter { it.length >= 3 }

        if (rawTokens.isEmpty()) return@withContext emptyList()

        loadAllDocuments().filter { doc ->
            val target = "${doc.cliente} ${doc.fileName} ${doc.extractedTextSnippet}".lowercase(Locale.ITALIAN)
            rawTokens.any { token -> target.contains(token) }
        }.take(5)
    }

    fun createShareOrOpenIntent(doc: ArchivedDocument, asShare: Boolean = false): Intent? {
        val file = File(doc.filePath)
        if (!file.exists()) return null
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mime = when {
            file.name.lowercase().endsWith(".pdf") -> "application/pdf"
            file.name.lowercase().let { it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") } -> "image/*"
            else -> "*/*"
        }
        return if (asShare) {
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${doc.cliente} - ${doc.oggetto}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    suspend fun getDocumentsForFascicolo(fascicoloId: String): List<ArchivedDocument> = withContext(Dispatchers.IO) {
        loadAllDocuments().filter { it.fascicoloId == fascicoloId }
    }

    private fun saveIndex(docs: List<ArchivedDocument>) {
        cachedDocuments = docs.filter { File(it.filePath).exists() }
            .sortedByDescending { it.createdAtMillis }
        val serialized = json.encodeToString(docs)
        val tmp = File(archiveRootDir, "archive_index.json.tmp")
        tmp.writeText(serialized, Charsets.UTF_8)
        if (tmp.exists()) {
            if (indexFile.exists()) indexFile.delete()
            tmp.renameTo(indexFile)
        }
    }
}
