package com.rhythmgame.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charts")
data class ChartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val difficulty: String,
    val chartJson: String,
)
