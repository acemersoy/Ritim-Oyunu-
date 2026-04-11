package com.rhythmgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rhythmgame.ui.components.*
import com.rhythmgame.ui.theme.*

private enum class StoreTab(val label: String) {
    THEMES("TEMALAR"),
    EFFECTS("EFEKTLER"),
    SKINS("GORUNUM"),
}

@Composable
fun StoreScreen(
    onBack: () -> Unit,
) {
    val colors = LocalAppColors.current
    var activeTab by remember { mutableStateOf(StoreTab.THEMES) }

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
                    text = "MAGAZA",
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    letterSpacing = 3.sp,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Coin balance
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
                        Text(
                            "BAKIYE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ManropeFontFamily,
                            color = colors.textMuted,
                            letterSpacing = 2.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Paid, null, tint = colors.accentGold, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "2,450",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGroteskFontFamily,
                                color = colors.accentGold,
                            )
                        }
                    }
                    ThemedButton(
                        text = "SATIN AL",
                        onClick = { },
                        isPrimary = true,
                        height = 40.dp,
                        fontSize = 12.sp,
                        icon = Icons.Default.Add,
                        modifier = Modifier.width(140.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tab bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StoreTab.entries.forEach { tab ->
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

            Spacer(modifier = Modifier.height(20.dp))

            // Tab content
            when (activeTab) {
                StoreTab.THEMES -> {
                    StoreItemGrid(
                        items = listOf(
                            StoreItem("Kozmik Mor", "Premium tema", 0, Icons.Default.Palette, true),
                            StoreItem("Sahne Rock", "Varsayilan tema", 0, Icons.Default.LocalFireDepartment, true),
                            StoreItem("Neon Siber", "Yakinda", 1500, Icons.Default.Bolt, false),
                            StoreItem("Okyanus Derin", "Yakinda", 1500, Icons.Default.Water, false),
                        ),
                    )
                }
                StoreTab.EFFECTS -> {
                    StoreItemGrid(
                        items = listOf(
                            StoreItem("Yildiz Patlamasi", "Hit efekti", 500, Icons.Default.AutoAwesome, false),
                            StoreItem("Alev Dalgasi", "Hit efekti", 750, Icons.Default.Whatshot, false),
                            StoreItem("Elektrik", "Hit efekti", 500, Icons.Default.ElectricBolt, false),
                            StoreItem("Gok Kusagi", "Kombo efekti", 1000, Icons.Default.FilterVintage, false),
                        ),
                    )
                }
                StoreTab.SKINS -> {
                    StoreItemGrid(
                        items = listOf(
                            StoreItem("Kristal Notalar", "Nota gorunumu", 800, Icons.Default.Diamond, false),
                            StoreItem("Ates Notalar", "Nota gorunumu", 800, Icons.Default.LocalFireDepartment, false),
                            StoreItem("Buz Notalar", "Nota gorunumu", 800, Icons.Default.AcUnit, false),
                            StoreItem("Altin Notalar", "Nota gorunumu", 1200, Icons.Default.Star, false),
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

private data class StoreItem(
    val name: String,
    val description: String,
    val price: Int,
    val icon: ImageVector,
    val owned: Boolean,
)

@Composable
private fun StoreItemGrid(items: List<StoreItem>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { item ->
                    StoreItemCard(
                        item = item,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StoreItemCard(
    item: StoreItem,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    GlassCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.3f),
                                colors.cardBg,
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = colors.primary,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ManropeFontFamily,
                color = colors.textMain,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                item.description,
                fontSize = 11.sp,
                fontFamily = ManropeFontFamily,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (item.owned) {
                Text(
                    "SAHIP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ManropeFontFamily,
                    color = colors.accentGreen,
                    modifier = Modifier
                        .background(colors.accentGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            } else if (item.price > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Default.Paid, null, tint = colors.accentGold, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${item.price}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ManropeFontFamily,
                        color = colors.accentGold,
                    )
                }
            }
        }
    }
}
