package com.rhythmgame.ui.screens

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.rhythmgame.data.repository.SongRepository
import com.rhythmgame.ui.components.*
import com.rhythmgame.ui.theme.*
import com.rhythmgame.util.AudioTrimmer
import com.rhythmgame.util.WaveformExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class EditState {
    LOADING, READY, PREVIEWING, TRIMMING, UPLOADING, ANALYZING, DONE, ERROR
}

@HiltViewModel
class AudioEditViewModel @Inject constructor(
    private val repository: SongRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditState.LOADING)
    val state = _state.asStateFlow()

    private val _waveformData = MutableStateFlow<List<Float>>(emptyList())
    val waveformData = _waveformData.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(0L)
    val totalDurationMs = _totalDurationMs.asStateFlow()

    private val _trimStartMs = MutableStateFlow(0L)
    val trimStartMs = _trimStartMs.asStateFlow()

    private val _trimEndMs = MutableStateFlow(0L)
    val trimEndMs = _trimEndMs.asStateFlow()

    private val _songName = MutableStateFlow("")
    val songName = _songName.asStateFlow()

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs = _playbackPositionMs.asStateFlow()

    private val _songId = MutableStateFlow<String?>(null)
    val songId = _songId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _analysisProgress = MutableStateFlow(0f)
    val analysisProgress = _analysisProgress.asStateFlow()

    var player: ExoPlayer? = null
        private set

    private var sourceFile: File? = null
    private var isPreviewPlaying = false

    fun loadFile(filePath: String, context: android.content.Context) {
        if (_state.value != EditState.LOADING) return
        sourceFile = File(filePath)
        viewModelScope.launch {
            try {
                val file = File(filePath)
                val duration = WaveformExtractor.getDurationMs(file)
                _totalDurationMs.value = duration
                _trimEndMs.value = duration
                _songName.value = file.nameWithoutExtension

                val amplitudes = WaveformExtractor.extractAmplitudes(file, 200)
                _waveformData.value = amplitudes

                // Initialize player
                player = ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                    prepare()
                }

                _state.value = EditState.READY
            } catch (e: Exception) {
                _state.value = EditState.ERROR
                _errorMessage.value = e.message ?: "Dosya yuklenemedi"
            }
        }
    }

    fun setTrimStart(fraction: Float) {
        _trimStartMs.value = (fraction * _totalDurationMs.value).toLong()
    }

    fun setTrimEnd(fraction: Float) {
        _trimEndMs.value = (fraction * _totalDurationMs.value).toLong()
    }

    fun setSongName(name: String) {
        _songName.value = name
    }

    fun togglePreview() {
        val p = player ?: return
        if (isPreviewPlaying) {
            p.pause()
            isPreviewPlaying = false
            _state.value = EditState.READY
        } else {
            p.seekTo(_trimStartMs.value)
            p.play()
            isPreviewPlaying = true
            _state.value = EditState.PREVIEWING

            // Track playback position and stop at trim end
            viewModelScope.launch {
                while (isPreviewPlaying && p.isPlaying) {
                    val pos = p.currentPosition
                    _playbackPositionMs.value = pos
                    if (pos >= _trimEndMs.value) {
                        p.pause()
                        isPreviewPlaying = false
                        _state.value = EditState.READY
                        break
                    }
                    delay(30)
                }
                if (!isPreviewPlaying) {
                    _playbackPositionMs.value = 0L
                }
            }
        }
    }

    fun analyzeRecording(context: android.content.Context) {
        val src = sourceFile ?: return
        val name = _songName.value.ifBlank { "Kayit" }

        viewModelScope.launch {
            try {
                _state.value = EditState.TRIMMING
                player?.pause()
                isPreviewPlaying = false

                // Trim the audio
                val trimmedDir = File(context.filesDir, "trimmed").also { it.mkdirs() }
                val trimmedFile = File(trimmedDir, "${name}.m4a")
                val success = AudioTrimmer.trim(src, trimmedFile, _trimStartMs.value, _trimEndMs.value)
                if (!success) throw Exception("Ses kirpma basarisiz oldu")

                // Upload via repository (same as file upload)
                _state.value = EditState.UPLOADING
                _analysisProgress.value = 0f

                // Animate progress during upload
                launch {
                    while (_state.value == EditState.UPLOADING && _analysisProgress.value < 30f) {
                        delay(50)
                        _analysisProgress.value = (_analysisProgress.value + 0.5f).coerceAtMost(30f)
                    }
                }

                val uri = Uri.fromFile(trimmedFile)
                val result = repository.importSong(context, uri)
                _songId.value = result.first
                val alreadyReady = result.second

                if (alreadyReady) {
                    _analysisProgress.value = 100f
                    _state.value = EditState.DONE
                } else {
                    _state.value = EditState.ANALYZING

                    // Animate progress during analysis
                    launch {
                        if (_analysisProgress.value < 30f) _analysisProgress.value = 30f
                        while (_state.value == EditState.ANALYZING && _analysisProgress.value < 95f) {
                            delay(60)
                            _analysisProgress.value = (_analysisProgress.value + 0.25f).coerceAtMost(95f)
                        }
                    }

                    repository.analyzeSong(context, result.first)
                    _analysisProgress.value = 100f
                    _state.value = EditState.DONE
                }

                // Cleanup trimmed file
                trimmedFile.delete()
            } catch (e: Exception) {
                _state.value = EditState.ERROR
                _errorMessage.value = e.message ?: "Analiz basarisiz oldu"
            }
        }
    }

    fun resetError() {
        _state.value = EditState.READY
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        player?.release()
        player = null
    }
}

@Composable
fun AudioEditScreen(
    filePath: String,
    onAnalysisComplete: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: AudioEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val waveformData by viewModel.waveformData.collectAsState()
    val totalDurationMs by viewModel.totalDurationMs.collectAsState()
    val trimStartMs by viewModel.trimStartMs.collectAsState()
    val trimEndMs by viewModel.trimEndMs.collectAsState()
    val songName by viewModel.songName.collectAsState()
    val playbackPositionMs by viewModel.playbackPositionMs.collectAsState()
    val songId by viewModel.songId.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val analysisProgress by viewModel.analysisProgress.collectAsState()
    val context = LocalContext.current
    val colors = LocalAppColors.current

    // Load file on first composition
    LaunchedEffect(filePath) {
        viewModel.loadFile(filePath, context)
    }

    // Navigate when done
    LaunchedEffect(state, songId) {
        if (state == EditState.DONE && songId != null) {
            delay(400)
            onAnalysisComplete(songId!!)
        }
    }

    // Pulsing animation for analysis state
    val infiniteTransition = rememberInfiniteTransition(label = "edit")
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pingAlpha",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ThemedBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(48.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(48.dp)
                        .background(colors.cardBg, CircleShape),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textMain,
                    )
                }
                Text(
                    text = "SES DUZENLE",
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.textMain,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    letterSpacing = 3.sp,
                )
            }

            when (state) {
                EditState.LOADING -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = colors.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Dalga formu yukleniyor...",
                                color = colors.textMuted,
                                fontSize = 14.sp,
                                fontFamily = ManropeFontFamily,
                            )
                        }
                    }
                }

                EditState.READY, EditState.PREVIEWING -> {
                    // 2-column landscape layout
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left (55%): Waveform
                        Column(
                            modifier = Modifier.weight(0.55f),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            WaveformView(
                                amplitudes = waveformData,
                                trimStartFraction = if (totalDurationMs > 0) trimStartMs.toFloat() / totalDurationMs else 0f,
                                trimEndFraction = if (totalDurationMs > 0) trimEndMs.toFloat() / totalDurationMs else 1f,
                                playbackFraction = if (totalDurationMs > 0 && state == EditState.PREVIEWING)
                                    playbackPositionMs.toFloat() / totalDurationMs else 0f,
                                onTrimStartChanged = { viewModel.setTrimStart(it) },
                                onTrimEndChanged = { viewModel.setTrimEnd(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Time labels
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    formatTime(trimStartMs),
                                    fontSize = 12.sp,
                                    fontFamily = ManropeFontFamily,
                                    color = colors.textMuted,
                                )
                                Text(
                                    formatTime(trimEndMs),
                                    fontSize = 12.sp,
                                    fontFamily = ManropeFontFamily,
                                    color = colors.textMuted,
                                )
                            }
                        }

                        // Right (45%): Controls
                        Column(
                            modifier = Modifier.weight(0.45f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            // Song name input
                            OutlinedTextField(
                                value = songName,
                                onValueChange = { viewModel.setSongName(it) },
                                label = { Text("Sarki Adi", fontFamily = ManropeFontFamily) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.textMuted.copy(alpha = 0.3f),
                                    focusedLabelColor = colors.primary,
                                    unfocusedLabelColor = colors.textMuted,
                                    cursorColor = colors.primary,
                                    focusedTextColor = colors.textMain,
                                    unfocusedTextColor = colors.textMain,
                                ),
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Selected duration info
                            val selectedDuration = (trimEndMs - trimStartMs) / 1000f
                            Text(
                                "Secilen Sure: %.1fs".format(selectedDuration),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = ManropeFontFamily,
                                color = colors.textMuted,
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Preview button
                            ThemedButton(
                                text = if (state == EditState.PREVIEWING) "DURDUR" else "DINLE",
                                onClick = { viewModel.togglePreview() },
                                isPrimary = false,
                                height = 44.dp,
                                fontSize = 13.sp,
                                icon = if (state == EditState.PREVIEWING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Analyze button
                            ThemedButton(
                                text = "ANALIZ ET",
                                onClick = { viewModel.analyzeRecording(context) },
                                isPrimary = true,
                                height = 52.dp,
                                fontSize = 15.sp,
                                icon = Icons.Outlined.GraphicEq,
                                enabled = songName.isNotBlank(),
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            ThemedButton(
                                text = "GERI DON",
                                onClick = onBack,
                                isPrimary = false,
                                height = 40.dp,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                EditState.TRIMMING, EditState.UPLOADING, EditState.ANALYZING -> {
                    // Analysis progress UI (matches UploadScreen)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.weight(0.25f))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            ThemedWaveformBars(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .background(colors.backgroundDeep.copy(alpha = 0.85f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.GraphicEq,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = colors.primary,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Text(
                            when (state) {
                                EditState.TRIMMING -> "Kirpiliyor..."
                                EditState.UPLOADING -> "Yukleniyor..."
                                else -> "Analiz ediliyor..."
                            },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGroteskFontFamily,
                            color = colors.textMain,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Yapay zekamiz kaydinizi\nanaliz ediyor. Bir dakika surecek.",
                            textAlign = TextAlign.Center,
                            color = colors.textMuted,
                            fontSize = 14.sp,
                            fontFamily = ManropeFontFamily,
                            lineHeight = 22.sp,
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(
                                    "NOTALARI ESLESTIRIYOR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = ManropeFontFamily,
                                    color = colors.primary,
                                    letterSpacing = 3.sp,
                                )
                                Text(
                                    "${analysisProgress.toInt()}%",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = SpaceGroteskFontFamily,
                                    color = colors.textMain,
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            ThemedProgressBar(
                                progress = analysisProgress / 100f,
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary.copy(alpha = pingAlpha)),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "MIDI VERISI SENKRONIZE EDILIYOR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ManropeFontFamily,
                                    color = colors.textMuted,
                                    letterSpacing = 2.5.sp,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(0.5f))
                    }
                }

                EditState.DONE -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }

                EditState.ERROR -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "Hata",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGroteskFontFamily,
                            color = colors.textMain,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            errorMessage ?: "Bilinmeyen hata",
                            color = colors.accentRed,
                            textAlign = TextAlign.Center,
                            fontFamily = ManropeFontFamily,
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        ThemedButton(
                            text = "TEKRAR DENE",
                            onClick = { viewModel.resetError() },
                            isPrimary = true,
                            height = 52.dp,
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ThemedButton(
                            text = "GERI DON",
                            onClick = onBack,
                            isPrimary = false,
                            height = 52.dp,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
