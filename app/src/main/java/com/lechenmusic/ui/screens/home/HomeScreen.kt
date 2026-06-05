package com.lechenmusic.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lechenmusic.data.model.Album
import com.lechenmusic.data.model.Song
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onAlbumClick: (String) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToAlbums: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToAllSongs: () -> Unit = {},
    onNavigateToRecentPlayed: () -> Unit = {},
    onNavigateToRadio: () -> Unit = {}
) {
    val newestAlbums by viewModel.newestAlbums.collectAsState()
    val randomAlbums by viewModel.randomAlbums.collectAsState()
    val dailySongs by viewModel.dailySongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recentPlayedSongs by viewModel.recentPlayedSongs.collectAsState()
    val radioStations by viewModel.radioStations.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 160.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("悦音", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            }
        }

        // 最新专辑
        if (newestAlbums.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(28.dp))
                SectionHeader("🆕 最新专辑", "更多 ›") { onNavigateToAlbums() }
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    items(newestAlbums) { album ->
                        AlbumCard(
                            album = album,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onClick = { onAlbumClick(album.id) }
                        )
                    }
                }
            }
        }

        // 每日推荐
        if (dailySongs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(28.dp))
                SectionHeader("🎯 每日推荐", "换一批 ↻") { viewModel.refreshDailySongs() }
            }
            items(dailySongs.take(5)) { song ->
                SongItem(
                    song = song,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    onClick = { onSongClick(song, dailySongs) }
                )
            }
        }

        // 随机专辑
        if (randomAlbums.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(28.dp))
                SectionHeader("🎲 随机专辑", "换一批 ↻") { viewModel.refreshRandomAlbums() }
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    items(randomAlbums) { album ->
                        AlbumCard(
                            album = album,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onClick = { onAlbumClick(album.id) }
                        )
                    }
                }
            }
        }

        // 最近播放
        item {
            Spacer(modifier = Modifier.height(28.dp))
            SectionHeader("⏱️ 最近播放", "更多 ›") { onNavigateToRecentPlayed() }
        }
        if (recentPlayedSongs.isNotEmpty()) {
            items(recentPlayedSongs.take(5)) { song ->
                SongItem(
                    song = song,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    onClick = { onSongClick(song, recentPlayedSongs) }
                )
            }
        } else {
            item {
                Text(
                    "播放歌曲后将显示在此处",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }

        // 歌单
        if (playlists.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(28.dp))
                SectionHeader("📋 歌单", "同步 ↻") { viewModel.syncPlaylists() }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    playlists.take(3).forEach { playlist ->
                        PlaylistCard(
                            name = playlist.name,
                            count = playlist.songCount,
                            coverArt = playlist.coverArt,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onClick = { onPlaylistClick(playlist.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 电台 (from Navidrome server)
        if (radioStations.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(28.dp))
                SectionHeader("📻 电台", "更多 ›") { onNavigateToRadio() }
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
                ) {
                    val radioColors = listOf(
                        Color(0xFFFF4757),
                        Color(0xFFA55EEA),
                        Color(0xFF5352ED),
                        Color(0xFF2ED573),
                        Color(0xFF1E90FF),
                        Color(0xFFFF6348)
                    )
                    items(radioStations.take(6)) { station ->
                        val color = radioColors[radioStations.indexOf(station) % radioColors.size]
                        RadioCard(
                            name = station.name,
                            desc = "网络电台",
                            color = color,
                            onClick = { viewModel.playerManager.playRadioStation(station) }
                        )
                    }
                }
            }
        } else {
            item {
                Spacer(modifier = Modifier.height(28.dp))
                SectionHeader("📻 电台") { }
            }
            item {
                Text(
                    "暂无电台数据",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    name: String,
    count: Int,
    coverArt: String?,
    serverUrl: String,
    username: String,
    password: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        CoverImage(
            coverArtId = coverArt,
            serverUrl = serverUrl,
            username = username,
            password = password,
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        )
        Text(
            name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "${count}首",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RadioCard(
    name: String,
    desc: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(200.dp)
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = color
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Headphones, contentDescription = null, tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
