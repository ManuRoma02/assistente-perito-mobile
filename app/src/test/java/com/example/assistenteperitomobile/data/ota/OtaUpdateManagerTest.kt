package com.example.assistenteperitomobile.data.ota

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OtaUpdateManagerTest {

    @Test
    fun testParseValidManifestJson() {
        val jsonPayload = """
            {
                "latestVersionCode": 2,
                "latestVersionName": "1.1.0",
                "downloadUrl": "https://example.com/app-v1.1.0.apk",
                "releaseNotes": "Nuova gestione comandi orari e responsività multi-device.",
                "mandatory": false,
                "publishedAt": "2026-10-18T22:00:00Z"
            }
        """.trimIndent()

        val parsedResult = OtaUpdateManifest.fromJson(jsonPayload)
        assertTrue(parsedResult.isSuccess)
        val manifest = parsedResult.getOrNull()
        assertNotNull(manifest)
        assertEquals(2, manifest!!.latestVersionCode)
        assertEquals("1.1.0", manifest.latestVersionName)
        assertEquals("https://example.com/app-v1.1.0.apk", manifest.downloadUrl)
        assertEquals("Nuova gestione comandi orari e responsività multi-device.", manifest.releaseNotes)
        assertFalse(manifest.mandatory)
    }

    @Test
    fun testParseManifestWithUnknownKeys() {
        val jsonPayload = """
            {
                "latestVersionCode": 5,
                "latestVersionName": "2.0.0",
                "downloadUrl": "https://example.com/release.apk",
                "extraField": "something_ignored",
                "author": "Davez Studio"
            }
        """.trimIndent()

        val parsedResult = OtaUpdateManifest.fromJson(jsonPayload)
        assertTrue(parsedResult.isSuccess)
        val manifest = parsedResult.getOrNull()
        assertNotNull(manifest)
        assertEquals(5, manifest!!.latestVersionCode)
        assertEquals("2.0.0", manifest.latestVersionName)
        assertEquals("", manifest.releaseNotes)
    }

    @Test
    fun testIsUpdateAvailableComparison() {
        val manifest = OtaUpdateManifest(
            latestVersionCode = 2,
            latestVersionName = "1.1.0",
            downloadUrl = "https://example.com/app.apk",
            releaseNotes = "Fix bug"
        )

        // Version on phone is 1 (older) -> update available
        assertTrue(OtaUpdateManager.isUpdateAvailable(manifest, currentVersionCode = 1))

        // Version on phone is 2 (same) -> no update
        assertFalse(OtaUpdateManager.isUpdateAvailable(manifest, currentVersionCode = 2))

        // Version on phone is 3 (newer dev build) -> no update
        assertFalse(OtaUpdateManager.isUpdateAvailable(manifest, currentVersionCode = 3))
    }

    @Test
    fun testEvaluationResultStatus() {
        val manifest = OtaUpdateManifest(
            latestVersionCode = 3,
            latestVersionName = "1.2.0",
            downloadUrl = "https://example.com/app.apk",
            releaseNotes = "Changelog"
        )

        val resultOlder = OtaUpdateManager.evaluateUpdate(manifest, currentVersionCode = 2, currentVersionName = "1.1.0")
        assertEquals(UpdateCheckStatus.UPDATE_AVAILABLE, resultOlder.status)
        assertEquals(manifest, resultOlder.manifest)

        val resultSame = OtaUpdateManager.evaluateUpdate(manifest, currentVersionCode = 3, currentVersionName = "1.2.0")
        assertEquals(UpdateCheckStatus.UP_TO_DATE, resultSame.status)
    }
}
