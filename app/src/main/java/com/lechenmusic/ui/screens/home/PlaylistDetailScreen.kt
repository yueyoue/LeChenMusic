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

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showPublicDialog by remember { mutableStateOf(false) }

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
                Text(
                    "${currentPlaylist.songCount} 首 · ${currentPlaylist.owner}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Play all button
                    Button(
                        onClick = {
                            val songs = currentPlaylist.songs
                            if (songs.isNotEmpty()) onSongClick(songs.first(), songs)
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("播放全部")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Create playlist button
                    OutlinedButton(
                        onClick = { showCreatePlaylistDialog = true },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("创建歌单")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Sync button
                    OutlinedButton(
                        onClick = {
                            viewModel.syncPlaylists()
                            viewModel.loadPlaylistDetail(playlistId)
                            Toast.makeText(context, "正在同步歌单...", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                // Public/Private toggle (for owner)
                if (isOwner) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (currentPlaylist.public) Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (currentPlaylist.public) "公开歌单" else "私密歌单",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = currentPlaylist.public,
                            onCheckedChange = { showPublicDialog = true }
                        )
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
    if (showCreatePlaylistDialog) {
        var newPlaylistName by remember { mutableStateOf("") }
        var newPlaylistPublic by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
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
                        viewModel.createPlaylist(newPlaylistName, newPlaylistPublic)
                        showCreatePlaylistDialog = false
                    }
                }) { Text("创建", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("取消") }
            }
        )
    }

    // Public/Private toggle confirmation dialog
    if (showPublicDialog) {
        val newPublic = !currentPlaylist.public
        AlertDialog(
            onDismissRequest = { showPublicDialog = false },
            title = { Text(if (newPublic) "设为公开" else "设为私密") },
            text = {
                Text(if (newPublic) "确定要将此歌单设为公开吗？其他用户将可以看到此歌单。"
                else "确定要将此歌单设为私密吗？其他用户将无法看到此歌单。")
            },
            confirmButton = {
                TextButton(onClick = {
                    // Use updatePlaylist API to toggle public
                    viewModel.togglePlaylistPublic(playlistId, newPublic)
                    showPublicDialog = false
                }) { Text("确定", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showPublicDialog = false }) { Text("取消") }
            }
        )
    }
}
