package com.rhythmgame.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Central design tokens for "Guitar Hero / Stage Rock" design language.
 * All screens reference this object instead of local hardcoded colors.
 */
object DesignTokens {

    // ── Stage Colors (Guitar Hero theme) ─────────────────────────────────────
    object Stage {
        val FireOrange = Color(0xFFFF6B00)
        val FireYellow = Color(0xFFFFAA00)
        val FlameRed = Color(0xFFFF2D2D)
        val ElectricBlue = Color(0xFF00A8FF)
        val StageGold = Color(0xFFFFD700)
        val SpotlightWhite = Color(0xFFF5F0E8)
        val SteelGray = Color(0xFF3A3A4A)
        val DarkChrome = Color(0xFF252530)
        val CrowdDark = Color(0xFF0A0A12)
    }

    // ── Cosmic Purple Colors ─────────────────────────────────────────────────
    object Cosmic {
        val DeepBg = Color(0xFF0A0014)
        val PrimaryGlow = Color(0xFF8B5CF6)
        val SecondaryGlow = Color(0xFFA855F7)
        val BorderHighlight = Color(0xFF8B5CF6).copy(alpha = 0.4f)
        val CardBg = Color(0xFF0F051E).copy(alpha = 0.6f)
        val CardGlass = Color(0xFF140528).copy(alpha = 0.85f)
        val TextMain = Color(0xFFF0E6FF)
        val TextMuted = Color(0xFFA78BCC)
        val AccentGreen = Color(0xFF22C55E)
        val AccentGold = Color(0xFFFFAA00)
        val AccentRed = Color(0xFFEF4444)
        val ContainerLow = Color(0xFF120822)
        val ContainerHigh = Color(0xFF1A0E2E)
        val SurfaceBright = Color(0xFF2A1A3C)
    }

    // ── Neon Colors (kept for GameRenderer backward compat) ──────────────────
    object Neon {
        val Cyan = Color(0xFF50E1F9)
        val CyanDark = Color(0xFF00BCD4)
        val Purple = Color(0xFFC47FFF)
        val PurpleDark = Color(0xFF9C27B0)
        val Gold = Color(0xFFFFE792)
        val GoldBright = Color(0xFFFFD700)
        val Red = Color(0xFFFF5252)
        val Green = Color(0xFF4ADE80)
    }

    // ── Surface Colors ────────────────────────────────────────────────────────
    object Surface {
        val Void = Color(0xFF080810)
        val ContainerLow = Color(0xFF141420)
        val ContainerHigh = Color(0xFF1E1E2E)
        val Bright = Color(0xFF2A2A3C)
        val Tint = Stage.FireOrange
    }

    // ── Text Colors ───────────────────────────────────────────────────────────
    object Text {
        val Primary = Color(0xFFF5F0E8)
        val Secondary = Color(0xFFB0A8C0)
        val Disabled = Color(0xFF514067)
    }

    // ── Outline / Border ──────────────────────────────────────────────────────
    val GhostBorder = Color(0xFF514067).copy(alpha = 0.15f)

    // ── Lane Colors (for game highway - UNCHANGED) ───────────────────────────
    object Lane {
        val colors = listOf(
            Color(0xFF50E1F9),  // Lane 1 - Cyan
            Color(0xFFC47FFF),  // Lane 2 - Purple
            Color(0xFFFFE792),  // Lane 3 - Gold
            Color(0xFF4ADE80),  // Lane 4 - Green
            Color(0xFFFF6B6B),  // Lane 5 - Coral
        )
        val glowColors = listOf(
            Color(0xFF80EEFF),  // Lane 1 glow
            Color(0xFFD9A8FF),  // Lane 2 glow
            Color(0xFFFFF0B3),  // Lane 3 glow
            Color(0xFF86EFAC),  // Lane 4 glow
            Color(0xFFFF9999),  // Lane 5 glow
        )
    }

    // ── Difficulty Colors ─────────────────────────────────────────────────────
    object Difficulty {
        val Easy = Color(0xFF4ADE80)
        val Medium = Color(0xFFFFAA00)
        val Hard = Color(0xFFFF2D2D)
    }

    // ── Spacing ───────────────────────────────────────────────────────────────
    val spacing4 = 4.dp
    val spacing8 = 8.dp
    val spacing12 = 12.dp
    val spacing16 = 16.dp
    val spacing20 = 20.dp
    val spacing24 = 24.dp
    val spacing32 = 32.dp
    val spacing48 = 48.dp

    // ── Radius ────────────────────────────────────────────────────────────────
    val radiusSmall = 8.dp
    val radiusMedium = 16.dp
    val radiusLarge = 24.dp

    // ── Gradients ─────────────────────────────────────────────────────────────
    val fireGradient = Brush.horizontalGradient(
        colors = listOf(Stage.FireYellow, Stage.FireOrange, Stage.FlameRed)
    )

    val fireVerticalGradient = Brush.verticalGradient(
        colors = listOf(Stage.FireYellow, Stage.FireOrange, Stage.FlameRed)
    )

    val chromeGradient = Brush.verticalGradient(
        colors = listOf(
            Stage.SteelGray,
            Stage.DarkChrome,
            Stage.SteelGray.copy(alpha = 0.6f),
        )
    )

    val spotlightGlow = Brush.radialGradient(
        colors = listOf(
            Stage.FireOrange.copy(alpha = 0.12f),
            Stage.FireYellow.copy(alpha = 0.05f),
            Color.Transparent,
        )
    )

    val stageBackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0C0812),
            Stage.CrowdDark,
            Color(0xFF0A0610),
        )
    )

    val cosmicBackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Cosmic.DeepBg,
            Color(0xFF0D0020),
            Cosmic.DeepBg,
        )
    )

    val cosmicPrimaryGradient = Brush.horizontalGradient(
        colors = listOf(Cosmic.PrimaryGlow, Cosmic.SecondaryGlow)
    )

    val cosmicCardGradient = Brush.verticalGradient(
        colors = listOf(
            Cosmic.CardGlass,
            Cosmic.CardBg,
        )
    )

    // Legacy compat
    val primaryGradient = fireGradient

    val secondaryGradient = Brush.horizontalGradient(
        colors = listOf(Stage.DarkChrome, Stage.SteelGray)
    )

    val heroGradient = Brush.verticalGradient(
        colors = listOf(
            Stage.FireOrange.copy(alpha = 0.3f),
            Surface.Void,
        )
    )

    val cardGlow = Brush.horizontalGradient(
        colors = listOf(
            Stage.FireOrange.copy(alpha = 0.1f),
            Stage.StageGold.copy(alpha = 0.05f),
        )
    )
}

/**
 * Modifier extension for tinted glow shadow.
 */
fun Modifier.glowShadow(
    color: Color = DesignTokens.Stage.FireOrange,
    alpha: Float = 0.15f,
    radius: Dp = 16.dp,
): Modifier = this.drawBehind {
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = radius.toPx(),
    )
}

/**
 * Beveled 3D surface effect for buttons.
 */
fun Modifier.beveledSurface(
    highlightColor: Color = DesignTokens.Stage.SteelGray,
): Modifier = this
    .drawBehind {
        // Top highlight edge
        drawRect(
            color = highlightColor.copy(alpha = 0.3f),
            size = size.copy(height = 2f),
        )
        // Bottom shadow edge
        drawRect(
            color = Color.Black.copy(alpha = 0.4f),
            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 2f),
            size = size.copy(height = 2f),
        )
    }

/**
 * Fire glow effect drawn behind content.
 */
fun Modifier.fireGlow(
    color: Color = DesignTokens.Stage.FireOrange,
    alpha: Float = 0.2f,
    radius: Dp = 32.dp,
): Modifier = this.drawBehind {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.3f),
                Color.Transparent,
            ),
        ),
        radius = radius.toPx(),
    )
}
