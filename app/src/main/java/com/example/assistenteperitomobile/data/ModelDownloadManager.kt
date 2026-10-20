package com.example.assistenteperitomobile.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.compose.runtime.Immutable
import androidx.core.app.NotificationCompat
import com.example.assistenteperitomobile.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.coroutineContext

@Immutable
data class DownloadableAiModel(
    val id: String,
    val roleBadge: String,
    val title: String,
    val subtitle: String,
    val sizeLabel: String,
    val expectedBytes: Long,
    val fileName: String,
    val downloadUrl: String,
    val isPrimaryReasoning: Boolean = false,
    val suggestedOllamaTag: String? = null
)

@Immutable
data class ModelDownloadProgress(
    val isDownloading: Boolean = false,
    val modelId: String? = null,
    val modelTitle: String = "",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val bytesPerSecond: Long = 0L,
    val currentQueueIndex: Int = 0,
    val totalQueueCount: Int = 0,
    val error: String? = null
) {
    val progressFraction: Float
        get() = if (totalBytes > 0L) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()

    val speedLabel: String
        get() {
            if (bytesPerSecond <= 0L) return ""
            val mbps = bytesPerSecond / (1024.0 * 1024.0)
            return String.format(Locale.ITALIAN, "%.1f MB/s", mbps)
        }

    val etaLabel: String
        get() {
            if (bytesPerSecond <= 0L || totalBytes <= downloadedBytes) return ""
            val remainingBytes = totalBytes - downloadedBytes
            val seconds = (remainingBytes / bytesPerSecond).coerceAtLeast(1L)
            return if (seconds < 60) {
                "~${seconds}s rimanenti"
            } else {
                val mins = seconds / 60
                val secs = seconds % 60
                "~${mins}m ${secs}s rimanenti"
            }
        }
}

/**
 * Foreground Service leggero per mantenere attivo il download dei modelli GGUF/ONNX
 * anche quando l'app è in background o lo schermo è spento, mostrando la notifica
 * con barra di avanzamento e pulsante "Interrompi".
 */
class ModelDownloadService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_DOWNLOAD -> {
                onCancelRequestedCallback?.invoke()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_STOP_SERVICE -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Download Modello IA in corso..."
                val sub = intent?.getStringExtra(EXTRA_SUBTITLE) ?: "Connessione a Hugging Face..."
                val percent = intent?.getIntExtra(EXTRA_PERCENT, 0) ?: 0
                val indeterminate = intent?.getBooleanExtra(EXTRA_INDETERMINATE, true) ?: true

                val notification = buildProgressNotification(
                    context = applicationContext,
                    title = title,
                    subtitle = sub,
                    percent = percent,
                    indeterminate = indeterminate
                )
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID_PROGRESS,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(NOTIFICATION_ID_PROGRESS, notification)
                    }
                } catch (_: Throwable) {
                    // Se il sistema blocca startForeground per restrizioni OEM, pubblica comunque la notifica standard
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    runCatching { nm.notify(NOTIFICATION_ID_PROGRESS, notification) }
                }
            }
        }
        return START_NOT_STICKY
    }

    companion object {
        const val CHANNEL_PROGRESS_ID = "perito_model_download_progress"
        const val CHANNEL_COMPLETE_ID = "perito_model_download_complete"
        const val NOTIFICATION_ID_PROGRESS = 4101
        const val NOTIFICATION_ID_COMPLETE = 4102

        const val ACTION_START_OR_UPDATE = "com.example.assistenteperitomobile.action.DOWNLOAD_UPDATE"
        const val ACTION_STOP_SERVICE = "com.example.assistenteperitomobile.action.DOWNLOAD_STOP"
        const val ACTION_CANCEL_DOWNLOAD = "com.example.assistenteperitomobile.action.DOWNLOAD_CANCEL"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_PERCENT = "extra_percent"
        const val EXTRA_INDETERMINATE = "extra_indeterminate"

        @Volatile
        var onCancelRequestedCallback: (() -> Unit)? = null

        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val progressChannel = NotificationChannel(
                    CHANNEL_PROGRESS_ID,
                    "Download Modelli IA (Avanzamento)",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Mostra la barra di avanzamento durante lo scaricamento dei modelli IA da Hugging Face"
                    setShowBadge(false)
                }
                val completeChannel = NotificationChannel(
                    CHANNEL_COMPLETE_ID,
                    "Download Modelli IA (Completamento)",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Avvisa quando i modelli IA sono stati scaricati o interrotti"
                }
                nm.createNotificationChannel(progressChannel)
                nm.createNotificationChannel(completeChannel)
            }
        }

        fun buildProgressNotification(
            context: Context,
            title: String,
            subtitle: String,
            percent: Int,
            indeterminate: Boolean
        ): Notification {
            ensureChannels(context)
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val cancelIntent = Intent(context, ModelDownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
            }
            val cancelPending = PendingIntent.getService(
                context,
                1,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            return NotificationCompat.Builder(context, CHANNEL_PROGRESS_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setSubText(if (indeterminate) "Avvio..." else "$percent%")
                .setProgress(100, percent.coerceIn(0, 100), indeterminate)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(openPending)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Pausa / Interrompi",
                    cancelPending
                )
                .build()
        }
    }
}

class ModelDownloadManager(private val context: Context) {

    /**
     * Cartella privata interna dell'app (/data/user/0/com.example.assistenteperitomobile/files/models).
     */
    val modelsDir: File
        get() = File(context.filesDir, "models").apply {
            if (!exists()) mkdirs()
        }

    /**
     * Modello Principale Unico per il REASONING:
     * Qwen 3.5 - 2B GPT-5.1 HighIQ Instruct in quantizzazione 4-bit Q4_K_M (1.25 GB).
     * Ultra-veloce, fluido, zero freeze di memoria, massima reattività logica on-device.
     */
    val primaryReasoningModel = DownloadableAiModel(
        id = "qwen35_2b_gpt51_highiq_q4km",
        roleBadge = "🧠 REASONING BASE (2B)",
        title = "Qwen 3.5 - 2B GPT-5.1 HighIQ (Q4_K_M)",
        subtitle = "Modello 2B ultra-veloce 4-bit Q4_K_M (1.25 GB): estrazione appuntamenti, comprensione contestuale e risposte peritali istantanee. Minimo consumo di memoria (mradermacher/Qwen3.5-2B-GPT-5.1-HighIQ-INSTRUCT-i1-GGUF)",
        sizeLabel = "1.25 GB",
        expectedBytes = 1_342_177_280L,
        fileName = "Qwen3.5-2B-GPT-5.1-HighIQ-INSTRUCT.i1-Q4_K_M.gguf",
        downloadUrl = "https://huggingface.co/mradermacher/Qwen3.5-2B-GPT-5.1-HighIQ-INSTRUCT-i1-GGUF/resolve/main/Qwen3.5-2B-GPT-5.1-HighIQ-INSTRUCT.i1-Q4_K_M.gguf",
        isPrimaryReasoning = true,
        suggestedOllamaTag = "qwen2.5:1.5b"
    )

    /**
     * Modello Potenziato per il REASONING:
     * Qwen 3.5 - 4B Instruct in quantizzazione 4-bit Q4_K_M (2.81 GB).
     * Modello a 4 miliardi di parametri per perizie complesse, deduzioni logiche forensi e stime articolate.
     * Versione suggerita per Ollama / GGUF: frob/qwen3.5-instruct:4b.
     */
    val powerfulReasoningModel = DownloadableAiModel(
        id = "qwen35_4b_instruct_q4km",
        roleBadge = "🚀 REASONING POTENZIATO (4B)",
        title = "Qwen 3.5 - 4B Instruct (Potenziato)",
        subtitle = "Versione 4B ad alta capacità logica per perizie complesse e stime articolate (2.81 GB). Suggerito sulla card: frob/qwen3.5-instruct:4b (Ollama / GGUF).",
        sizeLabel = "2.81 GB",
        expectedBytes = 3_013_027_808L,
        fileName = "Qwen_Qwen3.5-4B-Q4_K_M.gguf",
        downloadUrl = "https://huggingface.co/bartowski/Qwen_Qwen3.5-4B-GGUF/resolve/main/Qwen_Qwen3.5-4B-Q4_K_M.gguf",
        isPrimaryReasoning = true,
        suggestedOllamaTag = "frob/qwen3.5-instruct:4b"
    )

    val rizzoFlowModel = DownloadableAiModel(
        id = "rizzo_flow_17b_q4km",
        roleBadge = "⚡ DECISION MODEL (RIZZO FLOW)",
        title = "Rizzo Flow 1.7B (Q4_K_M GGUF)",
        subtitle = "Decision Model Jev-compatible (Rizzo AI Academy): classificazione istantanea nelle 6 Cartelle con 0 token generati (rizzoaiacademy/rizzo-flow-1.7b)",
        sizeLabel = "1.10 GB",
        expectedBytes = 1_181_116_000L,
        fileName = "spark-x2.5-1.7b-rizzo-flow-lora-q4_k_m.gguf",
        downloadUrl = "https://huggingface.co/rizzoaiacademy/rizzo-flow-1.7b/resolve/main/spark-x2.5-1.7b-rizzo-flow-lora-q4_k_m.gguf",
        isPrimaryReasoning = false
    )

    /**
     * Modelli di reasoning selezionabili dallo Studio Tecnico (Base 2B o Potenziato 4B).
     */
    val reasoningModels: List<DownloadableAiModel> = listOf(primaryReasoningModel, powerfulReasoningModel)

    fun getActiveReasoningModel(modelId: String): DownloadableAiModel {
        return reasoningModels.find { it.id == modelId } ?: primaryReasoningModel
    }

    /**
     * Modelli specialistici aggiuntivi opzionali scaricabili su richiesta.
     */
    val fixedTaskModels: List<DownloadableAiModel> = listOf(rizzoFlowModel)

    /**
     * Tutti i modelli gestiti dallo Studio Tecnico (Reasoning Base 2B + Reasoning Potenziato 4B + Decision Model 1.7B).
     */
    val availableModels: List<DownloadableAiModel> = listOf(
        primaryReasoningModel,
        powerfulReasoningModel,
        rizzoFlowModel
    )

    fun isModelDownloaded(model: DownloadableAiModel): Boolean {
        val f = File(modelsDir, model.fileName)
        return f.exists() && f.length() > 10_000_000L
    }

    fun isPrimaryReasoningDownloaded(customPath: String? = null, modelId: String? = null): Boolean {
        if (!modelId.isNullOrBlank()) {
            val m = reasoningModels.find { it.id == modelId }
            if (m != null && isModelDownloaded(m)) return true
        }
        if (reasoningModels.any { isModelDownloaded(it) }) return true
        if (!customPath.isNullOrBlank()) {
            val customFile = File(customPath)
            if (customFile.exists() && customFile.isFile && customFile.length() > 10_000_000L) {
                return true
            }
        }
        return false
    }

    fun areAllModelsDownloaded(customReasoningPath: String? = null): Boolean {
        return isPrimaryReasoningDownloaded(customReasoningPath) &&
            fixedTaskModels.all { isModelDownloaded(it) }
    }

    fun getDownloadedModelFile(model: DownloadableAiModel): File {
        return File(modelsDir, model.fileName)
    }

    fun listDownloadedModelFiles(): List<File> {
        val validExts = listOf(".gguf", ".onnx", ".task", ".litertlm", ".bin")
        return modelsDir.listFiles()?.filter { file ->
            file.isFile && validExts.any { ext -> file.name.lowercase().endsWith(ext) }
        } ?: emptyList()
    }

    fun deleteModelFile(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }

    /**
     * All'avvio o dopo un aggiornamento dell'app, rimuove automaticamente dalla cartella interna
     * `files/models` qualsiasi modello (o file .part) che non fa più parte della suite attuale
     * (es. il vecchio modello sostituito dal 2B Q4_K_M),
     * liberando immediatamente spazio sul dispositivo.
     *
     * @return Pair(lista nomi file rimossi, totale byte liberati)
     */
    fun purgeObsoleteModels(): Pair<List<String>, Long> {
        val allowedNames = availableModels
            .flatMap { listOf(it.fileName, "${it.fileName}.part") }
            .toSet()

        val removedNames = mutableListOf<String>()
        var freedBytes = 0L

        modelsDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name !in allowedNames) {
                val size = file.length()
                val name = file.name
                if (file.delete()) {
                    freedBytes += size
                    if (!name.endsWith(".part")) {
                        removedNames.add(name)
                    }
                }
            }
        }
        return removedNames to freedBytes
    }

    /**
     * Avvia il Foreground Service e mostra/aggiorna la notifica Android con barra di avanzamento.
     */
    fun notifyDownloadProgress(
        modelTitle: String,
        downloadedBytes: Long,
        totalBytes: Long,
        queueIndex: Int = 1,
        queueTotal: Int = 1,
        isFirstStart: Boolean = false,
        bytesPerSecond: Long = 0L
    ) {
        runCatching {
            ModelDownloadService.ensureChannels(context)
            val queueTag = if (queueTotal > 1) " ($queueIndex/$queueTotal)" else ""
            val title = "⬇️ Download$queueTag: $modelTitle"
            val dlMb = String.format(Locale.ITALIAN, "%.1f MB", downloadedBytes / (1024.0 * 1024.0))
            val totMb = if (totalBytes > 0L) {
                String.format(Locale.ITALIAN, "%.1f MB", totalBytes / (1024.0 * 1024.0))
            } else "..."
            val percent = if (totalBytes > 0L) {
                ((downloadedBytes.toDouble() / totalBytes.toDouble()) * 100.0).toInt().coerceIn(0, 100)
            } else 0
            val speedPart = if (bytesPerSecond > 0L) {
                val mbps = bytesPerSecond / (1024.0 * 1024.0)
                String.format(Locale.ITALIAN, " • %.1f MB/s", mbps)
            } else ""
            val etaPart = if (bytesPerSecond > 0L && totalBytes > downloadedBytes) {
                val remSec = ((totalBytes - downloadedBytes) / bytesPerSecond).coerceAtLeast(1L)
                if (remSec < 60) " • ~${remSec}s" else " • ~${remSec / 60}m ${remSec % 60}s"
            } else ""
            val subtitle = "$percent% • $dlMb / $totMb$speedPart$etaPart"
            val indeterminate = downloadedBytes <= 0L

            if (isFirstStart) {
                val serviceIntent = Intent(context, ModelDownloadService::class.java).apply {
                    action = ModelDownloadService.ACTION_START_OR_UPDATE
                    putExtra(ModelDownloadService.EXTRA_TITLE, title)
                    putExtra(ModelDownloadService.EXTRA_SUBTITLE, subtitle)
                    putExtra(ModelDownloadService.EXTRA_PERCENT, percent)
                    putExtra(ModelDownloadService.EXTRA_INDETERMINATE, indeterminate)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } else {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notif = ModelDownloadService.buildProgressNotification(
                    context = context,
                    title = title,
                    subtitle = subtitle,
                    percent = percent,
                    indeterminate = indeterminate
                )
                nm.notify(ModelDownloadService.NOTIFICATION_ID_PROGRESS, notif)
            }
        }
    }

    /**
     * Ferma il Foreground Service di download e mostra una notifica finale di completamento o pausa.
     */
    fun notifyDownloadFinished(title: String, message: String, isSuccess: Boolean) {
        runCatching {
            ModelDownloadService.ensureChannels(context)
            val stopIntent = Intent(context, ModelDownloadService::class.java).apply {
                action = ModelDownloadService.ACTION_STOP_SERVICE
            }
            context.startService(stopIntent)

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(ModelDownloadService.NOTIFICATION_ID_PROGRESS)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val doneNotif = NotificationCompat.Builder(context, ModelDownloadService.CHANNEL_COMPLETE_ID)
                .setSmallIcon(
                    if (isSuccess) android.R.drawable.stat_sys_download_done
                    else android.R.drawable.stat_notify_sync_noanim
                )
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(openPending)
                .build()

            nm.notify(ModelDownloadService.NOTIFICATION_ID_COMPLETE, doneNotif)
        }
    }

    suspend fun downloadModel(
        model: DownloadableAiModel,
        onProgress: (downloadedBytes: Long, totalBytes: Long, bytesPerSecond: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val targetFile = File(modelsDir, model.fileName)
        val tempFile = File(modelsDir, "${model.fileName}.part")

        // Supporta il resume automatico se un download precedente si era interrotto
        val existingPartBytes = if (tempFile.exists()) tempFile.length() else 0L

        var currentUrl = model.downloadUrl
        var redirects = 0
        var connection: HttpURLConnection? = null

        try {
            while (redirects < 8) {
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 20_000
                    readTimeout = 45_000
                    setRequestProperty("User-Agent", "PeritoAI-Android/2.8")
                    if (existingPartBytes > 0L) {
                        setRequestProperty("Range", "bytes=$existingPartBytes-")
                    }
                }
                val code = conn.responseCode
                if (code in 301..308) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (loc.isNullOrBlank()) break
                    currentUrl = if (loc.startsWith("http")) loc else URL(url, loc).toString()
                    redirects++
                } else {
                    connection = conn
                    break
                }
            }

            var activeConn = connection ?: throw IllegalStateException("Impossibile connettersi a Hugging Face")
            var responseCode = activeConn.responseCode

            // Se il server risponde con HTTP 416 (Range Not Satisfiable), il file .part è corrotto o completato
            if (responseCode == 416 && existingPartBytes > 0L) {
                activeConn.disconnect()
                tempFile.delete()
                val freshConn = (URL(model.downloadUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 20_000
                    readTimeout = 45_000
                    setRequestProperty("User-Agent", "PeritoAI-Android/2.8")
                }
                activeConn = freshConn
                responseCode = activeConn.responseCode
            }

            val isResume = responseCode == HttpURLConnection.HTTP_PARTIAL && existingPartBytes > 0L && tempFile.exists()

            if (responseCode != HttpURLConnection.HTTP_OK && !isResume) {
                throw IllegalStateException("Errore HTTP $responseCode durante il download da Hugging Face")
            }

            val contentLength = activeConn.contentLengthLong.coerceAtLeast(0L)
            val totalBytes = if (isResume) {
                existingPartBytes + contentLength
            } else {
                if (contentLength > 0L) contentLength else model.expectedBytes
            }
            var downloaded = if (isResume) existingPartBytes else 0L
            onProgress(downloaded, totalBytes, 0L)

            activeConn.inputStream.use { input ->
                FileOutputStream(tempFile, isResume).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    var lastReportTimeMs = SystemClock.elapsedRealtime()
                    var lastReportBytes = downloaded
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val nowMs = SystemClock.elapsedRealtime()
                        val deltaMs = nowMs - lastReportTimeMs
                        // Throttle a 350ms per evitare ricomposizioni eccessive della UI durante lo scroll
                        if (deltaMs >= 350L || downloaded >= totalBytes) {
                            val speed = if (deltaMs > 0) ((downloaded - lastReportBytes) * 1000L) / deltaMs else 0L
                            lastReportTimeMs = nowMs
                            lastReportBytes = downloaded
                            onProgress(downloaded, totalBytes, speed)
                        }
                    }
                    output.flush()
                }
            }

            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)
            targetFile
        } catch (e: CancellationException) {
            // Mantiene il file .part così l'utente può riprendere il download dal punto in cui era rimasto!
            throw e
        } catch (e: Throwable) {
            throw e
        } finally {
            connection?.disconnect()
        }
    }
}
