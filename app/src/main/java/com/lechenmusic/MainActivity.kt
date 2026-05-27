package com.lechenmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lechenmusic.ui.MainViewModel
import com.lechenmusic.ui.components.MiniPlayer
import com.lechenmusic.ui.navi.Screen
import com.lechenmusic.ui.screens.albums.AlbumDetailScreen
import com.lechenmusic.ui.screens.albums.AlbumsScreen
import com.lechenmusic.ui.screens.artists.ArtistDetailScreen
import com.lechenmusic.ui.screens.artists.ArtistsScreen
import com.lechenmusic.ui.screens.favorites.FavoritesScreen
import com.lechenmusic.ui.screens.home.HomeScreen
import com.lechenmusic.ui.screens.home.PlaylistDetailScreen
import com.lechenmusic.ui.screens.login.LoginScreen
import com.lechenmusic.ui.screens.player.PlayerScreen
import com.lechenmusic.ui.screens.recent.RecentPlayedScreen
import com.lechenmusic.ui.screens.search.SearchScreen
import com.lechenmusic.ui.screens.settings.SettingsScreen
import com.lechenmusic.ui.screens.songs.AllSongsScreen
import com.lechenmusic.ui.theme.LeChenMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = themeMode == "dark"

            LeChenMusicTheme(darkTheme = isDark) {
                LeChenMusicApp(viewModel)
            }
        }
    }
}

@Composable
fun LeChenMusicApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentSong by viewModel.playerManager.currentSong.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Bottom tab items
    data class BottomTab(val route: String, val label: String, val icon: ImageVector)
    val tabs = listOf(
        BottomTab(Screen.Home.route, "首页", Icons.Default.Home),
        BottomTab(Screen.Favorites.route, "收藏", Icons.Default.Favorite),
        BottomTab(Screen.Search.route, "搜索", Icons.Default.Search),
        BottomTab(Screen.Artists.route, "歌手", Icons.Default.Person),
        BottomTab(Screen.Albums.route, "专辑", Icons.Default.Album),
        BottomTab(Screen.AllSongs.route, "歌曲", Icons.Default.MusicNote)
    )

    val showBottomBar = currentRoute in tabs.map { it.route }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isLoggedIn) {
            LoginScreen(viewModel = viewModel, onLoginSuccess = { })
        } else {
            Scaffold(
                bottomBar = {
                    AnimatedVisibility(
                        visible = showBottomBar || (currentSong != null && currentRoute != Screen.Player.route),
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        Column {
                            // Mini Player
                            if (currentSong != null && currentRoute != Screen.Player.route) {
                                MiniPlayer(
                                    playerManager = viewModel.playerManager,
                                    serverUrl = serverUrl,
                                    username = username,
                                    password = password,
                                    onClick = { navController.navigate(Screen.Player.route) }
                                )
                            }
                            // Bottom Tab Bar
                            if (showBottomBar) {
                                NavigationBar {
                                    tabs.forEach { tab ->
                                        NavigationBarItem(
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label, fontSize = 10.sp) },
                                            selected = currentRoute == tab.route,
                                            onClick = {
                                                navController.navigate(tab.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            viewModel = viewModel,
                            onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) },
                            onSongClick = { song, playlist -> viewModel.playSong(song, playlist) },
                            onPlaylistClick = { navController.navigate(Screen.PlaylistDetail.createRoute(it)) },
                            onSettingsClick = { navController.navigate(Screen.Settings.route) },
                            onNavigateToAlbums = { navController.navigate(Screen.Albums.route) },
                            onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                            onNavigateToAllSongs = { navController.navigate(Screen.AllSongs.route) },
                            onNavigateToRecentPlayed = { navController.navigate(Screen.RecentPlayed.route) }
                        )
                    }
                    composable(Screen.Favorites.route) {
                        FavoritesScreen(
                            viewModel = viewModel,
                            onSongClick = { song, playlist -> viewModel.playSong(song, playlist) }
                        )
                    }
                    composable(Screen.Search.route) {
                        SearchScreen(
                            viewModel = viewModel,
                            onSongClick = { song, playlist -> viewModel.playSong(song, playlist) },
                            onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) },
                            onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) }
                        )
                    }
                    composable(Screen.Artists.route) {
                        ArtistsScreen(
                            viewModel = viewModel,
                            onArtistClick = { navController.navigate(Screen.ArtistDetail.createRoute(it)) }
                        )
                    }
                    composable(Screen.Albums.route) {
                        AlbumsScreen(
                            viewModel = viewModel,
                            onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) }
                        )
                    }
                    composable(Screen.AllSongs.route) {
                        AllSongsScreen(
                            viewModel = viewModel,
                            onSongClick = { song, playlist -> viewModel.playSong(song, playlist) }
                        )
                    }
                    composable(Screen.RecentPlayed.route) {
                        RecentPlayedScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onSongClick = { song, playlist -> viewModel.playSong(song, playlist) }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onLogout = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Screen.Player.route) {
                        PlayerScreen(
                            playerManager = viewModel.playerManager,
                            viewModel = viewModel,
                            serverUrl = serverUrl,
                            username = username,
                            password = password,
                            onBack = { navController.popBackStack() },
                            onShowPlaylist = { },
                            onShowMore = { },
                            onNavigateToArtist = { artistId ->
                                navController.navigate(Screen.ArtistDetail.createRoute(artistId))
                            },
                            onNavigateToAlbum = { albumId ->
                                navController.navigate(Screen.AlbumDetail.createRoute(albumId))
                            }
                        )
                    }
                    composable(Screen.AlbumDetail.route) { backStackEntry ->
                        val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                        AlbumDetailScreen(
                            viewModel = viewModel,
                            albumId = albumId,
                            onBack = { navController.popBackStack() },
                            onSongClick = { song, playlist -> viewModel.playSong(song, playlist) }
                        )
                    }
                    composable(Screen.ArtistDetail.route) { backStackEntry ->
                        val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
                        ArtistDetailScreen(
                            viewModel = viewModel,
                            artistId = artistId,
                            onBack = { navController.popBackStack() },
                            onAlbumClick = { navController.navigate(Screen.AlbumDetail.createRoute(it)) },
                            onSongClick = { song, playlist -> viewModel.playSong(song, playlist) }
                        )
                    }
                    composable(Screen.PlaylistDetail.route) { backStackEntry ->
                        val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                        PlaylistDetailScreen(
                            viewModel = viewModel,
                            playlistId = playlistId,
                            onBack = { navController.popBackStack() },
                            onSongClick = { song, playlist -> viewModel.playSong(song, playlist) }
                        )
                    }
                }
            }
        }
    }
}
