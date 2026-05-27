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
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val currentUser by viewModel.username.collectAsState()
    val context = LocalContext.current

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
}
