package com.example.assistenteperitomobile

import com.example.assistenteperitomobile.data.MorningBriefingWorker
import com.example.assistenteperitomobile.domain.AiEngineConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class MorningBriefingConfigTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testDefaultMorningBriefingConfigValues() {
        val config = AiEngineConfig()
        assertTrue("Briefing should be enabled by default", config.morningBriefingEnabled)
        assertEquals("Default briefing time must be 08:30", "08:30", config.morningBriefingTime)
    }

    @Test
    fun testSerializationPreservesConfiguredTime() {
        val customConfig = AiEngineConfig(
            morningBriefingEnabled = true,
            morningBriefingTime = "07:45"
        )
        val serialized = json.encodeToString(AiEngineConfig.serializer(), customConfig)
        val deserialized = json.decodeFromString(AiEngineConfig.serializer(), serialized)

        assertEquals("07:45", deserialized.morningBriefingTime)
        assertTrue(deserialized.morningBriefingEnabled)
    }

    @Test
    fun testBackwardCompatibilityWithoutTimeField() {
        val legacyJson = """{"morningBriefingEnabled": true}"""
        val deserialized = json.decodeFromString(AiEngineConfig.serializer(), legacyJson)
        assertEquals("08:30", deserialized.morningBriefingTime)
    }

    @Test
    fun testParseBriefingTime() {
        assertEquals(Pair(8, 30), MorningBriefingWorker.parseBriefingTime("08:30"))
        assertEquals(Pair(7, 0), MorningBriefingWorker.parseBriefingTime("07:00"))
        assertEquals(Pair(9, 15), MorningBriefingWorker.parseBriefingTime("09:15"))
        assertEquals(Pair(8, 45), MorningBriefingWorker.parseBriefingTime("08.45"))
        assertEquals(Pair(8, 30), MorningBriefingWorker.parseBriefingTime(""))
        assertEquals(Pair(8, 30), MorningBriefingWorker.parseBriefingTime("non-a-time"))
        assertEquals(Pair(23, 59), MorningBriefingWorker.parseBriefingTime("25:70"))
    }
}
