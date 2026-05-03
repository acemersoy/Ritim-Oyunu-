package com.rhythmgame.ui.screens

import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rhythmgame.data.local.AchievementEntity
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
    resultViewModel: ResultViewModel = hiltViewModel(),
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
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

    val xpEarned by resultViewModel.xpEarned.collectAsState()
    val starsEarned by resultViewModel.starsEarned.collectAsState()
    val coinsEarned by resultViewModel.coinsEarned.collectAsState()
    val xpProgress by resultViewModel.xpProgress.collectAsState()
    val newAchievements by resultViewModel.newAchievements.collectAsState()

    LaunchedEffect(Unit) {
        resultViewModel.computeAndPersistRewards(
            songId = songId,
            difficulty = difficulty,
            accuracy = accuracy,
            maxCombo = maxCombo,
            perfectCount = perfect,
            totalNotes = totalNotes,
        )
    }

    // Play achievement sound when new achievements arrive
    LaunchedEffect(newAchievements) {
        if (newAchievements.isNotEmpty()) {
            try {
                // Looks for res/raw/achievement_unlock (.mp3/.ogg/.wav)
                // Add your custom sound file there — the app plays it automatically
                val resId = context.resources.getIdentifier(
                    "achievement_unlock", "raw", context.packageName,
                )
                if (resId != 0) {
                    MediaPlayer.create(context, resId)?.apply {
                        setOnCompletionListener { release() }
                        start()
                    }
                }
            } catch (_: Exception) { }
        }
    }

    var showDifficultySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Staggered animations - can be skipped by tapping
    val gradeAlpha = remember { Animatable(0f) }
    val scoreAnim = remember { Animatable(0f) }
    val statsAlpha = remember { Animatable(0f) }
    val buttonsAlpha = remember { Animatable(0f) }

    var animatedScore by remember { mutableIntStateOf(0) }
    var animationSkipped by remember { mutableStateOf(false) }

    fun skipAnimations() {
        if (animationSkipped) return
        animationSkipped = true
        animatedScore = score
    }

    LaunchedEffect(animationSkipped) {
        if (animationSkipped) {
            gradeAlpha.snapTo(1f)
            scoreAnim.snapTo(1f)
            statsAlpha.snapTo(1f)
            buttonsAlpha.snapTo(1f)
        }
    }

    LaunchedEffect(Unit) { gradeAlpha.animateTo(1f, tween(600)) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        scoreAnim.animateTo(1f, tween(600))
    }
    LaunchedEffect(score, animationSkipped) {
        if (animationSkipped) { animatedScore = score; return@LaunchedEffect }
        kotlinx.coroutines.delay(200)
        val startTime = withFrameNanos { it }
        val durationNanos = 1_500_000_000L
        while (true) {
            if (animationSkipped) { animatedScore = score; break }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { skipAnimations() },
    ) {
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

                // XP + Stars earned section
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
                    ThemedProgressBar(progress = xpProgress)
                    if (starsEarned > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "KAZANILAN YILDIZ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ManropeFontFamily,
                                color = colors.textMuted,
                                letterSpacing = 2.sp,
                            )
                            Text(
                                "+$starsEarned \u2B50",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGroteskFontFamily,
                                color = colors.accentGold,
                            )
                        }
                    }
                    if (coinsEarned > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "KAZANILAN COIN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ManropeFontFamily,
                                color = colors.textMuted,
                                letterSpacing = 2.sp,
                            )
                            Text(
                                "+$coinsEarned",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGroteskFontFamily,
                                color = colors.primary,
                            )
                        }
                    }
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

        // Achievement toast overlay — top-right
        AchievementToastOverlay(
            achievements = newAchievements,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  ACHIEVEMENT TOAST — slides in from right, shows each unlocked achievement
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun AchievementToastOverlay(
    achievements: List<AchievementEntity>,
    modifier: Modifier = Modifier,
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(achievements) {
        if (achievements.isEmpty()) return@LaunchedEffect
        // Wait for the result screen entrance animations to settle
        kotlinx.coroutines.delay(2000)
        for (i in achievements.indices) {
            currentIndex = i
            visible = true
            kotlinx.coroutines.delay(3500)
            visible = false
            kotlinx.coroutines.delay(500)
        }
    }

    val achievement = achievements.getOrNull(currentIndex)

    AnimatedVisibility(
        visible = visible && achievement != null,
        enter = slideInHorizontally(tween(400)) { it } + fadeIn(tween(400)),
        exit = slideOutHorizontally(tween(350)) { it } + fadeOut(tween(350)),
        modifier = modifier,
    ) {
        if (achievement != null) {
            AchievementToastCard(achievement)
        }
    }
}

@Composable
private fun AchievementToastCard(achievement: AchievementEntity) {
    val categoryColor = when (achievement.category) {
        "easy" -> DesignTokens.Difficulty.Easy
        "medium" -> DesignTokens.Difficulty.Medium
        "hard" -> DesignTokens.Difficulty.Hard
        else -> DesignTokens.Stage.StageGold
    }

    Row(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF1A0E00).copy(alpha = 0.92f),
                        Color(0xFF2A1500).copy(alpha = 0.95f),
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        DesignTokens.Stage.StageGold.copy(alpha = 0.6f),
                        DesignTokens.Stage.FireOrange.copy(alpha = 0.3f),
                    )
                ),
                RoundedCornerShape(14.dp),
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Trophy icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = DesignTokens.Stage.StageGold,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "BASARIM ACILDI!",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ManropeFontFamily,
                color = DesignTokens.Stage.StageGold,
                letterSpacing = 1.5.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                achievement.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGroteskFontFamily,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                achievement.description,
                fontSize = 10.sp,
                fontFamily = ManropeFontFamily,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
