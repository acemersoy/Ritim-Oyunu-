package com.rhythmgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
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

// -- Design colors --
private val ScreenBackground = Color(0xFF0A0A1A)
private val CardBackground = Color(0xFF14142B)
private val EasyColor = Color(0xFF4ADE80)
private val MediumColor = Color(0xFFA855F7)
private val HardColor = Color(0xFFEF4444)
private val CyanAccent = Color(0xFF22D3EE)
private val GoldColor = Color(0xFFFFD700)
private val GradientPink = Color(0xFFE040FB)
private val GradientPurple = Color(0xFF7C3AED)
private val SubtleGray = Color(0xFF9CA3AF)

private fun difficultyDotColor(difficulty: String): Color = when (difficulty) {
    "easy" -> EasyColor
    "medium" -> MediumColor
    "hard" -> HardColor
    else -> Color.White
}

private fun difficultyTextColor(difficulty: String): Color = when (difficulty) {
    "easy" -> EasyColor
    "medium" -> MediumColor
    "hard" -> HardColor
    else -> Color.White
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
    LaunchedEffect(songId) {
        viewModel.loadSong(songId)
    }

    val song by viewModel.song.collectAsState()
    val isDeleted by viewModel.isDeleted.collectAsState()
    var selectedDifficulty by remember { mutableStateOf("medium") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isDeleted) {
        if (isDeleted) onBack()
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Song", color = Color.White) },
            text = { Text("Bu şarkıyı silmek istediğinize emin misiniz?", color = SubtleGray) },
            containerColor = CardBackground,
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSong(songId)
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = HardColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = SubtleGray)
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        if (song == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = GradientPink)
            }
            return@Box
        }

        val currentSong = song!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ---- TOP BAR ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding() // Apply padding here to push content down
                    .padding(top = 8.dp, bottom = 8.dp)
                    .height(56.dp) // Increased height for better touch targets
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .size(48.dp) // Large touch target
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "SONG DETAILS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // ---- ALBUM ART ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(GradientPink, GradientPurple)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Color.White.copy(alpha = 0.9f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---- SONG NAME ----
            Text(
                text = currentSong.filename
                    .removeSuffix(".mp3")
                    .removeSuffix(".m4a")
                    .removeSuffix(".wav")
                    .removeSuffix(".ogg")
                    .removeSuffix(".flac"),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ---- ARTIST NAME (placeholder) ----
            Text(
                text = "Unknown Artist",
                fontSize = 14.sp,
                color = MediumColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ---- INFO ROW (BPM + DURATION) ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // BPM
                if (currentSong.bpm != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "BPM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SubtleGray,
                            letterSpacing = 1.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CyanAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${currentSong.bpm!!.toInt()}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }

                // DURATION
                if (currentSong.durationMs != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "DURATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SubtleGray,
                            letterSpacing = 1.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CyanAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val min = currentSong.durationMs!! / 60000
                            val sec = (currentSong.durationMs!! % 60000) / 1000
                            Text(
                                text = "$min:${String.format("%02d", sec)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ---- SELECT DIFFICULTY HEADER ----
            Text(
                text = "SELECT DIFFICULTY",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SubtleGray,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ---- DIFFICULTY ROWS ----
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DifficultyRow(
                    label = "EASY",
                    difficulty = "easy",
                    highScore = currentSong.highScoreEasy,
                    selected = selectedDifficulty == "easy",
                    onClick = { selectedDifficulty = "easy" },
                )
                DifficultyRow(
                    label = "MEDIUM",
                    difficulty = "medium",
                    highScore = currentSong.highScoreMedium,
                    selected = selectedDifficulty == "medium",
                    onClick = { selectedDifficulty = "medium" },
                )
                DifficultyRow(
                    label = "HARD",
                    difficulty = "hard",
                    highScore = currentSong.highScoreHard,
                    selected = selectedDifficulty == "hard",
                    onClick = { selectedDifficulty = "hard" },
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ---- START PERFORMANCE BUTTON ----
            val isReady = currentSong.status == "ready"
            val isProcessing = currentSong.status in listOf("processing", "analyzing", "converting")
            val isError = currentSong.status == "error"

            val buttonText = when {
                isReady -> "START PERFORMANCE"
                isProcessing -> "ANALYZING..."
                isError -> "ANALYSIS FAILED"
                else -> "UNAVAILABLE"
            }

            val buttonColor = when {
                isReady -> GoldColor
                isError -> HardColor
                else -> SubtleGray
            }

            Button(
                onClick = { if (isReady) onPlayClick(selectedDifficulty) },
                enabled = isReady || isError, // Allow clicking on error to maybe see details or just show it's failed
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = if (isReady) Color.Black else Color.White,
                    disabledContainerColor = SubtleGray.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else if (isReady) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (isError) {
                    Icon(
                        Icons.Default.EmojiEvents, // Warning icon would be better but using available
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Text(
                    text = buttonText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            
            // Show error message if exists
            if (isError && !currentSong.errorMessage.isNullOrEmpty()) {
                Text(
                    text = "Error: ${currentSong.errorMessage}",
                    color = HardColor,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 16.dp)
                )
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
    val dotColor = difficultyDotColor(difficulty)
    val textColor = difficultyTextColor(difficulty)
    val borderColor = if (selected) textColor.copy(alpha = 0.7f) else Color.Transparent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = CardBackground,
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Colored dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Difficulty label
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )

            Spacer(modifier = Modifier.weight(1f))

            // High score with trophy
            if (highScore > 0) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = GoldColor,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "BEST: ${formatScore(highScore)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = GoldColor,
                )
            } else {
                Text(
                    text = "BEST: --",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = SubtleGray,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Chevron right
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = SubtleGray,
            )
        }
    }
}
