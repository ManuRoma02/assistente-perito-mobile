package com.example.assistenteperitomobile.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.assistenteperitomobile.MainActivity
import com.example.assistenteperitomobile.domain.AiEngineConfig
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class MorningBriefingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return sendBriefingNotification(applicationContext)
    }

    companion object {
        const val WORK_NAME = "perito_daily_briefing_0800"
        const val CHANNEL_ID = "perito_morning_briefing"
        const val NOTIFICATION_ID = 8001

        private val json = Json { ignoreUnknownKeys = true }

        fun isBriefingEnabled(context: Context): Boolean {
            val file = File(context.filesDir, "ai_engine_config.json")
            if (!file.exists()) return true
            return try {
                val cfg = json.decodeFromString<AiEngineConfig>(file.readText())
                cfg.morningBriefingEnabled
            } catch (_: Throwable) {
                true
            }
        }

        suspend fun sendBriefingNotification(context: Context): Result {
            if (!isBriefingEnabled(context)) {
                return Result.success()
            }

            val calRepo = CalendarRepository(context)
            val today = LocalDate.now()
            val todayEvents = calRepo.getEventsForDate(today)
            val tomorrowEvents = calRepo.getEventsForDate(today.plusDays(1))

            val title = if (todayEvents.isEmpty()) {
                "☀️ Briefing Mattutino: Nessun sopralluogo per oggi"
            } else {
                "☀️ Briefing Mattutino: ${todayEvents.size} appuntament${if (todayEvents.size == 1) "o" else "i"} oggi"
            }

            val body = buildString {
                if (todayEvents.isEmpty()) {
                    append("Giornata libera da sopralluoghi oggi. ")
                    if (tomorrowEvents.isNotEmpty()) {
                        append("Domani previsti ${tomorrowEvents.size} appuntamenti.")
                    } else {
                        append("Tocca per aprire l'archivio perizie.")
                    }
                } else {
                    val summaryList = todayEvents.take(4).map { ev ->
                        val timeStr = ev.startIso.drop(11).take(5)
                        "• $timeStr ${ev.titolo} (${ev.luogo})"
                    }
                    append(summaryList.joinToString("\n"))
                    if (tomorrowEvents.isNotEmpty()) {
                        append("\n📅 Domani: ${tomorrowEvents.size} appuntament${if (tomorrowEvents.size == 1) "o" else "i"}")
                    }
                }
            }

            showNotification(context, title, body)
            return Result.success()
        }

        fun showNotification(context: Context, title: String, body: String) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Briefing Mattutino Peritale (08:00)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Riepilogo giornaliero alle 08:00 dei sopralluoghi con link Google Maps e fascicolo"
                enableVibration(true)
                setShowBadge(true)
            }
            nm.createNotificationChannel(channel)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("NAVIGATE_TO_TAB", "AGENDA")
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            nm.notify(NOTIFICATION_ID, notification)
        }

        fun scheduleDailyBriefing(context: Context) {
            if (!isBriefingEnabled(context)) {
                cancelDailyBriefing(context)
                return
            }

            val now = LocalDateTime.now()
            var next8am = now.withHour(8).withMinute(0).withSecond(0)
            if (!now.isBefore(next8am)) {
                next8am = next8am.plusDays(1)
            }
            val initialDelayMinutes = Duration.between(now, next8am).toMinutes().coerceAtLeast(1)

            val request = PeriodicWorkRequestBuilder<MorningBriefingWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancelDailyBriefing(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        suspend fun triggerImmediateBriefing(context: Context) {
            sendBriefingNotification(context)
        }
    }
}
