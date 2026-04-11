package com.rhythmgame.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.SharedPreferences
import androidx.compose.foundation.shape.CircleShape
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.rhythmgame.BuildConfig
import com.rhythmgame.network.DiscoveryState
import com.rhythmgame.network.ServerDiscovery
import com.rhythmgame.ui.components.*
import com.rhythmgame.ui.theme.*
import com.rhythmgame.util.AudioCalibration
import com.rhythmgame.util.GamePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val calibration: AudioCalibration,
    private val prefs: SharedPreferences,
    val serverDiscovery: ServerDiscovery,
    private val gamePreferences: GamePreferences,
) : ViewModel() {
    val offsetMs: Long get() = calibration.offsetMs

    val gameScreenStyle: String get() = gamePreferences.gameScreenStyle

    fun setGameScreenStyle(style: String) {
        gamePreferences.gameScreenStyle = style
    }

    fun setOffsetMs(value: Long) {
        calibration.offsetMs = value.coerceIn(
            AudioCalibration.MIN_OFFSET_MS,
            AudioCalibration.MAX_OFFSET_MS,
        )
    }

    fun getServerUrl(): String {
        return serverDiscovery.currentBaseUrl
    }

    fun setServerUrl(url: String) {
        serverDiscovery.setManualUrl(url)
    }

    fun resetServerUrl() {
        prefs.edit().remove("server_url").apply()
        prefs.edit().remove("discovered_server_url").apply()
    }

    fun startDiscovery() {
        serverDiscovery.startDiscovery()
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var offsetValue by remember { mutableFloatStateOf(viewModel.offsetMs.toFloat()) }
    var serverUrl by remember { mutableStateOf(viewModel.getServerUrl()) }
    var showRestartHint by remember { mutableStateOf(false) }
    val colors = LocalAppColors.current
    var musicVolume by remember { mutableFloatStateOf(0.8f) }
    var sfxVolume by remember { mutableFloatStateOf(0.7f) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var particlesEnabled by remember { mutableStateOf(true) }
    var showFps by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        ThemedBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Top bar
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textMain)
                }
                Text(
                    text = "AYARLAR",
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    letterSpacing = 3.sp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── SES section ──────────────────────────────────────────
            SectionHeader("SES")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingRow(icon = Icons.Default.MusicNote, label = "Muzik Sesi") {
                    Slider(
                        value = musicVolume,
                        onValueChange = { musicVolume = it },
                        modifier = Modifier.width(120.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.textDisabled.copy(alpha = 0.3f),
                        ),
                    )
                }
                SettingRow(icon = Icons.Default.VolumeUp, label = "Efekt Sesleri") {
                    Slider(
                        value = sfxVolume,
                        onValueChange = { sfxVolume = it },
                        modifier = Modifier.width(120.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.textDisabled.copy(alpha = 0.3f),
                        ),
                    )
                }
                SettingRow(icon = Icons.Default.Vibration, label = "Titresim") {
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.primary,
                            checkedTrackColor = colors.primary.copy(alpha = 0.3f),
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── GRAFIK section ───────────────────────────────────────
            SectionHeader("GRAFIK")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SettingRow(icon = Icons.Default.AutoAwesome, label = "Parcacik Efektleri") {
                    Switch(
                        checked = particlesEnabled,
                        onCheckedChange = { particlesEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.primary,
                            checkedTrackColor = colors.primary.copy(alpha = 0.3f),
                        ),
                    )
                }
                SettingRow(icon = Icons.Default.Speed, label = "FPS Goster") {
                    Switch(
                        checked = showFps,
                        onCheckedChange = { showFps = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.primary,
                            checkedTrackColor = colors.primary.copy(alpha = 0.3f),
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── OYUN EKRANI section ──────────────────────────────────
            SectionHeader("OYUN EKRANI")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                var selectedStyle by remember { mutableStateOf(viewModel.gameScreenStyle) }

                Text(
                    "Oyun ekrani gorunum stilini secin.",
                    fontSize = 13.sp,
                    fontFamily = ManropeFontFamily,
                    color = colors.textMuted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        GamePreferences.STYLE_HIGHWAY to "Highway",
                        GamePreferences.STYLE_ARC to "Yay",
                    ).forEach { (style, label) ->
                        val isSelected = selectedStyle == style
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(DesignTokens.radiusSmall))
                                .background(
                                    if (isSelected) colors.primary.copy(alpha = 0.2f)
                                    else colors.cardBg
                                )
                                .clickable {
                                    selectedStyle = style
                                    viewModel.setGameScreenStyle(style)
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ManropeFontFamily,
                                color = if (isSelected) colors.primary else colors.textMuted,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── KALIBRASYON section ──────────────────────────────────
            SectionHeader("KALIBRASYON")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(DesignTokens.radiusMedium))
                    .background(colors.cardBg)
                    .padding(20.dp),
            ) {
                val valueColor by animateColorAsState(
                    targetValue = when {
                        offsetValue.toLong() == 0L -> colors.textMain
                        offsetValue < 0 -> colors.secondary
                        else -> colors.primary
                    },
                    animationSpec = tween(200),
                    label = "offset_color",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.backgroundDeep)
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${offsetValue.toLong()} ms",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = SpaceGroteskFontFamily,
                        color = valueColor,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Notalar erken veya gec geliyorsa ayarlayin.\nNegatif = daha erken, Pozitif = daha gec.",
                    fontSize = 13.sp,
                    fontFamily = ManropeFontFamily,
                    color = colors.textMuted,
                    lineHeight = 18.sp,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Slider(
                    value = offsetValue,
                    onValueChange = {
                        offsetValue = it
                        viewModel.setOffsetMs(it.toLong())
                    },
                    valueRange = -500f..500f,
                    steps = 99,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.textDisabled.copy(alpha = 0.3f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("-500ms", fontSize = 11.sp, fontFamily = ManropeFontFamily, color = colors.textDisabled)
                    Text("0", fontSize = 11.sp, fontFamily = ManropeFontFamily, color = colors.textDisabled)
                    Text("+500ms", fontSize = 11.sp, fontFamily = ManropeFontFamily, color = colors.textDisabled)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(-100L, -50L, 0L, 50L, 100L).forEach { preset ->
                        val isSelected = offsetValue.toLong() == preset
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(DesignTokens.radiusSmall))
                                .background(
                                    if (isSelected) colors.primary.copy(alpha = 0.2f)
                                    else colors.backgroundDeep
                                )
                                .clickable {
                                    offsetValue = preset.toFloat()
                                    viewModel.setOffsetMs(preset)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (preset == 0L) "0" else "${if (preset > 0) "+" else ""}$preset",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = ManropeFontFamily,
                                color = if (isSelected) colors.primary else colors.textMuted,
                            )
                        }
                    }
                }
            }

            // ── SUNUCU section ─────────────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("SUNUCU")

            val discoveryState by viewModel.serverDiscovery.state.collectAsState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(DesignTokens.radiusMedium))
                    .background(colors.cardBg)
                    .padding(20.dp),
            ) {
                // Connection status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val statusColor = when (discoveryState) {
                        is DiscoveryState.Found -> Color(0xFF4CAF50)
                        is DiscoveryState.Searching -> colors.accentGold
                        is DiscoveryState.NotFound -> Color(0xFFF44336)
                        is DiscoveryState.Idle -> colors.textDisabled
                    }
                    val statusText = when (discoveryState) {
                        is DiscoveryState.Found -> "Bagli"
                        is DiscoveryState.Searching -> "Araniyor..."
                        is DiscoveryState.NotFound -> "Bulunamadi"
                        is DiscoveryState.Idle -> "Bekleniyor"
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        statusText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ManropeFontFamily,
                        color = statusColor,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (discoveryState is DiscoveryState.Found) {
                        Text(
                            (discoveryState as DiscoveryState.Found).url
                                .removePrefix("http://")
                                .removeSuffix("/api/")
                                .removeSuffix("/"),
                            fontSize = 12.sp,
                            fontFamily = ManropeFontFamily,
                            color = colors.textMuted,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search button
                Button(
                    onClick = { viewModel.startDiscovery() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DesignTokens.radiusSmall),
                    enabled = discoveryState !is DiscoveryState.Searching,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary.copy(alpha = 0.2f),
                        contentColor = colors.primary,
                    ),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sunucu Ara", fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = colors.textDisabled.copy(alpha = 0.2f))

                Spacer(modifier = Modifier.height(16.dp))

                // Manual URL entry
                Text("Manuel Adres", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = colors.textMain)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Otomatik kesif basarisizsa manuel girebilirsiniz.", fontSize = 12.sp, fontFamily = ManropeFontFamily, color = colors.textMuted)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(BuildConfig.BASE_URL, color = colors.textDisabled, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textMain,
                        unfocusedTextColor = colors.textMain,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.textDisabled.copy(alpha = 0.5f),
                        cursorColor = colors.primary,
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.setServerUrl(serverUrl); showRestartHint = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(DesignTokens.radiusSmall),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary.copy(alpha = 0.2f),
                            contentColor = colors.primary,
                        ),
                    ) {
                        Text("Kaydet", fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.resetServerUrl()
                            serverUrl = BuildConfig.BASE_URL
                            showRestartHint = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(DesignTokens.radiusSmall),
                    ) {
                        Text("Sifirla", color = colors.textMuted, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, fontSize = 13.sp)
                    }
                }

                if (showRestartHint) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Degisiklikler aninda uygulanir.", fontSize = 11.sp, fontFamily = ManropeFontFamily, color = colors.accentGold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── HAKKINDA section ─────────────────────────────────────
            SectionHeader("HAKKINDA")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(DesignTokens.radiusMedium))
                    .background(colors.cardBg)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Rhythm Game", fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGroteskFontFamily, color = colors.accentGold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Versiyon 2.4.0", fontSize = 14.sp, fontFamily = ManropeFontFamily, color = colors.textMuted)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = LocalAppColors.current
    Text(
        title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = ManropeFontFamily,
        color = colors.primary,
        letterSpacing = 3.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}
