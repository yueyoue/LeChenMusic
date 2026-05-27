package com.lechenmusic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lechenmusic.LeChenApp
import com.lechenmusic.data.model.*
import com.lechenmusic.data.repository.MusicRepository
import com.lechenmusic.data.repository.SettingsRepository
import com.lechenmusic.data.repository.ServerStats
import com.lechenmusic.player.MusicPlayerManager
import com.lechenmusic.player.RepeatMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LeChenApp
    private val repository: MusicRepository = app.repository
    private val settings: SettingsRepository = app.settingsRepository
    val playerManager: MusicPlayerManager = app.playerManager
    private val gson = Gson()

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

    // All songs
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    // Songs loading state
    private val _songsLoading = MutableStateFlow(false)
    val songsLoading: StateFlow<Boolean> = _songsLoading.asStateFlow()

    // Timer countdown (seconds remaining)
    private val _timerRemainingSeconds = MutableStateFlow(0L)
    val timerRemainingSeconds: StateFlow<Long> = _timerRemainingSeconds.asStateFlow()

    // Sync status
    private val _syncStatus = MutableStateFlow("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    // Toast message for user feedback
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToast() {
        _toastMessage.value = null
    }

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
            try {
                combine(settings.serverUrl, settings.username, settings.password) { url, user, pass ->
                    Triple(url, user, pass)
                }.collect { (url, user, pass) ->
                    try {
                        if (url.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
                            repository.configure(url, user, pass)
                            _isLoggedIn.value = true
                            loadHomeData()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Update progress periodically
        viewModelScope.launch {
            while (true) {
                try {
                    kotlinx.coroutines.delay(500)
                    playerManager.updateProgress()
                } catch (_: Exception) {}
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
            try {
                repository.getNewestAlbums(10).onSuccess { _newestAlbums.value = it }
            } catch (_: Exception) {}
            try {
                repository.getRandomAlbums(10).onSuccess { _randomAlbums.value = it }
            } catch (_: Exception) {}
            try {
                repository.getRandomSongs(4).onSuccess { _dailySongs.value = it }
            } catch (_: Exception) {}
            try {
                repository.getPlaylists().onSuccess { _playlists.value = it }
            } catch (_: Exception) {}
            try {
                repository.getStarred().onSuccess { _starredSongs.value = it.songs }
            } catch (_: Exception) {}
            try {
                loadRecentPlayedSongs()
            } catch (_: Exception) {}
            try {
                repository.getServerStats().onSuccess { _serverStats.value = it }
            } catch (_: Exception) {}
            try {
                loadAllSongsFromCacheOrServer()
            } catch (_: Exception) {}
        }
    }

    private suspend fun loadRecentPlayedSongs() {
        val idsStr = settings.recentPlayIds.first()
        if (idsStr.isBlank()) return
        val ids = idsStr.split(",").filter { it.isNotEmpty() }
        if (ids.isEmpty()) return
        val recentSongs = mutableListOf<Song>()
        // Search through multiple album sources to find the songs
        val albumSources = mutableListOf<List<Album>>()
        repository.getRecentAlbums(20).onSuccess { albumSources.add(it) }
        repository.getNewestAlbums(20).onSuccess { albumSources.add(it) }
        repository.getFrequentAlbums(20).onSuccess { albumSources.add(it) }
        for (albums in albumSources) {
            for (album in albums) {
                if (recentSongs.size >= ids.size) break
                repository.getAlbum(album.id).onSuccess { detail ->
                    detail.song?.forEach { song ->
                        if (ids.contains(song.id) && recentSongs.none { it.id == song.id }) {
                            recentSongs.add(song)
                        }
                    }
                }
            }
            if (recentSongs.size >= ids.size) break
        }
        // Sort by recent play order
        val sorted = ids.mapNotNull { id -> recentSongs.find { it.id == id } }
        if (sorted.isNotEmpty()) {
            _recentPlayedSongs.value = sorted
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
        if (albumId.isBlank()) return
        viewModelScope.launch {
            try {
                repository.getAlbum(albumId).onSuccess { _currentAlbum.value = it }
            } catch (_: Exception) {}
        }
    }

    fun loadArtistDetail(artistId: String) {
        if (artistId.isBlank()) return
        viewModelScope.launch {
            try {
                repository.getArtist(artistId).onSuccess { _currentArtist.value = it }
            } catch (_: Exception) {}
        }
    }

    fun loadPlaylistDetail(playlistId: String) {
        if (playlistId.isBlank()) return
        viewModelScope.launch {
            try {
                repository.getPlaylist(playlistId).onSuccess { _currentPlaylist.value = it }
            } catch (_: Exception) {}
        }
    }

    fun playSong(song: Song, playlist: List<Song> = listOf(song)) {
        try {
            playerManager.playSong(song, playlist)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        viewModelScope.launch {
            try {
                settings.addRecentPlay(song.id)
                repository.scrobble(song.id)
                loadRecentPlayedSongs()
            } catch (_: Exception) {}
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
     * Load all songs with local caching:
     * 1. First load from local cache (instant)
     * 2. Then refresh from server in background
     * 3. Only load new songs from server (incremental)
     */
    fun loadAllSongs() {
        viewModelScope.launch {
            _songsLoading.value = true
            try {
                val cachedJson = settings.getCachedSongs()
                if (cachedJson.isNotBlank()) {
                    try {
                        val type = object : TypeToken<List<Song>>() {}.type
                        val cachedSongs: List<Song> = gson.fromJson(cachedJson, type)
                        if (cachedSongs.isNotEmpty()) {
                            _allSongs.value = cachedSongs
                            _songsLoading.value = false
                            refreshSongsFromServer(cachedSongs)
                            return@launch
                        }
                    } catch (_: Exception) {}
                }

                try {
                    val result = repository.getAllSongs()
                    result.onSuccess { songs ->
                        _allSongs.value = songs
                        saveSongsToCache(songs)
                    }.onFailure {
                        repository.getRandomSongs(500).onSuccess { songs ->
                            _allSongs.value = songs
                            saveSongsToCache(songs)
                        }
                    }
                } catch (_: Exception) {}
            } catch (_: Exception) {}
            finally {
                _songsLoading.value = false
            }
        }
    }

    /**
     * Load from cache first, then refresh from server
     */
    private fun loadAllSongsFromCacheOrServer() {
        viewModelScope.launch {
            val cachedJson = settings.getCachedSongs()
            if (cachedJson.isNotBlank()) {
                try {
                    val type = object : TypeToken<List<Song>>() {}.type
                    val cachedSongs: List<Song> = gson.fromJson(cachedJson, type)
                    if (cachedSongs.isNotEmpty()) {
                        _allSongs.value = cachedSongs
                        // Refresh from server in background
                        refreshSongsFromServer(cachedSongs)
                        return@launch
                    }
                } catch (_: Exception) { }
            }
        }
    }

    /**
     * Refresh songs from server, merging with cached songs (only add new ones)
     */
    private suspend fun refreshSongsFromServer(existingSongs: List<Song>) {
        try {
            val existingIds = existingSongs.map { it.id }.toSet()
            val serverResult = repository.getAllSongs()
            serverResult.onSuccess { serverSongs ->
                val newSongs = serverSongs.filter { it.id !in existingIds }
                if (newSongs.isNotEmpty()) {
                    val merged = existingSongs + newSongs
                    _allSongs.value = merged
                    saveSongsToCache(merged)
                } else {
                    // No new songs, just update cache timestamp
                    saveSongsToCache(existingSongs)
                }
            }
        } catch (_: Exception) { }
    }

    private suspend fun saveSongsToCache(songs: List<Song>) {
        try {
            val json = gson.toJson(songs)
            settings.saveCachedSongs(json)
        } catch (_: Exception) { }
    }

    fun addToPlaylist(playlistId: String, songId: String, playlistOwner: String = "") {
        viewModelScope.launch {
            try {
                val currentUser = username.value
                if (playlistOwner.isNotBlank() && playlistOwner != currentUser) {
                    _toastMessage.value = "不能添加歌曲到别人的歌单"
                    return@launch
                }
                repository.addToPlaylist(playlistId, songId).onSuccess {
                    _toastMessage.value = "已添加到歌单"
                    repository.getPlaylist(playlistId).onSuccess { _currentPlaylist.value = it }
                }.onFailure {
                    _toastMessage.value = "添加失败: ${it.message}"
                }
            } catch (e: Exception) {
                _toastMessage.value = "添加失败: ${e.message}"
            }
        }
    }

    fun removeFromPlaylist(playlistId: String, songIndex: Int) {
        viewModelScope.launch {
            try {
                repository.removeFromPlaylist(playlistId, songIndex).onSuccess {
                    _toastMessage.value = "已从歌单移除"
                    repository.getPlaylist(playlistId).onSuccess { _currentPlaylist.value = it }
                    repository.getPlaylists().onSuccess { _playlists.value = it }
                }.onFailure {
                    _toastMessage.value = "移除失败: ${it.message}"
                }
            } catch (e: Exception) {
                _toastMessage.value = "移除失败: ${e.message}"
            }
        }
    }

    fun createPlaylistAndAddSong(name: String, songId: String, isPublic: Boolean = false) {
        viewModelScope.launch {
            repository.createPlaylist(name, songId, isPublic).onSuccess {
                _toastMessage.value = "歌单创建成功"
                // Refresh playlists after creating
                repository.getPlaylists().onSuccess { _playlists.value = it }
            }.onFailure {
                _toastMessage.value = "创建失败: ${it.message}"
            }
        }
    }

    fun createPlaylist(name: String, isPublic: Boolean = false) {
        viewModelScope.launch {
            repository.createPlaylist(name, isPublic = isPublic).onSuccess {
                _toastMessage.value = "歌单创建成功"
                repository.getPlaylists().onSuccess { _playlists.value = it }
            }.onFailure {
                _toastMessage.value = "创建失败: ${it.message}"
            }
        }
    }

    // Timer countdown job
    private var countdownJob: kotlinx.coroutines.Job? = null

    fun setTimerWithCountdown(minutes: Int) {
        try {
            cancelTimerWithCountdown()
            playerManager.setTimer(minutes)
            _timerRemainingSeconds.value = minutes * 60L
            countdownJob = viewModelScope.launch {
                while (_timerRemainingSeconds.value > 0) {
                    try {
                        kotlinx.coroutines.delay(1000)
                        _timerRemainingSeconds.value = (_timerRemainingSeconds.value - 1).coerceAtLeast(0)
                    } catch (_: Exception) { break }
                }
                try { playerManager.forcePause() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    fun cancelTimerWithCountdown() {
        try {
            countdownJob?.cancel()
            countdownJob = null
            playerManager.cancelTimer()
            _timerRemainingSeconds.value = 0
        } catch (_: Exception) {}
    }

    fun syncData() {
        viewModelScope.launch {
            _syncStatus.value = "同步中..."
            try {
                _syncStatus.value = "同步歌单 (1/4)..."
                try { repository.getPlaylists().onSuccess { _playlists.value = it } } catch (_: Exception) {}

                _syncStatus.value = "同步歌手 (2/4)..."
                try { repository.getArtists().onSuccess { _artists.value = it } } catch (_: Exception) {}

                _syncStatus.value = "同步收藏 (3/4)..."
                try { repository.getStarred().onSuccess { _starredSongs.value = it.songs } } catch (_: Exception) {}

                _syncStatus.value = "同步歌曲和专辑 (4/4)..."
                try { repository.getNewestAlbums(10).onSuccess { _newestAlbums.value = it } } catch (_: Exception) {}
                try { repository.getRandomAlbums(10).onSuccess { _randomAlbums.value = it } } catch (_: Exception) {}
                try { repository.getRandomSongs(4).onSuccess { _dailySongs.value = it } } catch (_: Exception) {}
                try { repository.getServerStats().onSuccess { _serverStats.value = it } } catch (_: Exception) {}
                try { loadRecentPlayedSongs() } catch (_: Exception) {}
                try { settings.clearSongsCache() } catch (_: Exception) {}

                _syncStatus.value = "同步完成 ✓\n已同步: 歌单、歌手、收藏、专辑、歌曲、服务器统计"
            } catch (e: Exception) {
                _syncStatus.value = "同步失败: ${e.message}"
            }
            try {
                kotlinx.coroutines.delay(5000)
                _syncStatus.value = ""
            } catch (_: Exception) {}
        }
    }

    fun syncPlaylists() {
        viewModelScope.launch {
            repository.getPlaylists().onSuccess { _playlists.value = it }
            repository.getStarred().onSuccess { _starredSongs.value = it.songs }
        }
    }

    fun togglePlaylistPublic(playlistId: String, isPublic: Boolean) {
        viewModelScope.launch {
            repository.updatePlaylistPublic(playlistId, isPublic).onSuccess {
                _toastMessage.value = if (isPublic) "歌单已设为公开" else "歌单已设为私密"
                // Refresh playlist detail
                repository.getPlaylist(playlistId).onSuccess { _currentPlaylist.value = it }
                // Refresh playlist list
                repository.getPlaylists().onSuccess { _playlists.value = it }
            }.onFailure {
                _toastMessage.value = "修改失败: ${it.message}"
            }
        }
    }
}
