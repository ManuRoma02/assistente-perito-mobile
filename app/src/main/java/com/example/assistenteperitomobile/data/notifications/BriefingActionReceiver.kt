package com.example.assistenteperitomobile.data.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.assistenteperitomobile.data.MorningBriefingWorker
import java.util.concurrent.TimeUnit

class BriefingActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_NAVIGATE_MAPS = "com.example.assistenteperitomobile.ACTION_NAVIGATE_MAPS"
        const val ACTION_SNOOZE = "com.example.assistenteperitomobile.ACTION_SNOOZE"
        const val EXTRA_LOCATION_QUERY = "extra_location_query"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            ACTION_NAVIGATE_MAPS -> {
                val location = intent.getStringExtra(EXTRA_LOCATION_QUERY)?.trim()
                if (!location.isNullOrBlank()) {
                    val geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(location))
                    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(mapIntent)
                    } catch (_: Throwable) {
                        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(location))
                        val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(webIntent)
                        } catch (_: Throwable) {
                            Toast.makeText(context, "Impossibile aprire l'app Mappe", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            ACTION_SNOOZE -> {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(MorningBriefingWorker.NOTIFICATION_ID)

                val snoozeWork = OneTimeWorkRequestBuilder<MorningBriefingWorker>()
                    .setInitialDelay(15, TimeUnit.MINUTES)
                    .addTag("briefing_snooze")
                    .build()
                WorkManager.getInstance(context).enqueue(snoozeWork)

                Toast.makeText(context, "⏰ Briefing posticipato di 15 minuti", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
