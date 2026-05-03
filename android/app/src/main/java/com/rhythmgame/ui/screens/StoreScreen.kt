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
import androidx.hilt.navigation.compose.hiltViewModel
import com.rhythmgame.ui.components.*
import com.rhythmgame.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

private enum class StoreTab(val label: String) {
    THEMES("TEMALAR"),
    EFFECTS("EFEKTLER"),
    SKINS("GORUNUM"),
}

@Composable
fun StoreScreen(
    onBack: () -> Unit,
    viewModel: StoreViewModel = hiltViewModel(),
) {
    val colors = LocalAppColors.current
    var activeTab by remember { mutableStateOf(StoreTab.THEMES) }
    val coins by viewModel.coins.collectAsState()
    val purchaseResult by viewModel.purchaseResult.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Purchase confirmation dialog
    var pendingPurchaseItem by remember { mutableStateOf<StoreItem?>(null) }

    if (pendingPurchaseItem != null) {
        val item = pendingPurchaseItem!!
        AlertDialog(
            onDismissRequest = { pendingPurchaseItem = null },
            title = { Text("Satin Al", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Text("\"${item.name}\" ogesi ${item.price} coin'e satin alinsin mi?", fontFamily = ManropeFontFamily)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.purchaseItem(item.price)
                    pendingPurchaseItem = null
                }) { Text("Satin Al") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPurchaseItem = null }) { Text("Iptal") }
            },
        )
    }

    // Handle purchase result
    LaunchedEffect(purchaseResult) {
        when (purchaseResult) {
            PurchaseResult.SUCCESS -> {
                snackbarHostState.showSnackbar("Basariyla satin alindi!")
                viewModel.clearPurchaseResult()
            }
            PurchaseResult.INSUFFICIENT_FUNDS -> {
                snackbarHostState.showSnackbar("Yetersiz bakiye!")
                viewModel.clearPurchaseResult()
            }
            null -> {}
        }
    }

    val formattedCoins = remember(coins) {
        NumberFormat.getNumberInstance(Locale.US).format(coins)
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
                                formattedCoins,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGroteskFontFamily,
                                color = colors.accentGold,
                            )
                        }
                    }
                    ThemedButton(
                        text = "REKLAM IZLE",
                        onClick = { viewModel.addCoinsFromAd(10) },
                        isPrimary = true,
                        height = 40.dp,
                        fontSize = 12.sp,
                        icon = Icons.Default.PlayCircle,
                        modifier = Modifier.width(150.dp),
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
                        onItemClick = { item -> if (!item.owned && item.price > 0) pendingPurchaseItem = item },
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
                        onItemClick = { item -> if (!item.owned && item.price > 0) pendingPurchaseItem = item },
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
                        onItemClick = { item -> if (!item.owned && item.price > 0) pendingPurchaseItem = item },
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
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
private fun StoreItemGrid(items: List<StoreItem>, onItemClick: (StoreItem) -> Unit = {}) {
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
                        onClick = { onItemClick(item) },
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    GlassCard(
        modifier = modifier.clickable(enabled = !item.owned && item.price > 0) { onClick() },
    ) {
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
