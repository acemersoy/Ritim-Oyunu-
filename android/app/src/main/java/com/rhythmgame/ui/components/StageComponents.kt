package com.rhythmgame.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rhythmgame.R
import com.rhythmgame.ui.theme.DesignTokens
import com.rhythmgame.ui.theme.ManropeFontFamily
import com.rhythmgame.ui.theme.SpaceGroteskFontFamily
import com.rhythmgame.ui.theme.beveledSurface
import com.rhythmgame.ui.theme.fireGlow
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Horizontal electric-guitar body silhouette with 6 tuning pegs on the upper bout.
 * Left side = large lower bout (semicircle), middle = narrow waist, right side = smaller
 * upper bout with peg nubs sticking out.
 */
object GuitarBodyShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return Outline.Rectangle(Rect(0f, 0f, w, h))

        val rL = h / 2f
        val rR = (h * 0.34f).coerceAtMost(w * 0.18f)
        val rightCx = w - rR
        val rightCy = h / 2f
        val rightTop = rightCy - rR
        val rightBot = rightCy + rR

        val waistTopY = h * 0.22f
        val waistBotY = h - waistTopY

        val leftEdgeEndX = w * 0.30f
        val rightApproachX = rightCx - rR * 0.6f

        val p = Path().apply {
            // Start at top of left bout
            moveTo(rL, 0f)
            // Top edge of lower bout moving right
            lineTo(leftEdgeEndX, 0f)
            // Waist cut in (top)
            cubicTo(
                w * 0.40f, 0f,
                w * 0.43f, waistTopY,
                w * 0.50f, waistTopY,
            )
            // Approach upper bout (top)
            cubicTo(
                w * 0.57f, waistTopY,
                w * 0.60f, rightTop,
                rightApproachX, rightTop,
            )
            lineTo(rightCx, rightTop)
            // Right half-circle (upper bout)
            arcTo(
                rect = Rect(rightCx - rR, rightTop, rightCx + rR, rightBot),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )
            // Return from upper bout (bottom)
            lineTo(rightApproachX, rightBot)
            cubicTo(
                w * 0.60f, rightBot,
                w * 0.57f, waistBotY,
                w * 0.50f, waistBotY,
            )
            // Waist cut in (bottom)
            cubicTo(
                w * 0.43f, waistBotY,
                w * 0.40f, h,
                leftEdgeEndX, h,
            )
            lineTo(rL, h)
            // Left half-circle (lower bout)
            arcTo(
                rect = Rect(0f, 0f, 2 * rL, h),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )
            close()

            // 6 tuning pegs on the upper-bout rim (3 top + 3 bottom).
            val pegRadius = h * 0.085f
            val topAngles = floatArrayOf(60f, 90f, 120f)
            for (deg in topAngles) {
                val rad = Math.toRadians(deg.toDouble())
                val px = rightCx + rR * cos(rad).toFloat()
                val py = rightCy - rR * sin(rad).toFloat()
                addOval(Rect(px - pegRadius, py - pegRadius, px + pegRadius, py + pegRadius))
            }
            val botAngles = floatArrayOf(240f, 270f, 300f)
            for (deg in botAngles) {
                val rad = Math.toRadians(deg.toDouble())
                val px = rightCx + rR * cos(rad).toFloat()
                val py = rightCy - rR * sin(rad).toFloat()
                addOval(Rect(px - pegRadius, py - pegRadius, px + pegRadius, py + pegRadius))
            }
        }
        return Outline.Generic(p)
    }
}

@Composable
fun StageBackground(
    modifier: Modifier = Modifier,
    spotlightIntensity: Float = 0.12f,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stage_bg")
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing))
    )
    val spotlightPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = EaseInOut), repeatMode = RepeatMode.Reverse)
    )

    Box(modifier = modifier.background(DesignTokens.stageBackgroundGradient)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val topY = size.height * 0.05f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(DesignTokens.Stage.FireOrange.copy(alpha = spotlightIntensity * spotlightPulse), DesignTokens.Stage.FireYellow.copy(alpha = spotlightIntensity * 0.3f * spotlightPulse), Color.Transparent),
                    center = Offset(cx * 0.6f, topY), radius = size.height * 0.5f
                ),
                center = Offset(cx * 0.6f, topY), radius = size.height * 0.5f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(DesignTokens.Stage.FireYellow.copy(alpha = spotlightIntensity * 0.7f * spotlightPulse), Color.Transparent),
                    center = Offset(cx * 1.4f, topY), radius = size.height * 0.4f
                ),
                center = Offset(cx * 1.4f, topY), radius = size.height * 0.4f
            )
            for (i in 0 until 20) {
                val angle = (i * Math.PI.toFloat() * 2f / 20f) + particlePhase * (if (i % 2 == 0) 0.8f else -0.5f)
                val baseRadius = 140f + i * 18f
                val radius = baseRadius + sin(particlePhase + i * 0.6f) * 25f
                drawCircle(
                    color = (if (i % 3 == 0) DesignTokens.Stage.FireOrange else if (i % 3 == 1) DesignTokens.Stage.FireYellow else DesignTokens.Stage.StageGold).copy(alpha = (0.06f + 0.12f * sin(particlePhase + i * 0.4f)).coerceIn(0f, 0.3f)),
                    radius = 1.5f + (i % 3) * 1f,
                    center = Offset(cx + cos(angle) * radius, size.height * 0.5f + sin(angle) * radius * 0.6f)
                )
            }
        }
    }
}

private data class ShootingStar(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var length: Float, var alpha: Float, var life: Float,
    var maxLife: Float, var thickness: Float,
)

private fun spawnShootingStar(width: Float, height: Float, rng: Random): ShootingStar {
    val edge = rng.nextInt(3)
    val startX = if (edge == 1) -20f else rng.nextFloat() * width
    val startY = if (edge == 1) rng.nextFloat() * height * 0.5f else -20f
    val angleRad = Math.toRadians((30f + rng.nextFloat() * 50f).toDouble()).toFloat()
    val speed = 380f + rng.nextFloat() * 520f
    return ShootingStar(
        x = startX, y = startY, vx = cos(angleRad) * speed, vy = sin(angleRad) * speed,
        length = 60f + rng.nextFloat() * 90f, alpha = 0f, life = 0f,
        maxLife = 0.9f + rng.nextFloat() * 1.1f, thickness = 1.4f + rng.nextFloat() * 1.6f
    )
}

@Composable
fun NebulaMenuBackground(modifier: Modifier = Modifier, blurRadius: Dp = 14.dp, starCount: Int = 14) {
    val rng = remember { Random(System.currentTimeMillis()) }
    val stars = remember { mutableStateListOf<ShootingStar>() }
    var lastFrameNs by remember { mutableStateOf(0L) }
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                val dt = if (lastFrameNs == 0L) 0f else (now - lastFrameNs) / 1_000_000_000f
                lastFrameNs = now
                val clamped = dt.coerceIn(0f, 0.05f)
                if (stars.isEmpty()) repeat(starCount) { stars.add(spawnShootingStar(1000f, 1000f, rng).apply { life = rng.nextFloat() * maxLife * 0.6f }) }
                for (i in stars.indices) {
                    val s = stars[i]; s.life += clamped; s.x += s.vx * clamped; s.y += s.vy * clamped
                    val t = (s.life / s.maxLife).coerceIn(0f, 1f)
                    s.alpha = (if (t < 0.2f) (t / 0.2f) else (1f - (t - 0.2f) / 0.8f)).coerceIn(0f, 1f)
                    if (s.life >= s.maxLife || s.x > 2000f || s.y > 2000f) stars[i] = spawnShootingStar(1000f, 1000f, rng)
                }
                tick++
            }
        }
    }
    Box(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize().blur(blurRadius)) {
            Image(painter = painterResource(id = R.drawable.menu_nebula_bg), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Canvas(modifier = Modifier.fillMaxSize()) {
                @Suppress("UNUSED_EXPRESSION") tick
                for (s in stars) {
                    if (s.alpha <= 0.01f) continue
                    val mag = kotlin.math.sqrt(s.vx * s.vx + s.vy * s.vy).coerceAtLeast(0.0001f)
                    val tailX = s.x - (s.vx / mag) * s.length
                    val tailY = s.y - (s.vy / mag) * s.length
                    drawLine(brush = Brush.linearGradient(listOf(Color.Transparent, Color(0xFFFFE7B0).copy(alpha = s.alpha * 0.55f), Color.White.copy(alpha = s.alpha)), start = Offset(tailX, tailY), end = Offset(s.x, s.y)), start = Offset(tailX, tailY), end = Offset(s.x, s.y), strokeWidth = s.thickness * 3.0f, cap = StrokeCap.Round)
                    drawLine(brush = Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = s.alpha)), start = Offset(tailX, tailY), end = Offset(s.x, s.y)), start = Offset(tailX, tailY), end = Offset(s.x, s.y), strokeWidth = s.thickness * 1.4f, cap = StrokeCap.Round)
                    drawCircle(color = Color.White.copy(alpha = s.alpha), radius = s.thickness * 2.4f, center = Offset(s.x, s.y))
                    drawCircle(color = Color(0xFFFFE7B0).copy(alpha = s.alpha * 0.6f), radius = s.thickness * 4.2f, center = Offset(s.x, s.y))
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xCC000000), Color(0x66000000), Color(0xAA000000)))))
    }
}

enum class GuitarHeroButtonStyle { FIRE, CHROME }

@Composable
fun GuitarHeroButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, style: GuitarHeroButtonStyle = GuitarHeroButtonStyle.FIRE, height: Dp = 64.dp, fontSize: TextUnit = 20.sp, icon: @Composable (() -> Unit)? = null, enabled: Boolean = true) {
    val bgBrush = when (style) {
        GuitarHeroButtonStyle.FIRE -> Brush.horizontalGradient(listOf(DesignTokens.Stage.FireYellow, DesignTokens.Stage.FireOrange, DesignTokens.Stage.FlameRed.copy(alpha = 0.85f)))
        GuitarHeroButtonStyle.CHROME -> Brush.horizontalGradient(listOf(DesignTokens.Stage.SteelGray, DesignTokens.Stage.DarkChrome, DesignTokens.Stage.SteelGray.copy(alpha = 0.7f)))
    }
    Box(modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(DesignTokens.radiusMedium)).background(if (enabled) bgBrush else Brush.horizontalGradient(listOf(DesignTokens.Text.Disabled.copy(alpha = 0.3f), DesignTokens.Text.Disabled.copy(alpha = 0.2f)))).beveledSurface().then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) { icon(); Spacer(modifier = Modifier.width(10.dp)) }
            Text(text = text, fontSize = fontSize, fontWeight = FontWeight.Bold, fontFamily = SpaceGroteskFontFamily, color = if (enabled && style == GuitarHeroButtonStyle.FIRE) Color.Black else if (enabled) DesignTokens.Text.Primary else DesignTokens.Text.Disabled, letterSpacing = 3.sp)
        }
    }
}

@Composable
fun StarRating(starCount: Int, totalStars: Int = 5, starSize: Dp = 36.dp, animated: Boolean = true, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until totalStars) {
            val filled = i < starCount
            val scale = remember { Animatable(if (animated) 0f else 1f) }
            LaunchedEffect(filled) { if (animated && filled) { delay(i * 120L); scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) } }
            Icon(imageVector = if (filled) Icons.Filled.Star else Icons.Filled.StarOutline, contentDescription = null, tint = if (filled) DesignTokens.Stage.StageGold else DesignTokens.Text.Disabled, modifier = Modifier.size(starSize).graphicsLayer { scaleX = scale.value; scaleY = scale.value }.then(if (filled) Modifier.fireGlow(DesignTokens.Stage.StageGold, 0.15f, starSize / 2) else Modifier))
        }
    }
}

@Composable
fun FlameEffect(modifier: Modifier = Modifier, intensity: Float = 1f) {
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val phase by infiniteTransition.animateFloat(initialValue = 0f, targetValue = (2 * Math.PI).toFloat(), animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)))
    Canvas(modifier = modifier) {
        for (i in 0 until 12) {
            val flameHeight = size.height * 0.4f * ((sin(phase * 1.5f + i * 0.8f) * 0.3f + 0.7f) * intensity)
            drawCircle(brush = Brush.verticalGradient(listOf(DesignTokens.Stage.FireYellow.copy(alpha = 0.15f * intensity), DesignTokens.Stage.FireOrange.copy(alpha = 0.1f * intensity), Color.Transparent), startY = size.height - flameHeight, endY = size.height), radius = size.width / 8f, center = Offset(size.width / 2f + (i - 6) * (size.width / 12f), size.height - flameHeight * 0.5f))
        }
    }
}

@Composable
fun MusicalNotesBackground(modifier: Modifier = Modifier, noteCount: Int = 14, intensity: Float = 0.1f) {
    val noteSymbols = listOf("\u2669", "\u266A", "\u266B", "\u266C")
    val phase by rememberInfiniteTransition().animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing)))
    Canvas(modifier = modifier) {
        val paint = android.graphics.Paint().apply { isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER }
        for (i in 0 until noteCount) {
            val x = size.width * ((i * 137 + 73) % 100 / 100f) + sin(phase * Math.PI.toFloat() * 2f + i * 0.8f) * 20f
            val y = size.height * (1f - ((phase * (0.5f + (i * 137 % 50) / 100f) + i * 0.07f) % 1f))
            val color = if (i % 3 == 0) DesignTokens.Stage.FireOrange else if (i % 3 == 1) DesignTokens.Stage.StageGold else DesignTokens.Stage.FireYellow
            paint.color = android.graphics.Color.argb(((intensity * (0.5f + 0.5f * sin(phase * Math.PI.toFloat() * 2f + i * 1.2f))).coerceIn(0.03f, 0.18f) * 255).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
            paint.textSize = 16f + (i * 137 % 12)
            drawContext.canvas.nativeCanvas.save(); drawContext.canvas.nativeCanvas.rotate(sin(phase * Math.PI.toFloat() * 2f + i * 0.5f) * 15f, x, y); drawContext.canvas.nativeCanvas.drawText(noteSymbols[i % noteSymbols.size], x, y, paint); drawContext.canvas.nativeCanvas.restore()
        }
    }
}

@Composable
fun GalaxySwirl(modifier: Modifier = Modifier, arms: Int = 3, intensity: Float = 0.15f) {
    val rotation by rememberInfiniteTransition().animateFloat(initialValue = 0f, targetValue = (2 * Math.PI).toFloat(), animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)))
    Canvas(modifier = modifier) {
        val maxRadius = minOf(size.width, size.height) * 0.45f
        for (arm in 0 until arms) {
            for (p in 0 until 35) {
                val t = p.toFloat() / 35; val angle = t * 3f * Math.PI.toFloat() + arm * (2 * Math.PI.toFloat() / arms) + rotation; val r = t * maxRadius
                val color = if (p % 3 == 0) DesignTokens.Stage.FireOrange else if (p % 3 == 1) DesignTokens.Stage.StageGold else DesignTokens.Stage.FireYellow
                drawCircle(color = color.copy(alpha = (intensity * (1f - t * 0.6f).coerceIn(0.2f, 1f) * (0.6f + 0.4f * sin(rotation * 2f + p * 0.3f))).coerceIn(0.02f, 0.2f)), radius = (1.5f + t * 2.5f) * (0.8f + 0.2f * sin(rotation + p * 0.5f)), center = Offset(size.width / 2f + cos(angle) * r, size.height / 2f + sin(angle) * r))
            }
        }
        drawCircle(brush = Brush.radialGradient(listOf(DesignTokens.Stage.StageGold.copy(alpha = intensity * 0.4f), DesignTokens.Stage.FireOrange.copy(alpha = intensity * 0.15f), Color.Transparent), center = Offset(size.width / 2f, size.height / 2f), radius = maxRadius * 0.25f), center = Offset(size.width / 2f, size.height / 2f), radius = maxRadius * 0.25f)
    }
}

@Composable
fun WaveEqualizerBars(modifier: Modifier = Modifier, barCount: Int = 24, intensity: Float = 0.7f, animated: Boolean = true) {
    val phase by rememberInfiniteTransition().animateFloat(initialValue = 0f, targetValue = (2 * Math.PI).toFloat(), animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)))
    Canvas(modifier = modifier) {
        val barWidth = (size.width - 4f * (barCount - 1)) / barCount
        for (i in 0 until barCount) {
            val barHeight = size.height * (sin(i * 0.5f + (if (animated) phase else 0f)) * 0.35f * intensity + 0.55f * intensity).coerceIn(0.08f, 1f)
            drawRoundRect(brush = Brush.verticalGradient(listOf(DesignTokens.Stage.FireYellow, DesignTokens.Stage.FireOrange, DesignTokens.Stage.FlameRed), startY = (size.height - barHeight) / 2f, endY = (size.height + barHeight) / 2f), topLeft = Offset(i * (barWidth + 4f), (size.height - barHeight) / 2f), size = Size(barWidth, barHeight), cornerRadius = CornerRadius(barWidth / 2))
        }
    }
}

@Composable
fun SeasonBanner(seasonNumber: Int, subtitle: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(44.dp).background(Brush.horizontalGradient(listOf(DesignTokens.Stage.FlameRed.copy(alpha = 0.8f), DesignTokens.Stage.FireOrange.copy(alpha = 0.9f), DesignTokens.Stage.FireYellow.copy(alpha = 0.8f)))).drawBehind { drawRect(color = DesignTokens.Stage.StageGold.copy(alpha = 0.4f), size = size.copy(height = 2f)); drawRect(color = DesignTokens.Stage.StageGold.copy(alpha = 0.3f), topLeft = Offset(0f, size.height - 2f), size = size.copy(height = 2f)) }, contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(text = "\uD83D\uDD25", fontSize = 14.sp); Spacer(modifier = Modifier.width(8.dp))
            Text(text = "SEASON $seasonNumber: $subtitle", fontSize = 13.sp, fontWeight = FontWeight.Black, fontFamily = SpaceGroteskFontFamily, color = Color.Black, letterSpacing = 3.sp); Spacer(modifier = Modifier.width(8.dp)); Text(text = "\uD83D\uDD25", fontSize = 14.sp)
        }
    }
}

@Composable
fun LeaderboardEntry(rank: Int, name: String, score: Int, isCurrentUser: Boolean = false, modifier: Modifier = Modifier) {
    val accent = when (rank) { 1 -> DesignTokens.Stage.StageGold; 2 -> DesignTokens.Stage.SpotlightWhite; 3 -> DesignTokens.Stage.FireOrange; else -> DesignTokens.Stage.SteelGray }
    Row(modifier = modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(DesignTokens.radiusSmall)).background(if (isCurrentUser) DesignTokens.Stage.FireOrange.copy(alpha = 0.1f) else DesignTokens.Stage.DarkChrome).drawBehind { drawRect(color = accent, size = Size(4f, size.height)); if (rank <= 3) drawCircle(color = accent.copy(alpha = 0.08f), radius = size.height * 0.6f, center = Offset(0f, size.height / 2f)) }.padding(start = 12.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = "#$rank", fontSize = if (rank <= 3) 18.sp else 14.sp, fontWeight = FontWeight.Black, fontFamily = SpaceGroteskFontFamily, color = accent, modifier = Modifier.width(40.dp))
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(if (rank <= 3) Brush.linearGradient(listOf(accent.copy(alpha = 0.4f), accent.copy(alpha = 0.2f))) else Brush.linearGradient(listOf(DesignTokens.Stage.SteelGray, DesignTokens.Stage.DarkChrome))), contentAlignment = Alignment.Center) { Text(text = name.take(1).uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGroteskFontFamily, color = if (rank <= 3) Color.Black else DesignTokens.Text.Secondary) }
        Spacer(modifier = Modifier.width(12.dp)); Text(text = name, fontSize = 14.sp, fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium, fontFamily = ManropeFontFamily, color = if (isCurrentUser) DesignTokens.Stage.FireOrange else DesignTokens.Text.Primary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = "%,d".format(score), fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGroteskFontFamily, color = if (rank <= 3) accent else DesignTokens.Text.Secondary)
    }
}

@Composable
fun StatsCard(icon: ImageVector, value: String, label: String, accentColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(DesignTokens.radiusMedium)).background(DesignTokens.Stage.DarkChrome).drawBehind { drawRect(color = accentColor, size = Size(size.width, 3f)) }.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp)); Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = SpaceGroteskFontFamily, color = DesignTokens.Text.Primary); Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = DesignTokens.Text.Secondary, letterSpacing = 1.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun AchievementBadge(icon: ImageVector, title: String, unlocked: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(90.dp).clip(RoundedCornerShape(DesignTokens.radiusMedium)).background(if (unlocked) DesignTokens.Stage.DarkChrome else DesignTokens.Stage.DarkChrome.copy(alpha = 0.5f)).then(if (unlocked) Modifier.drawBehind { drawRoundRect(brush = Brush.linearGradient(listOf(DesignTokens.Stage.FireYellow.copy(alpha = 0.3f), DesignTokens.Stage.FireOrange.copy(alpha = 0.3f))), cornerRadius = CornerRadius(DesignTokens.radiusMedium.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)) } else Modifier).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = if (unlocked) DesignTokens.Stage.StageGold else DesignTokens.Stage.SteelGray.copy(alpha = 0.5f), modifier = Modifier.size(28.dp)); Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = if (unlocked) DesignTokens.Text.Primary else DesignTokens.Text.Disabled, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (!unlocked) { Spacer(modifier = Modifier.height(4.dp)); Text(text = "\uD83D\uDD12", fontSize = 10.sp) }
    }
}

