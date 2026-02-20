package com.rhythmgame.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmgame.data.model.Chart
import com.rhythmgame.data.model.Song
import com.rhythmgame.data.repository.SongRepository
import com.rhythmgame.game.AudioSyncManager
import com.rhythmgame.game.GameEngine
import com.rhythmgame.game.GamePhase
import com.rhythmgame.game.GameState
import com.rhythmgame.util.AudioCalibration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

// ── Colors ──────────────────────────────────────────────────────────────────────
private val NeonPurple = Color(0xFFBB86FC)
private val NeonCyan = Color(0xFF00E5FF)
private val SuccessGreen = Color(0xFF22C55E)

// ── View Model ──────────────────────────────────────────────────────────────────
@HiltViewModel
class GameViewModel @Inject constructor(
    private val repository: SongRepository,
    private val calibration: AudioCalibration,
) : ViewModel() {

    private val _chart = MutableStateFlow<Chart?>(null)
    val chart = _chart.asStateFlow()

    private val _song = MutableStateFlow<Song?>(null)
    val song = _song.asStateFlow()

    private val _audioUri = MutableStateFlow<Uri?>(null)
    val audioUri = _audioUri.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    val audioOffsetMs: Long get() = calibration.offsetMs

    fun loadChart(songId: String, difficulty: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _song.value = repository.getSongById(songId)
                _chart.value = repository.getChart(songId, difficulty)
                _audioUri.value = repository.getAudioUri(songId)
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load chart"
                _isLoading.value = false
            }
        }
    }

    fun saveScore(songId: String, difficulty: String, score: Int) {
        viewModelScope.launch {
            try {
                repository.updateHighScore(songId, difficulty, score)
            } catch (_: Exception) { }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────────
private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatScore(score: Int): String {
    return NumberFormat.getNumberInstance(Locale.US).format(score)
}

// ═══════════════════════════════════════════════════════════════════════════════
//  GAME SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun GameScreen(
    songId: String,
    difficulty: String,
    onGameFinished: (score: Int, maxCombo: Int, perfect: Int, great: Int, good: Int, miss: Int) -> Unit,
    onBack: () -> Unit,
    onRestart: () -> Unit = {},
    viewModel: GameViewModel = hiltViewModel(),
) {
    val chart by viewModel.chart.collectAsState()
    val song by viewModel.song.collectAsState()
    val audioUri by viewModel.audioUri.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(songId, difficulty) {
        viewModel.loadChart(songId, difficulty)
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = NeonPurple,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading chart...", fontSize = 18.sp, color = Color.White)
            }
        }
        return
    }

    if (error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error: $error", color = Color(0xFFFF5252))
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                ) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val currentChart = chart ?: return

    var gameEngine by remember { mutableStateOf<GameEngine?>(null) }
    var isPaused by remember { mutableStateOf(false) }

    // Collect game state from engine
    val gameStateValue by remember(gameEngine) {
        gameEngine?.gameState ?: MutableStateFlow(GameState())
    }.collectAsState()

    // Song display name
    val displayName = song?.filename
        ?.removeSuffix(".mp3")
        ?.removeSuffix(".m4a")
        ?.removeSuffix(".wav")
        ?.removeSuffix(".ogg")
        ?.removeSuffix(".flac")
        ?: "Unknown"

    // Back button pauses the game instead of navigating away
    BackHandler(enabled = !isPaused) {
        gameEngine?.pauseGame()
        isPaused = true
    }

    DisposableEffect(currentChart) {
        onDispose {
            gameEngine?.stopGame()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Game SurfaceView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val audioSync = AudioSyncManager(ctx).apply {
                    setAudioOffset(viewModel.audioOffsetMs)
                    audioUri?.let { loadAudio(it) }
                }

                GameEngine(
                    context = ctx,
                    chart = currentChart,
                    audioSyncManager = audioSync,
                    audioOffsetMs = viewModel.audioOffsetMs,
                    onGameFinished = { state ->
                        viewModel.saveScore(songId, difficulty, state.score)
                        onGameFinished(
                            state.score,
                            state.maxCombo,
                            state.perfectCount,
                            state.greatCount,
                            state.goodCount,
                            state.missCount,
                        )
                    },
                ).also { engine ->
                    gameEngine = engine
                }
            },
        )

        // Pause button (top-left)
        if (!isPaused) {
            IconButton(
                onClick = {
                    gameEngine?.pauseGame()
                    isPaused = true
                },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape),
            ) {
                Icon(
                    Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // ── Pause overlay ──────────────────────────────────────────────────────
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // ── Header: Score, Combo, Progress ─────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 28.dp, vertical = 24.dp),
                    ) {
                        // Score + Combo row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    "SCORE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.4f),
                                    letterSpacing = 3.sp,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    formatScore(gameStateValue.score),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    color = Color.White,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "COMBO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.4f),
                                    letterSpacing = 3.sp,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "x${gameStateValue.combo}",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    color = NeonPurple,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Progress labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                formatTime(gameStateValue.currentTimeMs),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.3f),
                                letterSpacing = 2.sp,
                            )
                            Text(
                                "SONG PROGRESS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.3f),
                                letterSpacing = 2.sp,
                            )
                            Text(
                                formatTime(gameStateValue.songDurationMs),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.3f),
                                letterSpacing = 2.sp,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.1f)),
                        ) {
                            val progress = if (gameStateValue.songDurationMs > 0) {
                                (gameStateValue.currentTimeMs.toFloat() / gameStateValue.songDurationMs)
                                    .coerceIn(0f, 1f)
                            } else 0f
                            if (progress > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonPurple),
                                )
                            }
                        }
                    }

                    // ── Center: PAUSED + Buttons + Song Card ───────────────────
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        // PAUSED text
                        Text(
                            "PAUSED",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = Color.White,
                            letterSpacing = 4.sp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // Purple underline
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(NeonPurple),
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        // Resume button - green
                        Button(
                            onClick = {
                                isPaused = false
                                gameEngine?.resumeGame()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen,
                                contentColor = Color.Black,
                            ),
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "RESUME",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Restart button - dark glass
                        Button(
                            onClick = {
                                gameEngine?.stopGame()
                                onRestart()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                contentColor = Color.White,
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "RESTART",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quit button - outlined
                        OutlinedButton(
                            onClick = {
                                gameEngine?.stopGame()
                                onBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White.copy(alpha = 0.6f),
                            ),
                        ) {
                            Text(
                                "QUIT",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Song info card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(16.dp),
                                )
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Album art placeholder
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(listOf(NeonPurple, NeonCyan)),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    displayName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Unknown Artist",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.4f),
                                    letterSpacing = 2.sp,
                                )
                            }

                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    // ── Footer ─────────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 28.dp, vertical = 24.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "VER 2.4.0",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.2f),
                            letterSpacing = 4.sp,
                        )
                    }
                }
            }
        }
    }
}
