package com.rhythmgame.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rhythmgame.ui.components.*
import com.rhythmgame.ui.theme.*

@Composable
fun RankedScreen(
    onBack: () -> Unit,
) {
    val colors = LocalAppColors.current

    // Dummy leaderboard data
    val leaderboard = remember {
        listOf(
            Triple("ProDrummer", 98500, false),
            Triple("RhythmKing", 87200, false),
            Triple("BeatMaster", 76800, false),
            Triple("NoteHunter", 65400, false),
            Triple("Player", 45200, true),
            Triple("MusicLover", 38900, false),
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ThemedBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, bottom = 8.dp)
                    .height(48.dp),
            ) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textMain)
                }
                Text(
                    text = "RANKED",
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    letterSpacing = 3.sp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Season info card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("SEZON 1", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGroteskFontFamily, color = colors.primary, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Kozmik Baslangic", fontSize = 13.sp, fontFamily = ManropeFontFamily, color = colors.textMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("KALAN SURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = colors.textMuted, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("28g 14s", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGroteskFontFamily, color = colors.accentGold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Rank emblem
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RankEmblem(
                    rankName = "Bronz",
                    rankTier = "III",
                    rp = 250,
                    rpMax = 500,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Leaderboard section header
            Text(
                "SIRALAMA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ManropeFontFamily,
                color = colors.primary,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Leaderboard entries
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                leaderboard.forEachIndexed { index, (name, score, isUser) ->
                    LeaderboardEntry(
                        rank = index + 1,
                        name = name,
                        score = score,
                        isCurrentUser = isUser,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Play button
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ThemedButton(
                    text = "MACA BASLA",
                    onClick = { },
                    isPrimary = true,
                    height = 56.dp,
                    fontSize = 16.sp,
                    icon = Icons.Default.PlayArrow,
                    badge = "YAKINDA",
                    enabled = false,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
