package com.example.assistenteperitomobile

import com.example.assistenteperitomobile.domain.CategoryReferenceFormat
import com.example.assistenteperitomobile.domain.PeritoEventCategory
import com.example.assistenteperitomobile.domain.defaultCategoryFormats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryReferenceFormatTest {

    @Test
    fun testAllFieldsEnabled() {
        val format = CategoryReferenceFormat(
            showTime = true,
            showType = true,
            showSubject = true,
            showPlace = true,
            showNotes = true
        )
        val title = format.formatTitle(
            startTimeIso = "2026-10-22T10:30:00",
            categoryType = "Sopralluogo",
            subject = "Unioncasa",
            place = "Via Roma 14, Monza",
            notes = "Infiltrazioni copertura"
        )
        assertEquals("10:30 - Sopralluogo - Unioncasa (Via Roma 14, Monza • Infiltrazioni copertura)", title)
    }

    @Test
    fun testMinimalTypeAndSubject() {
        val format = CategoryReferenceFormat(
            showTime = false,
            showType = true,
            showSubject = true,
            showPlace = false,
            showNotes = false
        )
        val title = format.formatTitle(
            startTimeIso = "2026-10-22T10:30:00",
            categoryType = "Rilievo Tecnico",
            subject = "Agenzia Orso",
            place = "Via Garibaldi 3",
            notes = "Rilievo laser"
        )
        assertEquals("Rilievo Tecnico - Agenzia Orso", title)
    }

    @Test
    fun testOnlySubjectEnabled() {
        val format = CategoryReferenceFormat(
            showTime = false,
            showType = false,
            showSubject = true,
            showPlace = false,
            showNotes = false
        )
        val title = format.formatTitle(
            startTimeIso = "2026-10-22T10:30:00",
            categoryType = "Incontro",
            subject = "Unioncasa",
            place = "Via Dante 10",
            notes = ""
        )
        assertEquals("Unioncasa", title)
    }

    @Test
    fun testAllFieldsDisabledFallback() {
        val format = CategoryReferenceFormat(
            showTime = false,
            showType = false,
            showSubject = false,
            showPlace = false,
            showNotes = false
        )
        val title = format.formatTitle(
            startTimeIso = "2026-10-22T10:30:00",
            categoryType = "Tribunale",
            subject = "Mario Rossi",
            place = "Milano",
            notes = "Udienza"
        )
        assertEquals("Tribunale", title)
    }

    @Test
    fun testIgnoresGenericClienteAndDaDefinire() {
        val format = CategoryReferenceFormat(
            showTime = false,
            showType = true,
            showSubject = true,
            showPlace = true,
            showNotes = false
        )
        val title = format.formatTitle(
            startTimeIso = "2026-10-22T10:30:00",
            categoryType = "Sopralluogo",
            subject = "Cliente",
            place = "Da definire",
            notes = ""
        )
        assertEquals("Sopralluogo", title)
    }

    @Test
    fun testDefaultCategoryFormatsCoverAllPeritoEventCategories() {
        val defaults = defaultCategoryFormats()
        PeritoEventCategory.entries.forEach { cat ->
            assertTrue("Category ${cat.id} should have a default format", defaults.containsKey(cat.id))
        }
        val unioncasaFmt = defaults[PeritoEventCategory.UNIONCASA.id]!!
        assertTrue(unioncasaFmt.showSubject)
        assertTrue(unioncasaFmt.showPlace)
    }
}
