package com.rhythmgame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.rhythmgame.R

// ── Theme Mode ───────────────────────────────────────────────────────────────
enum class ThemeMode { STAGE_ROCK, COSMIC_PURPLE }

// ── App Colors ───────────────────────────────────────────────────────────────
data class AppColors(
    val mode: ThemeMode,
    val primary: Color,
    val primaryGlow: Color,
    val secondary: Color,
    val cardBg: Color,
    val cardGlass: Color,
    val textMain: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val accentGreen: Color,
    val accentGold: Color,
    val accentRed: Color,
    val backgroundDeep: Color,
    val containerLow: Color,
    val containerHigh: Color,
    val surfaceBright: Color,
    val primaryGradient: Brush,
    val backgroundGradient: Brush,
    val buttonPrimaryBg: Brush,
    val buttonSecondaryBg: Brush,
    val cardGradient: Brush,
)

val stageRockColors = AppColors(
    mode = ThemeMode.STAGE_ROCK,
    primary = DesignTokens.Stage.FireOrange,
    primaryGlow = DesignTokens.Stage.FireYellow,
    secondary = DesignTokens.Stage.ElectricBlue,
    cardBg = DesignTokens.Stage.DarkChrome,
    cardGlass = DesignTokens.Stage.DarkChrome,
    textMain = DesignTokens.Text.Primary,
    textMuted = DesignTokens.Text.Secondary,
    textDisabled = DesignTokens.Text.Disabled,
    accentGreen = DesignTokens.Difficulty.Easy,
    accentGold = DesignTokens.Stage.StageGold,
    accentRed = DesignTokens.Stage.FlameRed,
    backgroundDeep = DesignTokens.Surface.Void,
    containerLow = DesignTokens.Surface.ContainerLow,
    containerHigh = DesignTokens.Surface.ContainerHigh,
    surfaceBright = DesignTokens.Surface.Bright,
    primaryGradient = DesignTokens.fireGradient,
    backgroundGradient = DesignTokens.stageBackgroundGradient,
    buttonPrimaryBg = Brush.horizontalGradient(
        listOf(DesignTokens.Stage.FireYellow, DesignTokens.Stage.FireOrange, DesignTokens.Stage.FlameRed.copy(alpha = 0.85f))
    ),
    buttonSecondaryBg = Brush.horizontalGradient(
        listOf(DesignTokens.Stage.SteelGray, DesignTokens.Stage.DarkChrome, DesignTokens.Stage.SteelGray.copy(alpha = 0.7f))
    ),
    cardGradient = Brush.verticalGradient(
        listOf(DesignTokens.Stage.DarkChrome, DesignTokens.Surface.ContainerLow)
    ),
)

val cosmicPurpleColors = AppColors(
    mode = ThemeMode.COSMIC_PURPLE,
    primary = DesignTokens.Cosmic.PrimaryGlow,
    primaryGlow = DesignTokens.Cosmic.SecondaryGlow,
    secondary = DesignTokens.Cosmic.SecondaryGlow,
    cardBg = DesignTokens.Cosmic.CardBg,
    cardGlass = DesignTokens.Cosmic.CardGlass,
    textMain = DesignTokens.Cosmic.TextMain,
    textMuted = DesignTokens.Cosmic.TextMuted,
    textDisabled = DesignTokens.Text.Disabled,
    accentGreen = DesignTokens.Cosmic.AccentGreen,
    accentGold = DesignTokens.Cosmic.AccentGold,
    accentRed = DesignTokens.Cosmic.AccentRed,
    backgroundDeep = DesignTokens.Cosmic.DeepBg,
    containerLow = DesignTokens.Cosmic.ContainerLow,
    containerHigh = DesignTokens.Cosmic.ContainerHigh,
    surfaceBright = DesignTokens.Cosmic.SurfaceBright,
    primaryGradient = DesignTokens.cosmicPrimaryGradient,
    backgroundGradient = DesignTokens.cosmicBackgroundGradient,
    buttonPrimaryBg = Brush.horizontalGradient(
        listOf(DesignTokens.Cosmic.PrimaryGlow, DesignTokens.Cosmic.SecondaryGlow)
    ),
    buttonSecondaryBg = Brush.horizontalGradient(
        listOf(DesignTokens.Cosmic.ContainerHigh, DesignTokens.Cosmic.CardGlass)
    ),
    cardGradient = DesignTokens.cosmicCardGradient,
)

val LocalAppColors = staticCompositionLocalOf { stageRockColors }

// ── Google Fonts ─────────────────────────────────────────────────────────────

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val oswaldFont = GoogleFont("Oswald")

private val OswaldFontFamily = FontFamily(
    Font(googleFont = oswaldFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = oswaldFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = oswaldFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = oswaldFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = oswaldFont, fontProvider = provider, weight = FontWeight.Bold),
)

val SpaceGroteskFontFamily = OswaldFontFamily

private val manropeFont = GoogleFont("Manrope")

val ManropeFontFamily = FontFamily(
    Font(googleFont = manropeFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = manropeFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = manropeFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = manropeFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = manropeFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = manropeFont, fontProvider = provider, weight = FontWeight.ExtraBold),
)

// Legacy aliases
val OrbitronFontFamily = SpaceGroteskFontFamily
val RajdhaniFontFamily = ManropeFontFamily

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = OswaldFontFamily, fontWeight = FontWeight.Bold, fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = OswaldFontFamily, fontWeight = FontWeight.Bold, fontSize = 45.sp),
    displaySmall = TextStyle(fontFamily = OswaldFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = OswaldFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = OswaldFontFamily, fontWeight = FontWeight.Medium, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = OswaldFontFamily, fontWeight = FontWeight.Medium, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = ManropeFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

// ── Color Schemes ────────────────────────────────────────────────────────────

private val StageRockColorScheme = darkColorScheme(
    primary = DesignTokens.Stage.FireOrange,
    primaryContainer = DesignTokens.Stage.FireYellow,
    secondary = DesignTokens.Stage.ElectricBlue,
    secondaryContainer = DesignTokens.Stage.ElectricBlue.copy(alpha = 0.7f),
    tertiary = DesignTokens.Stage.StageGold,
    background = DesignTokens.Surface.Void,
    surface = DesignTokens.Surface.Void,
    surfaceVariant = DesignTokens.Surface.ContainerLow,
    surfaceBright = DesignTokens.Surface.Bright,
    surfaceContainerLow = DesignTokens.Surface.ContainerLow,
    surfaceContainerHigh = DesignTokens.Surface.ContainerHigh,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = DesignTokens.Text.Primary,
    onSurface = DesignTokens.Text.Primary,
    onSurfaceVariant = DesignTokens.Text.Secondary,
    outlineVariant = DesignTokens.Stage.SteelGray,
    surfaceTint = DesignTokens.Surface.Tint,
    error = DesignTokens.Stage.FlameRed,
)

private val CosmicPurpleColorScheme = darkColorScheme(
    primary = DesignTokens.Cosmic.PrimaryGlow,
    primaryContainer = DesignTokens.Cosmic.SecondaryGlow,
    secondary = DesignTokens.Cosmic.SecondaryGlow,
    secondaryContainer = DesignTokens.Cosmic.SecondaryGlow.copy(alpha = 0.7f),
    tertiary = DesignTokens.Cosmic.AccentGold,
    background = DesignTokens.Cosmic.DeepBg,
    surface = DesignTokens.Cosmic.DeepBg,
    surfaceVariant = DesignTokens.Cosmic.ContainerLow,
    surfaceBright = DesignTokens.Cosmic.SurfaceBright,
    surfaceContainerLow = DesignTokens.Cosmic.ContainerLow,
    surfaceContainerHigh = DesignTokens.Cosmic.ContainerHigh,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DesignTokens.Cosmic.TextMain,
    onSurface = DesignTokens.Cosmic.TextMain,
    onSurfaceVariant = DesignTokens.Cosmic.TextMuted,
    outlineVariant = DesignTokens.Cosmic.PrimaryGlow.copy(alpha = 0.3f),
    surfaceTint = DesignTokens.Cosmic.PrimaryGlow,
    error = DesignTokens.Cosmic.AccentRed,
)

// ── Theme Composable ─────────────────────────────────────────────────────────

@Composable
fun RhythmGameTheme(
    themeMode: ThemeMode = ThemeMode.STAGE_ROCK,
    content: @Composable () -> Unit,
) {
    val appColors = when (themeMode) {
        ThemeMode.STAGE_ROCK -> stageRockColors
        ThemeMode.COSMIC_PURPLE -> cosmicPurpleColors
    }
    val colorScheme = when (themeMode) {
        ThemeMode.STAGE_ROCK -> StageRockColorScheme
        ThemeMode.COSMIC_PURPLE -> CosmicPurpleColorScheme
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
