package com.lechenmusic.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.AlbumCard
import com.lechenmusic.ui.components.SongItem

@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit
) {
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Text(
            "搜索",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // Search Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (it.length >= 2) viewModel.search(it)
                },
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索歌曲、歌手、专辑") },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; viewModel.search("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清除")
                        }
                    }
                }
            )
            if (query.isNotEmpty()) {
                Text(
                    "取消",
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable { query = ""; viewModel.search("") },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tabs
        if (searchResults != null) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(top = 16.dp),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                Tab(selectedTab == 0, onClick = { selectedTab = 0 }) { Text("歌曲", modifier = Modifier.padding(12.dp)) }
                Tab(selectedTab == 1, onClick = { selectedTab = 1 }) { Text("歌手", modifier = Modifier.padding(12.dp)) }
                Tab(selectedTab == 2, onClick = { selectedTab = 2 }) { Text("专辑", modifier = Modifier.padding(12.dp)) }
            }
        }

        // Results
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 160.dp)) {
            if (searchResults == null) {
                // Hot searches
                item {
                    Text(
                        "🔥 热门搜索",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                }
                item {
                    val hotTags = listOf("周杰伦", "林俊杰", "陈奕迅", "薛之谦", "邓紫棋", "五月天", "Taylor Swift", "周深")
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        hotTags.forEach { tag ->
                            SuggestionChip(
                                onClick = { query = tag; viewModel.search(tag) },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        val songs = searchResults?.song ?: emptyList()
                        items(songs) { song ->
                            SongItem(
                                song = song,
                                serverUrl = serverUrl,
                                username = username,
                                password = password,
                                onClick = { onSongClick(song, songs) }
                            )
                        }
                    }
                    1 -> {
                        val artists = searchResults?.artist ?: emptyList()
                        items(artists) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onArtistClick(artist.id) }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(56.dp),
                                    shape = RoundedCornerShape(28.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {}
                                Column(modifier = Modifier.padding(start = 14.dp)) {
                                    Text(artist.name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text("${artist.albumCount} 张专辑", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    2 -> {
                        val albums = searchResults?.album ?: emptyList()
                        items(albums.chunked(2)) { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                row.forEach { album ->
                                    AlbumCard(
                                        album = album,
                                        serverUrl = serverUrl,
                                        username = username,
                                        password = password,
                                        onClick = { onAlbumClick(album.id) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable () -> Unit
) {
    // Simple flow row implementation
    Row(modifier = modifier, horizontalArrangement = horizontalArrangement) {
        content()
    }
}
