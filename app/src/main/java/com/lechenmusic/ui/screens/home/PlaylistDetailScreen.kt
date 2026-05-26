package com.lechenmusic.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.SongItem

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

    LaunchedEffect(playlistId) {
        if (playlistId.isNotBlank()) {
            viewModel.loadPlaylistDetail(playlistId)
        }
    }

    // Reset playlist when leaving
    DisposableEffect(Unit) {
        onDispose {
            // Don't reset, keep for back navigation
        }
    }

    val currentPlaylist = playlist

    if (currentPlaylist == null) {
        // Loading state
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

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
                Text(currentPlaylist.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
                        val songs = currentPlaylist.song ?: emptyList()
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
        itemsIndexed(currentPlaylist.song ?: emptyList()) { index, song ->
            SongItem(
                song = song,
                serverUrl = serverUrl,
                username = username,
                password = password,
                onClick = { onSongClick(song, currentPlaylist.song ?: emptyList()) },
                trailing = {
                    Text("${index + 1}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )
        }
    }
}
