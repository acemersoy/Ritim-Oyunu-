package com.rhythmgame.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import com.rhythmgame.data.model.Song
import java.text.NumberFormat
import java.util.Locale

// ── Color palette ────────────────────────────────────────────────────────────
private val DarkBg = Color(0xFF0A0A1A)
private val CardBg = Color(0xFF12122A)
private val SurfaceDark = Color(0xFF1A1A2E)
private val NeonPurple = Color(0xFFBB86FC)
private val NeonCyan = Color(0xFF00E5FF)
private val Gold = Color(0xFFFFD700)
private val TextWhite = Color(0xFFEEEEEE)
private val TextGray = Color(0xFF8888AA)
private val DimGray = Color(0xFF555577)

// ── Bottom nav tab definition ────────────────────────────────────────────────
private enum class BottomTab(val label: String, val icon: ImageVector) {
    HOME("HOME", Icons.Filled.Home),
    RANKS("RANKS", Icons.Filled.Leaderboard),
    PROFILE("PROFILE", Icons.Filled.Person),
}

// ── Icon choices for song cards (cycles through these) ───────────────────────
private val songIcons = listOf(
    Icons.Filled.MusicNote,
    Icons.Outlined.Audiotrack,
    Icons.Outlined.GraphicEq,
    Icons.Outlined.LibraryMusic,
)

// ═══════════════════════════════════════════════════════════════════════════════
//  HOME SCREEN
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onUploadClick: () -> Unit,
    onSongClick: (String) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsState(initial = emptyList())
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var selectedTab by remember { mutableStateOf(BottomTab.HOME) }
    val context = LocalContext.current

    // ── Delete confirmation dialog ───────────────────────────────────────────
    if (songToDelete != null) {
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            containerColor = SurfaceDark,
            titleContentColor = TextWhite,
            textContentColor = TextGray,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Delete Song", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("\"${songToDelete!!.filename}\" silinecek. Emin misiniz?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSong(songToDelete!!.songId)
                        songToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text("Cancel", color = TextGray)
                }
            },
        )
    }

    // ── Main scaffold ────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "RHYTHM GAME",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextGray,
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextGray,
                    )
                }
            }

            // ── Content area ─────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 4.dp,
                    bottom = 80.dp, // room for bottom nav
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {

                // ── "YOUR TRACKS" header ─────────────────────────────────────
                item(key = "header_tracks") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "YOUR TRACKS",
                            color = NeonPurple,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.weight(1f),
                        )
                        // Song count pill
                        Box(
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = NeonCyan,
                                    shape = RoundedCornerShape(50),
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "${songs.size} SONG${if (songs.size != 1) "S" else ""}",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // ── Loading indicator ────────────────────────────────────────
                if (isRefreshing && songs.isEmpty()) {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = NeonPurple,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }

                // ── Empty state ──────────────────────────────────────────────
                if (songs.isEmpty() && !isRefreshing) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = DimGray,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No songs yet",
                                    color = TextGray,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Tap + to upload your first track",
                                    color = DimGray,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                }

                // ── Song cards (swipe-to-delete) ─────────────────────────────
                items(songs, key = { it.songId }) { song ->
                    val iconIndex = song.songId.hashCode().and(0x7FFFFFFF) % songIcons.size
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                songToDelete = song
                            }
                            false // Don't auto-dismiss; wait for dialog
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFFF5252).copy(alpha = 0.85f))
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        },
                        enableDismissFromStartToEnd = false,
                    ) {
                        SongCard(
                            song = song,
                            icon = songIcons[iconIndex],
                            onClick = { onSongClick(song.songId) },
                        )
                    }
                }

                // ── "RECENTLY PLAYED" section ────────────────────────────────
                item(key = "header_recent") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "RECENTLY PLAYED",
                            color = NeonPurple,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "VIEW ALL",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Coming Soon", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── Placeholder card ─────────────────────────────────────────
                item(key = "recent_placeholder") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        NeonPurple.copy(alpha = 0.25f),
                                        NeonCyan.copy(alpha = 0.15f),
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        NeonPurple.copy(alpha = 0.4f),
                                        NeonCyan.copy(alpha = 0.4f),
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Play a track to see it here",
                            color = DimGray,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }

        // ── Bottom navigation bar ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(SurfaceDark)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val tint by animateColorAsState(
                    targetValue = if (isSelected) NeonPurple else DimGray,
                    animationSpec = tween(200),
                    label = "tab_color",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedTab = tab
                            if (tab != BottomTab.HOME) {
                                Toast
                                    .makeText(context, "Coming Soon", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = tint,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }

            // ── Plus (upload) button ─────────────────────────────────────────
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(NeonPurple)
                    .clickable { onUploadClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Upload Song",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  SONG CARD
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun SongCard(
    song: Song,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val isReady = song.status == "ready"
    val bestScore = maxOf(song.highScoreEasy, song.highScoreMedium, song.highScoreHard)
    val displayName = song.filename
        .removeSuffix(".mp3")
        .removeSuffix(".m4a")
        .removeSuffix(".wav")
        .removeSuffix(".ogg")
        .removeSuffix(".flac")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isReady) { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                colors = listOf(
                    NeonPurple.copy(alpha = 0.6f),
                    NeonCyan.copy(alpha = 0.6f),
                )
            )
        ),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── Circle icon ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                NeonPurple.copy(alpha = 0.35f),
                                NeonCyan.copy(alpha = 0.2f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = NeonPurple,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // ── Song info ────────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = TextWhite,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (song.bpm != null) {
                        Text(
                            text = "${song.bpm.toInt()} BPM",
                            fontSize = 12.sp,
                            color = NeonCyan,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    if (song.bpm != null && bestScore > 0) {
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    if (bestScore > 0) {
                        val formatted = NumberFormat.getNumberInstance(Locale.US).format(bestScore)
                        Text(
                            text = "\uD83C\uDFC6 HS: $formatted",
                            fontSize = 12.sp,
                            color = Gold,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                // Duration (subtle)
                if (song.durationMs != null) {
                    val minutes = song.durationMs / 60000
                    val seconds = (song.durationMs % 60000) / 1000
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${minutes}:${String.format("%02d", seconds)}",
                        fontSize = 11.sp,
                        color = DimGray,
                    )
                }
            }

            // ── Right side: status / play button ─────────────────────────────
            when (song.status) {
                "processing" -> CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp,
                    color = NeonCyan,
                )
                "error" -> Text(
                    "Error",
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                "ready" -> {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(NeonPurple, NeonCyan)
                                ),
                                shape = CircleShape,
                            )
                            .clip(CircleShape)
                            .clickable { onClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = NeonCyan,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

