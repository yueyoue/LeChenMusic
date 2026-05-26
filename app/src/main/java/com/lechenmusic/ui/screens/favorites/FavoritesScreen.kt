package com.lechenmusic.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun FavoritesScreen(
    viewModel: MainViewModel,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val starredSongs by viewModel.starredSongs.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
        item {
            Text(
                "我的收藏",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }

        item {
            Row(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("${starredSongs.size} 首歌曲", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (starredSongs.isEmpty()) {
            item {
                Text(
                    "暂无收藏歌曲",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(starredSongs) { song ->
            SongItem(
                song = song,
                serverUrl = serverUrl,
                username = username,
                password = password,
                onClick = { onSongClick(song, starredSongs) }
            )
        }
    }
}
