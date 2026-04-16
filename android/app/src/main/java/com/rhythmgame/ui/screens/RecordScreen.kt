package com.rhythmgame.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmgame.ui.components.*
import com.rhythmgame.ui.theme.*
import com.rhythmgame.util.AudioRecordingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

enum class RecordingState {
    IDLE, PERMISSION_NEEDED, RECORDING, DONE, ERROR
}

@HiltViewModel
class RecordViewModel @Inject constructor() : ViewModel() {

    private val recordingManager = AudioRecordingManager()

    private val _state = MutableStateFlow(RecordingState.IDLE)
    val state = _state.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs = _recordingDurationMs.asStateFlow()

    private val _amplitudeSamples = MutableStateFlow<List<Float>>(emptyList())
    val amplitudeSamples = _amplitudeSamples.asStateFlow()

    private val _recordedFilePath = MutableStateFlow<String?>(null)
    val recordedFilePath = _recordedFilePath.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun checkPermission(hasPermission: Boolean) {
        if (!hasPermission) {
            _state.value = RecordingState.PERMISSION_NEEDED
        } else if (_state.value == RecordingState.PERMISSION_NEEDED) {
            _state.value = RecordingState.IDLE
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.value = if (granted) RecordingState.IDLE else RecordingState.PERMISSION_NEEDED
    }

    fun startRecording(context: android.content.Context) {
        try {
            val recordingsDir = File(context.filesDir, "recordings").also { it.mkdirs() }
            val outputFile = File(recordingsDir, "${UUID.randomUUID()}.m4a")
            recordingManager.start(context, outputFile)
            _state.value = RecordingState.RECORDING
            _recordingDurationMs.value = 0L
            _amplitudeSamples.value = emptyList()

            // Start polling amplitude and duration
            viewModelScope.launch {
                val startTime = System.currentTimeMillis()
                while (_state.value == RecordingState.RECORDING) {
                    delay(50)
                    _recordingDurationMs.value = System.currentTimeMillis() - startTime
                    val amp = recordingManager.getMaxAmplitude()
                    val normalized = (amp.toFloat() / 32767f).coerceIn(0f, 1f)
                    val current = _amplitudeSamples.value.toMutableList()
                    current.add(normalized)
                    // Keep last 60 samples for visualization
                    if (current.size > 60) current.removeAt(0)
                    _amplitudeSamples.value = current
                }
            }
        } catch (e: Exception) {
            _state.value = RecordingState.ERROR
            _errorMessage.value = e.message ?: "Kayit baslatilamadi"
        }
    }

    fun stopRecording() {
        if (_recordingDurationMs.value < 3000) {
            _errorMessage.value = "Kayit en az 3 saniye olmali"
            _state.value = RecordingState.ERROR
            recordingManager.cancel()
            return
        }
        val file = recordingManager.stop()
        if (file != null && file.exists()) {
            _recordedFilePath.value = file.absolutePath
            _state.value = RecordingState.DONE
        } else {
            _state.value = RecordingState.ERROR
            _errorMessage.value = "Kayit kaydedilemedi"
        }
    }

    fun cancelRecording() {
        recordingManager.cancel()
        _state.value = RecordingState.IDLE
        _recordingDurationMs.value = 0L
        _amplitudeSamples.value = emptyList()
    }

    fun resetError() {
        _state.value = RecordingState.IDLE
        _errorMessage.value = null
    }
}

@Composable
fun RecordScreen(
    onRecordingDone: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: RecordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val durationMs by viewModel.recordingDurationMs.collectAsState()
    val amplitudes by viewModel.amplitudeSamples.collectAsState()
    val recordedFilePath by viewModel.recordedFilePath.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current
    val colors = LocalAppColors.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    // Check permission on first composition
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.checkPermission(hasPermission)
    }

    // Navigate when done
    LaunchedEffect(state, recordedFilePath) {
        if (state == RecordingState.DONE && recordedFilePath != null) {
            onRecordingDone(recordedFilePath!!)
        }
    }

    // Pulsing animation for recording indicator
    val infiniteTransition = rememberInfiniteTransition(label = "record")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
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
                    onClick = {
                        if (state == RecordingState.RECORDING) viewModel.cancelRecording()
                        onBack()
                    },
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
                    text = "SES KAYIT",
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.textMain,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    letterSpacing = 3.sp,
                )
            }

            when (state) {
                RecordingState.PERMISSION_NEEDED -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = colors.primary,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Mikrofon Izni Gerekli",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGroteskFontFamily,
                            color = colors.textMain,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Ses kaydi yapabilmek icin\nmikrofon erisim izni vermeniz gerekiyor.",
                            textAlign = TextAlign.Center,
                            color = colors.textMuted,
                            fontSize = 14.sp,
                            fontFamily = ManropeFontFamily,
                            lineHeight = 22.sp,
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        ThemedButton(
                            text = "IZIN VER",
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            isPrimary = true,
                            height = 52.dp,
                            fontSize = 16.sp,
                            icon = Icons.Default.Mic,
                        )
                    }
                }

                RecordingState.IDLE -> {
                    // Ready to record
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left: record indicator area
                        Column(
                            modifier = Modifier.weight(0.6f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = colors.primary.copy(alpha = 0.6f),
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Kayda Hazir",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGroteskFontFamily,
                                color = colors.textMain,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Butona basarak\nses kaydini baslatin",
                                textAlign = TextAlign.Center,
                                color = colors.textMuted,
                                fontSize = 13.sp,
                                fontFamily = ManropeFontFamily,
                                lineHeight = 20.sp,
                            )
                        }

                        // Right: start button
                        Column(
                            modifier = Modifier.weight(0.4f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            ThemedButton(
                                text = "KAYIT BASLAT",
                                onClick = { viewModel.startRecording(context) },
                                isPrimary = true,
                                height = 56.dp,
                                fontSize = 14.sp,
                                icon = Icons.Default.Mic,
                            )
                        }
                    }
                }

                RecordingState.RECORDING -> {
                    // Recording in progress - 2-column landscape layout
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Left (60%): pulsing indicator + timer + live waveform
                        Column(
                            modifier = Modifier.weight(0.6f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            // Pulsing record circle
                            Box(contentAlignment = Alignment.Center) {
                                // Outer pulse glow
                                Box(
                                    modifier = Modifier
                                        .size((48 * pulseScale).dp)
                                        .background(
                                            colors.accentRed.copy(alpha = 0.2f * pulseAlpha),
                                            CircleShape,
                                        ),
                                )
                                // Inner solid circle
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(colors.accentRed, CircleShape),
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Timer MM:SS
                            val minutes = (durationMs / 60000).toInt()
                            val seconds = ((durationMs % 60000) / 1000).toInt()
                            Text(
                                text = "%02d:%02d".format(minutes, seconds),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGroteskFontFamily,
                                color = colors.textMain,
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Live amplitude bars
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .padding(horizontal = 16.dp),
                            ) {
                                val barCount = amplitudes.size
                                if (barCount == 0) return@Canvas
                                val barWidth = (size.width - 2f * (barCount - 1)) / barCount
                                if (barWidth <= 0f) return@Canvas
                                for (i in amplitudes.indices) {
                                    val amp = amplitudes[i].coerceIn(0.05f, 1f)
                                    val barHeight = size.height * amp
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                DesignTokens.Stage.FireYellow,
                                                DesignTokens.Stage.FireOrange,
                                            ),
                                            startY = (size.height - barHeight) / 2f,
                                            endY = (size.height + barHeight) / 2f,
                                        ),
                                        topLeft = Offset(
                                            i * (barWidth + 2f),
                                            (size.height - barHeight) / 2f,
                                        ),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(barWidth / 2),
                                    )
                                }
                            }
                        }

                        // Right (40%): stop + cancel buttons
                        Column(
                            modifier = Modifier.weight(0.4f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            ThemedButton(
                                text = "KAYDI DURDUR",
                                onClick = { viewModel.stopRecording() },
                                isPrimary = true,
                                height = 56.dp,
                                fontSize = 14.sp,
                                icon = Icons.Default.Stop,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            ThemedButton(
                                text = "IPTAL",
                                onClick = { viewModel.cancelRecording() },
                                isPrimary = false,
                                height = 48.dp,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }

                RecordingState.ERROR -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "Kayit Hatasi",
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

                RecordingState.DONE -> {
                    // Brief transitional state before navigation
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
            }
        }
    }
}
