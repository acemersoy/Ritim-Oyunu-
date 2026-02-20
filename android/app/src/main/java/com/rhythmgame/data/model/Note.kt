package com.rhythmgame.data.model

import com.google.gson.annotations.SerializedName

data class Note(
    @SerializedName("time_ms") val timeMs: Int,
    @SerializedName("lane") val lane: Int,
    @SerializedName("duration_ms") val durationMs: Int,
    @SerializedName("type") val type: String  // "tap" or "hold"
) {
    val isTap: Boolean get() = type == "tap"
    val isHold: Boolean get() = type == "hold"
}
