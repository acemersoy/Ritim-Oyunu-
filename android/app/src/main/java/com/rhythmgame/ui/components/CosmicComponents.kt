package com.rhythmgame.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.rhythmgame.R
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rhythmgame.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════════════════
// COSMIC PURPLE THEME COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Cosmic animated background with star particles and shooting stars.
 */
@Composable
fun CosmicBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "cosmic_bg")
    val twinklePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(6000, easing = LinearEasing))
    )
    val shootingStarPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing))
    )

    Box(modifier = modifier.background(DesignTokens.cosmicBackgroundGradient)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (i in 0 until 40) {
                val seed = i * 137 + 73
                val x = (seed % 1000) / 1000f * size.width
                val y = ((seed * 7) % 1000) / 1000f * size.height
                val twinkle = (sin(twinklePhase + i * 0.7f) * 0.5f + 0.5f).coerceIn(0.1f, 0.8f)
                drawCircle(color = (when (i % 4) { 0 -> DesignTokens.Cosmic.PrimaryGlow; 1 -> DesignTokens.Cosmic.SecondaryGlow; 2 -> DesignTokens.Cosmic.TextMain; else -> DesignTokens.Cosmic.AccentGold }).copy(alpha = twinkle * 0.4f), radius = 1f + (i % 3) * 0.8f, center = Offset(x, y))
            }
            drawCircle(brush = Brush.radialGradient(colors = listOf(DesignTokens.Cosmic.PrimaryGlow.copy(alpha = 0.06f), DesignTokens.Cosmic.SecondaryGlow.copy(alpha = 0.03f), Color.Transparent), center = Offset(size.width * 0.3f, size.height * 0.2f), radius = size.height * 0.4f), center = Offset(size.width * 0.3f, size.height * 0.2f), radius = size.height * 0.4f)
            drawCircle(brush = Brush.radialGradient(colors = listOf(DesignTokens.Cosmic.SecondaryGlow.copy(alpha = 0.04f), Color.Transparent), center = Offset(size.width * 0.8f, size.height * 0.7f), radius = size.height * 0.3f), center = Offset(size.width * 0.8f, size.height * 0.7f), radius = size.height * 0.3f)
            val ssX = shootingStarPhase * size.width * 1.5f - size.width * 0.2f
            val ssY = size.height * 0.15f + shootingStarPhase * size.height * 0.3f
            if (shootingStarPhase in 0.1f..0.6f) {
                val alpha = (if (shootingStarPhase < 0.3f) (shootingStarPhase - 0.1f) / 0.2f else (0.6f - shootingStarPhase) / 0.3f).coerceIn(0f, 0.6f)
                drawCircle(color = Color.White.copy(alpha = alpha), radius = 2f, center = Offset(ssX, ssY))
                drawLine(color = DesignTokens.Cosmic.PrimaryGlow.copy(alpha = alpha * 0.3f), start = Offset(ssX - 40f, ssY - 12f), end = Offset(ssX, ssY), strokeWidth = 1.5f)
            }
        }
    }
}

/**
 * Glassmorphism card with semi-transparent background and purple border glow.
 */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Column(modifier = modifier.clip(RoundedCornerShape(DesignTokens.radiusMedium)).background(colors.cardGlass).border(width = 1.dp, brush = Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.3f), colors.primary.copy(alpha = 0.1f))), shape = RoundedCornerShape(DesignTokens.radiusMedium)).padding(16.dp), content = content)
}

/**
 * Pre-rendered guitar button: shows the given PNG at full width, keeps its natural aspect.
 * Text/icon are baked into the drawable; no overlay is drawn.
 */
@Composable
fun GuitarButton(imageRes: Int, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    androidx.compose.foundation.Image(
        painter = painterResource(id = imageRes),
        contentDescription = null,
        modifier = modifier.fillMaxWidth().then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentScale = ContentScale.FillWidth,
        alpha = 0.70f,
    )
}

/**
 * Themed button that adapts to current theme.
 */
@Composable
fun ThemedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isPrimary: Boolean = true, height: Dp = 56.dp, fontSize: TextUnit = 16.sp, icon: ImageVector? = null, subtitle: String? = null, badge: String? = null, enabled: Boolean = true, glass: Boolean = false, guitarShape: Boolean = false) {
    val colors = LocalAppColors.current
    if (guitarShape) {
        Box(modifier = modifier.fillMaxWidth().height(height).then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Image(painter = painterResource(id = R.drawable.btn_guitar_fire), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) { Icon(imageVector = icon, contentDescription = null, tint = if (!enabled) colors.textDisabled else Color.White, modifier = Modifier.size(26.dp)); Spacer(modifier = Modifier.width(12.dp)) }
                Column(modifier = if (badge != null) Modifier.weight(1f, fill = false) else Modifier) { Text(text = text, fontSize = fontSize, fontWeight = FontWeight.Bold, fontFamily = SpaceGroteskFontFamily, color = if (!enabled) colors.textDisabled else Color.White, letterSpacing = 2.sp); if (subtitle != null) Text(text = subtitle, fontSize = 10.sp, fontFamily = ManropeFontFamily, color = Color.White.copy(alpha = 0.75f)) }
                if (badge != null) { Spacer(modifier = Modifier.width(8.dp)); Text(text = badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = Color.White, modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) }
            }
        }
        return
    }
    val shape: Shape = RoundedCornerShape(DesignTokens.radiusMedium)
    Box(modifier = modifier.fillMaxWidth().height(height).clip(shape).background(when { !enabled -> Brush.horizontalGradient(listOf(colors.textDisabled.copy(alpha = 0.3f), colors.textDisabled.copy(alpha = 0.2f))); glass -> Brush.verticalGradient(listOf(Color.White.copy(alpha = if (isPrimary) 0.32f else 0.18f), Color.White.copy(alpha = if (isPrimary) 0.10f else 0.06f), Color.White.copy(alpha = if (isPrimary) 0.18f else 0.10f))); else -> if (isPrimary) colors.buttonPrimaryBg else colors.buttonSecondaryBg }).then(when { glass -> Modifier.border(1.dp, Color.White.copy(alpha = 0.35f), shape); colors.mode == ThemeMode.COSMIC_PURPLE && !isPrimary -> Modifier.border(1.dp, colors.primary.copy(alpha = 0.3f), shape); colors.mode == ThemeMode.STAGE_ROCK -> Modifier.beveledSurface(); else -> Modifier }).then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier), contentAlignment = Alignment.Center) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) { Icon(imageVector = icon, contentDescription = null, tint = if (isPrimary && colors.mode == ThemeMode.STAGE_ROCK) Color.Black else if (isPrimary) Color.White else colors.primary, modifier = Modifier.size(22.dp)); Spacer(modifier = Modifier.width(12.dp)) }
            Column(modifier = if (badge != null) Modifier.weight(1f) else Modifier) { Text(text = text, fontSize = fontSize, fontWeight = FontWeight.Bold, fontFamily = SpaceGroteskFontFamily, color = if (!enabled) colors.textDisabled else if (isPrimary && colors.mode == ThemeMode.STAGE_ROCK) Color.Black else if (isPrimary) Color.White else colors.textMain, letterSpacing = 2.sp); if (subtitle != null) Text(text = subtitle, fontSize = 10.sp, fontFamily = ManropeFontFamily, color = colors.textMuted) }
            if (badge != null) { Spacer(modifier = Modifier.width(8.dp)); Text(text = badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = colors.primary, modifier = Modifier.background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) }
        }
    }
}

/**
 * Menu button row - adapts to theme.
 */
@Composable
fun ThemedMenuButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, badge: String? = null, glass: Boolean = false, guitarShape: Boolean = false, height: Dp = 56.dp, fontSize: TextUnit = 15.sp) {
    val colors = LocalAppColors.current
    if (guitarShape) {
        Box(modifier = modifier.fillMaxWidth().height(height).clickable(onClick = onClick)) {
            androidx.compose.foundation.Image(painter = painterResource(id = R.drawable.btn_guitar_fire), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp)); Spacer(modifier = Modifier.width(14.dp))
                Text(text = label, fontSize = fontSize, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = Color.White, modifier = Modifier.weight(1f))
                if (badge != null) { Text(text = badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = Color.White, modifier = Modifier.background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp)); Spacer(modifier = Modifier.width(8.dp)) }
            }
        }
        return
    }
    val shape: Shape = RoundedCornerShape(DesignTokens.radiusMedium)
    Row(modifier = modifier.fillMaxWidth().height(height).clip(shape).background(if (glass) Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.10f))) else Brush.verticalGradient(listOf(colors.cardBg, colors.cardBg))).then(if (glass) Modifier.border(1.dp, Color.White.copy(alpha = 0.30f), shape) else if (colors.mode == ThemeMode.COSMIC_PURPLE) Modifier.border(1.dp, colors.primary.copy(alpha = 0.15f), shape) else Modifier.beveledSurface()).clickable(onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(28.dp).clip(RoundedCornerShape(2.dp)).background(Brush.verticalGradient(listOf(colors.primaryGlow, colors.primary)))); Spacer(modifier = Modifier.width(14.dp))
        Icon(imageVector = icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp)); Spacer(modifier = Modifier.width(14.dp))
        Text(text = label, fontSize = fontSize, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = colors.textMain, modifier = Modifier.weight(1f))
        if (badge != null) Text(text = badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = colors.primary, modifier = Modifier.background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
        Spacer(modifier = Modifier.width(8.dp)); Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textDisabled, modifier = Modifier.size(20.dp))
    }
}

/**
 * Currency bar showing energy + coins.
 */
@Composable
fun CurrencyBar(energy: Int, maxEnergy: Int, coins: Long, modifier: Modifier = Modifier, onAddCoins: () -> Unit = {}) {
    val colors = LocalAppColors.current
    Row(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(DesignTokens.radiusMedium)).background(colors.cardGlass).border(1.dp, colors.primary.copy(alpha = 0.15f), RoundedCornerShape(DesignTokens.radiusMedium)).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Bolt, null, tint = colors.accentGold, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(text = "$energy/$maxEnergy", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = colors.textMain)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.MonetizationOn, null, tint = colors.accentGold, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(text = "%,d".format(coins), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = colors.textMain)
        Spacer(Modifier.width(8.dp)); Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.2f)).clickable(onClick = onAddCoins), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, null, tint = colors.primary, modifier = Modifier.size(14.dp)) }
    }
}

/**
 * User profile header.
 */
@Composable
fun UserProfileHeader(username: String, level: Int, modifier: Modifier = Modifier, onSettingsClick: () -> Unit = {}) {
    val colors = LocalAppColors.current
    Row(modifier = modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.4f), colors.primaryGlow.copy(alpha = 0.2f)))).border(1.5.dp, colors.primary.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(22.dp)) }
        Spacer(modifier = Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { Text(text = username, color = colors.textMain, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGroteskFontFamily); Text(text = "Seviye $level", color = colors.textMuted, fontSize = 11.sp, fontFamily = ManropeFontFamily) }
        IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = colors.textMuted) }
    }
}

@Composable
fun ThemedProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 8.dp) {
    val colors = LocalAppColors.current
    val shineOffset by rememberInfiniteTransition().animateFloat(initialValue = -1f, targetValue = 2f, animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)))
    Box(modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(colors.containerLow)) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).clip(RoundedCornerShape(height / 2)).background(colors.primaryGradient).drawBehind { val shineX = size.width * shineOffset; drawRect(brush = Brush.horizontalGradient(colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.2f), Color.Transparent), startX = shineX - 40f, endX = shineX + 40f)) })
    }
}

@Composable
fun SongCard(title: String, artist: String, difficulty: String, bpm: String, duration: String, isSelected: Boolean = false, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val diffColor = when (difficulty.lowercase()) { "easy" -> colors.accentGreen; "medium" -> colors.accentGold; "hard" -> colors.accentRed; else -> colors.textMuted }
    Row(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(DesignTokens.radiusMedium)).background(if (isSelected) colors.primary.copy(alpha = 0.1f) else colors.cardBg).then(if (isSelected) Modifier.border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(DesignTokens.radiusMedium)) else if (colors.mode == ThemeMode.COSMIC_PURPLE) Modifier.border(1.dp, colors.primary.copy(alpha = 0.1f), RoundedCornerShape(DesignTokens.radiusMedium)) else Modifier).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.3f), colors.primaryGlow.copy(alpha = 0.15f)))), contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = colors.primary, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = colors.textMain, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(text = artist, fontSize = 11.sp, fontFamily = ManropeFontFamily, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis); Row(verticalAlignment = Alignment.CenterVertically) { Text(text = "$bpm BPM", fontSize = 10.sp, fontFamily = ManropeFontFamily, color = colors.textMuted); Spacer(Modifier.width(8.dp)); Text(text = duration, fontSize = 10.sp, fontFamily = ManropeFontFamily, color = colors.textMuted) } }
        Text(text = difficulty.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = diffColor, modifier = Modifier.background(diffColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
fun SettingRow(icon: ImageVector, label: String, modifier: Modifier = Modifier, value: String? = null, control: @Composable (() -> Unit)? = null) {
    val colors = LocalAppColors.current
    Row(modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(DesignTokens.radiusSmall)).background(colors.cardBg).then(if (colors.mode == ThemeMode.COSMIC_PURPLE) Modifier.border(1.dp, colors.primary.copy(alpha = 0.08f), RoundedCornerShape(DesignTokens.radiusSmall)) else Modifier).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = colors.primary, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(14.dp)); Text(text = label, fontSize = 14.sp, fontFamily = ManropeFontFamily, color = colors.textMain, modifier = Modifier.weight(1f)); if (value != null) Text(text = value, fontSize = 13.sp, fontFamily = ManropeFontFamily, color = colors.textMuted); if (control != null) control()
    }
}

@Composable
fun RankEmblem(rankName: String, rankTier: String, rp: Int, rpMax: Int, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val rankColor = when (rankName.lowercase()) { "bronz" -> Color(0xFFCD7F32); "silver" -> Color(0xFFC0C0C0); "gold" -> colors.accentGold; "platin" -> Color(0xFF00CED1); "elmas" -> Color(0xFFB9F2FF); else -> colors.primary }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Brush.radialGradient(listOf(rankColor.copy(alpha = 0.3f), rankColor.copy(alpha = 0.05f)))).border(2.dp, rankColor.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Shield, null, tint = rankColor, modifier = Modifier.size(36.dp)) }
        Text(text = "$rankName $rankTier".uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = SpaceGroteskFontFamily, color = rankColor, letterSpacing = 2.sp); Text(text = "$rp / $rpMax RP", fontSize = 12.sp, fontFamily = ManropeFontFamily, color = colors.textMuted); ThemedProgressBar(progress = rp.toFloat() / rpMax.coerceAtLeast(1), modifier = Modifier.width(160.dp))
    }
}

@Composable
fun HitBreakdownRow(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(12.dp)); Text(text = label, fontSize = 14.sp, fontFamily = ManropeFontFamily, color = colors.textMain, modifier = Modifier.weight(1f)); Text(text = count.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGroteskFontFamily, color = color)
    }
}

@Composable
fun GradeCircle(grade: String, modifier: Modifier = Modifier, size: Dp = 100.dp) {
    val colors = LocalAppColors.current
    val gradeColor = when (grade) { "S" -> colors.accentGold; "A" -> colors.accentGreen; "B" -> DesignTokens.Cosmic.PrimaryGlow; "C" -> colors.accentGold; else -> colors.accentRed }
    Box(modifier = modifier.size(size).clip(CircleShape).background(Brush.radialGradient(listOf(gradeColor.copy(alpha = 0.2f), Color.Transparent))).border(3.dp, gradeColor.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) { Text(text = grade, fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.Black, fontFamily = SpaceGroteskFontFamily, color = gradeColor) }
}

@Composable
fun ThemedBackground(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    if (colors.mode == ThemeMode.COSMIC_PURPLE) CosmicBackground(modifier = modifier) else StageBackground(modifier = modifier)
}

@Composable
fun ThemedWaveformBars(modifier: Modifier = Modifier, barCount: Int = 24, intensity: Float = 0.7f, animated: Boolean = true) {
    val colors = LocalAppColors.current
    val phase by rememberInfiniteTransition().animateFloat(initialValue = 0f, targetValue = (2 * Math.PI).toFloat(), animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)))
    Canvas(modifier = modifier) {
        val barWidth = (size.width - 4f * (barCount - 1)) / barCount
        for (i in 0 until barCount) {
            val barHeight = size.height * (sin(i * 0.5f + (if (animated) phase else 0f)) * 0.35f * intensity + 0.55f * intensity).coerceIn(0.08f, 1f)
            drawRoundRect(brush = Brush.verticalGradient(colors = if (colors.mode == ThemeMode.COSMIC_PURPLE) listOf(colors.primaryGlow, colors.primary, colors.secondary) else listOf(DesignTokens.Stage.FireYellow, DesignTokens.Stage.FireOrange, DesignTokens.Stage.FlameRed), startY = (size.height - barHeight) / 2f, endY = (size.height + barHeight) / 2f), topLeft = Offset(i * (barWidth + 4f), (size.height - barHeight) / 2f), size = Size(barWidth, barHeight), cornerRadius = CornerRadius(barWidth / 2))
        }
    }
}
