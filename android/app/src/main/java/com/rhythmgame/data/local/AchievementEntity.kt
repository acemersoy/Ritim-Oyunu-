package com.rhythmgame.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,       // "easy", "medium", "hard"
    val isUnlocked: Boolean = false,
    val unlockedAt: Long = 0,
    val progress: Int = 0,
    val target: Int = 1,
)
