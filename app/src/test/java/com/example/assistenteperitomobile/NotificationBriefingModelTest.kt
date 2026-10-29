package com.example.assistenteperitomobile

import com.example.assistenteperitomobile.domain.CalendarEventItem
import com.example.assistenteperitomobile.data.notifications.NotificationBriefingFormatter
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class NotificationBriefingModelTest {

    private val testDate = LocalDate.of(2026, 10, 29)

    @Test
    fun testMultipleEventsSynthesis() {
        val ev1 = CalendarEventItem(
            id = "1",
            titolo = "Perizia Incendio",
            startIso = "2026-10-29T09:30:00",
            endIso = "2026-10-29T11:00:00",
            startMillis = 1000L,
            endMillis = 2000L,
            luogo = "Via Dante 14, Monza",
            cliente = "Mario Rossi",
            note = "Fascicolo #2024-89",
            categoria = "Sinistro Incendio",
            fascicoloId = "FASC-101"
        )
        val ev2 = CalendarEventItem(
            id = "2",
            titolo = "Sinistro Auto Concorsuale",
            startIso = "2026-10-29T14:00:00",
            endIso = "2026-10-29T15:30:00",
            startMillis = 3000L,
            endMillis = 4000L,
            luogo = "Corso Buenos Aires, Milano",
            cliente = "UnionCasa",
            note = "",
            categoria = "Sinistro Auto"
        )
        val ev3 = CalendarEventItem(
            id = "3",
            titolo = "Rilievo APE Residenziale",
            startIso = "2026-10-29T17:00:00",
            endIso = "2026-10-29T18:00:00",
            startMillis = 5000L,
            endMillis = 6000L,
            luogo = "Via Roma 2, Seregno",
            cliente = "Ing. Verdi",
            note = "",
            categoria = "APE"
        )

        val data = NotificationBriefingFormatter.computeBriefingData(
            today = testDate,
            todayEvents = listOf(ev1, ev2, ev3),
            tomorrowEvents = emptyList()
        )

        assertTrue("hasEvents must be true", data.hasEvents)
        assertEquals(3, data.totalTodayCount)
        assertEquals("29", data.dayNumber)
        assertEquals("OTT", data.monthShort)
        assertEquals("3 OGGI", data.badgeCountText)
        assertTrue(data.categoryPillText.contains("INCENDIO"))
        assertEquals("3 Sopralluoghi Programmati", data.mainHeadline)
        assertTrue(data.nextEventSubtitle.contains("09:30"))
        assertTrue(data.nextEventSubtitle.contains("Mario Rossi"))
        assertTrue(data.locationText.contains("Via Dante 14, Monza"))
        assertEquals("Via Dante 14, Monza", data.rawLocationQuery)
        assertTrue(data.timelineText.contains("09:30"))
        assertTrue(data.timelineText.contains("14:00"))
        assertTrue(data.timelineText.contains("17:00"))
        assertEquals("FASC-101", data.targetDossierId)
    }

    @Test
    fun testSingleEventSynthesis() {
        val ev1 = CalendarEventItem(
            id = "1",
            titolo = "Udienza CTU",
            startIso = "2026-10-29T11:00:00",
            endIso = "2026-10-29T12:00:00",
            startMillis = 1000L,
            endMillis = 2000L,
            luogo = "Tribunale di Milano",
            cliente = "Avv. Bianchi",
            note = "",
            categoria = "Tribunale"
        )

        val data = NotificationBriefingFormatter.computeBriefingData(
            today = testDate,
            todayEvents = listOf(ev1),
            tomorrowEvents = emptyList()
        )

        assertTrue(data.hasEvents)
        assertEquals(1, data.totalTodayCount)
        assertEquals("1 OGGI", data.badgeCountText)
        assertEquals("1 Sopralluogo Programmato", data.mainHeadline)
        assertTrue(data.nextEventSubtitle.contains("11:00"))
        assertTrue(data.categoryPillText.contains("TRIBUNALE"))
        assertEquals("Tribunale di Milano", data.rawLocationQuery)
    }

    @Test
    fun testZeroEventsSynthesis() {
        val tomorrowEv = CalendarEventItem(
            id = "9",
            titolo = "Rilievo",
            startIso = "2026-10-30T10:00:00",
            endIso = "2026-10-30T11:00:00",
            startMillis = 1000L,
            endMillis = 2000L,
            luogo = "Monza",
            cliente = "Rossi",
            note = ""
        )

        val data = NotificationBriefingFormatter.computeBriefingData(
            today = testDate,
            todayEvents = emptyList(),
            tomorrowEvents = listOf(tomorrowEv, tomorrowEv)
        )

        assertFalse(data.hasEvents)
        assertEquals(0, data.totalTodayCount)
        assertEquals("LIBERO", data.badgeCountText)
        assertEquals("Nessun Sopralluogo Oggi", data.mainHeadline)
        assertTrue(data.nextEventSubtitle.contains("Domani: 2 appuntamenti"))
        assertEquals("", data.rawLocationQuery)
    }

    @Test
    fun testMissingLocationFallback() {
        val ev = CalendarEventItem(
            id = "1",
            titolo = "Incontro",
            startIso = "2026-10-29T15:00:00",
            endIso = "2026-10-29T16:00:00",
            startMillis = 1000L,
            endMillis = 2000L,
            luogo = "",
            cliente = "Studio",
            note = ""
        )

        val data = NotificationBriefingFormatter.computeBriefingData(
            today = testDate,
            todayEvents = listOf(ev),
            tomorrowEvents = emptyList()
        )

        assertEquals("📍 Sede / Da definire", data.locationText)
        assertEquals("", data.rawLocationQuery)
    }
}
