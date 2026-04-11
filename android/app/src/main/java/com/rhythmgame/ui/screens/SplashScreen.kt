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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rhythmgame.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val colors = LocalAppColors.current

    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    val ringScale = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f))
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(800))
    }
    LaunchedEffect(Unit) {
        ringScale.animateTo(1.2f, tween(1200, easing = EaseOut))
    }
    LaunchedEffect(Unit) {
        delay(2200)
        onSplashFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "particle_phase",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundGradient),
        contentAlignment = Alignment.Center,
    ) {
        // Particle ring
        Canvas(modifier = Modifier.size(300.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val ringR = size.width * 0.4f

            for (i in 0 until 24) {
                val angle = (i * Math.PI.toFloat() * 2f / 24f) + particlePhase * 0.5f
                val px = cx + cos(angle) * ringR
                val py = cy + sin(angle) * ringR
                val dotAlpha = 0.3f + 0.3f * sin(particlePhase + i * 0.5f)

                drawCircle(
                    color = colors.primary.copy(alpha = dotAlpha.coerceIn(0f, 0.6f)),
                    radius = 2f + (i % 3),
                    center = Offset(px, py),
                )
            }
        }

        // Center icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            colors.primary.copy(alpha = 0.3f),
                            Color.Transparent,
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = colors.primary,
            )
        }

        // Title below icon
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 160.dp)
                .graphicsLayer { this.alpha = alpha.value },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "RHYTHM",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGroteskFontFamily,
                color = colors.primary,
                letterSpacing = 6.sp,
            )
            Text(
                "GAME",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGroteskFontFamily,
                color = colors.textMain,
                letterSpacing = 8.sp,
            )
        }
    }
}
