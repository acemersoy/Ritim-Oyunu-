package com.rhythmgame.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.rhythmgame.util.AudioCalibration
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// ── Colors ──────────────────────────────────────────────────────────────────────
private val ScreenBg = Color(0xFF0A0A1A)
private val CardBg = Color(0xFF12122A)
private val NeonPurple = Color(0xFFBB86FC)
private val NeonCyan = Color(0xFF00E5FF)
private val TextGray = Color(0xFF8888AA)
private val DimGray = Color(0xFF555577)

// ── View Model ──────────────────────────────────────────────────────────────────
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val calibration: AudioCalibration,
) : ViewModel() {
    val offsetMs: Long get() = calibration.offsetMs

    fun setOffsetMs(value: Long) {
        calibration.offsetMs = value.coerceIn(
            AudioCalibration.MIN_OFFSET_MS,
            AudioCalibration.MAX_OFFSET_MS,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  SETTINGS SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var offsetValue by remember { mutableFloatStateOf(viewModel.offsetMs.toFloat()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, bottom = 8.dp)
                    .height(48.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
                Text(
                    text = "SETTINGS",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Audio Calibration Section ──────────────────────────────────────
            Text(
                "AUDIO CALIBRATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NeonPurple,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(CardBg, RoundedCornerShape(16.dp))
                    .padding(20.dp),
            ) {
                // ── Neon value display ─────────────────────────────────────────
                val valueColor by animateColorAsState(
                    targetValue = when {
                        offsetValue.toLong() == 0L -> Color.White
                        offsetValue < 0 -> NeonCyan
                        else -> NeonPurple
                    },
                    animationSpec = tween(200),
                    label = "offset_color",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ScreenBg)
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    NeonPurple.copy(alpha = 0.5f),
                                    NeonCyan.copy(alpha = 0.5f),
                                ),
                            ),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${offsetValue.toLong()} ms",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = valueColor,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Adjust if notes feel early or late.\nNegative = earlier, Positive = later.",
                    fontSize = 13.sp,
                    color = TextGray,
                    lineHeight = 18.sp,
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Slider ─────────────────────────────────────────────────────
                Slider(
                    value = offsetValue,
                    onValueChange = {
                        offsetValue = it
                        viewModel.setOffsetMs(it.toLong())
                    },
                    valueRange = -500f..500f,
                    steps = 99,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonPurple,
                        activeTrackColor = NeonPurple,
                        inactiveTrackColor = DimGray.copy(alpha = 0.3f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                )

                // ── Range labels ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("-500ms", fontSize = 11.sp, color = DimGray)
                    Text("0", fontSize = 11.sp, color = DimGray)
                    Text("+500ms", fontSize = 11.sp, color = DimGray)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Quick preset chips ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(-100L, -50L, 0L, 50L, 100L).forEach { preset ->
                        val isSelected = offsetValue.toLong() == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier.background(NeonPurple.copy(alpha = 0.3f))
                                    } else {
                                        Modifier.background(ScreenBg)
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeonPurple else DimGray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .clickable {
                                    offsetValue = preset.toFloat()
                                    viewModel.setOffsetMs(preset)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (preset == 0L) "0"
                                else "${if (preset > 0) "+" else ""}$preset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) NeonPurple else TextGray,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── About Section ─────────────────────────────────────────────────
            Text(
                "ABOUT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NeonPurple,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(CardBg, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Rhythm Game",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Version 2.4.0",
                    fontSize = 14.sp,
                    color = TextGray,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Studio Neon",
                    fontSize = 12.sp,
                    color = DimGray,
                    letterSpacing = 2.sp,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
