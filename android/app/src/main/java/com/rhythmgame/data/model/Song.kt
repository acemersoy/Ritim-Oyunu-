package com.rhythmgame.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val songId: String,
    val filename: String,
    val status: String,
    val bpm: Float? = null,
    val durationMs: Int? = null,
    val highScoreEasy: Int = 0,
    val highScoreMedium: Int = 0,
    val highScoreHard: Int = 0,
    val localAudioPath: String? = null,
    val errorMessage: String? = null,
)
