package com.rhythmgame.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.room.withTransaction
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.rhythmgame.data.local.AppDatabase
import com.rhythmgame.data.local.ProfileEntity
import com.rhythmgame.data.local.UserDao
import com.rhythmgame.util.GameRewardCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepository @Inject constructor(
    private val userDao: UserDao,
    private val database: AppDatabase,
    @ApplicationContext private val context: Context,
) {
    val profile: Flow<ProfileEntity?> = userDao.getProfile()

    val coins: Flow<Long> = profile.map { it?.coins ?: 0L }
    val energy: Flow<Int> = profile.map { it?.energy ?: 120 }
    val maxEnergy: Flow<Int> = profile.map { it?.maxEnergy ?: 120 }
    val level: Flow<Int> = profile.map { it?.level ?: 1 }
    val xp: Flow<Long> = profile.map { it?.xp ?: 0L }
    val stars: Flow<Int> = profile.map { it?.stars ?: 0 }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "energy_prefs_encrypted",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            // Fallback to regular SharedPreferences if encryption fails (e.g. rooted device)
            context.getSharedPreferences("energy_prefs", Context.MODE_PRIVATE)
        }
    }

    suspend fun addCoins(amount: Long) {
        userDao.addCoins(amount)
    }

    suspend fun spendCoins(amount: Long): Boolean {
        val current = getCurrentCoins()
        if (current < amount) return false
        userDao.addCoins(-amount)
        return true
    }

    suspend fun setEnergy(energy: Int) {
        userDao.setEnergy(energy)
    }

    suspend fun addXp(amount: Long) {
        database.withTransaction {
            val currentProfile = userDao.getProfileSync() ?: return@withTransaction
            val newTotalXp = currentProfile.xp + amount
            val newLevel = GameRewardCalculator.levelFromTotalXp(newTotalXp)
            userDao.updateXpAndLevel(newTotalXp, newLevel)
        }
    }

    suspend fun addStars(amount: Int) {
        userDao.addStars(amount)
    }

    suspend fun consumeEnergy(): Boolean {
        val currentProfile = getCurrentProfile() ?: return false
        if (currentProfile.energy <= 0) return false
        userDao.setEnergy(currentProfile.energy - 1)
        return true
    }

    suspend fun regenerateEnergy() {
        val currentProfile = getCurrentProfile() ?: return
        val lastRegenTime = prefs.getLong("lastEnergyRegenTime", System.currentTimeMillis())
        val now = System.currentTimeMillis()
        val elapsed = (now - lastRegenTime) / 600_000L // 10 minutes per energy
        if (elapsed > 0) {
            val newEnergy = (currentProfile.energy + elapsed.toInt()).coerceAtMost(currentProfile.maxEnergy)
            userDao.setEnergy(newEnergy)
            prefs.edit().putLong("lastEnergyRegenTime", now).apply()
        }
    }

    fun getNextEnergyTime(): Long {
        val lastRegenTime = prefs.getLong("lastEnergyRegenTime", System.currentTimeMillis())
        return lastRegenTime + 600_000L // 10 minutes per energy
    }

    suspend fun ensureProfileExists() {
        val existing = getCurrentProfile()
        if (existing == null) {
            userDao.updateProfile(
                ProfileEntity(
                    username = "Player",
                    level = 1,
                    xp = 0,
                    coins = 0,
                    energy = 120,
                    maxEnergy = 120,
                    stars = 0,
                )
            )
            prefs.edit().putLong("lastEnergyRegenTime", System.currentTimeMillis()).apply()
        }
    }

    private suspend fun getCurrentProfile(): ProfileEntity? {
        return profile.first()
    }

    private suspend fun getCurrentCoins(): Long {
        return coins.first()
    }
}
