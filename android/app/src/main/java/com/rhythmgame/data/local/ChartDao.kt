package com.rhythmgame.data.local

import androidx.room.*

@Dao
interface ChartDao {
    @Query("SELECT * FROM charts WHERE songId = :songId AND difficulty = :difficulty LIMIT 1")
    suspend fun getChart(songId: String, difficulty: String): ChartEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChart(chart: ChartEntity)

    @Query("DELETE FROM charts WHERE songId = :songId")
    suspend fun deleteChartsForSong(songId: String)

    @Query("SELECT COUNT(*) FROM charts WHERE songId = :songId")
    suspend fun getChartCountForSong(songId: String): Int
}
