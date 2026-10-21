package com.example.assistenteperitomobile.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.assistenteperitomobile.domain.AppointmentParseResult
import com.example.assistenteperitomobile.domain.CalendarEventItem
import com.example.assistenteperitomobile.domain.CategoryReferenceFormat
import com.example.assistenteperitomobile.domain.PeritoEventCategory
import com.example.assistenteperitomobile.domain.SystemCalendarInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import java.util.UUID

class CalendarRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val mutex = Mutex()

    private val eventsFile: File
        get() = File(context.filesDir, "calendar_events.json")

    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    var isSyncEnabled: Boolean = true
    var selectedCalendarId: Long? = null

    fun hasNativeCalendarPermissions(): Boolean {
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
        return read && write
    }

    fun hasCalendarPermissions(): Boolean {
        return isSyncEnabled && hasNativeCalendarPermissions()
    }

    /**
     * Controlla conflitti bidirezionali (sia nel database locale dell'app sia nel Google/Android Calendar nativo del telefono)
     * con un buffer di sicurezza di ±15 minuti.
     */
    suspend fun checkConflicts(
        proposed: AppointmentParseResult,
        bufferMinutes: Long = 15L
    ): List<CalendarEventItem> = withContext(Dispatchers.IO) {
        val startIso = proposed.data_inizio_iso ?: return@withContext emptyList()
        val endIso = proposed.data_fine_iso ?: return@withContext emptyList()

        val zone = ZoneId.systemDefault()
        val startMillis = runCatching {
            LocalDateTime.parse(startIso.take(19)).atZone(zone).toInstant().toEpochMilli()
        }.getOrNull() ?: return@withContext emptyList()
        val endMillis = runCatching {
            LocalDateTime.parse(endIso.take(19)).atZone(zone).toInstant().toEpochMilli()
        }.getOrNull() ?: (startMillis + 3_600_000L)

        val windowMin = startMillis - bufferMinutes * 60_000L
        val windowMax = endMillis + bufferMinutes * 60_000L

        val allEvents = getEventsInRange(windowMin - 3_600_000L, windowMax + 3_600_000L)
        allEvents.filter { ev ->
            ev.startMillis < windowMax && ev.endMillis > windowMin
        }
    }

    /**
     * Inserisce l'appuntamento con i 3 promemoria automatici (24 ore, 60 min, 30 min prima)
     * sia nel Calendario di sistema Android/Google sia nel registro locale dell'app.
     */
    suspend fun createEvent(
        proposed: AppointmentParseResult,
        format: CategoryReferenceFormat? = null
    ): CalendarEventItem = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val startDt = proposed.data_inizio_iso?.let {
            runCatching { LocalDateTime.parse(it.take(19)) }.getOrNull()
        } ?: LocalDateTime.now().plusHours(1)
        val endDt = proposed.data_fine_iso?.let {
            runCatching { LocalDateTime.parse(it.take(19)) }.getOrNull()
        } ?: startDt.plusHours(1)

        val startMillis = startDt.atZone(zone).toInstant().toEpochMilli()
        val endMillis = endDt.atZone(zone).toInstant().toEpochMilli()
        val reminders = listOf(30, 60, 1440)

        val resolvedSubject = if (proposed.cliente.isNotBlank() && proposed.cliente != "Cliente") {
            proposed.cliente
        } else if (proposed.categoria == PeritoEventCategory.UNIONCASA.displayName) {
            "Unioncasa"
        } else if (proposed.categoria == PeritoEventCategory.AGENZIA_ORSO.displayName) {
            "Agenzia Orso"
        } else if (proposed.luogo.isNotBlank() && proposed.luogo != "Da definire") {
            proposed.luogo
        } else {
            "Studio"
        }

        val activityType = if (proposed.titolo.isNotBlank() && proposed.titolo.contains(" - ")) {
            proposed.titolo.substringBefore(" - ").trim()
        } else if (proposed.categoria.isNotBlank()) {
            proposed.categoria
        } else {
            "Sopralluogo"
        }

        val effectiveTitle = if (format != null) {
            format.formatTitle(
                startTimeIso = startDt.format(isoFormatter),
                categoryType = activityType,
                subject = resolvedSubject,
                place = proposed.luogo,
                notes = proposed.note
            )
        } else if (proposed.titolo.isNotBlank() && (proposed.titolo.contains(" - ") || proposed.titolo.contains(" / "))) {
            proposed.titolo
        } else if (proposed.titolo.isNotBlank() && proposed.titolo != proposed.categoria) {
            "${proposed.titolo} - $resolvedSubject"
        } else {
            "${proposed.categoria.ifBlank { "Sopralluogo" }} - $resolvedSubject"
        }

        var sysEventId: Long? = null
        if (hasCalendarPermissions()) {
            sysEventId = insertIntoSystemCalendar(
                title = effectiveTitle,
                description = proposed.note.trim(),
                location = proposed.luogo,
                startMillis = startMillis,
                endMillis = endMillis,
                remindersMinutes = reminders
            )
        }

        val item = CalendarEventItem(
            id = UUID.randomUUID().toString(),
            systemCalendarEventId = sysEventId,
            titolo = effectiveTitle,
            startIso = startDt.format(isoFormatter),
            endIso = endDt.format(isoFormatter),
            startMillis = startMillis,
            endMillis = endMillis,
            luogo = proposed.luogo,
            cliente = if (proposed.cliente == "Cliente" && resolvedSubject != "Cliente") resolvedSubject else proposed.cliente,
            note = proposed.note.trim(),
            remindersMinutes = reminders,
            isFromSystemCalendar = false,
            fascicoloId = proposed.fascicoloId,
            categoria = proposed.categoria.ifBlank { "Sopralluogo" }
        )

        mutex.withLock {
            val current = loadLocalEvents().toMutableList()
            current.add(item)
            saveLocalEvents(current.sortedBy { it.startMillis })
        }
        item
    }

    suspend fun replaceConflictsAndCreate(
        conflicts: List<CalendarEventItem>,
        proposed: AppointmentParseResult,
        format: CategoryReferenceFormat? = null
    ): CalendarEventItem = withContext(Dispatchers.IO) {
        for (c in conflicts) {
            deleteEvent(c)
        }
        createEvent(proposed, format)
    }

    suspend fun deleteEvent(event: CalendarEventItem) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val local = loadLocalEvents().toMutableList()
            local.removeAll { it.id == event.id || (event.systemCalendarEventId != null && it.systemCalendarEventId == event.systemCalendarEventId) }
            saveLocalEvents(local)
        }

        if (hasCalendarPermissions() && event.systemCalendarEventId != null) {
            try {
                val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.systemCalendarEventId)
                context.contentResolver.delete(deleteUri, null, null)
            } catch (_: Throwable) {
            }
        }
    }

    suspend fun updateEvent(
        event: CalendarEventItem,
        newTitle: String,
        newCategory: String,
        newStartIso: String,
        newEndIso: String,
        newLocation: String = event.luogo,
        newNotes: String = ""
    ): CalendarEventItem = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val startDt = runCatching { LocalDateTime.parse(newStartIso.take(19)) }.getOrNull()
            ?: LocalDateTime.now()
        val endDt = runCatching { LocalDateTime.parse(newEndIso.take(19)) }.getOrNull()
            ?: startDt.plusHours(1)
        val startMillis = startDt.atZone(zone).toInstant().toEpochMilli()
        val endMillis = endDt.atZone(zone).toInstant().toEpochMilli()

        if (hasCalendarPermissions() && event.systemCalendarEventId != null) {
            try {
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.systemCalendarEventId)
                val values = ContentValues().apply {
                    put(CalendarContract.Events.TITLE, newTitle)
                    put(CalendarContract.Events.DESCRIPTION, newNotes.trim())
                    put(CalendarContract.Events.EVENT_LOCATION, newLocation)
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                }
                context.contentResolver.update(uri, values, null, null)
            } catch (_: Throwable) {
            }
        }

        val updated = event.copy(
            titolo = newTitle,
            categoria = newCategory,
            startIso = startDt.format(isoFormatter),
            endIso = endDt.format(isoFormatter),
            startMillis = startMillis,
            endMillis = endMillis,
            luogo = newLocation,
            note = newNotes.trim()
        )

        mutex.withLock {
            val local = loadLocalEvents().toMutableList()
            val idx = local.indexOfFirst { it.id == event.id || (event.systemCalendarEventId != null && it.systemCalendarEventId == event.systemCalendarEventId) }
            if (idx >= 0) {
                local[idx] = updated
            } else {
                local.add(updated)
            }
            saveLocalEvents(local.sortedBy { it.startMillis })
        }
        updated
    }

    /**
     * Restituisce tutti gli appuntamenti (locali + Google Calendar del telefono) in un intervallo temporale.
     */
    suspend fun getEventsInRange(minMillis: Long, maxMillis: Long): List<CalendarEventItem> =
        withContext(Dispatchers.IO) {
            val localEvents = loadLocalEvents().filter { it.endMillis >= minMillis && it.startMillis <= maxMillis }
            val systemEvents = if (hasCalendarPermissions()) {
                querySystemCalendarEvents(minMillis, maxMillis)
            } else {
                emptyList()
            }

            val merged = localEvents.toMutableList()
            for (sysEv in systemEvents) {
                val alreadyTracked = merged.any { loc ->
                    (loc.systemCalendarEventId != null && loc.systemCalendarEventId == sysEv.systemCalendarEventId) ||
                        (kotlin.math.abs(loc.startMillis - sysEv.startMillis) < 60_000L &&
                            loc.titolo.equals(sysEv.titolo, ignoreCase = true))
                }
                if (!alreadyTracked) {
                    merged.add(sysEv)
                }
            }
            merged.sortedBy { it.startMillis }
        }

    suspend fun getEventsForDate(date: LocalDate): List<CalendarEventItem> {
        val zone = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return getEventsInRange(startOfDay, endOfDay)
    }

    fun loadLocalEvents(): List<CalendarEventItem> {
        if (!eventsFile.exists()) return emptyList()
        return try {
            json.decodeFromString<List<CalendarEventItem>>(eventsFile.readText())
                .sortedBy { it.startMillis }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun launchGoogleMaps(context: Context, location: String) {
        if (location.isBlank()) return
        val encoded = Uri.encode(location)
        val gmmIntentUri = Uri.parse("geo:0,0?q=$encoded")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(mapIntent)
        } catch (_: Throwable) {
            try {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (_: Throwable) {
            }
        }
    }

    fun createGoogleMapsIntent(location: String): Intent {
        val encoded = Uri.encode(location)
        val gmmIntentUri = Uri.parse("geo:0,0?q=$encoded")
        return Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun saveLocalEvents(events: List<CalendarEventItem>) {
        try {
            val serialized = json.encodeToString(events)
            val tmp = File(context.filesDir, "calendar_events.json.tmp")
            tmp.writeText(serialized, Charsets.UTF_8)
            if (tmp.exists()) {
                if (eventsFile.exists()) eventsFile.delete()
                tmp.renameTo(eventsFile)
            }
        } catch (_: Throwable) {
        }
    }

    /**
     * Elenca tutti i calendari rilevati sul dispositivo (Google Workspace, Gmail, Exchange, Locale)
     * per consentire all'utente di selezionare solo il calendario professionale/studio desiderato.
     */
    fun getAvailableCalendars(): List<SystemCalendarInfo> {
        if (!hasNativeCalendarPermissions()) return emptyList()
        val list = mutableListOf<SystemCalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        // Seleziona solo calendari visibili e scrivibili (per consentire all'utente di aggiungere/sincronizzare impegni)
        val selection = "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR}"
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                null,
                "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.ACCOUNT_NAME} ASC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val displayName = cursor.getString(1) ?: "Calendario $id"
                    val accountName = cursor.getString(2) ?: ""
                    val accountType = cursor.getString(3) ?: ""
                    val isPrimary = cursor.getInt(4) == 1
                    val color = if (!cursor.isNull(5)) cursor.getInt(5) else null
                    val access = cursor.getInt(6)
                    val canWrite = access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR

                    // Escludi calendari spazzatura / pseudo-calendari contatti come compleanni Samsung/Google o festività
                    val isJunkOrBirthday = accountType.equals("local.samsungbirthday", ignoreCase = true) ||
                        accountType.contains("birthday", ignoreCase = true) ||
                        displayName.contains("birthday", ignoreCase = true) ||
                        displayName.contains("compleann", ignoreCase = true) ||
                        displayName.contains("date importanti", ignoreCase = true) ||
                        displayName.contains("holiday", ignoreCase = true) ||
                        displayName.contains("festivit", ignoreCase = true)
                    if (isJunkOrBirthday) continue

                    list.add(
                        SystemCalendarInfo(
                            id = id,
                            name = displayName,
                            accountName = accountName,
                            accountType = accountType,
                            isPrimary = isPrimary,
                            color = color,
                            canWrite = canWrite
                        )
                    )
                }
            }
        } catch (_: Throwable) {
        }

        // Se nessun calendario scrivibile è stato trovato, esegui fallback sui calendari visibili
        if (list.isEmpty()) {
            try {
                context.contentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    "${CalendarContract.Calendars.VISIBLE} = 1",
                    null,
                    "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.ACCOUNT_NAME} ASC"
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val displayName = cursor.getString(1) ?: "Calendario $id"
                        val accountName = cursor.getString(2) ?: ""
                        val accountType = cursor.getString(3) ?: ""
                        val isPrimary = cursor.getInt(4) == 1
                        val color = if (!cursor.isNull(5)) cursor.getInt(5) else null
                        val access = cursor.getInt(6)
                        val canWrite = access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR

                        val isJunkOrBirthday = accountType.equals("local.samsungbirthday", ignoreCase = true) ||
                            accountType.contains("birthday", ignoreCase = true) ||
                            displayName.contains("birthday", ignoreCase = true) ||
                            displayName.contains("compleann", ignoreCase = true) ||
                            displayName.contains("date importanti", ignoreCase = true) ||
                            displayName.contains("holiday", ignoreCase = true) ||
                            displayName.contains("festivit", ignoreCase = true)
                        if (isJunkOrBirthday) continue

                        list.add(
                            SystemCalendarInfo(
                                id = id,
                                name = displayName,
                                accountName = accountName,
                                accountType = accountType,
                                isPrimary = isPrimary,
                                color = color,
                                canWrite = canWrite
                            )
                        )
                    }
                }
            } catch (_: Throwable) {
            }
        }

        return list
    }

    private fun findWritableCalendarId(): Long? {
        val targetId = selectedCalendarId
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        return try {
            var fallbackId: Long? = null
            var matchedSelectedId: Long? = null
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = 1",
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val isPrimary = cursor.getInt(1) == 1
                    val access = cursor.getInt(2)
                    if (access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                        if (targetId != null && id == targetId) {
                            matchedSelectedId = id
                            break
                        }
                        if (isPrimary && fallbackId == null) {
                            fallbackId = id
                        } else if (fallbackId == null) {
                            fallbackId = id
                        }
                    }
                }
            }
            matchedSelectedId ?: fallbackId
        } catch (_: SecurityException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun insertIntoSystemCalendar(
        title: String,
        description: String,
        location: String,
        startMillis: Long,
        endMillis: Long,
        remindersMinutes: List<Int>
    ): Long? {
        val calId = findWritableCalendarId() ?: return null
        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.EVENT_LOCATION, location)
                put(CalendarContract.Events.CALENDAR_ID, calId)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.HAS_ALARM, 1)
            }
            val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = eventUri?.lastPathSegment?.toLongOrNull() ?: return null

            for (mins in remindersMinutes) {
                val remValues = ContentValues().apply {
                    put(CalendarContract.Reminders.MINUTES, mins)
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                }
                context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, remValues)
            }
            eventId
        } catch (_: Throwable) {
            null
        }
    }

    private fun querySystemCalendarEvents(minMillis: Long, maxMillis: Long): List<CalendarEventItem> {
        val result = mutableListOf<CalendarEventItem>()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DESCRIPTION
        )
        val targetCalId = selectedCalendarId
        val selection: String
        val selectionArgs: Array<String>
        if (targetCalId != null) {
            selection = "${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.DTEND} >= ? AND ${CalendarContract.Events.DELETED} = 0 AND ${CalendarContract.Events.CALENDAR_ID} = ?"
            selectionArgs = arrayOf(maxMillis.toString(), minMillis.toString(), targetCalId.toString())
        } else {
            selection = "${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.DTEND} >= ? AND ${CalendarContract.Events.DELETED} = 0"
            selectionArgs = arrayOf(maxMillis.toString(), minMillis.toString())
        }

        try {
            val zone = ZoneId.systemDefault()
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val sysId = cursor.getLong(0)
                    val title = cursor.getString(1) ?: "Impegno Calendario"
                    val dtStart = cursor.getLong(2)
                    val dtEnd = cursor.getLong(3).let { if (it > dtStart) it else dtStart + 3_600_000L }
                    val loc = cursor.getString(4)?.takeIf { it.isNotBlank() } ?: "Da definire"
                    val desc = cursor.getString(5) ?: ""

                    val startIso = Instant.ofEpochMilli(dtStart).atZone(zone).toLocalDateTime().format(isoFormatter)
                    val endIso = Instant.ofEpochMilli(dtEnd).atZone(zone).toLocalDateTime().format(isoFormatter)

                    result.add(
                        CalendarEventItem(
                            id = "sys_$sysId",
                            systemCalendarEventId = sysId,
                            titolo = title,
                            startIso = startIso,
                            endIso = endIso,
                            startMillis = dtStart,
                            endMillis = dtEnd,
                            luogo = loc,
                            cliente = "",
                            note = desc,
                            isFromSystemCalendar = true,
                            categoria = PeritoEventCategory.fromString(title).displayName
                        )
                    )
                }
            }
        } catch (_: Throwable) {
        }
        return result
    }
}
