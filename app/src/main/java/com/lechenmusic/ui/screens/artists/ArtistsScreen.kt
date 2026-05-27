package com.lechenmusic.ui.screens.artists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.lechenmusic.data.model.Artist
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.CoverImage

@Composable
fun ArtistsScreen(
    viewModel: MainViewModel,
    onArtistClick: (String) -> Unit
) {
    val artists by viewModel.artists.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadArtists()
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 160.dp)) {
        item {
            Text(
                "歌手",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(21.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("搜索歌手", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        }

        if (artists.isEmpty()) {
            item {
                Text(
                    "加载中...",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(artists) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artist.id) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Use artistImageUrl first (full URL), fallback to coverArt ID, then use artist ID as coverArt
                val coverModifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable { onArtistClick(artist.id) }
                val effectiveCoverArt = artist.coverArt ?: artist.id
                if (artist.artistImageUrl != null) {
                    AsyncImage(
                        model = artist.artistImageUrl,
                        contentDescription = artist.name,
                        modifier = coverModifier,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CoverImage(
                        coverArtId = effectiveCoverArt,
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                        modifier = coverModifier
                    )
                }
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(artist.name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "${artist.albumCount} 张专辑",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (artists.indexOf(artist) < artists.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}
