package com.rhythmgame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rhythmgame.data.model.Song
import com.rhythmgame.ui.components.*
import com.rhythmgame.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

private val songIcons = listOf(
    Icons.Filled.MusicNote,
    Icons.Outlined.Audiotrack,
    Icons.Outlined.GraphicEq,
    Icons.Outlined.LibraryMusic,
)

private enum class SongFilter(val label: String) {
    ALL("TUMU"),
    FAVORITE("FAVORI"),
    RECENT("SON"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListScreen(
    onSongClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val songs by viewModel.songs.collectAsState(initial = emptyList())
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val colors = LocalAppColors.current
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(SongFilter.ALL) }

    val filteredSongs = songs
        .filter { song ->
            val matchesSearch = searchQuery.isBlank() || song.filename.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (activeFilter) {
                SongFilter.ALL -> true
                SongFilter.FAVORITE -> song.isFavorite
                SongFilter.RECENT -> song.lastPlayedAt > 0L
            }
            matchesSearch && matchesFilter
        }
        .let { list ->
            when (activeFilter) {
                SongFilter.RECENT -> list.sortedByDescending { it.lastPlayedAt }
                else -> list
            }
        }

    if (songToDelete != null) {
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            containerColor = colors.cardGlass,
            titleContentColor = colors.textMain,
            textContentColor = colors.textMuted,
            shape = RoundedCornerShape(DesignTokens.radiusLarge),
            title = { Text("Sarkiyi Sil", fontWeight = FontWeight.Bold) },
            text = { Text("\"${songToDelete!!.filename}\" silinecek. Emin misiniz?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSong(songToDelete!!.songId)
                    songToDelete = null
                }) {
                    Text("Sil", color = colors.accentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text("Iptal", color = colors.textMuted)
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ThemedBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .height(48.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textMain,
                    )
                }
                Text(
                    text = "SARKI SEC",
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.textMain,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    letterSpacing = 2.sp,
                )
            }

            // ── Search bar ──────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = {
                    Text("Sarki ara...", color = colors.textMuted, fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = colors.textMuted)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, tint = colors.textMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(DesignTokens.radiusMedium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.primary.copy(alpha = 0.2f),
                    cursorColor = colors.primary,
                    focusedTextColor = colors.textMain,
                    unfocusedTextColor = colors.textMain,
                    focusedContainerColor = colors.cardBg,
                    unfocusedContainerColor = colors.cardBg,
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Filter tabs ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SongFilter.entries.forEach { filter ->
                    val isActive = activeFilter == filter
                    Text(
                        text = filter.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ManropeFontFamily,
                        color = if (isActive) colors.textMain else colors.textMuted,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isActive) colors.primary.copy(alpha = 0.2f)
                                else colors.cardBg
                            )
                            .then(
                                if (isActive) Modifier.border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                else Modifier
                            )
                            .clickable { activeFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${filteredSongs.size} SARKI",
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ManropeFontFamily,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Song list ───────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isRefreshing && filteredSongs.isEmpty()) {
                    item(key = "loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = colors.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }
                }

                if (filteredSongs.isEmpty() && !isRefreshing) {
                    item(key = "empty") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = colors.textDisabled,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Henuz sarki yok",
                                    color = colors.textMuted,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = ManropeFontFamily,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Ana menuден ilk parcanizi yukleyin",
                                    color = colors.textDisabled,
                                    fontSize = 13.sp,
                                    fontFamily = ManropeFontFamily,
                                )
                            }
                        }
                    }
                }

                items(filteredSongs, key = { it.songId }) { song ->
                    val iconIndex = song.songId.hashCode().and(0x7FFFFFFF) % songIcons.size
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                songToDelete = song
                            }
                            false
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(DesignTokens.radiusMedium))
                                    .background(colors.accentRed.copy(alpha = 0.85f))
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = colors.textMain,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        },
                        enableDismissFromStartToEnd = false,
                    ) {
                        ThemedSongCard(
                            song = song,
                            icon = songIcons[iconIndex],
                            onClick = { onSongClick(song.songId) },
                            onToggleFavorite = { viewModel.toggleFavorite(song) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemedSongCard(
    song: Song,
    icon: ImageVector,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit = {},
) {
    val colors = LocalAppColors.current
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
            .height(80.dp)
            .clickable(enabled = isReady) { onClick() },
        shape = RoundedCornerShape(DesignTokens.radiusMedium),
        colors = CardDefaults.cardColors(containerColor = colors.cardBg),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left accent line
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(colors.primaryGlow, colors.primary))
                    )
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Circle icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(colors.primary.copy(alpha = 0.25f), colors.primaryGlow.copy(alpha = 0.1f))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, modifier = Modifier.size(26.dp), tint = colors.primary)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    fontWeight = FontWeight.Bold,
                    fontFamily = ManropeFontFamily,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.textMain,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (song.bpm != null) {
                        Text(
                            text = "${song.bpm.toInt()} BPM",
                            fontSize = 12.sp,
                            color = colors.primary,
                            fontWeight = FontWeight.Medium,
                            fontFamily = ManropeFontFamily,
                        )
                    }
                    if (song.bpm != null && bestScore > 0) {
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    if (bestScore > 0) {
                        val formatted = NumberFormat.getNumberInstance(Locale.US).format(bestScore)
                        Text(
                            text = "HS: $formatted",
                            fontSize = 12.sp,
                            color = colors.accentGold,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = ManropeFontFamily,
                        )
                    }
                }
                if (song.durationMs != null) {
                    val minutes = song.durationMs / 60000
                    val seconds = (song.durationMs % 60000) / 1000
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${minutes}:${String.format("%02d", seconds)}",
                        fontSize = 11.sp,
                        color = colors.textDisabled,
                        fontFamily = ManropeFontFamily,
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (song.isFavorite) "Favoriden cikar" else "Favoriye ekle",
                    tint = if (song.isFavorite) colors.accentRed else colors.textMuted,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            when (song.status) {
                "processing" -> CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 2.dp,
                    color = colors.primary,
                )
                "error" -> Text(
                    "Hata",
                    color = colors.accentRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = ManropeFontFamily,
                )
                "ready" -> {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(colors.primary.copy(alpha = 0.2f), colors.primaryGlow.copy(alpha = 0.1f))
                                )
                            )
                            .clickable { onClick() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))
        }
    }
}
