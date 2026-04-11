package com.rhythmgame.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: String = "current_user",
    val username: String,
    val level: Int = 1,
    val xp: Long = 0,
    val league: String = "Bronz",
    val totalGamesPlayed: Int = 0,
    val highestScore: Long = 0,
    val avatarUri: String? = null,
    val lastSyncTimestamp: Long = 0,
    val isSynced: Boolean = false,
    val coins: Long = 0,
    val energy: Int = 120,
    val maxEnergy: Int = 120
)
