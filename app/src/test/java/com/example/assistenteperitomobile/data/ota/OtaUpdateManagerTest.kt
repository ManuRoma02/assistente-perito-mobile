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

        val resultAlwaysAvailable = OtaUpdateManager.evaluateUpdate(manifest, currentVersionCode = 3, currentVersionName = "1.2.0", alwaysAvailable = true)
        assertEquals(UpdateCheckStatus.UPDATE_AVAILABLE, resultAlwaysAvailable.status)
        assertEquals(manifest, resultAlwaysAvailable.manifest)

        val resultNewerLocalWithAlwaysAvailable = OtaUpdateManager.evaluateUpdate(manifest, currentVersionCode = 10, currentVersionName = "2.0.0", alwaysAvailable = true)
        assertEquals(UpdateCheckStatus.UPDATE_AVAILABLE, resultNewerLocalWithAlwaysAvailable.status)
    }

    @Test
    fun testParseManifestWithSha256AndFileSize() {
        val jsonPayload = """
            {
                "latestVersionCode": 16,
                "latestVersionName": "1.0.15",
                "downloadUrl": "https://example.com/app-release.apk",
                "releaseNotes": "• **Release**: fix e ottimizzazioni",
                "mandatory": true,
                "publishedAt": "2026-10-22T10:47:00Z",
                "sha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "fileSize": 25165824
            }
        """.trimIndent()

        val parsedResult = OtaUpdateManifest.fromJson(jsonPayload)
        assertTrue(parsedResult.isSuccess)
        val manifest = parsedResult.getOrThrow()
        assertEquals(16, manifest.latestVersionCode)
        assertEquals("1.0.15", manifest.latestVersionName)
        assertTrue(manifest.mandatory)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", manifest.sha256)
        assertEquals(25165824L, manifest.fileSize)
    }

    @Test
    fun testResolveRedirectUrl() {
        val base = "https://github.com/ManuRoma02/assistente-perito-mobile/releases/download/v1.0.15/app.apk"
        
        // Relative redirect
        val relativeLocation = "/objects.githubusercontent.com/download/app.apk"
        val resolvedRel = OtaUpdateManager.resolveRedirectUrl(base, relativeLocation)
        assertEquals("https://github.com/objects.githubusercontent.com/download/app.apk", resolvedRel)

        // Absolute redirect
        val absoluteLocation = "https://objects.githubusercontent.com/download/app.apk?token=xyz"
        val resolvedAbs = OtaUpdateManager.resolveRedirectUrl(base, absoluteLocation)
        assertEquals(absoluteLocation, resolvedAbs)
    }

    @Test
    fun testSha256CalculationAndIntegrity() {
        val tempFile = java.io.File.createTempFile("ota_test_", ".bin").apply {
            writeBytes("PeritoAI Test Payload 2026".toByteArray(Charsets.UTF_8))
            deleteOnExit()
        }

        val calculated = OtaUpdateManager.calculateSha256(tempFile)
        assertTrue(calculated.isNotBlank())
        assertEquals(64, calculated.length)

        // Verificando lo stesso hash deve dare esito positivo
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val expected = digest.digest("PeritoAI Test Payload 2026".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        assertEquals(expected, calculated)
    }
}
