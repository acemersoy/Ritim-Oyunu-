package com.rhythmgame.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rhythmgame.R
import com.rhythmgame.ui.components.*
import com.rhythmgame.ui.theme.*

@Composable
fun MultiplayerScreen(
    onBack: () -> Unit,
) {
    val colors = LocalAppColors.current

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
                    text = "COK OYUNCULU",
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    letterSpacing = 3.sp,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Globe image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.planet_globe),
                    contentDescription = null,
                    modifier = Modifier.size(180.dp),
                    contentScale = ContentScale.Fit,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "GERCEK ZAMANLI",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    color = colors.primary,
                    letterSpacing = 4.sp,
                )
                Text(
                    "COK OYUNCULU",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    color = colors.textMain,
                    letterSpacing = 4.sp,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Feature list
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                FeatureBullet("Arkadaslarinla gercek zamanli yarisma")
                Spacer(modifier = Modifier.height(10.dp))
                FeatureBullet("Online siralamalar ve odeme")
                Spacer(modifier = Modifier.height(10.dp))
                FeatureBullet("Ozel odalar olusturma")
                Spacer(modifier = Modifier.height(10.dp))
                FeatureBullet("2-4 oyuncu destek")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CTA button
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ThemedButton(
                    text = "GELISMELERI TAKIP ET",
                    onClick = { },
                    isPrimary = true,
                    height = 56.dp,
                    fontSize = 14.sp,
                    icon = Icons.Default.Notifications,
                    badge = "YAKINDA",
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Release date card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.CalendarMonth, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Tahmini Cikis: 2025 Q3",
                        fontSize = 14.sp,
                        fontFamily = ManropeFontFamily,
                        color = colors.textMuted,
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FeatureBullet(text: String) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(colors.primary),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontFamily = ManropeFontFamily,
            color = colors.textMain,
        )
    }
}
