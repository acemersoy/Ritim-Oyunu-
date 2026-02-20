package com.rhythmgame.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.*

// Death Metal color palette
private val DeathBlack = Color(0xFF0A0A0A)
private val DeathGreenDark = Color(0xFF0D2B0D)
private val DeathGreen = Color(0xFF1B5E20)
private val ToxicGreen = Color(0xFF4CAF50)
private val NeonGreen = Color(0xFF00FF41)
private val BloodRed = Color(0xFF8B0000)
private val BloodBright = Color(0xFFB71C1C)
private val BoneWhite = Color(0xFFE8E0D0)
private val StarGold = Color(0xFFFFD700)
private val StarEmpty = Color(0xFF2A2A2A)

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
    onPlayAgain: (String) -> Unit,
    onHome: () -> Unit,
) {
    val totalNotes = perfect + great + good + miss

    // Karma star calculation: (perfect + great) ratio + combo bonus
    val karmaRatio = if (totalNotes > 0) (perfect + great).toFloat() / totalNotes else 0f
    val comboBonus = if (totalNotes > 0) (maxCombo.toFloat() / totalNotes).coerceAtMost(1f) * 0.1f else 0f
    val karma = (karmaRatio + comboBonus).coerceAtMost(1f)

    val starCount = when {
        karma >= 0.95f -> 5
        karma >= 0.80f -> 4
        karma >= 0.65f -> 3
        karma >= 0.50f -> 2
        karma >= 0.30f -> 1
        else -> 0
    }

    // Bottom sheet state
    var showDifficultySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Animations
    val guitarAlpha = remember { Animatable(0f) }
    val scoreAnim = remember { Animatable(0f) }
    val starsAnim = remember { Animatable(0f) }
    val statsAlpha = remember { Animatable(0f) }
    val buttonsAlpha = remember { Animatable(0f) }
    val badgeAlpha = remember { Animatable(0f) }

    var animatedScore by remember { mutableIntStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "metal")
    val bloodPulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bloodPulse",
    )
    val skullGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skullGlow",
    )

    LaunchedEffect(Unit) {
        guitarAlpha.animateTo(1f, tween(800))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        scoreAnim.animateTo(1f, tween(600))
    }
    LaunchedEffect(score) {
        kotlinx.coroutines.delay(300)
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
        starsAnim.animateTo(1f, tween(800, easing = EaseOut))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200)
        statsAlpha.animateTo(1f, tween(600))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1400)
        badgeAlpha.animateTo(1f, tween(500))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1600)
        buttonsAlpha.animateTo(1f, tween(500))
    }

    // Formatted score with commas
    val formattedScore = remember(animatedScore) {
        NumberFormat.getNumberInstance(Locale.US).format(animatedScore)
    }

    // Difficulty bottom sheet
    if (showDifficultySheet) {
        ModalBottomSheet(
            onDismissRequest = { showDifficultySheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1A1A1A),
            contentColor = BoneWhite,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(40.dp, 4.dp)
                        .background(DeathGreen, RoundedCornerShape(2.dp))
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(modifier = Modifier.size(40.dp)) {
                    drawSkull(center, 18f, NeonGreen)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "SELECT DIFFICULTY",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonGreen,
                    letterSpacing = 4.sp,
                )

                Spacer(modifier = Modifier.height(24.dp))

                DifficultySheetOption("EASY", "3 lanes, on-beat notes", ToxicGreen) {
                    onPlayAgain("easy"); showDifficultySheet = false
                }
                Spacer(modifier = Modifier.height(12.dp))
                DifficultySheetOption("MEDIUM", "5 lanes, eighth notes", Color(0xFFFFEB3B)) {
                    onPlayAgain("medium"); showDifficultySheet = false
                }
                Spacer(modifier = Modifier.height(12.dp))
                DifficultySheetOption("HARD", "5 lanes, all notes + holds", BloodBright) {
                    onPlayAgain("hard"); showDifficultySheet = false
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Main content
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeathBlack, DeathGreenDark, DeathBlack),
                )
            ),
    ) {
        // Blood drips background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBloodDrips(bloodPulse)
        }

        // Guitar silhouette background canvas (fills available space behind content)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.Center)
        ) {
            val cw = size.width
            val ch = size.height
            val cx = cw / 2
            val cy = ch / 2
            val scale = minOf(cw / 400f, ch / 600f)

            drawGuitarSilhouette(cx, cy, scale, guitarAlpha.value, skullGlow)

            // Skull at guitar headstock
            val skullY = cy - 210 * scale
            drawSkull(Offset(cx, skullY), 22f * scale, NeonGreen.copy(alpha = guitarAlpha.value * skullGlow))
        }

        // Scrollable content layered on top
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // "SONG COMPLETED" header
            Text(
                "SONG COMPLETED",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = NeonGreen,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 5 Gold Stars row
            if (starsAnim.value > 0f) {
                Canvas(
                    modifier = Modifier
                        .width(260.dp)
                        .height(48.dp)
                        .graphicsLayer { alpha = starsAnim.value }
                ) {
                    val starSize = 20f
                    val starSpacing = size.width / 5f
                    val starsStartX = starSpacing / 2f
                    val starsY = size.height / 2f

                    for (i in 0 until 5) {
                        val filled = i < starCount
                        val starAlpha = if (i.toFloat() / 5f < starsAnim.value) starsAnim.value else 0f
                        val starX = starsStartX + i * starSpacing
                        // Glow for filled stars
                        if (filled && starAlpha > 0f) {
                            drawStar(starX, starsY, starSize * 1.3f, starSize * 0.55f, StarGold.copy(alpha = starAlpha * 0.25f))
                        }
                        drawStar(
                            starX, starsY, starSize, starSize * 0.45f,
                            if (filled) StarGold.copy(alpha = starAlpha) else StarEmpty.copy(alpha = starAlpha * 0.6f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // "FINAL SCORE" label
            if (scoreAnim.value > 0f) {
                Text(
                    "FINAL SCORE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen.copy(alpha = 0.7f * scoreAnim.value),
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Large score number
                Text(
                    formattedScore,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonGreen.copy(alpha = scoreAnim.value),
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { alpha = scoreAnim.value },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // "NEW HIGH SCORE" badge
            if (badgeAlpha.value > 0f) {
                Surface(
                    modifier = Modifier
                        .graphicsLayer { alpha = badgeAlpha.value },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0A0A0A).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "NEW HIGH SCORE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            letterSpacing = 2.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stats card
            if (statsAlpha.value > 0f) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = statsAlpha.value }
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF0F1A0F))),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(16.dp),
                ) {
                    // MAX COMBO row
                    MetalStatRow("MAX COMBO", "${maxCombo}x", NeonGreen)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DeathGreen.copy(alpha = 0.3f))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hit stats with progress bars
                    HitStatProgressRow(
                        label = "PERFECT",
                        count = perfect,
                        totalNotes = totalNotes,
                        color = NeonGreen,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HitStatProgressRow(
                        label = "GREAT",
                        count = great,
                        totalNotes = totalNotes,
                        color = StarGold,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HitStatProgressRow(
                        label = "GOOD",
                        count = good,
                        totalNotes = totalNotes,
                        color = Color(0xFFFF9800),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HitStatProgressRow(
                        label = "MISS",
                        count = miss,
                        totalNotes = totalNotes,
                        color = BloodBright,
                    )
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
                    // PLAY AGAIN - red filled, full width
                    Button(
                        onClick = { showDifficultySheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BloodRed,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            "PLAY AGAIN",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            fontSize = 16.sp,
                        )
                    }

                    // GO BACK - red outline, full width
                    OutlinedButton(
                        onClick = onHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, BloodRed.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BloodBright,
                        ),
                    ) {
                        Text(
                            "GO BACK",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- Compose helpers ---

@Composable
private fun MetalStatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = BoneWhite.copy(alpha = 0.5f),
            letterSpacing = 2.sp,
        )
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = valueColor,
        )
    }
}

@Composable
private fun HitStatProgressRow(
    label: String,
    count: Int,
    totalNotes: Int,
    color: Color,
) {
    val fraction = if (totalNotes > 0) count.toFloat() / totalNotes else 0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Label
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 1.5.sp,
            modifier = Modifier.width(68.dp),
        )

        // Progress bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF2A2A2A))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Count value
        Text(
            "$count",
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = color,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun DifficultySheetOption(name: String, description: String, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color, letterSpacing = 3.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text(description, fontSize = 12.sp, color = BoneWhite.copy(alpha = 0.5f))
        }
    }
}

// --- Canvas drawing helpers ---

private fun DrawScope.drawBloodDrips(pulse: Float) {
    val drips = listOf(
        Triple(0.05f, 0.15f, 8f),
        Triple(0.12f, 0.25f, 6f),
        Triple(0.20f, 0.10f, 5f),
        Triple(0.35f, 0.20f, 7f),
        Triple(0.55f, 0.12f, 5f),
        Triple(0.70f, 0.30f, 9f),
        Triple(0.78f, 0.18f, 6f),
        Triple(0.88f, 0.22f, 7f),
        Triple(0.95f, 0.14f, 5f),
    )
    for ((xFrac, heightFrac, w) in drips) {
        val x = size.width * xFrac
        val dripH = size.height * heightFrac * pulse
        val dw = w * (size.width / 400f)
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(BloodRed.copy(alpha = 0.8f), BloodBright.copy(alpha = 0.4f), Color.Transparent),
                startY = 0f, endY = dripH,
            ),
            topLeft = Offset(x - dw / 2, 0f),
            size = Size(dw, dripH),
            cornerRadius = CornerRadius(dw / 2),
        )
        drawCircle(BloodRed.copy(alpha = 0.6f * pulse), dw * 0.8f, Offset(x, dripH))
    }
}

private fun DrawScope.drawGuitarSilhouette(
    cx: Float, cy: Float, scale: Float, alpha: Float, glowAlpha: Float,
) {
    val outline = NeonGreen.copy(alpha = alpha * 0.8f)
    val glow = NeonGreen.copy(alpha = alpha * glowAlpha * 0.15f)
    val fill = DeathGreenDark.copy(alpha = alpha * 0.6f)
    val sw = 2.5f * scale

    // Lower bout
    val lbCy = cy + 50 * scale; val lbRx = 100 * scale; val lbRy = 80 * scale
    // Upper bout
    val ubCy = cy - 40 * scale; val ubRx = 75 * scale; val ubRy = 60 * scale

    // Glow
    drawOval(glow, Offset(cx - lbRx - 10 * scale, lbCy - lbRy - 10 * scale), Size((lbRx + 10 * scale) * 2, (lbRy + 10 * scale) * 2))
    drawOval(glow, Offset(cx - ubRx - 10 * scale, ubCy - ubRy - 10 * scale), Size((ubRx + 10 * scale) * 2, (ubRy + 10 * scale) * 2))

    // Fill
    drawOval(fill, Offset(cx - lbRx, lbCy - lbRy), Size(lbRx * 2, lbRy * 2))
    drawOval(fill, Offset(cx - ubRx, ubCy - ubRy), Size(ubRx * 2, ubRy * 2))
    drawRect(fill, Offset(cx - 55 * scale, ubCy), Size(110 * scale, lbCy - ubCy))

    // Outline
    drawOval(outline, Offset(cx - lbRx, lbCy - lbRy), Size(lbRx * 2, lbRy * 2), style = Stroke(sw))
    drawOval(outline, Offset(cx - ubRx, ubCy - ubRy), Size(ubRx * 2, ubRy * 2), style = Stroke(sw))

    // Neck
    val nw = 30 * scale; val nTop = cy - 200 * scale; val nBot = ubCy - 30 * scale
    drawRect(fill, Offset(cx - nw / 2, nTop), Size(nw, nBot - nTop))
    drawRect(outline, Offset(cx - nw / 2, nTop), Size(nw, nBot - nTop), style = Stroke(sw))

    // Headstock
    val hw = 40 * scale; val hh = 35 * scale; val hTop = nTop - hh
    drawRoundRect(fill, Offset(cx - hw / 2, hTop), Size(hw, hh), CornerRadius(6 * scale))
    drawRoundRect(outline, Offset(cx - hw / 2, hTop), Size(hw, hh), CornerRadius(6 * scale), style = Stroke(sw))

    // Tuning pegs
    for (i in 0 until 5) {
        val side = if (i < 3) -1 else 1
        val idx = if (i < 3) i else i - 3
        val py = hTop + 8 * scale + idx * 10 * scale
        val px = cx + side * (hw / 2 + 6 * scale)
        drawCircle(outline, 3 * scale, Offset(px, py))
    }

    // Frets
    for (i in 1..8) {
        val fy = nTop + (nBot - nTop) * i / 9f
        drawLine(outline.copy(alpha = alpha * 0.4f), Offset(cx - nw / 2 + 2 * scale, fy), Offset(cx + nw / 2 - 2 * scale, fy), 1f * scale)
    }

    // Strings
    val ss = nw / 6
    for (i in 1..5) {
        val sx = cx - nw / 2 + ss * i
        drawLine(BoneWhite.copy(alpha = alpha * 0.3f), Offset(sx, hTop + 5 * scale), Offset(sx, lbCy + 30 * scale), 0.8f * scale)
    }

    // Pickups
    for (j in 0 until 2) {
        val py = cy + (if (j == 0) 10 else 60) * scale
        drawRoundRect(outline.copy(alpha = alpha * 0.5f), Offset(cx - 35 * scale, py - 6 * scale), Size(70 * scale, 12 * scale), CornerRadius(4 * scale), style = Stroke(1.5f * scale))
    }

    // Bridge
    val by = lbCy + 20 * scale
    drawRoundRect(outline.copy(alpha = alpha * 0.4f), Offset(cx - 25 * scale, by - 4 * scale), Size(50 * scale, 8 * scale), CornerRadius(2 * scale), style = Stroke(1f * scale))
}

private fun DrawScope.drawStar(cx: Float, cy: Float, outer: Float, inner: Float, color: Color) {
    val path = Path()
    for (i in 0 until 10) {
        val angle = -PI / 2 + i * PI / 5
        val r = if (i % 2 == 0) outer else inner
        val x = cx + (r * cos(angle)).toFloat()
        val y = cy + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawSkull(center: Offset, radius: Float, color: Color) {
    // Cranium
    drawCircle(color, radius, center, style = Stroke(2f))
    // Eyes
    val eo = radius * 0.35f; val er = radius * 0.2f
    drawCircle(color, er, Offset(center.x - eo, center.y - radius * 0.1f))
    drawCircle(color, er, Offset(center.x + eo, center.y - radius * 0.1f))
    // Nose
    val nose = Path().apply {
        moveTo(center.x, center.y + radius * 0.1f)
        lineTo(center.x - radius * 0.1f, center.y + radius * 0.3f)
        lineTo(center.x + radius * 0.1f, center.y + radius * 0.3f)
        close()
    }
    drawPath(nose, color)
    // Jaw + teeth
    drawLine(color, Offset(center.x - radius * 0.3f, center.y + radius * 0.55f), Offset(center.x + radius * 0.3f, center.y + radius * 0.55f), 1.5f)
    for (i in 0 until 4) {
        val tx = center.x - radius * 0.2f + i * radius * 0.13f
        drawLine(color, Offset(tx, center.y + radius * 0.45f), Offset(tx, center.y + radius * 0.65f), 1.5f)
    }
}
