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
import com.rhythmgame.ui.theme.DesignTokens
import com.rhythmgame.ui.theme.SpaceGroteskFontFamily
import com.rhythmgame.util.AudioCalibration
import com.rhythmgame.util.GamePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

// ── Stage Colors for Pause Overlay ──────────────────────────────────────────
private val FireOrange = DesignTokens.Stage.FireOrange
private val FireYellow = DesignTokens.Stage.FireYellow
private val FlameRed = DesignTokens.Stage.FlameRed
private val DarkChrome = DesignTokens.Stage.DarkChrome

// ── View Model ──────────────────────────────────────────────────────────────────
@HiltViewModel
class GameViewModel @Inject constructor(
    private val repository: SongRepository,
    private val calibration: AudioCalibration,
    private val gamePreferences: GamePreferences,
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
    val gameScreenStyle: String get() = gamePreferences.gameScreenStyle

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
    onGameFinished: (score: Int, maxCombo: Int, perfect: Int, great: Int, good: Int, miss: Int, overpress: Int) -> Unit,
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
                    color = FireOrange,
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
                Text("Error: $error", color = FlameRed)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                ) {
                    Text("Go Back", color = Color.Black)
                }
            }
        }
        return
    }

    val currentChart = chart ?: return

    var gameEngine by remember { mutableStateOf<GameEngine?>(null) }
    var audioSync by remember { mutableStateOf<AudioSyncManager?>(null) }
    var isPaused by remember { mutableStateOf(false) }

    val gameStateValue by remember(gameEngine) {
        gameEngine?.gameState ?: MutableStateFlow(GameState())
    }.collectAsState()

    val displayName = song?.filename
        ?.removeSuffix(".mp3")
        ?.removeSuffix(".m4a")
        ?.removeSuffix(".wav")
        ?.removeSuffix(".ogg")
        ?.removeSuffix(".flac")
        ?: "Unknown"

    BackHandler(enabled = !isPaused) {
        gameEngine?.pauseGame()
        isPaused = true
    }

    DisposableEffect(currentChart) {
        onDispose {
            gameEngine?.stopGame()
            if (gameEngine == null) {
                audioSync?.release()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Game SurfaceView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val syncManager = AudioSyncManager(ctx).apply {
                    setAudioOffset(viewModel.audioOffsetMs)
                    audioUri?.let { loadAudio(it) }
                }
                audioSync = syncManager

                GameEngine(
                    context = ctx,
                    chart = currentChart,
                    audioSyncManager = syncManager,
                    audioOffsetMs = viewModel.audioOffsetMs,
                    gameScreenStyle = viewModel.gameScreenStyle,
                    onGameFinished = { state ->
                        viewModel.saveScore(songId, difficulty, state.score)
                        onGameFinished(
                            state.score,
                            state.maxCombo,
                            state.perfectCount,
                            state.greatCount,
                            state.goodCount,
                            state.missCount,
                            state.overpressCount,
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

        // ── Pause overlay (landscape-optimized) ────────────────────────────────
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                // ── Top header row: Score · Progress · Combo ───────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "SCORE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 3.sp,
                        )
                        Text(
                            formatScore(gameStateValue.score),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = FireOrange,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                formatTime(gameStateValue.currentTimeMs),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.3f),
                                letterSpacing = 2.sp,
                            )
                            Text(
                                formatTime(gameStateValue.songDurationMs),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.3f),
                                letterSpacing = 2.sp,
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
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
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(FireYellow, FireOrange, FlameRed)
                                            )
                                        ),
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "COMBO",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 3.sp,
                        )
                        Text(
                            "x${gameStateValue.combo}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = FireYellow,
                        )
                    }
                }

                // ── Center body: two columns (info | actions) ──────────────────
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 64.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left: PAUSED title + song card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            "PAUSED",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            color = Color.White,
                            letterSpacing = 4.sp,
                            fontFamily = SpaceGroteskFontFamily,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(FireYellow, FireOrange, FlameRed)
                                    )
                                ),
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    DarkChrome.copy(alpha = 0.8f),
                                    RoundedCornerShape(14.dp),
                                )
                                .border(
                                    1.dp,
                                    DesignTokens.Stage.SteelGray.copy(alpha = 0.3f),
                                    RoundedCornerShape(14.dp),
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(listOf(FireOrange, FireYellow)),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    displayName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "Unknown Artist",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.4f),
                                    letterSpacing = 2.sp,
                                )
                            }
                        }
                    }

                    // Right: Resume / Restart / Quit buttons stacked
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        // Resume button - fire gradient
                        Button(
                            onClick = {
                                isPaused = false
                                gameEngine?.resumeGame()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Black,
                            ),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(FireYellow, FireOrange, FlameRed.copy(alpha = 0.8f))
                                        ),
                                        RoundedCornerShape(14.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp),
                                        tint = Color.Black,
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "RESUME",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Restart button - chrome/steel
                        Button(
                            onClick = {
                                gameEngine?.stopGame()
                                onRestart()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkChrome,
                                contentColor = Color.White,
                            ),
                            border = BorderStroke(1.dp, DesignTokens.Stage.SteelGray.copy(alpha = 0.5f)),
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "RESTART",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quit button - outlined with red hint
                        OutlinedButton(
                            onClick = {
                                gameEngine?.stopGame()
                                onBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, FlameRed.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = FlameRed.copy(alpha = 0.7f),
                            ),
                        ) {
                            Text(
                                "QUIT",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }

                // ── Footer ─────────────────────────────────────────────────────
                Text(
                    "VER 2.4.0",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.2f),
                    letterSpacing = 4.sp,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
    }
}
