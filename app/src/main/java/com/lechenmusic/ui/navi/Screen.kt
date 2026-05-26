package com.lechenmusic.ui.navi

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Favorites : Screen("favorites")
    object Search : Screen("search")
    object Artists : Screen("artists")
    object Albums : Screen("albums")
    object Settings : Screen("settings")
    object Player : Screen("player")
    object ArtistDetail : Screen("artist_detail/{artistId}") {
        fun createRoute(artistId: String) = "artist_detail/$artistId"
    }
    object AlbumDetail : Screen("album_detail/{albumId}") {
        fun createRoute(albumId: String) = "album_detail/$albumId"
    }
    object AllSongs : Screen("all_songs/{songType}") {
        fun createRoute(songType: String) = "all_songs/$songType"
    }
    object PlaylistDetail : Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist_detail/$playlistId"
    }
}
