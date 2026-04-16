package com.rhythmgame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.rhythmgame.ui.theme.DesignTokens
import kotlin.math.abs

@Composable
fun WaveformView(
    amplitudes: List<Float>,
    trimStartFraction: Float,
    trimEndFraction: Float,
    playbackFraction: Float,
    onTrimStartChanged: (Float) -> Unit,
    onTrimEndChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pre-allocated colors
    val activeStartColor = DesignTokens.Stage.FireYellow
    val activeEndColor = DesignTokens.Stage.FireOrange
    val inactiveColor = Color.White.copy(alpha = 0.15f)
    val handleColor = Color.White
    val playbackColor = DesignTokens.Stage.StageGold
    val trimOverlayColor = Color.Black.copy(alpha = 0.4f)

    var draggingHandle by remember { mutableStateOf<Int?>(null) } // 0=start, 1=end

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .pointerInput(trimStartFraction, trimEndFraction) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val fraction = offset.x / size.width
                        val distToStart = abs(fraction - trimStartFraction)
                        val distToEnd = abs(fraction - trimEndFraction)
                        draggingHandle = if (distToStart < distToEnd) 0 else 1
                    },
                    onDragEnd = { draggingHandle = null },
                    onDragCancel = { draggingHandle = null },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        when (draggingHandle) {
                            0 -> {
                                // Enforce minimum gap (3s worth ~ depends on total, use fraction 0.05 min)
                                val maxStart = (trimEndFraction - 0.05f).coerceAtLeast(0f)
                                onTrimStartChanged(fraction.coerceAtMost(maxStart))
                            }
                            1 -> {
                                val minEnd = (trimStartFraction + 0.05f).coerceAtMost(1f)
                                onTrimEndChanged(fraction.coerceAtLeast(minEnd))
                            }
                        }
                    },
                )
            },
    ) {
        if (amplitudes.isEmpty()) return@Canvas

        val barCount = amplitudes.size
        val gap = 2f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        if (barWidth <= 0f) return@Canvas

        // Draw bars
        for (i in amplitudes.indices) {
            val fraction = i.toFloat() / barCount
            val amp = amplitudes[i].coerceIn(0.05f, 1f)
            val barHeight = size.height * 0.85f * amp
            val x = i * (barWidth + gap)
            val y = (size.height - barHeight) / 2f

            val inTrim = fraction >= trimStartFraction && fraction <= trimEndFraction
            val brush = if (inTrim) {
                Brush.verticalGradient(
                    listOf(activeStartColor, activeEndColor),
                    startY = y,
                    endY = y + barHeight,
                )
            } else {
                Brush.verticalGradient(
                    listOf(inactiveColor, inactiveColor),
                    startY = y,
                    endY = y + barHeight,
                )
            }

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2),
            )
        }

        // Dim overlay outside trim region
        drawRect(
            color = trimOverlayColor,
            topLeft = Offset.Zero,
            size = Size(size.width * trimStartFraction, size.height),
        )
        drawRect(
            color = trimOverlayColor,
            topLeft = Offset(size.width * trimEndFraction, 0f),
            size = Size(size.width * (1f - trimEndFraction), size.height),
        )

        // Trim handles - vertical dashed lines
        val handleWidth = 3f
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))

        // Start handle
        val startX = size.width * trimStartFraction
        drawLine(
            color = handleColor,
            start = Offset(startX, 0f),
            end = Offset(startX, size.height),
            strokeWidth = handleWidth,
            pathEffect = dashEffect,
        )
        // Start handle circle
        drawCircle(
            color = handleColor,
            radius = 8f,
            center = Offset(startX, size.height / 2),
        )

        // End handle
        val endX = size.width * trimEndFraction
        drawLine(
            color = handleColor,
            start = Offset(endX, 0f),
            end = Offset(endX, size.height),
            strokeWidth = handleWidth,
            pathEffect = dashEffect,
        )
        drawCircle(
            color = handleColor,
            radius = 8f,
            center = Offset(endX, size.height / 2),
        )

        // Playback cursor
        if (playbackFraction > 0f) {
            val cursorX = size.width * playbackFraction
            drawLine(
                color = playbackColor,
                start = Offset(cursorX, 0f),
                end = Offset(cursorX, size.height),
                strokeWidth = 2.5f,
            )
        }
    }
}
