package com.lechenmusic.ui.screens.albums

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Album
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage

@Composable
fun AlbumsScreen(
    viewModel: MainViewModel,
    onAlbumClick: (String) -> Unit
) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var sortType by remember { mutableStateOf("newest") }
    var isLoadingAll by remember { mutableStateOf(false) }

    LaunchedEffect(sortType) {
        if (sortType == "all") {
            isLoadingAll = true
            viewModel.loadAllAlbums { albums = it; isLoadingAll = false }
        } else {
            viewModel.loadAlbums(sortType) { albums = it }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "专辑",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // Sort tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("newest" to "最新", "recent" to "最近", "random" to "随机", "all" to "全部").forEach { (type, label) ->
                FilterChip(
                    selected = sortType == type,
                    onClick = { sortType = type },
                    label = { Text(label) }
                )
            }
        }

        if (albums.isEmpty() && isLoadingAll) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("加载全部专辑中...", fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
                }
            }
        } else if (albums.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(albums) { album ->
                    Column(modifier = Modifier.clickable { onAlbumClick(album.id) }) {
                        CoverImage(
                            coverArtId = album.coverArt,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        )
                        Text(
                            album.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            "${album.artist} · ${album.year ?: ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
