package com.example.assistenteperitomobile.data.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.assistenteperitomobile.MainActivity
import com.example.assistenteperitomobile.R
import com.example.assistenteperitomobile.data.MorningBriefingWorker

object ModernNotificationBuilder {

    fun buildBriefingNotification(
        context: Context,
        data: NotificationBriefingData
    ): Notification {
        val coverBitmap = generateCoverBitmap(
            dayNumber = data.dayNumber,
            monthShort = data.monthShort,
            badgeText = data.badgeCountText
        )

        // PendingIntents
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO_TAB", "AGENDA")
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            2001,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val agendaIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO_TAB", "AGENDA")
        }
        val agendaPendingIntent = PendingIntent.getActivity(
            context,
            2002,
            agendaIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dossierIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO_TAB", "FASCICOLI")
            data.targetDossierId?.let { putExtra("FASCICOLO_ID", it) }
        }
        val dossierPendingIntent = PendingIntent.getActivity(
            context,
            2003,
            dossierIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mapsPendingIntent = if (data.rawLocationQuery.isNotBlank()) {
            val mapsIntent = Intent(context, BriefingActionReceiver::class.java).apply {
                action = BriefingActionReceiver.ACTION_NAVIGATE_MAPS
                putExtra(BriefingActionReceiver.EXTRA_LOCATION_QUERY, data.rawLocationQuery)
            }
            PendingIntent.getBroadcast(
                context,
                2004,
                mapsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            // Se nessun luogo, reindirizza su Agenda
            agendaPendingIntent
        }

        val snoozeIntent = Intent(context, BriefingActionReceiver::class.java).apply {
            action = BriefingActionReceiver.ACTION_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            2005,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Collapsed RemoteViews (Stile Spotify compatto: cover, brano/titolo, artista/luogo, mini progress bar)
        val collapsedViews = RemoteViews(context.packageName, R.layout.view_notification_briefing_collapsed).apply {
            setImageViewBitmap(R.id.notif_collapsed_cover, coverBitmap)
            setTextViewText(R.id.notif_collapsed_title, data.mainHeadline)
            setTextViewText(R.id.notif_collapsed_subtitle, data.nextEventSubtitle)
            setProgressBar(R.id.notif_collapsed_progress, 100, data.progressPercent, false)
        }

        // Expanded RemoteViews (Stile Spotify esteso: cover, category, titolo, dettagli, timeline scrubber)
        val expandedViews = RemoteViews(context.packageName, R.layout.view_notification_briefing_expanded).apply {
            setImageViewBitmap(R.id.notif_expanded_cover, coverBitmap)
            setTextViewText(R.id.notif_expanded_category, data.categoryPillText)
            setTextViewText(R.id.notif_expanded_title, data.mainHeadline)
            setTextViewText(R.id.notif_expanded_subtitle, data.nextEventSubtitle)
            setTextViewText(R.id.notif_expanded_location, data.locationText)
            setTextViewText(R.id.notif_expanded_timeline_text, data.timelineText)
            setProgressBar(R.id.notif_expanded_progress, 100, data.progressPercent, false)
        }

        return NotificationCompat.Builder(context, MorningBriefingWorker.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_perito_notification)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setContentIntent(mainPendingIntent)
            .setColor(0xFF4F0B18.toInt())
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
    }

    /**
     * Genera dinamicamente la copertina squircle ad alto contrasto per la data.
     */
    fun generateCoverBitmap(
        dayNumber: String,
        monthShort: String,
        badgeText: String
    ): Bitmap {
        val size = 240
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Sfondo con gradiente DavezBordeaux
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                Color.parseColor("#6A1628"),
                Color.parseColor("#380812"),
                Shader.TileMode.CLAMP
            )
        }
        val bgRect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(bgRect, 38f, 38f, bgPaint)

        // 2. Bordo di rifinitura luxury (stroke bordeaux semitrasparente)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#66FF7D90")
        }
        canvas.drawRoundRect(bgRect, 38f, 38f, strokePaint)

        // 3. Giorno del mese (es. "29")
        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 94f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(dayNumber, size / 2f, 105f, dayPaint)

        // 4. Mese abbreviato (es. "OTT")
        val monthPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFFB3C0")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.12f
        }
        canvas.drawText(monthShort.uppercase(), size / 2f, 150f, monthPaint)

        // 5. Pill badge di conteggio in basso (es. "3 OGGI" o "LIBERO")
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 32, 4, 10)
        }
        val pillRect = RectF(34f, 182f, size - 34f, 222f)
        canvas.drawRoundRect(pillRect, 14f, 14f, pillBgPaint)

        val pillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.argb(110, 255, 125, 144)
        }
        canvas.drawRoundRect(pillRect, 14f, 14f, pillStrokePaint)

        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (badgeText == "LIBERO") Color.parseColor("#60A5FA") else Color.parseColor("#4ADE80")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        canvas.drawText(badgeText, size / 2f, 210f, badgePaint)

        return bitmap
    }
}
