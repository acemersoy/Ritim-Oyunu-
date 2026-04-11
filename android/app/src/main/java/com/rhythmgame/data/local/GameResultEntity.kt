package com.rhythmgame.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_results")
data class GameResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val songTitle: String,
    val score: Long,
    val accuracy: Float, // 0.0 - 100.0
    val maxCombo: Int,
    val difficulty: String,
    val playedAt: Long = System.currentTimeMillis(),
    val xpEarned: Int,
    val isSynced: Boolean = false
)
