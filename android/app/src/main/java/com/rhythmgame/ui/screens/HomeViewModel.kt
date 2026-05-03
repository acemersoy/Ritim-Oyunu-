package com.rhythmgame.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmgame.data.local.ProfileEntity
import com.rhythmgame.data.model.Song
import com.rhythmgame.data.repository.AchievementRepository
import com.rhythmgame.data.repository.CurrencyRepository
import com.rhythmgame.data.repository.SongRepository
import com.rhythmgame.util.AudioCalibration
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

data class DailySong(
    val song: Song,
    val difficulty: String,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SongRepository,
    private val calibration: AudioCalibration,
    private val currencyRepository: CurrencyRepository,
    private val achievementRepository: AchievementRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val songs: Flow<List<Song>> = repository.getAllSongs()

    val profile: StateFlow<ProfileEntity?> = currencyRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailySongs: StateFlow<List<DailySong>> = combine(
        repository.getAllSongs(),
        MutableStateFlow(LocalDate.now().toEpochDay()),
    ) { allSongs, dateSeed ->
        val readySongs = allSongs.filter { it.status == "ready" }
        if (readySongs.isEmpty()) return@combine emptyList()
        val rng = Random(dateSeed)
        val difficulties = listOf("easy", "medium", "hard")
        readySongs.shuffled(rng).take(3).map { song ->
            DailySong(song = song, difficulty = difficulties[rng.nextInt(difficulties.size)])
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            currencyRepository.ensureProfileExists()
            currencyRepository.regenerateEnergy()
            achievementRepository.seedAchievements()
        }
    }

    fun refresh() {
        // No-op: all data is local, Flow automatically updates
    }

    fun deleteSong(songId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSong(appContext, songId)
            } catch (_: Exception) { }
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            try {
                repository.setFavorite(song.songId, !song.isFavorite)
            } catch (_: Exception) { }
        }
    }

    fun getOffsetMs(): Long = calibration.offsetMs

    fun setOffsetMs(value: Long) {
        calibration.offsetMs = value.coerceIn(AudioCalibration.MIN_OFFSET_MS, AudioCalibration.MAX_OFFSET_MS)
    }
}
