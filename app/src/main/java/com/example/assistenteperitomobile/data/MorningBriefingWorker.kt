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
import com.example.assistenteperitomobile.R
import com.example.assistenteperitomobile.data.notifications.ModernNotificationBuilder
import com.example.assistenteperitomobile.data.notifications.NotificationBriefingFormatter
import com.example.assistenteperitomobile.domain.AiEngineConfig
import com.example.assistenteperitomobile.domain.CalendarEventItem
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

        fun parseBriefingTime(timeStr: String): Pair<Int, Int> {
            return try {
                val clean = timeStr.trim().replace(".", ":")
                val parts = clean.split(":")
                val h = parts[0].trim().toInt().coerceIn(0, 23)
                val m = if (parts.size > 1) parts[1].trim().toInt().coerceIn(0, 59) else 0
                Pair(h, m)
            } catch (_: Throwable) {
                Pair(8, 30)
            }
        }

        fun getBriefingConfig(context: Context): Pair<Boolean, String> {
            val file = File(context.filesDir, "ai_engine_config.json")
            if (!file.exists()) return Pair(true, "08:30")
            return try {
                val cfg = json.decodeFromString<AiEngineConfig>(file.readText())
                Pair(cfg.morningBriefingEnabled, cfg.morningBriefingTime.ifBlank { "08:30" })
            } catch (_: Throwable) {
                Pair(true, "08:30")
            }
        }

        fun isBriefingEnabled(context: Context): Boolean = getBriefingConfig(context).first

        fun getBriefingTime(context: Context): String = getBriefingConfig(context).second

        suspend fun sendBriefingNotification(context: Context): Result {
            val (enabled, timeStr) = getBriefingConfig(context)
            if (!enabled) {
                return Result.success()
            }

            val calRepo = CalendarRepository(context)
            val today = LocalDate.now()
            val todayEvents = calRepo.getEventsForDate(today)
            val tomorrowEvents = calRepo.getEventsForDate(today.plusDays(1))

            showModernBriefingNotification(
                context = context,
                today = today,
                todayEvents = todayEvents,
                tomorrowEvents = tomorrowEvents,
                timeStr = timeStr
            )
            return Result.success()
        }

        fun showModernBriefingNotification(
            context: Context,
            today: LocalDate = LocalDate.now(),
            todayEvents: List<CalendarEventItem> = emptyList(),
            tomorrowEvents: List<CalendarEventItem> = emptyList(),
            timeStr: String = "08:30"
        ) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Briefing Mattutino Peritale ($timeStr)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Riepilogo giornaliero alle $timeStr dei sopralluoghi con link Google Maps e fascicolo"
                enableVibration(true)
                setShowBadge(true)
            }
            nm.createNotificationChannel(channel)

            val briefingData = NotificationBriefingFormatter.computeBriefingData(
                today = today,
                todayEvents = todayEvents,
                tomorrowEvents = tomorrowEvents
            )

            try {
                val modernNotif = ModernNotificationBuilder.buildBriefingNotification(context, briefingData)
                nm.notify(NOTIFICATION_ID, modernNotif)
            } catch (_: Throwable) {
                // Fallback sicuro su notifica standard
                showNotification(context, briefingData.mainHeadline, briefingData.timelineText, timeStr)
            }
        }

        fun showNotification(context: Context, title: String, body: String, timeStr: String = "08:30") {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Briefing Mattutino Peritale ($timeStr)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Riepilogo giornaliero alle $timeStr dei sopralluoghi con link Google Maps e fascicolo"
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
                .setSmallIcon(R.drawable.ic_perito_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setColor(0xFF730B1E.toInt())
                .setColorized(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            nm.notify(NOTIFICATION_ID, notification)
        }

        fun scheduleDailyBriefing(context: Context) {
            val (enabled, timeStr) = getBriefingConfig(context)
            if (!enabled) {
                cancelDailyBriefing(context)
                return
            }

            val (hour, minute) = parseBriefingTime(timeStr)
            val now = LocalDateTime.now()
            var nextBriefing = now.withHour(hour).withMinute(minute).withSecond(0)
            if (!now.isBefore(nextBriefing)) {
                nextBriefing = nextBriefing.plusDays(1)
            }
            val initialDelayMinutes = Duration.between(now, nextBriefing).toMinutes().coerceAtLeast(1)

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
