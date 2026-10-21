package com.example.assistenteperitomobile

import com.example.assistenteperitomobile.domain.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class PeritoCategoryDefinitionTest {
    @Test
    fun testDefaultCategoriesListContainsApeAndSystemCategories() {
        val defaults = defaultCategoriesList()
        val ape = defaults.firstOrNull { it.id == "ape" }
        assertNotNull("APE category must exist in defaults", ape)
        assertEquals("APE", ape!!.displayName)
        assertEquals("⚡", ape.emoji)
        assertTrue(ape.isSystem)
        assertTrue(ape.keywords.contains("ape"))
        assertTrue(ape.keywords.contains("prestazione energetica"))
    }

    @Test
    fun testSerializationAndConfigCompatibility() {
        val json = Json { ignoreUnknownKeys = true }
        val config = PeritoAiConfig()
        val encoded = json.encodeToString(PeritoAiConfig.serializer(), config)
        val decoded = json.decodeFromString(PeritoAiConfig.serializer(), encoded)
        assertTrue(decoded.categories.any { it.id == "ape" })
        assertTrue(decoded.categoryFormats.containsKey("ape"))
    }
}
