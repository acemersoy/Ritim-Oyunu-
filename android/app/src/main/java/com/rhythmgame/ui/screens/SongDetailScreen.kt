package com.rhythmgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmgame.data.model.Song
import com.rhythmgame.data.repository.SongRepository
import com.rhythmgame.ui.components.*
import com.rhythmgame.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SongDetailViewModel @Inject constructor(
    private val repository: SongRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    private val _song = MutableStateFlow<Song?>(null)
    val song = _song.asStateFlow()

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted = _isDeleted.asStateFlow()

    fun loadSong(songId: String) {
        viewModelScope.launch {
            _song.value = repository.getSongById(songId)
        }
    }

    fun deleteSong(songId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSong(appContext, songId)
                _isDeleted.value = true
            } catch (_: Exception) { }
        }
    }
}

private fun difficultyColor(difficulty: String): Color = when (difficulty) {
    "easy" -> DesignTokens.Difficulty.Easy
    "medium" -> DesignTokens.Difficulty.Medium
    "hard" -> DesignTokens.Difficulty.Hard
    else -> DesignTokens.Text.Primary
}

private fun formatScore(score: Int): String {
    return NumberFormat.getNumberInstance(Locale.US).format(score)
}

@Composable
fun SongDetailScreen(
    songId: String,
    onPlayClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SongDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(songId) { viewModel.loadSong(songId) }

    val song by viewModel.song.collectAsState()
    val isDeleted by viewModel.isDeleted.collectAsState()
    val colors = LocalAppColors.current
    var selectedDifficulty by remember { mutableStateOf("medium") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isDeleted) { if (isDeleted) onBack() }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Sarkiyi Sil", color = colors.textMain) },
            text = { Text("Bu sarkiyi silmek istediginizden emin misiniz?", color = colors.textMuted) },
            containerColor = colors.cardGlass,
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSong(songId); showDeleteDialog = false }) {
                    Text("Sil", color = colors.accentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Iptal", color = colors.textMuted)
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ThemedBackground(modifier = Modifier.fillMaxSize())

        if (song == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            return@Box
        }

        val currentSong = song!!

        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 4.dp, bottom = 4.dp)
                    .height(48.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(44.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textMain, modifier = Modifier.size(26.dp))
                }
                Text(
                    text = "SARKI DETAY",
                    color = colors.textMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // ── Body: square cover on left, difficulty + start on right ──────
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // ── Left: square album art + song info ──────────────────────
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(DesignTokens.radiusLarge))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        colors.primary.copy(alpha = 0.35f),
                                        colors.primaryGlow.copy(alpha = 0.15f),
                                        colors.cardBg,
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = colors.primary.copy(alpha = 0.8f),
                        )
                    }

                    // Song title
                    Text(
                        text = currentSong.filename
                            .removeSuffix(".mp3").removeSuffix(".m4a")
                            .removeSuffix(".wav").removeSuffix(".ogg").removeSuffix(".flac"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGroteskFontFamily,
                        color = colors.textMain,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )

                    // BPM · SURE inline
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (currentSong.bpm != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.primary))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "${currentSong.bpm!!.toInt()} BPM",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = ManropeFontFamily,
                                    color = colors.textMuted,
                                )
                            }
                        }
                        if (currentSong.durationMs != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.primary))
                                Spacer(modifier = Modifier.width(6.dp))
                                val min = currentSong.durationMs!! / 60000
                                val sec = (currentSong.durationMs!! % 60000) / 1000
                                Text(
                                    "$min:${String.format("%02d", sec)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = ManropeFontFamily,
                                    color = colors.textMuted,
                                )
                            }
                        }
                    }
                }

                // ── Right: difficulty selection + start button ─────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ZORLUK SEC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ManropeFontFamily,
                            color = colors.primary,
                            letterSpacing = 2.sp,
                        )
                        DifficultyRow("KOLAY", "easy", currentSong.highScoreEasy, selectedDifficulty == "easy") { selectedDifficulty = "easy" }
                        DifficultyRow("ORTA", "medium", currentSong.highScoreMedium, selectedDifficulty == "medium") { selectedDifficulty = "medium" }
                        DifficultyRow("ZOR", "hard", currentSong.highScoreHard, selectedDifficulty == "hard") { selectedDifficulty = "hard" }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Start button
                    val isReady = currentSong.status == "ready"
                    val isProcessing = currentSong.status in listOf("processing", "analyzing", "converting")
                    val isError = currentSong.status == "error"

                    val buttonText = when {
                        isReady -> "BASLA"
                        isProcessing -> "ANALIZ EDILIYOR..."
                        isError -> "ANALIZ BASARISIZ"
                        else -> "KULLANILAMIYOR"
                    }

                    Column {
                        ThemedButton(
                            text = buttonText,
                            onClick = { if (isReady) onPlayClick(selectedDifficulty) },
                            isPrimary = true,
                            height = 60.dp,
                            fontSize = 18.sp,
                            enabled = isReady,
                            icon = if (isReady) Icons.Default.PlayArrow else null,
                        )
                        if (isError && !currentSong.errorMessage.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Hata: ${currentSong.errorMessage}",
                                color = colors.accentRed,
                                fontSize = 11.sp,
                                fontFamily = ManropeFontFamily,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DifficultyRow(
    label: String,
    difficulty: String,
    highScore: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val color = difficultyColor(difficulty)
    val bgColor = if (selected) colors.cardGlass else colors.containerLow

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.width(3.dp).height(24.dp)
                    .clip(RoundedCornerShape(2.dp)).background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Row {
                val starCount = when (difficulty) { "easy" -> 1; "medium" -> 3; "hard" -> 5; else -> 1 }
                repeat(starCount) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(11.dp), tint = color)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = color)
            Spacer(modifier = Modifier.weight(1f))
            if (highScore > 0) {
                Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(14.dp), tint = colors.accentGold)
                Spacer(modifier = Modifier.width(4.dp))
                Text("EN IYI: ${formatScore(highScore)}", fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = ManropeFontFamily, color = colors.accentGold)
            } else {
                Text("EN IYI: --", fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = ManropeFontFamily, color = colors.textDisabled)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = colors.textDisabled)
        }
    }
}
