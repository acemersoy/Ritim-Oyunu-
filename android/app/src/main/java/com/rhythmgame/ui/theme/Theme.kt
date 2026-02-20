package com.rhythmgame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.rhythmgame.R

val Purple = Color(0xFF9C27B0)
val PurpleDark = Color(0xFF6A1B9A)
val Cyan = Color(0xFF00BCD4)
val CyanDark = Color(0xFF00838F)
val Pink = Color(0xFFE91E63)
val Orange = Color(0xFFFF9800)
val Green = Color(0xFF4CAF50)
val Yellow = Color(0xFFFFEB3B)
val DarkBg = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)

// Lane colors for the 5 lanes
val LaneColors = listOf(
    Color(0xFF4CAF50),  // Lane 1 - Green
    Color(0xFFFF5722),  // Lane 2 - Red/Orange
    Color(0xFFFFEB3B),  // Lane 3 - Yellow
    Color(0xFF2196F3),  // Lane 4 - Blue
    Color(0xFFFF9800),  // Lane 5 - Orange
)

// Google Fonts provider
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

// Rajdhani - a futuristic, clean font great for gaming UIs
private val rajdhaniFont = GoogleFont("Rajdhani")

val RajdhaniFontFamily = FontFamily(
    Font(googleFont = rajdhaniFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = rajdhaniFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = rajdhaniFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = rajdhaniFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = rajdhaniFont, fontProvider = provider, weight = FontWeight.Bold),
)

// Orbitron - a bold futuristic display font for titles and scores
private val orbitronFont = GoogleFont("Orbitron")

val OrbitronFontFamily = FontFamily(
    Font(googleFont = orbitronFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = orbitronFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = orbitronFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = orbitronFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = orbitronFont, fontProvider = provider, weight = FontWeight.ExtraBold),
)

private val AppTypography = Typography(
    // Display styles - Orbitron for big titles
    displayLarge = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
    ),
    // Headline styles - Orbitron
    headlineLarge = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
    ),
    // Title styles - Rajdhani
    titleLarge = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    ),
    // Body styles - Rajdhani
    bodyLarge = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
    // Label styles - Rajdhani
    labelLarge = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = RajdhaniFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
    ),
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple,
    secondary = Cyan,
    tertiary = Pink,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun RhythmGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}
