package com.rhythmgame.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmgame.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sin

// ── Colors ──────────────────────────────────────────────────────────────────────
private val ScreenBg = Color(0xFF0A0A1A)
private val NeonPurple = Color(0xFFBB86FC)
private val DeepPurple = Color(0xFF7F13EC)
private val NeonCyan = Color(0xFF00E5FF)
private val TextGray = Color(0xFF8888AA)
private val DimGray = Color(0xFF555577)

// ── Upload state ────────────────────────────────────────────────────────────────
enum class UploadState {
    IDLE, UPLOADING, ANALYZING, DONE, ERROR
}

// ── View Model ──────────────────────────────────────────────────────────────────
@HiltViewModel
class UploadViewModel @Inject constructor(
    private val repository: SongRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UploadState.IDLE)
    val state = _state.asStateFlow()

    private val _songId = MutableStateFlow<String?>(null)
    val songId = _songId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _filename = MutableStateFlow("")
    val filename = _filename.asStateFlow()

    fun upload(context: android.content.Context, uri: Uri) {
        _filename.value = resolveFilename(context, uri)
        viewModelScope.launch {
            try {
                _state.value = UploadState.UPLOADING
                val songId = repository.importSong(context, uri)
                _songId.value = songId

                _state.value = UploadState.ANALYZING
                repository.analyzeSong(context, songId)

                _state.value = UploadState.DONE
            } catch (e: Exception) {
                _state.value = UploadState.ERROR
                _errorMessage.value = e.message ?: "Analiz basarisiz"
            }
        }
    }

    private fun resolveFilename(context: android.content.Context, uri: Uri): String {
        context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment ?: "Unknown"
    }
}

// ── Animated wave bars ──────────────────────────────────────────────────────────
@Composable
private fun NeonWaveBars(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveBars")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Canvas(modifier = modifier) {
        val barCount = 24
        val gap = 4f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val maxHeight = size.height

        for (i in 0 until barCount) {
            val x = i * (barWidth + gap)
            val heightFraction = (sin(i * 0.5f + phase) * 0.35f + 0.55f).coerceIn(0.08f, 1f)
            val barHeight = maxHeight * heightFraction
            val top = (maxHeight - barHeight) / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(NeonCyan, NeonPurple),
                    startY = top,
                    endY = top + barHeight,
                ),
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  UPLOAD SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun UploadScreen(
    onUploadComplete: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: UploadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val songId by viewModel.songId.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val filename by viewModel.filename.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.upload(context, uri)
    }

    // Simulated progress animation
    var analysisProgress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state) {
        when (state) {
            UploadState.UPLOADING -> {
                analysisProgress = 0f
                while (analysisProgress < 30f) {
                    delay(50)
                    analysisProgress = (analysisProgress + 0.5f).coerceAtMost(30f)
                }
            }
            UploadState.ANALYZING -> {
                if (analysisProgress < 30f) analysisProgress = 30f
                while (analysisProgress < 95f) {
                    delay(60)
                    analysisProgress = (analysisProgress + 0.25f).coerceAtMost(95f)
                }
            }
            UploadState.DONE -> analysisProgress = 100f
            else -> {}
        }
    }

    // Navigate on complete
    LaunchedEffect(state, songId) {
        if (state == UploadState.DONE && songId != null) {
            delay(400)
            onUploadComplete(songId!!)
        }
    }

    // Pinging dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "upload")
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pingAlpha",
    )

    // ── UI ─────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg),
    ) {
        // Background purple glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.Center)
                .offset(y = (-60).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DeepPurple.copy(alpha = 0.1f),
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
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
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
                Text(
                    text = "Upload Song",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                )
            }

            // ── Content ───────────────────────────────────────────────────────
            when (state) {

                // ── IDLE: file picker ──────────────────────────────────────────
                UploadState.IDLE -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            DeepPurple.copy(alpha = 0.3f),
                                            Color.Transparent,
                                        ),
                                    ),
                                    CircleShape,
                                )
                                .border(2.dp, NeonPurple.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = NeonPurple,
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            "Upload a Song",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Select an MP3, WAV, OGG, or FLAC file\nto generate a rhythm chart",
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        Button(
                            onClick = { filePicker.launch("audio/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonPurple,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Choose File", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ── UPLOADING / ANALYZING ──────────────────────────────────────
                UploadState.UPLOADING, UploadState.ANALYZING -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.weight(0.25f))

                        // Wave animation + center icon
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            NeonWaveBars(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                            )
                            // Graphic EQ icon in glowing circle
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .background(ScreenBg.copy(alpha = 0.85f), CircleShape)
                                    .border(2.dp, NeonPurple.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.GraphicEq,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = NeonPurple,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        // Title
                        Text(
                            "Analyzing audio...",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Our AI is mapping the beats for your\ncustom track. This will only take a moment.",
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        // ── Progress section ──────────────────────────────────
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
                        ) {
                            // Label + percentage
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Text(
                                    "MAPPING NOTES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeonPurple,
                                    letterSpacing = 3.sp,
                                )
                                Text(
                                    "${analysisProgress.toInt()}%",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.1f)),
                            ) {
                                val fraction = (analysisProgress / 100f).coerceIn(0f, 1f)
                                if (fraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeonPurple),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Synchronizing MIDI Data indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = pingAlpha)),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "SYNCHRONIZING MIDI DATA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.3f),
                                    letterSpacing = 2.5.sp,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(0.5f))
                    }

                    // ── Detected track footer ─────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 32.dp)
                            .navigationBarsPadding()
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
                                "DETECTED TRACK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.4f),
                                letterSpacing = 2.sp,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = filename.ifEmpty { "Unknown" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "HIGH ENERGY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonPurple,
                                modifier = Modifier
                                    .background(
                                        NeonPurple.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp),
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Analyzing...",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.4f),
                            )
                        }
                    }
                }

                // ── DONE: brief loading ────────────────────────────────────────
                UploadState.DONE -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = NeonPurple)
                    }
                }

                // ── ERROR ──────────────────────────────────────────────────────
                UploadState.ERROR -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "Upload Failed",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            errorMessage ?: "Unknown error",
                            color = Color(0xFFFF5252),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { filePicker.launch("audio/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonPurple,
                            ),
                        ) {
                            Text("Try Again", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DimGray),
                        ) {
                            Text("Go Back", color = TextGray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
