package com.example.assistenteperitomobile

import com.example.assistenteperitomobile.data.MorningBriefingWorker
import com.example.assistenteperitomobile.domain.AiEngineConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class MorningBriefingConfigTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

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

    @Test
    fun testGetBriefingConfigReadsHarmonizedConfigFile() {
        val dir = tempFolder.newFolder("files_dir")
        val configFile = File(dir, MorningBriefingWorker.CONFIG_FILE_NAME)
        val customConfig = AiEngineConfig(
            morningBriefingEnabled = false,
            morningBriefingTime = "07:15"
        )
        configFile.writeText(json.encodeToString(AiEngineConfig.serializer(), customConfig))

        val (enabled, time) = MorningBriefingWorker.getBriefingConfigFromDir(dir)
        assertFalse("Should read disabled morningBriefingEnabled from ai_config.json", enabled)
        assertEquals("07:15", time)
    }

    @Test
    fun testGetBriefingConfigFallsBackToLegacyFileName() {
        val dir = tempFolder.newFolder("legacy_dir")
        val legacyFile = File(dir, "ai_engine_config.json")
        val legacyConfig = AiEngineConfig(
            morningBriefingEnabled = true,
            morningBriefingTime = "06:45"
        )
        legacyFile.writeText(json.encodeToString(AiEngineConfig.serializer(), legacyConfig))

        val (enabled, time) = MorningBriefingWorker.getBriefingConfigFromDir(dir)
        assertTrue(enabled)
        assertEquals("06:45", time)
    }

    @Test
    fun testGetBriefingConfigDefaultsWhenNoFileOrCorrupted() {
        val emptyDir = tempFolder.newFolder("empty_dir")
        val (defEnabled, defTime) = MorningBriefingWorker.getBriefingConfigFromDir(emptyDir)
        assertTrue(defEnabled)
        assertEquals("08:30", defTime)

        val corruptDir = tempFolder.newFolder("corrupt_dir")
        val corruptFile = File(corruptDir, MorningBriefingWorker.CONFIG_FILE_NAME)
        corruptFile.writeText("{ invalid json ")
        val (corruptEnabled, corruptTime) = MorningBriefingWorker.getBriefingConfigFromDir(corruptDir)
        assertTrue(corruptEnabled)
        assertEquals("08:30", corruptTime)
    }
}
