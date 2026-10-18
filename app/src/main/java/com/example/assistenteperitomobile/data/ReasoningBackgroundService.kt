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
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.assistenteperitomobile.MainActivity

/**
 * Foreground Service dedicato al Reasoning On-Device:
 * Mantiene la CPU attiva (WakeLock) se l'utente esce dall'app o spegne lo schermo,
 * continuando l'elaborazione di Qwen 3.5 in background, e invia una notifica
 * heads-up interattiva con la risposta generata al completamento.
 */
class ReasoningBackgroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PeritoAI:ReasoningWakeLock"
        )?.apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_REASONING -> {
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                runCatching { nm.cancel(NOTIFICATION_ID_ONGOING) }
                return START_NOT_STICKY
            }
            ACTION_NOTIFY_COMPLETION -> {
                val title = intent.getStringExtra(EXTRA_REPLY_TITLE) ?: "✅ Risposta PeritoAI pronta"
                val text = intent.getStringExtra(EXTRA_REPLY_TEXT) ?: "Elaborazione completata con successo."
                postCompletionNotificationDirect(applicationContext, title, text)
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                runCatching { nm.cancel(NOTIFICATION_ID_ONGOING) }
                return START_NOT_STICKY
            }
            else -> {
                acquireWakeLock()
                val taskTitle = intent?.getStringExtra(EXTRA_TASK_TITLE) ?: "🧠 PeritoAI in elaborazione..."
                val preview = intent?.getStringExtra(EXTRA_TASK_PREVIEW) ?: "Analisi con Qwen 3.5 2B (Q4_K_M) in background..."

                val notification = buildOngoingNotification(taskTitle, preview)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID_ONGOING,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(NOTIFICATION_ID_ONGOING, notification)
                    }
                } catch (_: Throwable) {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    runCatching { nm.notify(NOTIFICATION_ID_ONGOING, notification) }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(120_000L) // 2 minuti per garantire completamento anche a schermo spento
            }
        } catch (_: Throwable) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Throwable) {
        }
    }

    private fun buildOngoingNotification(title: String, preview: String): Notification {
        ensureChannels(applicationContext)
        val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            applicationContext,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(applicationContext, CHANNEL_ONGOING_ID)
            .setContentTitle(title)
            .setContentText(preview)
            .setSubText("PeritoAI Studio Restelli")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(0, 0, true)
            .build()
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ONGOING_ID = "perito_reasoning_ongoing"
        const val CHANNEL_COMPLETE_ID = "perito_reasoning_complete"
        const val NOTIFICATION_ID_ONGOING = 5201
        const val NOTIFICATION_ID_COMPLETION = 5202

        const val ACTION_START_REASONING = "com.example.assistenteperitomobile.action.REASONING_START"
        const val ACTION_NOTIFY_COMPLETION = "com.example.assistenteperitomobile.action.REASONING_COMPLETE"
        const val ACTION_STOP_REASONING = "com.example.assistenteperitomobile.action.REASONING_STOP"

        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_PREVIEW = "extra_task_preview"
        const val EXTRA_REPLY_TITLE = "extra_reply_title"
        const val EXTRA_REPLY_TEXT = "extra_reply_text"

        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val ongoingChannel = NotificationChannel(
                    CHANNEL_ONGOING_ID,
                    "Elaborazione IA in Background",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Mostra lo stato di elaborazione attiva del modello di Reasoning in background"
                    setShowBadge(false)
                }
                val completeChannel = NotificationChannel(
                    CHANNEL_COMPLETE_ID,
                    "Risposte IA & Appuntamenti",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Invia notifiche heads-up con la risposta pronta quando il modello conclude l'analisi"
                    enableVibration(true)
                    setShowBadge(true)
                }
                nm.createNotificationChannel(ongoingChannel)
                nm.createNotificationChannel(completeChannel)
            }
        }

        fun startReasoning(context: Context, taskTitle: String, previewText: String = "") {
            ensureChannels(context)
            val intent = Intent(context, ReasoningBackgroundService::class.java).apply {
                action = ACTION_START_REASONING
                putExtra(EXTRA_TASK_TITLE, taskTitle)
                putExtra(EXTRA_TASK_PREVIEW, previewText.take(100))
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (MainActivity.isAppInForeground) {
                        context.startService(intent)
                    } else {
                        context.startForegroundService(intent)
                    }
                } else {
                    context.startService(intent)
                }
            } catch (_: Throwable) {
            }
        }

        fun postCompletionNotificationDirect(context: Context, title: String, fullText: String) {
            ensureChannels(context)
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                1,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snippet = fullText.take(160).replace('\n', ' ')
            val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETE_ID)
                .setContentTitle(title)
                .setContentText(snippet)
                .setSubText("PeritoAI Studio Restelli")
                .setStyle(NotificationCompat.BigTextStyle().bigText(fullText))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(openPending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            runCatching { nm.notify(NOTIFICATION_ID_COMPLETION, notification) }
        }

        fun notifyCompletion(context: Context, title: String, replyContent: String) {
            // Notifica direttamente tramite NotificationManager evitando crash da startForegroundService
            postCompletionNotificationDirect(context, title, replyContent)
            stopReasoning(context)
        }

        fun stopReasoning(context: Context) {
            val intent = Intent(context, ReasoningBackgroundService::class.java).apply {
                action = ACTION_STOP_REASONING
            }
            try {
                context.startService(intent)
            } catch (_: Throwable) {
            }
        }
    }
}
