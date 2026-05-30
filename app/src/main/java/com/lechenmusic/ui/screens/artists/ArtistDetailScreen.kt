package com.lechenmusic.ui.screens.artists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.AlbumCard
import com.lechenmusic.ui.components.CoverImage

@Composable
fun ArtistDetailScreen(
    viewModel: MainViewModel,
    artistId: String,
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit
) {
    val artist by viewModel.currentArtist.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    LaunchedEffect(artistId) {
        viewModel.loadArtistDetail(artistId)
    }

    val currentArtist = artist ?: return

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
                Text("歌手详情", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Artist Info
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentArtist.artistImageUrl != null) {
                    AsyncImage(
                        model = currentArtist.artistImageUrl,
                        contentDescription = currentArtist.name,
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(80.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CoverImage(
                        coverArtId = currentArtist.coverArt,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(80.dp))
                    )
                }
                Text(
                    currentArtist.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    "${currentArtist.albumCount} 张专辑",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Albums
        if (!currentArtist.album.isNullOrEmpty()) {
            item {
                Text(
                    "专辑",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
            items(currentArtist.album!!.chunked(2)) { row ->
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
