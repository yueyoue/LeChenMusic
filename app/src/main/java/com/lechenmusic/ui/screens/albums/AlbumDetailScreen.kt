package com.lechenmusic.ui.screens.albums

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage
import com.lechenmusic.ui.components.SongItem

@Composable
fun AlbumDetailScreen(
    viewModel: MainViewModel,
    albumId: String,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val album by viewModel.currentAlbum.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    LaunchedEffect(albumId) {
        viewModel.loadAlbumDetail(albumId)
    }

    val currentAlbum = album ?: return

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
                Text("专辑详情", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Album Info
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverImage(
                    coverArtId = currentAlbum.coverArt,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(currentAlbum.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(currentAlbum.artist, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${currentAlbum.songCount} 首 · ${currentAlbum.year ?: ""}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val songs = currentAlbum.song ?: emptyList()
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
        }

        // Songs
        itemsIndexed(currentAlbum.song ?: emptyList()) { index, song ->
            SongItem(
                song = song,
                serverUrl = serverUrl,
                username = username,
                password = password,
                onClick = { onSongClick(song, currentAlbum.song ?: emptyList()) },
                trailing = {
                    Text("${index + 1}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )
        }
    }
}
