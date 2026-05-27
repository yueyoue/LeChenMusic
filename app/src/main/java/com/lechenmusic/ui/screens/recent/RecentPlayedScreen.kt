package com.lechenmusic.ui.screens.recent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.SongItem

@Composable
fun RecentPlayedScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val recentPlayedSongs by viewModel.recentPlayedSongs.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text("最近播放", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Row(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "${recentPlayedSongs.size} 首歌曲",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (recentPlayedSongs.isEmpty()) {
            item {
                Text(
                    "暂无最近播放记录",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(recentPlayedSongs) { song ->
            SongItem(
                song = song,
                serverUrl = serverUrl,
                username = username,
                password = password,
                onClick = { onSongClick(song, recentPlayedSongs) }
            )
        }
    }
}
