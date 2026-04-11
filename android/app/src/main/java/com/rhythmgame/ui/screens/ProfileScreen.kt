package com.rhythmgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rhythmgame.ui.components.*
import com.rhythmgame.ui.theme.*

private enum class ProfileTab(val label: String) {
    PROFILE("PROFIL"),
    ACHIEVEMENTS("BASARIMLAR"),
    COLLECTION("KOLEKSIYON"),
}

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
) {
    val colors = LocalAppColors.current
    var activeTab by remember { mutableStateOf(ProfileTab.PROFILE) }

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
                    text = "PROFIL",
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    letterSpacing = 3.sp,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar + username + level
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.4f), colors.primaryGlow.copy(alpha = 0.2f)))
                        )
                        .border(2.dp, colors.primary.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Person, null, tint = colors.textMuted, modifier = Modifier.size(40.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("PLAYER", fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGroteskFontFamily, color = colors.textMain)
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "Seviye 1",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ManropeFontFamily,
                    color = colors.primary,
                    modifier = Modifier
                        .background(colors.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // XP Bar
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ThemedProgressBar(progress = 0.35f)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("350 / 1000 XP", fontSize = 11.sp, fontFamily = ManropeFontFamily, color = colors.textMuted)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current rank card
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    RankEmblem(rankName = "Bronz", rankTier = "III", rp = 250, rpMax = 500)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatsCard(icon = Icons.Default.SportsEsports, value = "0", label = "TOPLAM\nOYUN", accentColor = colors.primary, modifier = Modifier.weight(1f))
                StatsCard(icon = Icons.Default.EmojiEvents, value = "0", label = "RANK\nMACI", accentColor = colors.accentGold, modifier = Modifier.weight(1f))
                StatsCard(icon = Icons.Default.GpsFixed, value = "0%", label = "TAM\nISABET", accentColor = colors.accentGreen, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tab bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileTab.entries.forEach { tab ->
                    val isActive = activeTab == tab
                    Text(
                        text = tab.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ManropeFontFamily,
                        color = if (isActive) colors.textMain else colors.textMuted,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isActive) colors.primary.copy(alpha = 0.2f) else colors.cardBg)
                            .then(
                                if (isActive) Modifier.border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                else Modifier
                            )
                            .clickable { activeTab = tab }
                            .padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab content
            when (activeTab) {
                ProfileTab.PROFILE -> {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Text("EN IYI SARKILAR", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = colors.textMuted, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Henuz oyun oynamadiniz.", fontSize = 14.sp, fontFamily = ManropeFontFamily, color = colors.textDisabled)
                    }
                }
                ProfileTab.ACHIEVEMENTS -> {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Text("BASARIMLAR", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = colors.textMuted, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Basarimlar yakinda eklenecek.", fontSize = 14.sp, fontFamily = ManropeFontFamily, color = colors.textDisabled)
                    }
                }
                ProfileTab.COLLECTION -> {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Text("KOLEKSIYON", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = ManropeFontFamily, color = colors.textMuted, letterSpacing = 2.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Koleksiyon ogeleriniz burada gorunecek.", fontSize = 14.sp, fontFamily = ManropeFontFamily, color = colors.textDisabled)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
