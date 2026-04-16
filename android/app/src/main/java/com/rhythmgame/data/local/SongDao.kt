package com.rhythmgame.data.local

import androidx.room.*
import com.rhythmgame.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY filename ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE songId = :songId")
    suspend fun getSongById(songId: String): Song?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Update
    suspend fun updateSong(song: Song)

    @Query("UPDATE songs SET highScoreEasy = :score WHERE songId = :songId AND highScoreEasy < :score")
    suspend fun updateHighScoreEasy(songId: String, score: Int)

    @Query("UPDATE songs SET highScoreMedium = :score WHERE songId = :songId AND highScoreMedium < :score")
    suspend fun updateHighScoreMedium(songId: String, score: Int)

    @Query("UPDATE songs SET highScoreHard = :score WHERE songId = :songId AND highScoreHard < :score")
    suspend fun updateHighScoreHard(songId: String, score: Int)

    @Query("UPDATE songs SET isFavorite = :favorite WHERE songId = :songId")
    suspend fun setFavorite(songId: String, favorite: Boolean)

    @Query("UPDATE songs SET lastPlayedAt = :timestamp WHERE songId = :songId")
    suspend fun setLastPlayedAt(songId: String, timestamp: Long)

    @Delete
    suspend fun deleteSong(song: Song)
}
