package com.lechenmusic.ui.screens.allsongs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllSongsScreen(
    viewModel: MainViewModel,
    initialType: String = "newest",
    onBack: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    var selectedTab by remember {
        mutableIntStateOf(
            when (initialType) {
                "newest" -> 0
                "recent" -> 1
                "random" -> 2
                else -> 0
            }
        )
    }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val tabType = when (selectedTab) {
        0 -> "newest"
        1 -> "recent"
        2 -> "random"
        else -> "newest"
    }

    LaunchedEffect(selectedTab) {
        isLoading = true
        viewModel.loadAllSongs(tabType) { loadedSongs ->
            songs = loadedSongs
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全部歌曲", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (songs.isNotEmpty()) {
                        IconButton(onClick = { onSongClick(songs.first(), songs) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "播放全部")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("最新") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("最近") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("随机") }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "暂无歌曲",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 160.dp)
                ) {
                    itemsIndexed(songs) { index, song ->
                        SongItem(
                            song = song,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onClick = { onSongClick(song, songs) },
                            trailing = {
                                Text(
                                    "${index + 1}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
