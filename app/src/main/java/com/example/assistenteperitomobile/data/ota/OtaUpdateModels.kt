package com.example.assistenteperitomobile.data.ota

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OtaUpdateManifest(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val downloadUrl: String,
    val releaseNotes: String = "",
    val mandatory: Boolean = false,
    val publishedAt: String = "",
    val sha256: String = "",
    val fileSize: Long = 0L
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        fun fromJson(jsonString: String): Result<OtaUpdateManifest> = runCatching {
            json.decodeFromString<OtaUpdateManifest>(jsonString)
        }
    }
}

enum class UpdateCheckStatus {
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    CHECK_FAILED
}

data class UpdateCheckResult(
    val status: UpdateCheckStatus,
    val manifest: OtaUpdateManifest? = null,
    val currentVersionCode: Int = 0,
    val currentVersionName: String = "",
    val errorMessage: String? = null
)
