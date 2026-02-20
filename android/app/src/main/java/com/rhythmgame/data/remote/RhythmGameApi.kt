package com.rhythmgame.data.remote

import okhttp3.MultipartBody
import retrofit2.http.*

interface RhythmGameApi {
    @Multipart
    @POST("upload")
    suspend fun uploadSong(@Part file: MultipartBody.Part): UploadResponse

    @GET("status/{songId}")
    suspend fun getStatus(@Path("songId") songId: String): StatusResponse

    @GET("chart/{songId}")
    suspend fun getChart(
        @Path("songId") songId: String,
        @Query("difficulty") difficulty: String
    ): ChartResponse
}

data class UploadResponse(
    val song_id: String,
    val status: String,
    val message: String
)

data class StatusResponse(
    val song_id: String,
    val status: String, // "processing", "ready", "error"
    val message: String?
)

// Chart response matching backend schema
data class ChartResponse(
    val song_id: String,
    val title: String,
    val duration_ms: Int,
    val bpm: Float,
    val difficulty: String,
    val notes: List<NoteResponse>
)

data class NoteResponse(
    val time_ms: Int,
    val lane: Int,
    val duration_ms: Int,
    val type: String
)
