package com.rhythmgame.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rhythmgame.data.local.ProfileEntity
import com.rhythmgame.ui.components.*
import com.rhythmgame.ui.theme.*
import com.rhythmgame.util.GameRewardCalculator

@Composable
fun HomeScreen(
    onUploadClick: () -> Unit,
    onSongClick: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    onRankedClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onMultiplayerClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    onRecordClick: () -> Unit = {},
    onDailySongClick: (String, String) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val colors = LocalAppColors.current
    val profile by viewModel.profile.collectAsState()
    val dailySongs by viewModel.dailySongs.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth > 840.dp
        val hPad = if (isTablet) 32.dp else 20.dp
        val colSpacing = if (isTablet) 24.dp else 16.dp
        val sideMaxWidth = if (isTablet) 320.dp else 600.dp
        val centerBtnMaxWidth = if (isTablet) 380.dp else 600.dp

        NebulaMenuBackground(modifier = Modifier.fillMaxSize())

        // Vortex particle swirl — fire-themed attractor + spiral arms + energy rings
        VortexParticleField(
            modifier = Modifier.fillMaxSize(),
            particleCount = 90,
            intensity = 0.18f,
        )

        if (colors.mode == ThemeMode.STAGE_ROCK) {
            MusicalNotesBackground(
                modifier = Modifier.fillMaxSize(),
                intensity = 0.06f,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = hPad, end = hPad, top = 28.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(colSpacing),
        ) {
            // ── LEFT COLUMN (weight=0.26f) ──
            Column(
                modifier = Modifier
                    .weight(0.26f)
                    .widthIn(max = sideMaxWidth)
                    .fillMaxHeight(),
            ) {
                ProfileWidget(
                    profile = profile,
                    onClick = onProfileClick,
                )

                Spacer(modifier = Modifier.weight(1f))

                DailySongsWidget(
                    dailySongs = dailySongs,
                    onSongClick = onDailySongClick,
                )
            }

            // ── CENTER COLUMN (weight=0.40f) ──
            Column(
                modifier = Modifier
                    .weight(0.40f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .widthIn(max = centerBtnMaxWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    GuitarButton(
                        imageRes = com.rhythmgame.R.drawable.btn_oyna,
                        onClick = onPlayClick,
                    )
                }
                Spacer(modifier = Modifier.height(if (isTablet) 12.dp else 8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .widthIn(max = centerBtnMaxWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    GuitarButton(
                        imageRes = com.rhythmgame.R.drawable.btn_sarki_ekle,
                        onClick = onUploadClick,
                    )
                }
                Spacer(modifier = Modifier.height(if (isTablet) 12.dp else 8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .widthIn(max = centerBtnMaxWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    GuitarButton(
                        imageRes = com.rhythmgame.R.drawable.btn_multi,
                        onClick = onMultiplayerClick,
                    )
                }
            }

            // ── RIGHT COLUMN (weight=0.26f) ──
            Column(
                modifier = Modifier
                    .weight(0.26f)
                    .widthIn(max = sideMaxWidth)
                    .fillMaxHeight(),
            ) {
                EconomyWidget(
                    profile = profile,
                    onSettingsClick = onSettingsClick,
                    onStoreClick = onStoreClick,
                )

                Spacer(modifier = Modifier.weight(1f))

                SeasonBanner(seasonNumber = 1, subtitle = "ATESIN RITMI")

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .widthIn(max = if (isTablet) 180.dp else 600.dp),
                    ) {
                        GuitarButton(
                            imageRes = com.rhythmgame.R.drawable.btn_kayit,
                            onClick = onRecordClick,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Store icon only — settings is in EconomyWidget top-right
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    QuickIcon(icon = Icons.Filled.Storefront, onClick = onStoreClick)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  LIQUID GLASS CARD — warm-tinted frosted glass with fire accent
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .clip(shape)
            // Base: dark translucent fill with a warm tint
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A0E0E).copy(alpha = 0.55f),
                        Color(0xFF120808).copy(alpha = 0.65f),
                    )
                )
            )
            // Inner specular highlight — top-left light
            .drawBehind {
                // Warm radial glow top-left
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF6B00).copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.2f, size.height * 0.15f),
                        radius = size.height * 0.7f,
                    ),
                    center = Offset(size.width * 0.2f, size.height * 0.15f),
                    radius = size.height * 0.7f,
                )
                // Subtle white specular strip along top edge
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = size.height * 0.35f,
                    ),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                )
            }
            // Outer border: warm fire tint fading out
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFFFF6B00).copy(alpha = 0.35f),
                        Color(0xFFFFAA00).copy(alpha = 0.15f),
                        Color(0xFFFF2D2D).copy(alpha = 0.10f),
                    )
                ),
                shape = shape,
            )
            // Inner highlight border (inset effect)
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.03f),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                    ),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 0.5.dp.toPx()),
                )
            }
            .padding(14.dp),
        content = content,
    )
}

// ── ProfileWidget ─────────────────────────────────────────────────────────────
@Composable
private fun ProfileWidget(
    profile: ProfileEntity?,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val username = profile?.username ?: "Player"
    val level = profile?.level ?: 1
    val totalXp = profile?.xp ?: 0L
    val xpProgress = GameRewardCalculator.xpProgress(totalXp, level)
    val xpCurrent = totalXp - GameRewardCalculator.totalXpForLevel(level)
    val xpNeeded = GameRewardCalculator.totalXpForLevel(level + 1) - GameRewardCalculator.totalXpForLevel(level)

    val context = LocalContext.current

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                DesignTokens.Stage.FireOrange.copy(alpha = 0.45f),
                                DesignTokens.Stage.FlameRed.copy(alpha = 0.30f),
                            )
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                DesignTokens.Stage.FireYellow.copy(alpha = 0.6f),
                                DesignTokens.Stage.FireOrange.copy(alpha = 0.3f),
                            )
                        ),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val avatarUri = profile?.avatarUri
                if (avatarUri != null) {
                    val bitmap = remember(avatarUri) {
                        try {
                            context.contentResolver.openInputStream(Uri.parse(avatarUri))?.use {
                                BitmapFactory.decodeStream(it)
                            }
                        } catch (_: Exception) { null }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = username.take(1).uppercase(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGroteskFontFamily,
                            color = Color.White,
                        )
                    }
                } else {
                    Text(
                        text = username.take(1).uppercase(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGroteskFontFamily,
                        color = Color.White,
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    color = colors.textMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "LVL $level",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ManropeFontFamily,
                    color = DesignTokens.Stage.StageGold,
                    modifier = Modifier
                        .background(
                            DesignTokens.Stage.StageGold.copy(alpha = 0.12f),
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        ThemedProgressBar(progress = xpProgress)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "XP: ${xpCurrent.coerceAtLeast(0)}/$xpNeeded",
            fontSize = 10.sp,
            fontFamily = ManropeFontFamily,
            color = colors.textMuted,
        )
    }
}

// ── DailySongsWidget ──────────────────────────────────────────────────────────
@Composable
private fun DailySongsWidget(
    dailySongs: List<DailySong>,
    onSongClick: (String, String) -> Unit,
) {
    val colors = LocalAppColors.current

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "GUNUN SARKILARI",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SpaceGroteskFontFamily,
            color = DesignTokens.Stage.StageGold,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (dailySongs.isEmpty()) {
            Text(
                text = "Sarki yukle!",
                fontSize = 11.sp,
                fontFamily = ManropeFontFamily,
                color = colors.textMuted,
            )
        } else {
            dailySongs.forEach { ds ->
                DailySongRow(
                    songName = ds.song.filename
                        .removeSuffix(".mp3")
                        .removeSuffix(".m4a")
                        .removeSuffix(".wav")
                        .removeSuffix(".ogg")
                        .removeSuffix(".flac"),
                    difficulty = ds.difficulty,
                    onClick = { onSongClick(ds.song.songId, ds.difficulty) },
                )
                Spacer(modifier = Modifier.height(5.dp))
            }
        }
    }
}

@Composable
private fun DailySongRow(
    songName: String,
    difficulty: String,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val diffColor = when (difficulty) {
        "easy" -> colors.accentGreen
        "hard" -> colors.accentRed
        else -> colors.accentGold
    }
    val diffLabel = when (difficulty) {
        "easy" -> "KOLAY"
        "hard" -> "ZOR"
        else -> "ORTA"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = DesignTokens.Stage.FireOrange.copy(alpha = 0.8f),
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = songName,
            fontSize = 11.sp,
            fontFamily = ManropeFontFamily,
            color = colors.textMain,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = diffLabel,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ManropeFontFamily,
            color = diffColor,
            modifier = Modifier
                .background(diffColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

// ── EconomyWidget ─────────────────────────────────────────────────────────────
@Composable
private fun EconomyWidget(
    profile: ProfileEntity?,
    onSettingsClick: () -> Unit,
    onStoreClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val energy = profile?.energy ?: 120
    val maxEnergy = profile?.maxEnergy ?: 120
    val coins = profile?.coins ?: 0L
    val stars = profile?.stars ?: 0

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        // Top row: energy + settings icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = "Energy",
                tint = DesignTokens.Stage.FireYellow,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$energy/$maxEnergy",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ManropeFontFamily,
                color = colors.textMain,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onSettingsClick),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Bottom row: coins + stars
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.MonetizationOn,
                contentDescription = "Coins",
                tint = DesignTokens.Stage.StageGold,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "%,d".format(coins),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ManropeFontFamily,
                color = colors.textMain,
                modifier = Modifier.clickable(onClick = onStoreClick),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Stars",
                tint = DesignTokens.Stage.StageGold,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$stars",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ManropeFontFamily,
                color = colors.textMain,
            )
        }
    }
}


@Composable
private fun QuickIcon(
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.size(22.dp),
        )
    }
}
