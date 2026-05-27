package com.lechenmusic.ui.screens.songs

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.SongItem

@Composable
fun AllSongsScreen(
    viewModel: MainViewModel,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val allSongs by viewModel.allSongs.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val songsLoading by viewModel.songsLoading.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (allSongs.isEmpty()) {
            viewModel.loadAllSongs()
        }
    }

    // Show loading toast
    LaunchedEffect(songsLoading) {
        if (songsLoading) {
            Toast.makeText(context, "加载中，请稍等", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "歌曲",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                if (songsLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "加载中...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (allSongs.isNotEmpty()) {
            item {
                Text(
                    "${allSongs.size} 首歌曲",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                )
            }
            items(allSongs) { song ->
                SongItem(
                    song = song,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    onClick = { onSongClick(song, allSongs) }
                )
            }
        } else if (!songsLoading) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "暂无歌曲",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadAllSongs() }) {
                        Text("重新加载")
                    }
                }
            }
        }
    }
}
