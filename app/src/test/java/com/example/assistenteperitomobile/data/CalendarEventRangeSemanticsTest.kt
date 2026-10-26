package com.example.assistenteperitomobile.data

import com.example.assistenteperitomobile.domain.CalendarEventItem
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class CalendarEventRangeSemanticsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    private fun createEvent(
        id: String,
        title: String,
        startLdt: LocalDateTime,
        endLdt: LocalDateTime,
        zone: ZoneId = ZoneId.systemDefault()
    ): CalendarEventItem {
        val startMillis = startLdt.atZone(zone).toInstant().toEpochMilli()
        val endMillis = endLdt.atZone(zone).toInstant().toEpochMilli()
        return CalendarEventItem(
            id = id,
            titolo = title,
            startIso = startLdt.format(isoFormatter),
            endIso = endLdt.format(isoFormatter),
            startMillis = startMillis,
            endMillis = endMillis,
            luogo = "Via Roma 1, Milano",
            cliente = "Mario Rossi",
            note = "Note test"
        )
    }

    private fun writeLocalEvents(file: File, events: List<CalendarEventItem>) {
        file.writeText(json.encodeToString(events))
    }

    @Test
    fun `test event ended at 21 of previous day is excluded from target day`() = runBlocking {
        val eventsFile = tempFolder.newFile("calendar_events_test1.json")
        val zone = ZoneId.systemDefault()
        val targetDay = LocalDate.of(2026, 10, 26)

        // Evento del giorno precedente (25 Ottobre) dalle 19:00 alle 21:00
        val evYesterday = createEvent(
            id = "ev_21_yesterday",
            title = "Sopralluogo serale concluso alle 21",
            startLdt = LocalDateTime.of(2026, 10, 25, 19, 0),
            endLdt = LocalDateTime.of(2026, 10, 25, 21, 0),
            zone = zone
        )
        writeLocalEvents(eventsFile, listOf(evYesterday))

        val repo = CalendarRepository(context = null, overrideEventsFile = eventsFile)
        val todayEvents = repo.getEventsForDate(targetDay)

        assertTrue(
            "L'evento terminato alle 21 del giorno precedente non deve comparire nel giorno successivo",
            todayEvents.isEmpty()
        )
    }

    @Test
    fun `test event ended exactly at midnight is excluded from target day`() = runBlocking {
        val eventsFile = tempFolder.newFile("calendar_events_test2.json")
        val zone = ZoneId.systemDefault()
        val targetDay = LocalDate.of(2026, 10, 26)

        // Evento del 25 Ottobre dalle 22:00 esattamente fino alla mezzanotte (26 Ottobre 00:00:00)
        val evMidnight = createEvent(
            id = "ev_exact_midnight",
            title = "Evento terminato esattamente a mezzanotte",
            startLdt = LocalDateTime.of(2026, 10, 25, 22, 0),
            endLdt = LocalDateTime.of(2026, 10, 26, 0, 0),
            zone = zone
        )
        writeLocalEvents(eventsFile, listOf(evMidnight))

        val repo = CalendarRepository(context = null, overrideEventsFile = eventsFile)
        val todayEvents = repo.getEventsForDate(targetDay)

        assertTrue(
            "Un evento che termina esattamente all'inizio del giorno (00:00:00) non deve appartenere al giorno successivo con semantica semiaperta [inizio, fine)",
            todayEvents.isEmpty()
        )

        // Verifica che appartenga invece al giorno precedente
        val yesterdayEvents = repo.getEventsForDate(targetDay.minusDays(1))
        assertEquals(1, yesterdayEvents.size)
        assertEquals("ev_exact_midnight", yesterdayEvents.first().id)
    }

    @Test
    fun `test all-day event is included in target day and excluded from adjacent days`() = runBlocking {
        val eventsFile = tempFolder.newFile("calendar_events_test3.json")
        val zone = ZoneId.systemDefault()
        val targetDay = LocalDate.of(2026, 10, 26)

        // Evento All-Day per il 26 Ottobre (dalle 00:00 alle 24:00/00:00 del 27)
        val allDayEvent = createEvent(
            id = "ev_all_day_26",
            title = "Udienza Tribunale Ordinario Giornata Intera",
            startLdt = LocalDateTime.of(2026, 10, 26, 0, 0),
            endLdt = LocalDateTime.of(2026, 10, 27, 0, 0),
            zone = zone
        )
        writeLocalEvents(eventsFile, listOf(allDayEvent))

        val repo = CalendarRepository(context = null, overrideEventsFile = eventsFile)

        val yesterdayEvents = repo.getEventsForDate(targetDay.minusDays(1))
        assertTrue("All-day del 26 non deve comparire il 25", yesterdayEvents.isEmpty())

        val todayEvents = repo.getEventsForDate(targetDay)
        assertEquals("All-day del 26 deve comparire il 26", 1, todayEvents.size)
        assertEquals("ev_all_day_26", todayEvents.first().id)

        val tomorrowEvents = repo.getEventsForDate(targetDay.plusDays(1))
        assertTrue("All-day del 26 non deve comparire il 27", tomorrowEvents.isEmpty())
    }

    @Test
    fun `test nocturnal event crossing midnight is included in both days`() = runBlocking {
        val eventsFile = tempFolder.newFile("calendar_events_test4.json")
        val zone = ZoneId.systemDefault()
        val targetDay = LocalDate.of(2026, 10, 26)

        // Evento notturno dalle 23:00 del 25 Ottobre alle 02:00 del 26 Ottobre
        val evNight = createEvent(
            id = "ev_night_cross",
            title = "Monitoraggio fessurimetro notturno",
            startLdt = LocalDateTime.of(2026, 10, 25, 23, 0),
            endLdt = LocalDateTime.of(2026, 10, 26, 2, 0),
            zone = zone
        )
        writeLocalEvents(eventsFile, listOf(evNight))

        val repo = CalendarRepository(context = null, overrideEventsFile = eventsFile)

        val yesterdayEvents = repo.getEventsForDate(targetDay.minusDays(1))
        assertEquals("Deve comparire il 25 perché iniziato il 25", 1, yesterdayEvents.size)

        val todayEvents = repo.getEventsForDate(targetDay)
        assertEquals("Deve comparire il 26 perché attivo fino alle 02:00 del 26", 1, todayEvents.size)
        assertEquals("ev_night_cross", todayEvents.first().id)
    }

    @Test
    fun `test daytime event in requested day is included`() = runBlocking {
        val eventsFile = tempFolder.newFile("calendar_events_test5.json")
        val zone = ZoneId.systemDefault()
        val targetDay = LocalDate.of(2026, 10, 26)

        val evDay = createEvent(
            id = "ev_regular_day",
            title = "Sopralluogo peritale danni infiltrazioni",
            startLdt = LocalDateTime.of(2026, 10, 26, 10, 30),
            endLdt = LocalDateTime.of(2026, 10, 26, 12, 0),
            zone = zone
        )
        writeLocalEvents(eventsFile, listOf(evDay))

        val repo = CalendarRepository(context = null, overrideEventsFile = eventsFile)
        val todayEvents = repo.getEventsForDate(targetDay)

        assertEquals(1, todayEvents.size)
        assertEquals("ev_regular_day", todayEvents.first().id)
    }

    @Test
    fun `test daylight saving time transition day handles 25-hour day cleanly`() = runBlocking {
        val eventsFile = tempFolder.newFile("calendar_events_dst.json")
        val romeZone = ZoneId.of("Europe/Rome")
        // In Italia il 25 Ottobre 2026 avviene il cambio ora legale -> solare (giornata di 25 ore)
        val dstDay = LocalDate.of(2026, 10, 25)

        val evEvening = createEvent(
            id = "ev_dst_evening",
            title = "Riunione serale pre-perizia",
            startLdt = LocalDateTime.of(2026, 10, 25, 21, 0),
            endLdt = LocalDateTime.of(2026, 10, 25, 23, 0),
            zone = romeZone
        )
        writeLocalEvents(eventsFile, listOf(evEvening))

        val repo = CalendarRepository(context = null, overrideEventsFile = eventsFile)

        val dstEvents = repo.getEventsForDate(dstDay)
        assertEquals(1, dstEvents.size)
        assertEquals("ev_dst_evening", dstEvents.first().id)

        val nextDayEvents = repo.getEventsForDate(dstDay.plusDays(1))
        assertTrue("Non deve comparire il giorno successivo al cambio ora legale", nextDayEvents.isEmpty())
    }

    @Test
    fun `test normalizeCalendarEventTimes for Android all-day event avoids timezone leak`() {
        val romeZone = ZoneId.of("Europe/Rome")

        // Android CalendarContract: ALL_DAY memorizza mezzanotte UTC (es. 26 Ottobre 2026 00:00:00 UTC)
        val rawStartUtc = Instant.parse("2026-10-26T00:00:00Z").toEpochMilli()
        val rawEndUtc = Instant.parse("2026-10-27T00:00:00Z").toEpochMilli()

        val (localStart, localEnd) = CalendarRepository.normalizeCalendarEventTimes(
            rawStart = rawStartUtc,
            rawEnd = rawEndUtc,
            isAllDay = true,
            durationStr = null,
            zone = romeZone
        )

        val expectedStart = LocalDate.of(2026, 10, 26).atStartOfDay(romeZone).toInstant().toEpochMilli()
        val expectedEnd = LocalDate.of(2026, 10, 27).atStartOfDay(romeZone).toInstant().toEpochMilli()

        assertEquals("Il localStart dell'evento all-day deve essere la mezzanotte locale esatta", expectedStart, localStart)
        assertEquals("Il localEnd dell'evento all-day deve essere la mezzanotte successiva esatta", expectedEnd, localEnd)
    }

    @Test
    fun `test normalizeCalendarEventTimes with duration string fallback`() {
        val nowMillis = 1700000000000L
        val (start, end) = CalendarRepository.normalizeCalendarEventTimes(
            rawStart = nowMillis,
            rawEnd = 0L,
            isAllDay = false,
            durationStr = "PT1H30M"
        )

        assertEquals(nowMillis, start)
        assertEquals(nowMillis + 5_400_000L, end)
    }
}
