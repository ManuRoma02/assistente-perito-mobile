package com.example.assistenteperitomobile.data.notifications

import com.example.assistenteperitomobile.domain.CalendarEventItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class NotificationBriefingData(
    val hasEvents: Boolean,
    val totalTodayCount: Int,
    val dayNumber: String,
    val monthShort: String,
    val badgeCountText: String,
    val categoryPillText: String,
    val mainHeadline: String,
    val nextEventSubtitle: String,
    val locationText: String,
    val rawLocationQuery: String,
    val timelineText: String,
    val progressPercent: Int,
    val targetDossierId: String?,
    val tomorrowCount: Int
)

object NotificationBriefingFormatter {

    fun computeBriefingData(
        today: LocalDate,
        todayEvents: List<CalendarEventItem>,
        tomorrowEvents: List<CalendarEventItem>
    ): NotificationBriefingData {
        val dayNum = today.dayOfMonth.toString()
        val monthShort = today.format(DateTimeFormatter.ofPattern("MMM", Locale.ITALIAN))
            .uppercase(Locale.ITALIAN).take(3)

        if (todayEvents.isEmpty()) {
            val tomorrowText = if (tomorrowEvents.isNotEmpty()) {
                "Domani: ${tomorrowEvents.size} appuntament${if (tomorrowEvents.size == 1) "o" else "i"}"
            } else {
                "Tocca per aprire l'archivio perizie"
            }
            return NotificationBriefingData(
                hasEvents = false,
                totalTodayCount = 0,
                dayNumber = dayNum,
                monthShort = monthShort,
                badgeCountText = "LIBERO",
                categoryPillText = "☀️ GIORNATA LIBERA",
                mainHeadline = "Nessun Sopralluogo Oggi",
                nextEventSubtitle = tomorrowText,
                locationText = "📍 Studio Tecnico Peritale",
                rawLocationQuery = "",
                timelineText = if (tomorrowEvents.isNotEmpty()) "Nessun impegno previsto per oggi. $tomorrowText" else "Tutti i fascicoli sono aggiornati",
                progressPercent = 100,
                targetDossierId = null,
                tomorrowCount = tomorrowEvents.size
            )
        }

        val count = todayEvents.size
        val firstEvent = todayEvents.first()
        val timeStr = if (firstEvent.startIso.length >= 16) firstEvent.startIso.drop(11).take(5) else "09:00"
        val clientStr = firstEvent.cliente.trim().ifBlank { "Cliente" }
        val categoryRaw = firstEvent.categoria.ifBlank { firstEvent.titolo }.trim()
        val catUpper = categoryRaw.uppercase(Locale.ITALIAN)
        val emoji = when {
            catUpper.contains("INCENDIO") || catUpper.contains("FUOCO") -> "🔥"
            catUpper.contains("AUTO") || catUpper.contains("VEICOL") || catUpper.contains("MOTO") -> "🚗"
            catUpper.contains("APE") || catUpper.contains("ENERGET") -> "⚡"
            catUpper.contains("TRIBUNALE") || catUpper.contains("CTU") || catUpper.contains("UDIENZA") -> "🏛️"
            catUpper.contains("COMUNE") || catUpper.contains("URB") -> "🏢"
            catUpper.contains("ACQUA") || catUpper.contains("ALLAG") || catUpper.contains("CONDOTTA") -> "💧"
            catUpper.contains("FURTO") -> "🛡️"
            else -> "📌"
        }
        val categoryPill = "$emoji $catUpper"

        val headline = if (count == 1) "1 Sopralluogo Programmato" else "$count Sopralluoghi Programmati"
        val subtitle = "Prossimo: $timeStr • $clientStr"

        val rawPlace = firstEvent.luogo.trim()
        val isPlaceBlank = rawPlace.isBlank() || rawPlace.equals("da definire", ignoreCase = true)
        val locationText = if (isPlaceBlank) {
            "📍 Sede / Da definire"
        } else {
            val dossierInfo = if (!firstEvent.note.isNullOrBlank() && firstEvent.note.contains("#")) {
                " • " + firstEvent.note.substringAfter("#").substringBefore(" ").let { "#$it" }
            } else ""
            "📍 $rawPlace$dossierInfo"
        }
        val rawLocationQuery = if (isPlaceBlank) "" else rawPlace

        val timelineParts = todayEvents.take(4).map { ev ->
            val evTime = if (ev.startIso.length >= 16) ev.startIso.drop(11).take(5) else ""
            val evPlace = extractCityOrShortPlace(ev.luogo)
            if (evPlace.isNotBlank()) "$evTime $evPlace" else evTime
        }.filter { it.isNotBlank() }

        val timelineText = timelineParts.joinToString("  •  ")
        val progressPercent = (100 / count).coerceIn(15, 100)

        return NotificationBriefingData(
            hasEvents = true,
            totalTodayCount = count,
            dayNumber = dayNum,
            monthShort = monthShort,
            badgeCountText = "$count OGGI",
            categoryPillText = categoryPill,
            mainHeadline = headline,
            nextEventSubtitle = subtitle,
            locationText = locationText,
            rawLocationQuery = rawLocationQuery,
            timelineText = timelineText,
            progressPercent = progressPercent,
            targetDossierId = firstEvent.fascicoloId,
            tomorrowCount = tomorrowEvents.size
        )
    }

    private fun extractCityOrShortPlace(rawPlace: String): String {
        val clean = rawPlace.trim()
        if (clean.isBlank() || clean.equals("da definire", ignoreCase = true)) return ""
        val commaParts = clean.split(",")
        return if (commaParts.size > 1) {
            commaParts.last().trim()
        } else {
            clean.take(18)
        }
    }
}
