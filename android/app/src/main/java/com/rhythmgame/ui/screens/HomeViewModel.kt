package com.rhythmgame.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmgame.data.model.Song
import com.rhythmgame.data.repository.SongRepository
import com.rhythmgame.util.AudioCalibration
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SongRepository,
    private val calibration: AudioCalibration,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    val songs: Flow<List<Song>> = repository.getAllSongs()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

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

    fun getOffsetMs(): Long = calibration.offsetMs

    fun setOffsetMs(value: Long) {
        calibration.offsetMs = value.coerceIn(AudioCalibration.MIN_OFFSET_MS, AudioCalibration.MAX_OFFSET_MS)
    }
}
