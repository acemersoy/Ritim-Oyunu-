package com.rhythmgame.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmgame.data.repository.SongRepository
import com.rhythmgame.ui.components.*
import com.rhythmgame.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UploadState {
    IDLE, UPLOADING, ANALYZING, DONE, ERROR
}

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
                val result = repository.importSong(context, uri)
                _songId.value = result.first
                val alreadyReady = result.second

                if (alreadyReady) {
                    // Cached — charts already exist, skip polling
                    _state.value = UploadState.DONE
                } else {
                    _state.value = UploadState.ANALYZING
                    repository.analyzeSong(context, result.first)
                    _state.value = UploadState.DONE
                }
            } catch (e: Exception) {
                _state.value = UploadState.ERROR
                _errorMessage.value = e.message ?: "Analysis failed"
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
    val colors = LocalAppColors.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.upload(context, uri)
    }

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

    LaunchedEffect(state, songId) {
        if (state == UploadState.DONE && songId != null) {
            delay(400)
            onUploadComplete(songId!!)
        }
    }

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
                    text = "SARKI EKLE",
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.textMain,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    letterSpacing = 3.sp,
                )
            }

            when (state) {
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
                                            colors.primary.copy(alpha = 0.15f),
                                            Color.Transparent,
                                        ),
                                    ),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = colors.primary,
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            "Sarki Yukle",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGroteskFontFamily,
                            color = colors.textMain,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "MP3, WAV, OGG veya FLAC dosyasi\nsecin ve ritim haritasi olusturun",
                            textAlign = TextAlign.Center,
                            color = colors.textMuted,
                            fontSize = 14.sp,
                            fontFamily = ManropeFontFamily,
                            lineHeight = 22.sp,
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        ThemedButton(
                            text = "DOSYA SEC",
                            onClick = { filePicker.launch("audio/*") },
                            isPrimary = true,
                            height = 56.dp,
                            fontSize = 16.sp,
                            icon = Icons.Default.Folder,
                        )
                    }
                }

                UploadState.UPLOADING, UploadState.ANALYZING -> {
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
                            "Analiz ediliyor...",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGroteskFontFamily,
                            color = colors.textMain,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Yapay zekamiz parcanizdaki ritimleri\nharitalandiriyor. Bir dakika surecek.",
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

                    // Detected track footer
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 32.dp)
                            .navigationBarsPadding(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.primaryGradient),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (colors.mode == ThemeMode.STAGE_ROCK) Color.Black else Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "TESPIT EDILEN PARCA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ManropeFontFamily,
                                    color = colors.textMuted,
                                    letterSpacing = 2.sp,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = filename.ifEmpty { "Bilinmiyor" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = ManropeFontFamily,
                                    color = colors.textMain,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                UploadState.DONE -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }

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
                            "Yukleme Basarisiz",
                            fontSize = 28.sp,
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
                            onClick = { filePicker.launch("audio/*") },
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
