package com.rhythmgame.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmgame.data.local.AchievementEntity
import com.rhythmgame.data.repository.AchievementRepository
import com.rhythmgame.data.repository.CurrencyRepository
import com.rhythmgame.data.repository.SongRepository
import com.rhythmgame.util.GameRewardCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val currencyRepository: CurrencyRepository,
    private val songRepository: SongRepository,
    private val achievementRepository: AchievementRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _xpEarned = MutableStateFlow(0L)
    val xpEarned = _xpEarned.asStateFlow()

    private val _starsEarned = MutableStateFlow(0)
    val starsEarned = _starsEarned.asStateFlow()

    private val _coinsEarned = MutableStateFlow(0)
    val coinsEarned = _coinsEarned.asStateFlow()

    private val _xpProgress = MutableStateFlow(0f)
    val xpProgress = _xpProgress.asStateFlow()

    private val _newAchievements = MutableStateFlow<List<AchievementEntity>>(emptyList())
    val newAchievements = _newAchievements.asStateFlow()

    fun computeAndPersistRewards(
        songId: String,
        difficulty: String,
        accuracy: Float,
        maxCombo: Int = 0,
        perfectCount: Int = 0,
        totalNotes: Int = 0,
    ) {
        val alreadyPersisted = savedStateHandle.get<Boolean>("rewards_persisted") ?: false
        if (alreadyPersisted) return
        savedStateHandle["rewards_persisted"] = true

        viewModelScope.launch {
            val song = songRepository.getSongById(songId)
            val durationMs = song?.durationMs ?: 0

            val stars = GameRewardCalculator.starsForGame(difficulty, durationMs)
            val xp = GameRewardCalculator.xpForGame(accuracy, stars)

            // Fetch chart to get noteCount for coin calculation
            val noteCount = try {
                val chart = songRepository.getChart(songId, difficulty)
                chart.notes.size
            } catch (_: Exception) { 0 }

            val coins = GameRewardCalculator.coinsForGame(difficulty, durationMs, noteCount)

            _starsEarned.value = stars
            _xpEarned.value = xp
            _coinsEarned.value = coins

            currencyRepository.addXp(xp)
            currencyRepository.addStars(stars)
            if (coins > 0) {
                currencyRepository.addCoins(coins.toLong())
            }

            val profile = currencyRepository.profile.first()
            if (profile != null) {
                _xpProgress.value = GameRewardCalculator.xpProgress(profile.xp, profile.level)
            }

            // Check achievements — collect newly unlocked ones
            try {
                val unlocked = achievementRepository.checkAndUnlock(
                    songId = songId,
                    difficulty = difficulty,
                    accuracy = accuracy,
                    maxCombo = maxCombo,
                    perfectCount = perfectCount,
                    totalNotes = totalNotes,
                    coinsEarned = coins,
                )
                _newAchievements.value = unlocked
            } catch (_: Exception) { }
        }
    }
}
