package com.lechenmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lechenmusic.LeChenApp
import com.lechenmusic.data.model.*
import com.lechenmusic.data.repository.MusicRepository
import com.lechenmusic.data.repository.SettingsRepository
import com.lechenmusic.data.repository.ServerStats
import com.lechenmusic.player.MusicPlayerManager
import com.lechenmusic.player.RepeatMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LeChenApp
    private val repository: MusicRepository = app.repository
    private val settings: SettingsRepository = app.settingsRepository
    val playerManager: MusicPlayerManager = app.playerManager

    // Theme
    val themeMode: StateFlow<String> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "dark")

    // Login state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Home data
    private val _newestAlbums = MutableStateFlow<List<Album>>(emptyList())
    val newestAlbums: StateFlow<List<Album>> = _newestAlbums.asStateFlow()

    private val _randomAlbums = MutableStateFlow<List<Album>>(emptyList())
    val randomAlbums: StateFlow<List<Album>> = _randomAlbums.asStateFlow()

    private val _dailySongs = MutableStateFlow<List<Song>>(emptyList())
    val dailySongs: StateFlow<List<Song>> = _dailySongs.asStateFlow()

    private val _recentSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentSongs: StateFlow<List<Song>> = _recentSongs.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // Artists
    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    // Search
    private val _searchResults = MutableStateFlow<SearchResult?>(null)
    val searchResults: StateFlow<SearchResult?> = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Starred
    private val _starredSongs = MutableStateFlow<List<Song>>(emptyList())
    val starredSongs: StateFlow<List<Song>> = _starredSongs.asStateFlow()

    // Server stats
    private val _serverStats = MutableStateFlow(ServerStats())
    val serverStats: StateFlow<ServerStats> = _serverStats.asStateFlow()

    // Lyrics
    private val _currentLyrics = MutableStateFlow<String?>(null)
    val currentLyrics: StateFlow<String?> = _currentLyrics.asStateFlow()

    // Recent plays (song objects)
    private val _recentPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentPlayedSongs: StateFlow<List<Song>> = _recentPlayedSongs.asStateFlow()

    // Newest songs (from newest albums)
    private val _newestSongs = MutableStateFlow<List<Song>>(emptyList())
    val newestSongs: StateFlow<List<Song>> = _newestSongs.asStateFlow()

    // Album detail
    private val _currentAlbum = MutableStateFlow<AlbumDetail?>(null)
    val currentAlbum: StateFlow<AlbumDetail?> = _currentAlbum.asStateFlow()

    // Artist detail
    private val _currentArtist = MutableStateFlow<ArtistDetail?>(null)
    val currentArtist: StateFlow<ArtistDetail?> = _currentArtist.asStateFlow()

    // Playlist detail
    private val _currentPlaylist = MutableStateFlow<PlaylistDetail?>(null)
    val currentPlaylist: StateFlow<PlaylistDetail?> = _currentPlaylist.asStateFlow()

    // Cache size setting
    val cacheSize: StateFlow<Int> = settings.cacheSize
        .stateIn(viewModelScope, SharingStarted.Eagerly, 4)

    // Server info
    val serverUrl: StateFlow<String> = settings.serverUrl
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val username: StateFlow<String> = settings.username
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val password: StateFlow<String> = settings.password
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        // Check if already logged in
        viewModelScope.launch {
            combine(settings.serverUrl, settings.username, settings.password) { url, user, pass ->
                Triple(url, user, pass)
            }.collect { (url, user, pass) ->
                if (url.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
                    repository.configure(url, user, pass)
                    _isLoggedIn.value = true
                    loadHomeData()
                }
            }
        }

        // Update progress periodically
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(500)
                playerManager.updateProgress()
            }
        }
    }

    fun login(serverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            try {
                repository.configure(serverUrl, username, password)
                val result = repository.ping()
                if (result.isSuccess) {
                    settings.saveLogin(serverUrl, username, password)
                    _isLoggedIn.value = true
                    loadHomeData()
                } else {
                    _loginError.value = result.exceptionOrNull()?.message ?: "连接失败"
                }
            } catch (e: Exception) {
                _loginError.value = e.message ?: "连接失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            settings.clearLogin()
            _isLoggedIn.value = false
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settings.setThemeMode(mode)
        }
    }

    fun setCacheSize(sizeGb: Int) {
        viewModelScope.launch {
            settings.setCacheSize(sizeGb)
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            // Load newest albums
            repository.getNewestAlbums(10).onSuccess { _newestAlbums.value = it }

            // Load random albums
            repository.getRandomAlbums(10).onSuccess { _randomAlbums.value = it }

            // Load daily random songs
            repository.getRandomSongs(4).onSuccess { _dailySongs.value = it }

            // Load playlists
            repository.getPlaylists().onSuccess { _playlists.value = it }

            // Load songs from newest albums
            loadSongsFromAlbums("newest") { _newestSongs.value = it }

            // Load starred songs
            repository.getStarred().onSuccess { _starredSongs.value = it.songs }

            // Load recent played songs from stored IDs
            loadRecentPlayedSongs()

            // Load server stats
            repository.getServerStats().onSuccess { _serverStats.value = it }
        }
    }

    private suspend fun loadRecentPlayedSongs() {
        settings.recentPlayIds.collect { idsStr ->
            if (idsStr.isBlank()) return@collect
            val ids = idsStr.split(",").filter { it.isNotEmpty() }
            if (ids.isEmpty()) return@collect
            // Fetch songs by loading all songs from random and filtering
            // Subsonic doesn't have a getSong endpoint, but we can use getAlbum or search
            // For simplicity, we'll fetch random songs and also try to get them from recent albums
            val recentSongs = mutableListOf<Song>()
            // Try to find songs from recent albums
            repository.getRecentAlbums(10).onSuccess { albums ->
                for (album in albums) {
                    repository.getAlbum(album.id).onSuccess { detail ->
                        detail.song?.forEach { song ->
                            if (ids.contains(song.id) && recentSongs.none { it.id == song.id }) {
                                recentSongs.add(song)
                            }
                        }
                    }
                }
            }
            // Sort by recent play order
            val sorted = ids.mapNotNull { id -> recentSongs.find { it.id == id } }
            if (sorted.isNotEmpty()) {
                _recentPlayedSongs.value = sorted
            }
        }
    }

    fun loadLyrics(song: Song) {
        _currentLyrics.value = null
        viewModelScope.launch {
            repository.getLyrics(song.artist, song.title).onSuccess { lyrics ->
                _currentLyrics.value = lyrics
            }
        }
    }

    fun loadArtists() {
        viewModelScope.launch {
            repository.getArtists().onSuccess { _artists.value = it }
        }
    }

    fun loadAlbums(type: String = "newest", callback: (List<Album>) -> Unit) {
        viewModelScope.launch {
            when (type) {
                "newest" -> repository.getNewestAlbums(50)
                "recent" -> repository.getRecentAlbums(50)
                "random" -> repository.getRandomAlbums(50)
                else -> repository.getNewestAlbums(50)
            }.onSuccess { callback(it) }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = null
            return
        }
        viewModelScope.launch {
            repository.search(query).onSuccess { _searchResults.value = it }
        }
    }

    fun loadAlbumDetail(albumId: String) {
        viewModelScope.launch {
            repository.getAlbum(albumId).onSuccess { _currentAlbum.value = it }
        }
    }

    fun loadArtistDetail(artistId: String) {
        viewModelScope.launch {
            repository.getArtist(artistId).onSuccess { _currentArtist.value = it }
        }
    }

    fun loadPlaylistDetail(playlistId: String) {
        viewModelScope.launch {
            repository.getPlaylist(playlistId).onSuccess { _currentPlaylist.value = it }
        }
    }

    fun playSong(song: Song, playlist: List<Song> = listOf(song)) {
        playerManager.playSong(song, playlist)
        viewModelScope.launch {
            settings.addRecentPlay(song.id)
            repository.scrobble(song.id)
        }
    }

    fun refreshRandomAlbums() {
        viewModelScope.launch {
            repository.getRandomAlbums(10).onSuccess { _randomAlbums.value = it }
        }
    }

    fun refreshDailySongs() {
        viewModelScope.launch {
            repository.getRandomSongs(4).onSuccess { _dailySongs.value = it }
        }
    }

    /**
     * Load songs from albums of a given type (newest/recent/random).
     * Fetches up to [albumCount] albums and collects their songs.
     */
    private suspend fun loadSongsFromAlbums(
        type: String,
        albumCount: Int = 10,
        callback: (List<Song>) -> Unit
    ) {
        val albumsResult = when (type) {
            "newest" -> repository.getNewestAlbums(albumCount)
            "recent" -> repository.getRecentAlbums(albumCount)
            else -> repository.getRandomAlbums(albumCount)
        }
        albumsResult.onSuccess { albums ->
            val songs = mutableListOf<Song>()
            for (album in albums) {
                repository.getAlbum(album.id).onSuccess { detail ->
                    detail.song?.forEach { song ->
                        if (songs.none { it.id == song.id }) {
                            songs.add(song)
                        }
                    }
                }
            }
            callback(songs)
        }
    }

    fun loadAllSongs(type: String, callback: (List<Song>) -> Unit) {
        viewModelScope.launch {
            when (type) {
                "newest" -> loadSongsFromAlbums("newest", 20, callback)
                "recent" -> loadSongsFromAlbums("recent", 20, callback)
                "random" -> repository.getRandomSongs(50).onSuccess { callback(it) }
                else -> loadSongsFromAlbums("newest", 20, callback)
            }
        }
    }
}
