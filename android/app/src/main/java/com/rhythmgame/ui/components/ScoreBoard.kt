package com.rhythmgame.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScoreBoard(
    score: Int,
    combo: Int,
    multiplier: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                "SCORE",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f),
            )
            Text(
                "$score",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        if (combo > 1) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${combo}x",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    "COMBO",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
        }

        if (multiplier > 1.0f) {
            Text(
                "x${String.format("%.1f", multiplier)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFFFD700),
            )
        }
    }
}
