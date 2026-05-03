package com.rhythmgame.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rhythmgame.data.local.AchievementEntity
import com.rhythmgame.data.local.GameResultEntity
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
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(ProfileTab.PROFILE) }

    val profile by viewModel.profile.collectAsState()
    val totalGames by viewModel.totalGames.collectAsState()
    val averageAccuracy by viewModel.averageAccuracy.collectAsState()
    val topSongs by viewModel.topSongs.collectAsState()
    val achievements by viewModel.achievements.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: Exception) { }
            viewModel.updateAvatar(uri.toString())
        }
    }

    val currentProfile = profile
    val username = currentProfile?.username ?: "Player"
    val level = currentProfile?.level ?: 1
    val xp = currentProfile?.xp ?: 0L
    val xpProgress = viewModel.xpProgress(xp, level)
    val xpNext = viewModel.xpForNextLevel(level)
    val xpCurrent = xp

    // Edit name dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Kullanici Adi", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editNameText,
                    onValueChange = { editNameText = it.take(20) },
                    singleLine = true,
                    placeholder = { Text("Yeni isim") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editNameText.isNotBlank()) {
                        viewModel.updateUsername(editNameText.trim())
                    }
                    showEditNameDialog = false
                }) { Text("Kaydet") }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Iptal") }
            },
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
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.4f), colors.primaryGlow.copy(alpha = 0.2f)))
                        )
                        .border(2.dp, colors.primary.copy(alpha = 0.6f), CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val avatarUri = currentProfile?.avatarUri
                    if (avatarUri != null) {
                        val bitmap = remember(avatarUri) {
                            try {
                                context.contentResolver.openInputStream(Uri.parse(avatarUri))?.use {
                                    BitmapFactory.decodeStream(it)
                                }
                            } catch (_: Exception) { null }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(Icons.Default.Person, null, tint = colors.textMuted, modifier = Modifier.size(40.dp))
                        }
                    } else {
                        Icon(Icons.Default.Person, null, tint = colors.textMuted, modifier = Modifier.size(40.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Username with edit icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        editNameText = username
                        showEditNameDialog = true
                    },
                ) {
                    Text(username.uppercase(), fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGroteskFontFamily, color = colors.textMain)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.Edit, null, tint = colors.textMuted, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "Seviye $level",
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
                    ThemedProgressBar(progress = xpProgress)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$xpCurrent / $xpNext XP", fontSize = 11.sp, fontFamily = ManropeFontFamily, color = colors.textMuted)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current rank card
            GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    RankEmblem(rankName = currentProfile?.league ?: "Bronz", rankTier = "III", rp = 250, rpMax = 500)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatsCard(icon = Icons.Default.SportsEsports, value = "$totalGames", label = "TOPLAM\nOYUN", accentColor = colors.primary, modifier = Modifier.weight(1f))
                StatsCard(icon = Icons.Default.EmojiEvents, value = "0", label = "RANK\nMACI", accentColor = colors.accentGold, modifier = Modifier.weight(1f))
                StatsCard(icon = Icons.Default.GpsFixed, value = "${"%.1f".format(averageAccuracy)}%", label = "ORT.\nISABET", accentColor = colors.accentGreen, modifier = Modifier.weight(1f))
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
                        if (topSongs.isEmpty()) {
                            Text("Henuz oyun oynamadiniz.", fontSize = 14.sp, fontFamily = ManropeFontFamily, color = colors.textDisabled)
                        } else {
                            topSongs.forEach { result ->
                                TopSongRow(result = result, colors = colors)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
                ProfileTab.ACHIEVEMENTS -> {
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        val unlockedCount = achievements.count { it.isUnlocked }
                        Text(
                            "BASARIMLAR ($unlockedCount/${achievements.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ManropeFontFamily,
                            color = colors.textMuted,
                            letterSpacing = 2.sp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (achievements.isEmpty()) {
                            Text("Basarimlar yukleniyor...", fontSize = 14.sp, fontFamily = ManropeFontFamily, color = colors.textDisabled)
                        } else {
                            achievements.forEach { achievement ->
                                AchievementRow(achievement = achievement, colors = colors)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
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

@Composable
private fun TopSongRow(result: GameResultEntity, colors: AppColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.backgroundDeep.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.MusicNote, null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                result.songTitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ManropeFontFamily,
                color = colors.textMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${"%.1f".format(result.accuracy)}% isabetle",
                fontSize = 11.sp,
                fontFamily = ManropeFontFamily,
                color = colors.textMuted,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${result.score}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGroteskFontFamily,
                color = colors.accentGold,
            )
            val diffColor = when (result.difficulty) {
                "easy" -> colors.accentGreen
                "hard" -> colors.primary
                else -> colors.accentGold
            }
            Text(
                result.difficulty.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ManropeFontFamily,
                color = diffColor,
            )
        }
    }
}

@Composable
private fun AchievementRow(achievement: AchievementEntity, colors: AppColors) {
    val alpha = if (achievement.isUnlocked) 1f else 0.5f
    val categoryColor = when (achievement.category) {
        "easy" -> colors.accentGreen
        "medium" -> colors.accentGold
        "hard" -> colors.primary
        else -> colors.textMuted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.backgroundDeep.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Lock/unlock icon
        Icon(
            imageVector = if (achievement.isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
            contentDescription = null,
            tint = if (achievement.isUnlocked) colors.accentGold else colors.textDisabled,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                achievement.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = ManropeFontFamily,
                color = colors.textMain.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                achievement.description,
                fontSize = 11.sp,
                fontFamily = ManropeFontFamily,
                color = colors.textMuted.copy(alpha = alpha),
            )
            // Progress bar
            if (!achievement.isUnlocked && achievement.target > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.textDisabled.copy(alpha = 0.2f)),
                ) {
                    val progress = (achievement.progress.toFloat() / achievement.target).coerceIn(0f, 1f)
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(2.dp))
                                .background(categoryColor),
                        )
                    }
                }
                Text(
                    "${achievement.progress}/${achievement.target}",
                    fontSize = 9.sp,
                    fontFamily = ManropeFontFamily,
                    color = colors.textMuted.copy(alpha = 0.7f),
                )
            }
        }
        // Category badge
        Text(
            when (achievement.category) {
                "easy" -> "KOLAY"
                "medium" -> "ORTA"
                "hard" -> "ZOR"
                else -> ""
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = ManropeFontFamily,
            color = categoryColor,
            modifier = Modifier
                .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
