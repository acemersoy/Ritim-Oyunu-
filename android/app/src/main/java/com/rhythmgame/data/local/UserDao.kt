package com.rhythmgame.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM profile WHERE id = :userId LIMIT 1")
    fun getProfile(userId: String = "current_user"): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProfile(profile: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGameResult(result: GameResultEntity)

    @Query("SELECT * FROM game_results ORDER BY playedAt DESC")
    fun getAllResults(): Flow<List<GameResultEntity>>

    @Query("SELECT * FROM game_results WHERE isSynced = 0")
    suspend fun getUnsyncedResults(): List<GameResultEntity>

    @Update
    suspend fun updateResultsSyncStatus(results: List<GameResultEntity>)

    @Query("UPDATE profile SET coins = coins + :amount WHERE id = :userId")
    suspend fun addCoins(amount: Long, userId: String = "current_user")

    @Query("UPDATE profile SET energy = :energy WHERE id = :userId")
    suspend fun setEnergy(energy: Int, userId: String = "current_user")

    @Query("UPDATE profile SET xp = :xp, level = :level WHERE id = :userId")
    suspend fun updateXpAndLevel(xp: Long, level: Int, userId: String = "current_user")
}
