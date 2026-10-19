package com.example.assistenteperitomobile.data.ota

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

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
         */
        fun evaluateUpdate(
            manifest: OtaUpdateManifest,
            currentVersionCode: Int,
            currentVersionName: String
        ): UpdateCheckResult {
            val available = isUpdateAvailable(manifest, currentVersionCode)
            return UpdateCheckResult(
                status = if (available) UpdateCheckStatus.UPDATE_AVAILABLE else UpdateCheckStatus.UP_TO_DATE,
                manifest = manifest,
                currentVersionCode = currentVersionCode,
                currentVersionName = currentVersionName
            )
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
                        currentUrl = newLocation
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
     */
    suspend fun checkForUpdate(
        manifestUrl: String = DEFAULT_MANIFEST_URL,
        explicitCurrentVersionCode: Int? = null,
        explicitCurrentVersionName: String? = null
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
        evaluateUpdate(manifest, currentCode, currentName)
    }

    /**
     * Scarica l'APK fornito nel downloadUrl nella cache dell'app notificando il progresso.
     */
    suspend fun downloadApk(
        downloadUrl: String,
        targetVersionCode: Int = 0,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply {
                if (!exists()) mkdirs()
            }
            val destinationFile = File(updatesDir, "perito_update_${targetVersionCode}_${System.currentTimeMillis()}.apk")
            val tempFile = File(updatesDir, "${destinationFile.name}.tmp")

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
                        currentUrl = newLocation
                        redirects++
                        continue
                    }
                }
                break
            }

            val conn = connection ?: error("Connessione non inizializzata")
            if (conn.responseCode !in 200..299) {
                error("Errore download APK: HTTP ${conn.responseCode}")
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
                tempFile.renameTo(destinationFile)
                destinationFile
            } else {
                error("Il file scaricato è vuoto o corrotto")
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
