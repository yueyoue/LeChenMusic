package com.lechenmusic.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.lechenmusic.data.model.Playlist
import com.lechenmusic.data.model.Song
import com.lechenmusic.player.MusicPlayerManager
import com.lechenmusic.player.RepeatMode
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage

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
    onShowMore: () -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {}
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
    val playlists by viewModel.playlists.collectAsState()

    var currentView by remember { mutableStateOf(PlayerView.COVER) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showPlaylistSelectionDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    val timerRemainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val context = LocalContext.current

    // Show toast messages
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

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
                        PlayerView.SIMILAR -> "推荐"
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
                                currentView = when (currentView) {
                                    PlayerView.COVER -> PlayerView.LYRICS
                                    PlayerView.LYRICS -> PlayerView.SIMILAR
                                    PlayerView.SIMILAR -> PlayerView.SIMILAR
                                }
                            } else if (dragAmount > 50) {
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
                    PlayerView.LYRICS -> LyricsView(song, currentLyrics, currentPosition, duration)
                    PlayerView.SIMILAR -> SimilarView(playlist, currentIndex, serverUrl, username, password) {
                        playerManager.playSong(it, playlist)
                    }
                }
            }

            // Song Info
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        song.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Quality badge
                    val qualityText = getQualityBadge(song)
                    if (qualityText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = getQualityBadgeColor(song).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = qualityText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = getQualityBadgeColor(song),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
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

            // Action Bar: 收藏, 添加到歌单, 定时, 队列
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
                    modifier = Modifier.clickable { showPlaylistSelectionDialog = true }) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "添加到歌单", modifier = Modifier.size(24.dp))
                    Text("添加到", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showTimerDialog = true }) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = "定时",
                        tint = if (timerRemainingSeconds > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        if (timerRemainingSeconds > 0) {
                            val min = timerRemainingSeconds / 60
                            val sec = timerRemainingSeconds % 60
                            "%d:%02d".format(min, sec)
                        } else "定时",
                        fontSize = 10.sp,
                        color = if (timerRemainingSeconds > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Queue button (新增)
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showQueueSheet = true }) {
                    Icon(Icons.Default.QueueMusic, contentDescription = "队列",
                        modifier = Modifier.size(24.dp))
                    Text("队列", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    if (timerRemainingSeconds > 0) {
                        val remainMin = timerRemainingSeconds / 60
                        val remainSec = timerRemainingSeconds % 60
                        Text(
                            "剩余时间: ${remainMin}分${remainSec}秒",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            "取消定时",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.cancelTimerWithCountdown()
                                    showTimerDialog = false
                                }
                                .padding(vertical = 14.dp),
                            fontSize = 15.sp,
                            color = Color.Red
                        )
                        HorizontalDivider()
                    }
                    listOf("15分钟" to 15, "30分钟" to 30, "1小时" to 60, "2小时" to 120).forEach { (label, min) ->
                        Text(
                            label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTimerWithCountdown(min)
                                    showTimerDialog = false
                                }
                                .padding(vertical = 14.dp),
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimerDialog = false }) { Text("取消") }
            }
        )
    }

    // Playlist Selection Dialog (for adding song to a playlist)
    if (showPlaylistSelectionDialog) {
        var showCreatePlaylist by remember { mutableStateOf(false) }
        var newPlaylistName by remember { mutableStateOf("") }
        var newPlaylistPublic by remember { mutableStateOf(false) }
        val currentUser by viewModel.username.collectAsState()

        AlertDialog(
            onDismissRequest = { showPlaylistSelectionDialog = false },
            title = { Text("添加到歌单") },
            text = {
                Column {
                    // Create new playlist option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreatePlaylist = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Add, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("新建歌单", fontSize = 15.sp, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    if (playlists.isEmpty()) {
                        Text("暂无歌单", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        playlists.forEach { pl ->
                            val isShared = pl.owner.isNotBlank() && pl.owner != currentUser
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isShared) {
                                            Toast.makeText(context, "不能添加歌曲到别人的歌单", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.addToPlaylist(pl.id, song.id, pl.owner)
                                            showPlaylistSelectionDialog = false
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlaylistPlay, contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(pl.name, fontSize = 15.sp)
                                        if (isShared) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFFFA502).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    "共享",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFFA502),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        "${pl.songCount}首${if (isShared) " · ${pl.owner}" else ""}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isShared) {
                                    Icon(Icons.Default.Lock, contentDescription = "不可添加",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistSelectionDialog = false }) { Text("取消") }
            }
        )

        // Create playlist sub-dialog
        if (showCreatePlaylist) {
            AlertDialog(
                onDismissRequest = { showCreatePlaylist = false },
                title = { Text("新建歌单") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            placeholder = { Text("输入歌单名称") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("公开歌单", fontSize = 14.sp)
                            Switch(
                                checked = newPlaylistPublic,
                                onCheckedChange = { newPlaylistPublic = it }
                            )
                        }
                        Text(
                            if (newPlaylistPublic) "其他用户可以看到此歌单" else "仅自己可见",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylistAndAddSong(newPlaylistName, song.id, newPlaylistPublic)
                            newPlaylistName = ""
                            newPlaylistPublic = false
                            showCreatePlaylist = false
                            showPlaylistSelectionDialog = false
                        }
                    }) { Text("创建", color = MaterialTheme.colorScheme.primary) }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylist = false; newPlaylistName = ""; newPlaylistPublic = false }) { Text("取消") }
                }
            )
        }
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

    // Queue Sheet (新增 - 当前播放队列)
    if (showQueueSheet) {
        ModalBottomSheet(onDismissRequest = { showQueueSheet = false }) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("播放队列", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${playlist.size} 首歌曲",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    itemsIndexed(playlist) { index, s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerManager.playSong(s, playlist)
                                    showQueueSheet = false
                                }
                                .background(
                                    if (index == currentIndex) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (index == currentIndex) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp).width(30.dp)
                                )
                            } else {
                                Text(
                                    "${index + 1}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(30.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                            CoverImage(
                                coverArtId = s.coverArt ?: s.albumId,
                                serverUrl = serverUrl,
                                username = username,
                                password = password,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
                            )
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    s.title,
                                    fontSize = 14.sp,
                                    color = if (index == currentIndex) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (index == currentIndex) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${s.artist} · ${s.album}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                s.durationFormatted,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                MoreItem(Icons.Default.PlaylistAdd, "添加到歌单") {
                    showMoreSheet = false
                    showPlaylistSelectionDialog = true
                }
                MoreItem(Icons.Default.Timer, "定时停止播放") {
                    showMoreSheet = false
                    showTimerDialog = true
                }
                MoreItem(Icons.Default.QueueMusic, "播放队列") {
                    showMoreSheet = false
                    showQueueSheet = true
                }
                MoreItem(Icons.Default.Person, "查看歌手") {
                    showMoreSheet = false
                    if (song.artistId.isNotBlank()) {
                        onNavigateToArtist(song.artistId)
                    }
                }
                MoreItem(Icons.Default.Album, "查看专辑") {
                    showMoreSheet = false
                    if (song.albumId.isNotBlank()) {
                        onNavigateToAlbum(song.albumId)
                    }
                }
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

@Composable
private fun LyricsView(
    song: Song,
    lyrics: String? = null,
    currentPosition: Long = 0L,
    duration: Long = 0L
) {
    val lines = remember(lyrics) {
        lyrics?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
    }

    // Calculate which line should be highlighted based on playback position
    val activeLineIndex = remember(currentPosition, duration, lines.size) {
        if (lines.isEmpty() || duration <= 0L) 0
        else {
            val progress = currentPosition.toFloat() / duration.toFloat()
            (progress * lines.size).toInt().coerceIn(0, lines.lastIndex)
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll to active line
    LaunchedEffect(activeLineIndex) {
        if (lines.isNotEmpty() && activeLineIndex in lines.indices) {
            listState.animateScrollToItem(activeLineIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lines.isNotEmpty()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(lines) { index, line ->
                    Text(
                        text = line.trim(),
                        fontSize = if (index == activeLineIndex) 18.sp else 16.sp,
                        fontWeight = if (index == activeLineIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == activeLineIndex)
                            MaterialTheme.colorScheme.onBackground
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
private fun SimilarView(
    playlist: List<Song>,
    currentIndex: Int,
    serverUrl: String,
    username: String,
    password: String,
    onSongClick: (Song) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Text(
                "推荐歌曲",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
                textAlign = TextAlign.Center
            )
        }
        val similar = playlist.filterIndexed { i, _ -> i != currentIndex }.take(10)
        items(similar) { song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSongClick(song) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverImage(
                    coverArtId = song.coverArt ?: song.albumId,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                )
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(song.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(song.artist, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun getQualityBadge(song: Song): String {
    val suffix = song.suffix.uppercase()
    val bitRate = song.bitRate
    return when {
        suffix == "FLAC" -> if (bitRate > 0) "FLAC ${bitRate}K" else "FLAC"
        suffix == "DSD" -> "DSD"
        suffix == "WAV" || suffix == "AIFF" -> suffix
        suffix == "MP3" && bitRate >= 320 -> "MP3 320K"
        suffix == "AAC" && bitRate >= 256 -> "AAC ${bitRate}K"
        suffix.isNotEmpty() && bitRate > 0 -> "$suffix ${bitRate}K"
        suffix.isNotEmpty() -> suffix
        else -> ""
    }
}

private fun getQualityBadgeColor(song: Song): Color {
    val suffix = song.suffix.uppercase()
    return when {
        suffix == "FLAC" || suffix == "DSD" || suffix == "WAV" || suffix == "AIFF" -> Color(0xFFFF6B81)
        song.bitRate >= 320 -> Color(0xFF5352ED)
        else -> Color(0xFF2ED573)
    }
}
