package com.rhythmgame.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rhythmgame.ui.components.*
import com.rhythmgame.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    songId: String,
    difficulty: String,
    score: Int,
    maxCombo: Int,
    perfect: Int,
    great: Int,
    good: Int,
    miss: Int,
    overpress: Int = 0,
    onPlayAgain: (String) -> Unit,
    onHome: () -> Unit,
) {
    val colors = LocalAppColors.current
    val totalNotes = perfect + great + good + miss

    val accuracy = if (totalNotes > 0) {
        (perfect * 100f + great * 75f + good * 50f) / (totalNotes * 100f) * 100f
    } else 0f

    val grade = when {
        accuracy >= 95f -> "S"
        accuracy >= 85f -> "A"
        accuracy >= 75f -> "B"
        accuracy >= 65f -> "C"
        else -> "D"
    }

    val xpEarned = (score / 100) + (perfect * 5) + (great * 3) + (good * 1)

    var showDifficultySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Staggered animations
    val gradeAlpha = remember { Animatable(0f) }
    val scoreAnim = remember { Animatable(0f) }
    val statsAlpha = remember { Animatable(0f) }
    val buttonsAlpha = remember { Animatable(0f) }

    var animatedScore by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { gradeAlpha.animateTo(1f, tween(600)) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        scoreAnim.animateTo(1f, tween(600))
    }
    LaunchedEffect(score) {
        kotlinx.coroutines.delay(200)
        val startTime = withFrameNanos { it }
        val durationNanos = 1_500_000_000L
        while (true) {
            val elapsed = withFrameNanos { it } - startTime
            val progress = (elapsed.toFloat() / durationNanos).coerceIn(0f, 1f)
            val easedProgress = 1f - (1f - progress) * (1f - progress)
            animatedScore = (score * easedProgress).toInt()
            if (progress >= 1f) break
        }
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(800)
        statsAlpha.animateTo(1f, tween(600))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200)
        buttonsAlpha.animateTo(1f, tween(500))
    }

    val formattedScore = remember(animatedScore) {
        NumberFormat.getNumberInstance(Locale.US).format(animatedScore)
    }

    // Difficulty selection bottom sheet
    if (showDifficultySheet) {
        ModalBottomSheet(
            onDismissRequest = { showDifficultySheet = false },
            sheetState = sheetState,
            containerColor = colors.cardGlass,
            contentColor = colors.textMain,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(40.dp, 4.dp)
                        .background(colors.primary.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "ZORLUK SEC",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    color = colors.primary,
                    letterSpacing = 3.sp,
                )
                Spacer(modifier = Modifier.height(24.dp))
                DifficultyOption("KOLAY", "3 serit, ritim ustu notalar", DesignTokens.Difficulty.Easy) {
                    onPlayAgain("easy"); showDifficultySheet = false
                }
                Spacer(modifier = Modifier.height(10.dp))
                DifficultyOption("ORTA", "5 serit, sekizlik notalar", DesignTokens.Difficulty.Medium) {
                    onPlayAgain("medium"); showDifficultySheet = false
                }
                Spacer(modifier = Modifier.height(10.dp))
                DifficultyOption("ZOR", "5 serit, tum notalar + akorlar", DesignTokens.Difficulty.Hard) {
                    onPlayAgain("hard"); showDifficultySheet = false
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ThemedBackground(modifier = Modifier.fillMaxSize())

        if (colors.mode == ThemeMode.STAGE_ROCK && grade in listOf("S", "A") && gradeAlpha.value > 0.5f) {
            FlameEffect(
                modifier = Modifier.fillMaxSize(),
                intensity = if (grade == "S") 0.8f else 0.4f,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Text(
                "TAMAMLANDI",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ManropeFontFamily,
                color = colors.textMuted,
                letterSpacing = 3.sp,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Difficulty badge
            Text(
                text = when (difficulty) {
                    "easy" -> "KOLAY"
                    "hard" -> "ZOR"
                    else -> "ORTA"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ManropeFontFamily,
                color = when (difficulty) {
                    "easy" -> colors.accentGreen
                    "hard" -> colors.accentRed
                    else -> colors.accentGold
                },
                letterSpacing = 2.sp,
                modifier = Modifier
                    .background(
                        when (difficulty) {
                            "easy" -> colors.accentGreen.copy(alpha = 0.12f)
                            "hard" -> colors.accentRed.copy(alpha = 0.12f)
                            else -> colors.accentGold.copy(alpha = 0.12f)
                        },
                        RoundedCornerShape(6.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Grade circle
            if (gradeAlpha.value > 0f) {
                GradeCircle(
                    grade = grade,
                    modifier = Modifier.graphicsLayer {
                        alpha = gradeAlpha.value
                        scaleX = 0.8f + gradeAlpha.value * 0.2f
                        scaleY = 0.8f + gradeAlpha.value * 0.2f
                    },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Score
            if (scoreAnim.value > 0f) {
                Text(
                    "SKOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ManropeFontFamily,
                    color = colors.textMuted.copy(alpha = scoreAnim.value),
                    letterSpacing = 4.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formattedScore,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    color = colors.textMain.copy(alpha = scoreAnim.value),
                    letterSpacing = 2.sp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row
            if (statsAlpha.value > 0f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = statsAlpha.value },
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatItem("DOGRULUK", "${String.format("%.1f", accuracy)}%")
                    StatItem("MAKS KOMBO", "${maxCombo}x")
                    StatItem("TOPLAM NOTA", "$totalNotes")
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Hit breakdown card
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = statsAlpha.value },
                ) {
                    HitBreakdownRow("Perfect", perfect, colors.accentGold)
                    HitBreakdownRow("Great", great, colors.primary)
                    HitBreakdownRow("Good", good, colors.accentGreen)
                    HitBreakdownRow("Miss", miss, colors.accentRed)
                    if (overpress > 0) {
                        HitBreakdownRow("Overpress", overpress, colors.accentRed.copy(alpha = 0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // XP earned section
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = statsAlpha.value },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "KAZANILAN XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ManropeFontFamily,
                            color = colors.textMuted,
                            letterSpacing = 2.sp,
                        )
                        Text(
                            "+$xpEarned XP",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGroteskFontFamily,
                            color = colors.accentGreen,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemedProgressBar(progress = 0.65f)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            if (buttonsAlpha.value > 0f) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = buttonsAlpha.value },
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ThemedButton(
                        text = "TEKRAR OYNA",
                        onClick = { showDifficultySheet = true },
                        isPrimary = true,
                        height = 56.dp,
                        fontSize = 16.sp,
                        icon = Icons.Default.Refresh,
                    )
                    ThemedButton(
                        text = "ANA MENU",
                        onClick = onHome,
                        isPrimary = false,
                        height = 48.dp,
                        fontSize = 14.sp,
                        icon = Icons.Default.Home,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SpaceGroteskFontFamily,
            color = colors.textMain,
        )
        Text(
            label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ManropeFontFamily,
            color = colors.textMuted,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun DifficultyOption(
    name: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGroteskFontFamily,
                color = color,
                letterSpacing = 3.sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                description,
                fontSize = 12.sp,
                fontFamily = ManropeFontFamily,
                color = colors.textMuted,
            )
        }
    }
}
