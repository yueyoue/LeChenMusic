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
import androidx.compose.ui.graphics.vector.ImageVector
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
    onNavigateToRadio: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToArtists: () -> Unit = {},
    onNavigateToAllPlaylists: () -> Unit = {}
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
    val allSongs by viewModel.allSongs.collectAsState()
    val cachedSongs by viewModel.cachedSongs.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 搜索框
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clickable { onNavigateToSearch() },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "搜索歌曲、专辑、歌手...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // 快捷入口:歌手、专辑、歌单、电台
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickAccessButton(
                    icon = Icons.Default.Person,
                    label = "歌手",
                    color = Color(0xFFA55EEA),
                    onClick = onNavigateToArtists
                )
                QuickAccessButton(
                    icon = Icons.Default.Album,
                    label = "专辑",
                    color = Color(0xFF5352ED),
                    onClick = onNavigateToAlbums
                )
                QuickAccessButton(
                    icon = Icons.Default.LibraryMusic,
                    label = "歌单",
                    color = Color(0xFF2ED573),
                    onClick = onNavigateToAllPlaylists
                )
                QuickAccessButton(
                    icon = Icons.Default.Headphones,
                    label = "电台",
                    color = Color(0xFFFF4757),
                    onClick = onNavigateToRadio
                )
            }
        }

        // 最新专辑
        if (newestAlbums.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
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

        // 歌单
        if (playlists.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(28.dp))
                SectionHeader("📋 歌单", "更多 ›") {
                    onNavigateToAllPlaylists()
                }
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

        // 缓存音乐
        item {
            Spacer(modifier = Modifier.height(28.dp))
            SectionHeader("💾 缓存音乐", if (cachedSongs.isNotEmpty()) "全部 ›" else null) {
                if (cachedSongs.isNotEmpty()) onNavigateToAllSongs()
            }
        }
        if (cachedSongs.isNotEmpty()) {
            items(cachedSongs.take(5)) { song ->
                SongItem(
                    song = song,
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    onClick = { onSongClick(song, cachedSongs) }
                )
            }
        } else {
            item {
                Text(
                    "暂无缓存音乐,播放过的歌曲将自动缓存",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
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
private fun QuickAccessButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
