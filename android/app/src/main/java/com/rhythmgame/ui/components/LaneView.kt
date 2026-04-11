package com.rhythmgame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rhythmgame.ui.theme.DesignTokens

@Composable
fun LaneView(
    laneIndex: Int,
    modifier: Modifier = Modifier,
) {
    val color = DesignTokens.Lane.colors.getOrElse(laneIndex) { Color.White }

    Canvas(
        modifier = modifier
            .width(2.dp)
            .fillMaxHeight()
    ) {
        drawLine(
            color = color.copy(alpha = 0.15f),
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = size.width,
        )
    }
}
