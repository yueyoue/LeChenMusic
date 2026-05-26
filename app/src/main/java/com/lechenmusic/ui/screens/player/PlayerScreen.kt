package com.lechenmusic.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Song
import com.lechenmusic.player.MusicPlayerManager
import com.lechenmusic.player.RepeatMode
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage
import kotlinx.coroutines.launch

enum class PlayerView { COVER, LYRICS, SIMILAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerManager: MusicPlayerManager,
    viewModel: MainViewModel,
    serverUrl: String,
    username: String,
    password: String,
    onBack: () -> Unit,
    onShowPlaylist: () -> Unit,
    onShowMore: () -> Unit
) {
    val currentSong by playerManager.currentSong.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val progress by playerManager.progress.collectAsState()
    val currentPosition by playerManager.currentPosition.collectAsState()
    val duration by playerManager.duration.collectAsState()
    val shuffleMode by playerManager.shuffleMode.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()
    val isStarred by playerManager.isStarred.collectAsState()
    val playlist by playerManager.playlist.collectAsState()
    val currentIndex by playerManager.currentIndex.collectAsState()
    val currentLyrics by viewModel.currentLyrics.collectAsState()

    var currentView by remember { mutableStateOf(PlayerView.COVER) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }

    val song = currentSong ?: return

    // Load lyrics when song changes
    LaunchedEffect(song.id) {
        viewModel.loadLyrics(song)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "返回")
                }
                Text("正在播放", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = { showMoreSheet = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
            }

            // View Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                PlayerView.values().forEach { view ->
                    val label = when (view) {
                        PlayerView.COVER -> "封面"
                        PlayerView.LYRICS -> "歌词"
                        PlayerView.SIMILAR -> "列表"
                    }
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (currentView == view) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (currentView == view) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .clickable { currentView = view }
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (currentView == view) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content area with swipe
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -50) {
                                // Swipe left -> lyrics
                                currentView = when (currentView) {
                                    PlayerView.COVER -> PlayerView.LYRICS
                                    PlayerView.LYRICS -> PlayerView.SIMILAR
                                    PlayerView.SIMILAR -> PlayerView.SIMILAR
                                }
                            } else if (dragAmount > 50) {
                                // Swipe right -> cover
                                currentView = when (currentView) {
                                    PlayerView.SIMILAR -> PlayerView.LYRICS
                                    PlayerView.LYRICS -> PlayerView.COVER
                                    PlayerView.COVER -> PlayerView.COVER
                                }
                            }
                        }
                    }
            ) {
                when (currentView) {
                    PlayerView.COVER -> CoverView(song, serverUrl, username, password)
                    PlayerView.LYRICS -> LyricsView(song, currentLyrics, currentPosition)
                    PlayerView.SIMILAR -> PlaylistView(playlist, currentIndex, serverUrl, username, password) {
                        playerManager.playSong(it, playlist)
                    }
                }
            }

            // Song Info
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    song.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${song.artist} · ${song.album}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Progress Bar
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Slider(
                    value = progress,
                    onValueChange = { playerManager.seekToProgress(it) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPosition), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatTime(duration), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerManager.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "随机",
                        tint = if (shuffleMode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { playerManager.skipPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "上一曲", modifier = Modifier.size(32.dp))
                }
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp
                ) {
                    IconButton(onClick = { playerManager.togglePlayPause() }) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                IconButton(onClick = { playerManager.skipNext() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "下一曲", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { playerManager.toggleRepeat() }) {
                    Icon(
                        when (repeatMode) {
                            RepeatMode.OFF -> Icons.Default.Repeat
                            RepeatMode.ALL -> Icons.Default.Repeat
                            RepeatMode.ONE -> Icons.Default.RepeatOne
                        },
                        contentDescription = "循环",
                        tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { playerManager.toggleStar() }) {
                    Icon(
                        if (isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "收藏",
                        tint = if (isStarred) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("收藏", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showMoreSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "添加到", modifier = Modifier.size(24.dp))
                    Text("添加到", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showTimerDialog = true }) {
                    Icon(Icons.Default.Timer, contentDescription = "定时", modifier = Modifier.size(24.dp))
                    Text("定时", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // Timer Dialog
    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = { Text("定时停止播放") },
            text = {
                Column {
                    listOf("15分钟" to 15, "30分钟" to 30, "1小时" to 60, "2小时" to 120).forEach { (label, min) ->
                        Text(
                            label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerManager.setTimer(min)
                                    showTimerDialog = false
                                }
                                .padding(vertical = 14.dp),
                            fontSize = 15.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimerDialog = false }) { Text("取消") }
            }
        )
    }

    // Playlist Sheet
    if (showPlaylistSheet) {
        ModalBottomSheet(onDismissRequest = { showPlaylistSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("播放列表", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                playlist.forEachIndexed { index, s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playerManager.playSong(s, playlist)
                                showPlaylistSheet = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}",
                            fontSize = 13.sp,
                            color = if (index == currentIndex) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(30.dp),
                            textAlign = TextAlign.Center
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                s.title,
                                fontSize = 14.sp,
                                color = if (index == currentIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (index == currentIndex) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(s.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // More Sheet
    if (showMoreSheet) {
        ModalBottomSheet(onDismissRequest = { showMoreSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("更多操作", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                MoreItem(Icons.Default.PlaylistAdd, "添加到歌单") { showMoreSheet = false }
                MoreItem(Icons.Default.Timer, "定时停止播放") { showTimerDialog = true; showMoreSheet = false }
                MoreItem(Icons.Default.Download, "下载") { showMoreSheet = false }
                MoreItem(Icons.Default.Person, "查看歌手") { showMoreSheet = false }
                MoreItem(Icons.Default.Album, "查看专辑") { showMoreSheet = false }
            }
        }
    }
}

@Composable
private fun CoverView(song: Song, serverUrl: String, username: String, password: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CoverImage(
            coverArtId = song.coverArt ?: song.albumId,
            serverUrl = serverUrl,
            username = username,
            password = password,
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(20.dp))
        )
    }
}

/**
 * Parse LRC format lyrics: [mm:ss.xx]text
 * Returns list of (timeMs, text) sorted by time.
 */
private fun parseLrc(lrc: String): List<Pair<Long, String>> {
    val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
    val result = mutableListOf<Pair<Long, String>>()
    for (line in lrc.lines()) {
        val match = regex.matchEntire(line.trim())
        if (match != null) {
            val min = match.groupValues[1].toLongOrNull() ?: 0L
            val sec = match.groupValues[2].toLongOrNull() ?: 0L
            val msStr = match.groupValues[3]
            val ms = if (msStr.length == 2) msStr.toLongOrNull()?.times(10) ?: 0L
                     else msStr.toLongOrNull() ?: 0L
            val timeMs = min * 60_000 + sec * 1000 + ms
            val text = match.groupValues[4].trim()
            if (text.isNotBlank()) {
                result.add(timeMs to text)
            }
        }
    }
    return result.sortedBy { it.first }
}

@Composable
private fun LyricsView(song: Song, lyrics: String? = null, currentPosition: Long = 0L) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lyrics != null && lyrics.isNotBlank()) {
            val parsedLyrics = remember(lyrics) { parseLrc(lyrics) }
            val lazyListState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            // Determine active line index based on current position
            val activeIndex = remember(currentPosition, parsedLyrics) {
                if (parsedLyrics.isEmpty()) -1
                else {
                    var idx = -1
                    for (i in parsedLyrics.indices) {
                        if (currentPosition >= parsedLyrics[i].first) idx = i
                        else break
                    }
                    idx
                }
            }

            // Auto-scroll to active line
            LaunchedEffect(activeIndex) {
                if (activeIndex >= 0) {
                    lazyListState.animateScrollToItem(activeIndex, scrollOffset = -120)
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(parsedLyrics) { index, (_, text) ->
                    val isActive = index == activeIndex
                    Text(
                        text = text,
                        fontSize = if (isActive) 18.sp else 15.sp,
                        color = if (isActive) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "暂无歌词",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${song.artist} · ${song.album}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun PlaylistView(
    playlist: List<Song>,
    currentIndex: Int,
    serverUrl: String,
    username: String,
    password: String,
    onSongClick: (Song) -> Unit
) {
    val lazyListState = rememberLazyListState()

    // Auto-scroll to current playing song
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            lazyListState.animateScrollToItem(currentIndex, scrollOffset = -120)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        item {
            Text(
                "播放列表 (${playlist.size}首)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )
        }
        itemsIndexed(playlist) { index, song ->
            val isCurrent = index == currentIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSongClick(song) }
                    .background(
                        if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else Color.Transparent
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${index + 1}",
                    fontSize = 13.sp,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(30.dp),
                    textAlign = TextAlign.Center
                )
                CoverImage(
                    coverArtId = song.coverArt ?: song.albumId,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                )
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        song.title,
                        fontSize = 14.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                    )
                    Text(song.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isCurrent) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, fontSize = 15.sp)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
