package com.rhythmgame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rhythmgame.ui.theme.LaneColors

@Composable
fun NoteView(
    lane: Int,
    modifier: Modifier = Modifier,
) {
    val color = LaneColors.getOrElse(lane - 1) { Color.White }

    Canvas(modifier = modifier.size(48.dp, 24.dp)) {
        drawRoundRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(8f, 8f),
        )
    }
}
