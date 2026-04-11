package com.rhythmgame.data.repository

import com.rhythmgame.data.local.ProfileEntity
import com.rhythmgame.data.local.UserDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrencyRepository @Inject constructor(
    private val userDao: UserDao,
) {
    val profile: Flow<ProfileEntity?> = userDao.getProfile()

    val coins: Flow<Long> = profile.map { it?.coins ?: 0L }
    val energy: Flow<Int> = profile.map { it?.energy ?: 120 }
    val maxEnergy: Flow<Int> = profile.map { it?.maxEnergy ?: 120 }
    val level: Flow<Int> = profile.map { it?.level ?: 1 }
    val xp: Flow<Long> = profile.map { it?.xp ?: 0L }

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
        val currentProfile = getCurrentProfile() ?: return
        var newXp = currentProfile.xp + amount
        var newLevel = currentProfile.level
        var xpNeeded = xpForLevel(newLevel)

        while (newXp >= xpNeeded) {
            newXp -= xpNeeded
            newLevel++
            xpNeeded = xpForLevel(newLevel)
        }

        userDao.updateXpAndLevel(newXp, newLevel)
    }

    fun xpForLevel(level: Int): Long = (1000L * level * 0.8).toLong().coerceAtLeast(500)

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
                )
            )
        }
    }

    private suspend fun getCurrentProfile(): ProfileEntity? {
        return profile.first()
    }

    private suspend fun getCurrentCoins(): Long {
        return coins.first()
    }
}
