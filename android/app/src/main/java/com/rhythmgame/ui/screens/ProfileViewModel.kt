package com.rhythmgame.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmgame.data.local.AchievementEntity
import com.rhythmgame.data.local.GameResultEntity
import com.rhythmgame.data.local.ProfileEntity
import com.rhythmgame.data.local.UserDao
import com.rhythmgame.data.repository.AchievementRepository
import com.rhythmgame.data.repository.CurrencyRepository
import com.rhythmgame.util.GameRewardCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    private val userDao: UserDao,
    private val achievementRepository: AchievementRepository,
) : ViewModel() {

    val profile: StateFlow<ProfileEntity?> = currencyRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val gameResults: StateFlow<List<GameResultEntity>> = userDao.getAllResults()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalGames: StateFlow<Int> = gameResults.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val averageAccuracy: StateFlow<Float> = gameResults.map { results ->
        if (results.isEmpty()) 0f
        else results.map { it.accuracy }.average().toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val topSongs: StateFlow<List<GameResultEntity>> = gameResults.map { results ->
        results
            .groupBy { it.songId }
            .map { (_, songs) -> songs.maxBy { it.score } }
            .sortedByDescending { it.score }
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val achievements: StateFlow<List<AchievementEntity>> = achievementRepository.achievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun xpProgress(totalXp: Long, level: Int): Float {
        return GameRewardCalculator.xpProgress(totalXp, level)
    }

    fun xpForNextLevel(level: Int): Long {
        return GameRewardCalculator.totalXpForLevel(level + 1)
    }

    fun updateUsername(newName: String) {
        viewModelScope.launch {
            val current = userDao.getProfileSync() ?: return@launch
            userDao.updateProfile(current.copy(username = newName))
        }
    }

    fun updateAvatar(uri: String) {
        viewModelScope.launch {
            val current = userDao.getProfileSync() ?: return@launch
            userDao.updateProfile(current.copy(avatarUri = uri))
        }
    }
}
