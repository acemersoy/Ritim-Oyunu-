package com.rhythmgame.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
) {
    // --- Colors ---
    val purple = Color(0xFF7C3AED)
    val cyan = Color(0xFF00BCD4)
    val bgTop = Color(0xFF0A0A1A)
    val bgBottom = Color(0xFF1A0A2E)

    // --- Entrance animations ---
    val iconScale = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val bottomAlpha = remember { Animatable(0f) }
    val loadingProgress = remember { Animatable(0f) }

    // --- Infinite (looping) animations ---
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Pulsing ring: scales 0.8x - 1.3x
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ringScale",
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ringAlpha",
    )

    // Floating particles phase
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particlePhase",
    )

    // --- Orchestrated launch sequence ---
    LaunchedEffect(Unit) {
        // 1. Icon bounces in
        iconScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
        // 2. Title fades in
        titleAlpha.animateTo(1f, animationSpec = tween(500))
        // 3. Subtitle fades in
        subtitleAlpha.animateTo(1f, animationSpec = tween(400))
        // 4. Bottom section fades in
        bottomAlpha.animateTo(1f, animationSpec = tween(400))
        // 5. Loading bar fills up
        loadingProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = EaseInOut),
        )
        // 6. Brief pause, then navigate
        delay(300)
        onSplashFinished()
    }

    // ===================== UI =====================
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgTop, bgBottom),
                )
            ),
    ) {
        // ---------- Background floating particles ----------
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            for (i in 0 until 16) {
                val speed = if (i % 2 == 0) 1f else -0.6f
                val angle =
                    (i * Math.PI.toFloat() * 2f / 16f) + particlePhase * speed
                val baseRadius = 160f + i * 22f
                val wobble = sin(particlePhase + i * 0.7f) * 35f
                val radius = baseRadius + wobble
                val px = cx + cos(angle) * radius
                val py = cy + sin(angle) * radius
                val dotSize = 2.5f + (i % 4) * 1.5f
                val alpha = 0.12f + 0.18f * sin(particlePhase + i * 0.5f)
                val color = if (i % 2 == 0) purple else cyan
                drawCircle(
                    color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
                    radius = dotSize,
                    center = Offset(px, py),
                )
            }
        }

        // ---------- Center + bottom content ----------
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ---- Icon area with pulsing ring ----
            Box(contentAlignment = Alignment.Center) {
                // Pulsing cyan glow ring
                Canvas(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(ringScale)
                        .alpha(ringAlpha),
                ) {
                    drawCircle(
                        color = cyan,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 4f),
                    )
                    // Soft outer glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                cyan.copy(alpha = 0.25f),
                                cyan.copy(alpha = 0.05f),
                                Color.Transparent,
                            ),
                        ),
                        radius = size.minDimension / 2 + 12f,
                    )
                }

                // Purple filled circle with white music note
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(iconScale.value)
                        .clip(CircleShape)
                        .background(purple),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = Color.White,
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // ---- RHYTHM text ----
            Text(
                text = "RHYTHM",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 8.sp,
                modifier = Modifier.alpha(titleAlpha.value),
            )

            // ---- GAME text ----
            Text(
                text = "GAME",
                fontSize = 40.sp,
                fontWeight = FontWeight.Light,
                color = cyan,
                letterSpacing = 12.sp,
                modifier = Modifier.alpha(titleAlpha.value),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ---- Feel the beat subtitle ----
            Text(
                text = "Feel the beat",
                fontSize = 14.sp,
                color = purple.copy(alpha = 0.85f),
                letterSpacing = 4.sp,
                modifier = Modifier.alpha(subtitleAlpha.value),
            )

            Spacer(modifier = Modifier.height(64.dp))

            // ---- Bottom loading section ----
            Column(
                modifier = Modifier.alpha(bottomAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Loading progress bar (cyan-to-purple gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(4.dp),
                ) {
                    // Track background
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.08f),
                            cornerRadius = CornerRadius(4f, 4f),
                            size = Size(size.width, size.height),
                        )
                    }
                    // Filled portion
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val filledWidth = size.width * loadingProgress.value
                        if (filledWidth > 0f) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(cyan, purple),
                                    startX = 0f,
                                    endX = size.width,
                                ),
                                cornerRadius = CornerRadius(4f, 4f),
                                size = Size(filledWidth, size.height),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // "LOADING PERFORMANCE DATA"
                Text(
                    text = "LOADING PERFORMANCE DATA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray.copy(alpha = 0.7f),
                    letterSpacing = 3.sp,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Version info
                Text(
                    text = "VER 2.4.0 \u2022 STUDIO NEON",
                    fontSize = 9.sp,
                    color = Color.Gray.copy(alpha = 0.45f),
                    letterSpacing = 2.sp,
                )
            }
        }
    }
}
