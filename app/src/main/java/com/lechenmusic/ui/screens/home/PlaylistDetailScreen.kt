package com.lechenmusic.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    viewModel: MainViewModel,
    playlistId: String,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val playlist by viewModel.currentPlaylist.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val currentUser by viewModel.username.collectAsState()
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistPublic by remember { mutableStateOf(false) }
    var showPublicToggleDialog by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }

    // Show toast messages
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylistDetail(playlistId)
    }

    val currentPlaylist = playlist

    if (currentPlaylist == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isOwner = currentPlaylist.owner.isBlank() || currentPlaylist.owner == currentUser
    var showRemoveDialog by remember { mutableStateOf<Pair<Int, Song>?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text("歌单详情", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                // Sync button
                IconButton(
                    onClick = {
                        isSyncing = true
                        viewModel.loadPlaylistDetail(playlistId)
                        viewModel.syncPlaylists()
                        Toast.makeText(context, "同步中...", Toast.LENGTH_SHORT).show()
                        isSyncing = false
                    }
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = "同步歌单",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Playlist Info
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(currentPlaylist.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (!isOwner) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFA502).copy(alpha = 0.15f)
                        ) {
                            Text(
                                "共享歌单",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFA502),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                if (!currentPlaylist.comment.isNullOrBlank()) {
                    Text(
                        currentPlaylist.comment!!,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        "${currentPlaylist.songCount} 首 · ${currentPlaylist.owner}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isOwner) {
                        Spacer(modifier = Modifier.width(12.dp))
                        // Public/Private toggle button
                        Surface(
                            onClick = { showPublicToggleDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentPlaylist.public) Color(0xFF2ED573).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (currentPlaylist.public) Icons.Default.Public else Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (currentPlaylist.public) Color(0xFF2ED573)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (currentPlaylist.public) "公开" else "私密",
                                    fontSize = 11.sp,
                                    color = if (currentPlaylist.public) Color(0xFF2ED573)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val songs = currentPlaylist.songs
                            if (songs.isNotEmpty()) onSongClick(songs.first(), songs)
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("播放全部")
                    }
                    // Create playlist button
                    OutlinedButton(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("创建歌单")
                    }
                }
            }
        }

        // Songs
        itemsIndexed(currentPlaylist.songs) { index, song ->
            SongItem(
                song = song,
                serverUrl = serverUrl,
                username = username,
                password = password,
                onClick = { onSongClick(song, currentPlaylist.songs) },
                trailing = {
                    if (isOwner) {
                        IconButton(
                            onClick = { showRemoveDialog = index to song },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "移除",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text("${index + 1}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }

    // Remove song confirmation dialog
    showRemoveDialog?.let { (index, song) ->
        AlertDialog(
            onDismissRequest = { showRemoveDialog = null },
            title = { Text("移除歌曲") },
            text = { Text("确定要将「${song.title}」从歌单中移除吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeFromPlaylist(playlistId, index)
                    showRemoveDialog = null
                }) { Text("移除", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = null }) { Text("取消") }
            }
        )
    }

    // Create playlist dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("创建歌单") },
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
                        viewModel.createPlaylistAndAddSong(newPlaylistName, "", newPlaylistPublic)
                        newPlaylistName = ""
                        newPlaylistPublic = false
                        showCreateDialog = false
                    }
                }) { Text("创建", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newPlaylistName = ""
                    newPlaylistPublic = false
                }) { Text("取消") }
            }
        )
    }

    // Public/Private toggle dialog
    if (showPublicToggleDialog) {
        AlertDialog(
            onDismissRequest = { showPublicToggleDialog = false },
            title = { Text("歌单可见性") },
            text = {
                Column {
                    Text(
                        "当前状态: ${if (currentPlaylist.public) "公开" else "私密"}",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        if (currentPlaylist.public) "切换为私密后，其他用户将无法看到此歌单"
                        else "切换为公开后，其他用户可以看到此歌单",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updatePlaylistPublic(playlistId, !currentPlaylist.public)
                    showPublicToggleDialog = false
                }) {
                    Text(
                        if (currentPlaylist.public) "设为私密" else "设为公开",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublicToggleDialog = false }) { Text("取消") }
            }
        )
    }
}
