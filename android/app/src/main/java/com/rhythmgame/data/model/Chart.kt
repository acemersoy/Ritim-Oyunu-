package com.rhythmgame.data.model

import com.google.gson.annotations.SerializedName

data class Chart(
    @SerializedName("song_id") val songId: String,
    @SerializedName("title") val title: String,
    @SerializedName("duration_ms") val durationMs: Int,
    @SerializedName("bpm") val bpm: Float,
    @SerializedName("difficulty") val difficulty: String,
    @SerializedName("notes") val notes: List<Note>
)
