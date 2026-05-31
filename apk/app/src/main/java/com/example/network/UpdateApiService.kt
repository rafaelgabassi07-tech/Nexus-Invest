package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

@JsonClass(generateAdapter = true)
data class AppUpdateInfo(
    @field:Json(name = "latestVersionCode") val versionCode: Int,
    @field:Json(name = "versionName") val versionName: String,
    @field:Json(name = "downloadUrl") val downloadUrl: String,
    @field:Json(name = "releaseDate") val releaseDate: String? = null,
    @field:Json(name = "changelog") val changelog: List<String>? = null,
    @field:Json(name = "releaseNotes") val releaseNotes: String? = null,
    @field:Json(name = "fileSize") val fileSize: String? = null,
    @field:Json(name = "isMandatory") val isMandatory: Boolean = false,
    @field:Json(name = "minRequiredVersionCode") val minRequiredVersionCode: Int = 1,
    @field:Json(name = "features") val features: List<UpdateFeature>? = null
)

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
