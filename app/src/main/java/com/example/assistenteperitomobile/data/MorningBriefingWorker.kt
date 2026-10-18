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
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class MorningBriefingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val calRepo = CalendarRepository(applicationContext)
        val todayEvents = calRepo.getEventsForDate(LocalDate.now())

        val title = if (todayEvents.isEmpty()) {
            "☀️ Briefing Giornaliero: Nessun sopralluogo peritale oggi"
        } else {
            "☀️ Briefing Giornaliero: ${todayEvents.size} appuntament${if (todayEvents.size == 1) "o" else "i"} oggi"
        }

        val body = if (todayEvents.isEmpty()) {
            "Giornata libera da sopralluoghi in agenda. Tocca per aprire l'archivio perizie."
        } else {
            todayEvents.take(3).joinToString("\n") { ev ->
                val timeStr = ev.startIso.drop(11).take(5)
                "• $timeStr ${ev.titolo} (${ev.luogo})"
            }
        }

        showNotification(title, body)
        return Result.success()
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "perito_morning_briefing"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Briefing Mattutino Peritale (08:00)",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Riepilogo giornaliero alle 08:00 dei sopralluoghi con link Google Maps e fascicolo"
        }
        nm.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(8001, notification)
    }

    companion object {
        private const val WORK_NAME = "perito_daily_briefing_0800"

        fun scheduleDailyBriefing(context: Context) {
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
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
