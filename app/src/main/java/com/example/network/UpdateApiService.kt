package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

/**
 * Manifesto de atualização do VALORAE.
 *
 * Compatível com o contrato antigo do app:
 * {
 *   "latestVersionCode": 48,
 *   "versionName": "2.0.38",
 *   "downloadUrl": "https://.../app.apk"
 * }
 *
 * E com o contrato novo solicitado para Vercel/GitHub:
 * {
 *   "latest_version": "2.0.38",
 *   "version_code": 48,
 *   "apk_url": "https://.../app.apk"
 * }
 */
@JsonClass(generateAdapter = true)
data class AppUpdateInfo(
    @field:Json(name = "latestVersionCode") val legacyLatestVersionCode: Int? = null,
    @field:Json(name = "versionCode") val legacyVersionCode: Int? = null,
    @field:Json(name = "version_code") val snakeVersionCode: Int? = null,

    @field:Json(name = "versionName") val legacyVersionName: String? = null,
    @field:Json(name = "latestVersion") val legacyLatestVersion: String? = null,
    @field:Json(name = "latest_version") val snakeLatestVersion: String? = null,

    @field:Json(name = "downloadUrl") val legacyDownloadUrl: String? = null,
    @field:Json(name = "apkUrl") val legacyApkUrl: String? = null,
    @field:Json(name = "apk_url") val snakeApkUrl: String? = null,

    @field:Json(name = "releaseDate") val releaseDate: String? = null,
    @field:Json(name = "release_date") val snakeReleaseDate: String? = null,
    @field:Json(name = "changelog") val changelog: List<String>? = null,
    @field:Json(name = "releaseNotes") val releaseNotes: String? = null,
    @field:Json(name = "release_notes") val snakeReleaseNotes: String? = null,
    @field:Json(name = "fileSize") val fileSize: String? = null,
    @field:Json(name = "file_size") val snakeFileSize: String? = null,
    @field:Json(name = "isMandatory") val isMandatory: Boolean = false,
    @field:Json(name = "mandatory") val mandatory: Boolean? = null,
    @field:Json(name = "minRequiredVersionCode") val minRequiredVersionCode: Int = 1,
    @field:Json(name = "min_required_version_code") val snakeMinRequiredVersionCode: Int? = null,
    @field:Json(name = "sha256") val sha256: String? = null,
    @field:Json(name = "sha_256") val snakeSha256: String? = null,
    @field:Json(name = "checksumSha256") val checksumSha256: String? = null,
    @field:Json(name = "checksum_sha256") val snakeChecksumSha256: String? = null,
    @field:Json(name = "apkSha256") val apkSha256: String? = null,
    @field:Json(name = "apk_sha256") val snakeApkSha256: String? = null,
    @field:Json(name = "fileSizeBytes") val fileSizeBytes: Long? = null,
    @field:Json(name = "file_size_bytes") val snakeFileSizeBytes: Long? = null,
    @field:Json(name = "sizeBytes") val sizeBytes: Long? = null,
    @field:Json(name = "apk_size_bytes") val apkSizeBytes: Long? = null,
    @field:Json(name = "features") val features: List<UpdateFeature>? = null
) {
    val versionCode: Int
        get() = legacyLatestVersionCode ?: legacyVersionCode ?: snakeVersionCode ?: 0

    val versionName: String
        get() = legacyVersionName ?: legacyLatestVersion ?: snakeLatestVersion ?: "${versionCode}"

    val downloadUrl: String
        get() = legacyDownloadUrl ?: legacyApkUrl ?: snakeApkUrl ?: ""

    val normalizedReleaseDate: String?
        get() = releaseDate ?: snakeReleaseDate

    val normalizedReleaseNotes: String?
        get() = releaseNotes ?: snakeReleaseNotes

    val normalizedFileSize: String?
        get() = fileSize ?: snakeFileSize

    val normalizedMandatory: Boolean
        get() = isMandatory || mandatory == true

    val normalizedMinRequiredVersionCode: Int
        get() = snakeMinRequiredVersionCode ?: minRequiredVersionCode

    val normalizedSha256: String?
        get() = listOf(sha256, snakeSha256, checksumSha256, snakeChecksumSha256, apkSha256, snakeApkSha256)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.matches(Regex("^[a-f0-9]{64}$")) }

    val normalizedFileSizeBytes: Long?
        get() = listOf(fileSizeBytes, snakeFileSizeBytes, sizeBytes, apkSizeBytes)
            .firstOrNull { it != null && it > 0L }
}

@JsonClass(generateAdapter = true)
data class UpdateFeature(
    @field:Json(name = "type") val type: String,
    @field:Json(name = "text") val text: String
)

interface UpdateApiService {
    @GET("update.json")
    suspend fun getLatestUpdateInfo(
        @retrofit2.http.Query("t") timestamp: Long,
        @retrofit2.http.Header("Cache-Control") cacheControl: String
    ): AppUpdateInfo
}
