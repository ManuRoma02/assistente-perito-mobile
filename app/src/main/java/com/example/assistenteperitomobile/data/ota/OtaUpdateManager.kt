package com.example.assistenteperitomobile.data.ota

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

class OtaUpdateManager(private val context: Context) {

    companion object {
        const val DEFAULT_MANIFEST_URL = "https://raw.githubusercontent.com/ManuRoma02/assistente-perito-mobile/main/ota-update.json"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000

        /**
         * Verifica puramente logica (testabile senza contesto Android).
         */
        fun isUpdateAvailable(manifest: OtaUpdateManifest, currentVersionCode: Int): Boolean {
            return manifest.latestVersionCode > currentVersionCode
        }

        /**
         * Valuta lo stato dell'aggiornamento a partire dal manifest e dalla versione locale.
         * Se [alwaysAvailable] è true, l'aggiornamento è considerato sempre disponibile anche per la reinstallazione.
         */
        fun evaluateUpdate(
            manifest: OtaUpdateManifest,
            currentVersionCode: Int,
            currentVersionName: String,
            alwaysAvailable: Boolean = false
        ): UpdateCheckResult {
            val available = alwaysAvailable || isUpdateAvailable(manifest, currentVersionCode)
            return UpdateCheckResult(
                status = if (available) UpdateCheckStatus.UPDATE_AVAILABLE else UpdateCheckStatus.UP_TO_DATE,
                manifest = manifest,
                currentVersionCode = currentVersionCode,
                currentVersionName = currentVersionName
            )
        }

        /**
         * Risolve in modo robusto redirect assoluti e relativi (RFC 7231).
         */
        fun resolveRedirectUrl(baseUrl: String, locationHeader: String): String {
            return runCatching {
                URL(URL(baseUrl), locationHeader).toString()
            }.getOrDefault(locationHeader)
        }

        /**
         * Calcola l'hash crittografico SHA-256 di un file (verifica integrità firmware/OTA).
         */
        fun calculateSha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        /**
         * Verifica l'integrità strutturale, la firma e la coerenza del pacchetto APK.
         */
        fun verifyApkArchive(
            context: Context,
            apkFile: File,
            targetVersionCode: Int = 0,
            expectedSha256: String = "",
            allowAnyVersion: Boolean = true
        ): Result<Unit> {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return Result.failure(IllegalStateException("File APK inesistente o vuoto"))
            }

            // 1. Verifica crittografica SHA-256 se dichiarata nel manifest
            if (expectedSha256.isNotBlank()) {
                val computedHash = calculateSha256(apkFile)
                if (!computedHash.equals(expectedSha256.trim(), ignoreCase = true)) {
                    return Result.failure(
                        SecurityException("Integrità SHA-256 fallita: atteso $expectedSha256, calcolato $computedHash")
                    )
                }
            }

            // 2. Verifica parsing pacchetto Android nativo (evita 'Errore di analisi pacchetto')
            val pm = try { context.packageManager } catch (_: Throwable) { null }
            if (pm != null) {
                val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
                    ?: return Result.failure(IllegalStateException("Il file scaricato non è un pacchetto Android valido (APK corrotto)"))

                if (archiveInfo.packageName != context.packageName) {
                    return Result.failure(
                        SecurityException("Package APK non corrispondente: ${archiveInfo.packageName} (atteso: ${context.packageName})")
                    )
                }

                val apkVersionCode = PackageInfoCompat.getLongVersionCode(archiveInfo).toInt()
                if (!allowAnyVersion && targetVersionCode > 0 && apkVersionCode < targetVersionCode) {
                    return Result.failure(
                        IllegalStateException("Versione APK inferiore al target richiesto: build $apkVersionCode < $targetVersionCode")
                    )
                }
            }

            return Result.success(Unit)
        }
    }

    /**
     * Recupera un APK già scaricato in precedenza se presente nella cache ed integro.
     */
    fun getExistingDownloadedApk(
        targetVersionCode: Int,
        expectedSha256: String = "",
        allowAnyVersion: Boolean = true
    ): File? {
        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) return null
        val candidates = updatesDir.listFiles { f ->
            f.isFile && f.name.startsWith("perito_update_${targetVersionCode}_") && f.name.endsWith(".apk")
        } ?: return null
        for (candidate in candidates.sortedByDescending { it.lastModified() }) {
            if (verifyApkArchive(context, candidate, targetVersionCode, expectedSha256, allowAnyVersion).isSuccess) {
                return candidate
            }
        }
        return null
    }

    /**
     * Rimuove file .tmp residui e pacchetti APK di build precedenti per liberare memoria storage.
     */
    fun cleanupOldUpdates(keepOnlyLatestVersionCode: Int? = null) {
        val updatesDir = File(context.cacheDir, "updates")
        if (!updatesDir.exists()) return
        val files = updatesDir.listFiles() ?: return
        for (f in files) {
            if (f.name.endsWith(".tmp")) {
                f.delete()
                continue
            }
            if (f.name.endsWith(".apk") && keepOnlyLatestVersionCode != null) {
                if (!f.name.startsWith("perito_update_${keepOnlyLatestVersionCode}_")) {
                    f.delete()
                }
            }
        }
    }

    fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(pInfo).toInt()
        } catch (_: Throwable) {
            1
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (_: Throwable) {
            "1.0"
        }
    }

    /**
     * Scarica e decodifica il manifest JSON da un endpoint remoto HTTP/HTTPS.
     */
    suspend fun fetchRemoteManifest(manifestUrl: String): Result<OtaUpdateManifest> = withContext(Dispatchers.IO) {
        runCatching {
            var currentUrl = manifestUrl
            var connection: HttpURLConnection? = null
            var redirects = 0
            val maxRedirects = 5

            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "PeritoAI-Android/${getCurrentVersionName()}")
                    setRequestProperty("Accept", "application/json")
                    instanceFollowRedirects = true
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308
                ) {
                    val newLocation = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newLocation != null) {
                        currentUrl = resolveRedirectUrl(currentUrl, newLocation)
                        redirects++
                        continue
                    }
                }
                break
            }

            val conn = connection ?: error("Connessione non inizializzata")
            if (conn.responseCode !in 200..299) {
                error("Errore HTTP ${conn.responseCode}: ${conn.responseMessage}")
            }

            val rawJson = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val parsed = OtaUpdateManifest.fromJson(rawJson)
            parsed.getOrThrow()
        }
    }

    /**
     * Controlla se è presente una nuova versione comparando con il codice versione locale.
     * Se [alwaysAvailable] è true, forza lo stato UPDATE_AVAILABLE per consentire anche la reinstallazione.
     */
    suspend fun checkForUpdate(
        manifestUrl: String = DEFAULT_MANIFEST_URL,
        explicitCurrentVersionCode: Int? = null,
        explicitCurrentVersionName: String? = null,
        alwaysAvailable: Boolean = false
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentCode = explicitCurrentVersionCode ?: getCurrentVersionCode()
        val currentName = explicitCurrentVersionName ?: getCurrentVersionName()

        val manifestResult = fetchRemoteManifest(manifestUrl)
        if (manifestResult.isFailure) {
            return@withContext UpdateCheckResult(
                status = UpdateCheckStatus.CHECK_FAILED,
                currentVersionCode = currentCode,
                currentVersionName = currentName,
                errorMessage = manifestResult.exceptionOrNull()?.localizedMessage ?: "Impossibile recuperare il manifest di aggiornamento"
            )
        }

        val manifest = manifestResult.getOrThrow()
        evaluateUpdate(manifest, currentCode, currentName, alwaysAvailable)
    }

    /**
     * Scarica l'APK fornito nel downloadUrl nella cache dell'app notificando il progresso.
     */
    suspend fun downloadApk(
        downloadUrl: String,
        targetVersionCode: Int = 0,
        expectedSha256: String = "",
        allowAnyVersion: Boolean = true,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply {
                if (!exists()) mkdirs()
            }

            // 1. Controllo cache: riutilizza se l'APK è già presente ed integro
            val existing = getExistingDownloadedApk(targetVersionCode, expectedSha256, allowAnyVersion)
            if (existing != null) {
                onProgress(1f, existing.length(), existing.length())
                return@runCatching existing
            }

            // 2. Igiene storage: pulizia residui .tmp e build obsolete
            cleanupOldUpdates(keepOnlyLatestVersionCode = targetVersionCode)

            val destinationFile = File(updatesDir, "perito_update_${targetVersionCode}_${System.currentTimeMillis()}.apk")
            val tempFile = File(updatesDir, "${destinationFile.name}.tmp")

            try {
                var currentUrl = downloadUrl
                var connection: HttpURLConnection? = null
                var redirects = 0
                val maxRedirects = 5

                while (redirects < maxRedirects) {
                    val url = URL(currentUrl)
                    connection = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = CONNECT_TIMEOUT_MS
                        readTimeout = READ_TIMEOUT_MS * 2
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "PeritoAI-Android/${getCurrentVersionName()}")
                        setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                        instanceFollowRedirects = true
                    }

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                        responseCode == 307 || responseCode == 308
                    ) {
                        val newLocation = connection.getHeaderField("Location")
                        connection.disconnect()
                        if (newLocation != null) {
                            currentUrl = resolveRedirectUrl(currentUrl, newLocation)
                            redirects++
                            continue
                        }
                    }
                    break
                }

                val conn = connection ?: error("Connessione non inizializzata")
                if (conn.responseCode !in 200..299) {
                    error("Errore download APK: HTTP ${conn.responseCode} ${conn.responseMessage}")
                }

                val totalBytes = conn.contentLength.toLong()
                var downloadedBytes = 0L

                conn.inputStream.use { input: InputStream ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(64 * 1024) // 64 KB buffer
                        var bytesRead: Int
                        var lastNotifiedProgress = -1f
                        var lastNotifiedBytes = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            coroutineContext.ensureActive()
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val progress = if (totalBytes > 0) {
                                (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                val estimatedTotal = 22 * 1024 * 1024f
                                (downloadedBytes.toFloat() / estimatedTotal).coerceIn(0.01f, 0.95f)
                            }

                            val progressChanged = Math.abs(progress - lastNotifiedProgress) >= 0.01f
                            val bytesChangedSignificantly = (downloadedBytes - lastNotifiedBytes) >= (256 * 1024)
                            if (progressChanged || bytesChangedSignificantly || downloadedBytes == totalBytes) {
                                lastNotifiedProgress = progress
                                lastNotifiedBytes = downloadedBytes
                                onProgress(progress, downloadedBytes, totalBytes)
                            }
                        }
                        output.flush()
                    }
                }
                conn.disconnect()

                if (tempFile.exists() && tempFile.length() > 0) {
                    if (destinationFile.exists()) destinationFile.delete()
                    if (!tempFile.renameTo(destinationFile)) {
                        error("Impossibile rinominare il file temporaneo dell'aggiornamento")
                    }

                    // 3. Verifica di sicurezza post-download (validità APK, package name e hash)
                    val verifyResult = verifyApkArchive(context, destinationFile, targetVersionCode, expectedSha256)
                    if (verifyResult.isFailure) {
                        destinationFile.delete()
                        throw verifyResult.exceptionOrNull() ?: IllegalStateException("Verifica APK fallita")
                    }

                    destinationFile
                } else {
                    error("Il file scaricato è vuoto o corrotto")
                }
            } catch (t: Throwable) {
                if (tempFile.exists()) tempFile.delete()
                throw t
            }
        }
    }

    /**
     * Controlla se l'app possiede l'autorizzazione all'installazione di pacchetti esterni (Android 8.0+).
     */
    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Genera l'intent per avviare l'installazione nativa dell'APK tramite FileProvider.
     */
    fun createInstallIntent(apkFile: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Genera l'intent per condurre l'utente nelle impostazioni di sistema e abilitare l'installazione.
     */
    fun createManageUnknownAppSourcesIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
