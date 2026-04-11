package com.rhythmgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rhythmgame.ui.components.*
import com.rhythmgame.ui.theme.*

@Composable
fun HomeScreen(
    onUploadClick: () -> Unit,
    onSongClick: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    onRankedClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onMultiplayerClick: () -> Unit = {},
    onStoreClick: () -> Unit = {},
    viewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val colors = LocalAppColors.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Nebula menu background: blurred image + shooting stars
        NebulaMenuBackground(modifier = Modifier.fillMaxSize())

        // Musical notes (only for stage rock)
        if (colors.mode == ThemeMode.STAGE_ROCK) {
            MusicalNotesBackground(
                modifier = Modifier.fillMaxSize(),
                intensity = 0.06f,
            )
        }

        // ── Settings icon (top-right corner) ─────────────────────────────────
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.85f),
            )
        }

        // ── Two-column landscape layout ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left column: primary actions
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GuitarButton(
                    imageRes = com.rhythmgame.R.drawable.btn_oyna,
                    onClick = onPlayClick,
                )
                GuitarButton(
                    imageRes = com.rhythmgame.R.drawable.btn_sarki_ekle,
                    onClick = onUploadClick,
                )
                GuitarButton(
                    imageRes = com.rhythmgame.R.drawable.btn_multi,
                    onClick = onMultiplayerClick,
                )
            }

            // Right column: store + profile, with inline badges
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    GuitarButton(
                        imageRes = com.rhythmgame.R.drawable.btn_magaza,
                        onClick = onStoreClick,
                    )
                    InfoBadge(
                        text = "2,450",
                        icon = Icons.Filled.MonetizationOn,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 10.dp),
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    GuitarButton(
                        imageRes = com.rhythmgame.R.drawable.btn_profil,
                        onClick = onProfileClick,
                    )
                    InfoBadge(
                        text = "24",
                        icon = Icons.Filled.Star,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoBadge(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DesignTokens.Stage.StageGold,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ManropeFontFamily,
        )
    }
}
