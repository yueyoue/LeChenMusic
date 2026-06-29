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
import com.lechenmusic.update.UpdateChecker
import com.lechenmusic.update.UpdateInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
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

    // All songs
    private val _allSongs = MutableStateFlow<List<Song>>(emptyList())
    val allSongs: StateFlow<List<Song>> = _allSongs.asStateFlow()

    // All songs loading state
    private val _allSongsLoading = MutableStateFlow(false)
    val allSongsLoading: StateFlow<Boolean> = _allSongsLoading.asStateFlow()

    private val _allSongsLoadError = MutableStateFlow<String?>(null)
    val allSongsLoadError: StateFlow<String?> = _allSongsLoadError.asStateFlow()

    // Cached songs (songs that have been cached by ExoPlayer for offline playback)
    private val _cachedSongs = MutableStateFlow<List<Song>>(emptyList())
    val cachedSongs: StateFlow<List<Song>> = _cachedSongs.asStateFlow()

    // Internet Radio Stations
    private val _radioStations = MutableStateFlow<List<InternetRadioStation>>(emptyList())
    val radioStations: StateFlow<List<InternetRadioStation>> = _radioStations.asStateFlow()

    // Timer countdown (seconds remaining)
    private val _timerRemainingSeconds = MutableStateFlow(0L)
    val timerRemainingSeconds: StateFlow<Long> = _timerRemainingSeconds.asStateFlow()

    // Sync status
    private val _syncStatus = MutableStateFlow("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    // App Update
    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _updateStatus = MutableStateFlow("")
    val updateStatus: StateFlow<String> = _updateStatus.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    // Toast message for user feedback
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToast() {
        _toastMessage.value = null
    }

    /**
     * 检查更新（自动或手动调用）
     * @param silent true=静默检查（无更新不提示），false=手动检查（无更新也提示）
     */
    fun checkForUpdate(silent: Boolean = true) {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            if (!silent) _updateStatus.value = "检查中..."
            try {
                val context = getApplication<Application>()
                val currentVersionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                    context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0).versionCode
                }
                val info = UpdateChecker.check(currentVersionCode)
                if (info != null) {
                    // If silent auto-check, respect skipped version
                    if (silent) {
                        val skippedCode = settings.skippedVersionCode.first()
                        if (skippedCode >= info.versionCode) {
                            // User already skipped this version, don't show
                            _isCheckingUpdate.value = false
                            _updateStatus.value = ""
                            return@launch
                        }
                    }
                    _updateInfo.value = info
                } else if (!silent) {
                    _toastMessage.value = "当前已是最新版本 ✓"
                }
            } catch (e: Exception) {
                if (!silent) _toastMessage.value = "检查更新失败: ${e.message}"
            } finally {
                _isCheckingUpdate.value = false
                if (!silent) _updateStatus.value = ""
            }
        }
    }

    /** Skip current update version */
    fun skipUpdate() {
        val info = _updateInfo.value ?: return
        viewModelScope.launch {
            settings.setSkippedVersionCode(info.versionCode)
            _updateInfo.value = null
            _updateStatus.value = ""
        }
    }

    /** 下载并安装更新 */
    fun downloadUpdate() {
        val info = _updateInfo.value ?: return
        viewModelScope.launch {
            _updateStatus.value = "通过${info.source}正在下载..."
            val context = getApplication<Application>()
            val apkFile = UpdateChecker.downloadApk(
                context = context,
                apkUrl = info.apkUrl,
                source = info.source,
                onProgress = { _updateStatus.value = it }
            )
            if (apkFile != null) {
                _updateStatus.value = "下载完成，正在安装..."
                val success = UpdateChecker.installApk(context, apkFile)
                if (!success) {
                    _updateStatus.value = "安装启动失败"
                    kotlinx.coroutines.delay(3000)
                } else {
                    // 延迟后重置状态，避免卡在"正在安装"
                    kotlinx.coroutines.delay(8000)
                }
                _updateStatus.value = ""
                _updateInfo.value = null
            } else {
                // downloadApk already set error message via onProgress
                kotlinx.coroutines.delay(3000)
                _updateStatus.value = ""
            }
        }
    }

    /** 关闭更新弹窗 */
    fun dismissUpdate() {
        _updateInfo.value = null
        _updateStatus.value = ""
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
            combine(settings.serverUrl, settings.username, settings.password) { url, user, pass ->
                Triple(url, user, pass)
            }.collect { (url, user, pass) ->
                if (url.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
                    repository.configure(url, user, pass)
                    _isLoggedIn.value = true
                    loadHomeData()
                    // Auto-sync all songs in background
                    loadAllSongs()
                }
            }
        }

        // Register callback for auto-advance (lock screen / background playback)
        playerManager.onSongAutoAdvanced = { song ->
            viewModelScope.launch {
                settings.addRecentPlay(song.id)
                addSongToRecentCache(song)
                // Refresh recent played songs list
                loadRecentPlayedSongs()
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
            _randomAlbums.value = emptyList()
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
            playerManager.updateCacheSize(sizeGb)
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            val gson = Gson()
            val albumType = object : TypeToken<List<Album>>() {}.type
            val playlistType = object : TypeToken<List<Playlist>>() {}.type
            val radioType = object : TypeToken<List<InternetRadioStation>>() {}.type

            // Step 1: Load cached data first for instant/offline display
            try {
                val cachedNewest = settings.cachedNewestAlbumsJson.first()
                if (cachedNewest.isNotBlank()) {
                    val cached: List<Album> = gson.fromJson(cachedNewest, albumType)
                    if (cached.isNotEmpty()) _newestAlbums.value = cached
                }
            } catch (_: Exception) {}
            try {
                val cachedRandom = settings.cachedRandomAlbumsJson.first()
                if (cachedRandom.isNotBlank()) {
                    val cached: List<Album> = gson.fromJson(cachedRandom, albumType)
                    if (cached.isNotEmpty()) _randomAlbums.value = cached
                }
            } catch (_: Exception) {}
            try {
                val cachedPlaylists = settings.cachedPlaylistsJson.first()
                if (cachedPlaylists.isNotBlank()) {
                    val cached: List<Playlist> = gson.fromJson(cachedPlaylists, playlistType)
                    if (cached.isNotEmpty()) _playlists.value = cached
                }
            } catch (_: Exception) {}
            try {
                val cachedRadio = settings.cachedRadioStationsJson.first()
                if (cachedRadio.isNotBlank()) {
                    val cached: List<InternetRadioStation> = gson.fromJson(cachedRadio, radioType)
                    if (cached.isNotEmpty()) _radioStations.value = cached
                }
            } catch (_: Exception) {}

            // Step 2: Fetch fresh data from server, update cache on success
            // Load newest albums
            repository.getNewestAlbums(10).onSuccess {
                _newestAlbums.value = it
                try { settings.saveCachedNewestAlbumsJson(gson.toJson(it)) } catch (_: Exception) {}
            }

            // Load random albums - only if not already loaded (user must click "换一批" to refresh)
            if (_randomAlbums.value.isEmpty()) {
                repository.getRandomAlbums(10).onSuccess {
                    _randomAlbums.value = it
                    try { settings.saveCachedRandomAlbumsJson(gson.toJson(it)) } catch (_: Exception) {}
                }
            }

            // Load daily random songs - use cache if same day AND has enough songs
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val cachedDate = settings.cachedDailySongsDate.first()
            val cachedDailyJson = settings.cachedDailySongsJson.first()
            val useCache = cachedDate == today && cachedDailyJson.isNotBlank()
            if (useCache) {
                try {
                    val type = object : TypeToken<List<Song>>() {}.type
                    val cachedSongs: List<Song> = Gson().fromJson(cachedDailyJson, type)
                    // Cache must have at least 5 songs (display count), otherwise re-fetch
                    if (cachedSongs.size >= 5) {
                        _dailySongs.value = cachedSongs
                    } else {
                        repository.getRandomSongs(20).onSuccess {
                            _dailySongs.value = it
                            settings.saveCachedDailySongs(Gson().toJson(it), today)
                        }
                    }
                } catch (_: Exception) {
                    repository.getRandomSongs(20).onSuccess {
                        _dailySongs.value = it
                        settings.saveCachedDailySongs(Gson().toJson(it), today)
                    }
                }
            } else {
                repository.getRandomSongs(20).onSuccess {
                    _dailySongs.value = it
                    settings.saveCachedDailySongs(Gson().toJson(it), today)
                }
            }

            // Load playlists
            repository.getPlaylists().onSuccess {
                _playlists.value = it
                try { settings.saveCachedPlaylistsJson(gson.toJson(it)) } catch (_: Exception) {}
            }

            // Load internet radio stations
            repository.getInternetRadioStations().onSuccess {
                _radioStations.value = it
                try { settings.saveCachedRadioStationsJson(gson.toJson(it)) } catch (_: Exception) {}
            }

            // Load starred songs
            repository.getStarred().onSuccess { _starredSongs.value = it.songs }

            // Load recent played songs from stored IDs
            loadRecentPlayedSongs()

            // Load cached songs (songs that are in ExoPlayer cache + recently played)
            loadCachedSongs()

            // Load server stats
            repository.getServerStats().onSuccess { _serverStats.value = it }
        }
    }

    fun loadCachedSongs() {
        viewModelScope.launch {
            val cachedIds = playerManager.getCachedSongIds()
            // Recently played songs that are also in the ExoPlayer cache
            val recentCached = _recentPlayedSongs.value.filter { it.id in cachedIds }
            // Also include all cached IDs matched from allSongs
            val allCached = _allSongs.value.filter { it.id in cachedIds }
            // Merge, deduplicate, recently played first
            val merged = mutableListOf<Song>()
            val seenIds = mutableSetOf<String>()
            for (song in recentCached) {
                if (seenIds.add(song.id)) merged.add(song)
            }
            for (song in allCached) {
                if (seenIds.add(song.id)) merged.add(song)
            }
            _cachedSongs.value = merged
        }
    }

    private suspend fun loadRecentPlayedSongs() {
        // Try cached song objects first (most reliable)
        try {
            val cachedJson = settings.cachedRecentSongsJson.first()
            if (cachedJson.isNotBlank()) {
                val type = object : TypeToken<List<Song>>() {}.type
                val cachedSongs: List<Song> = Gson().fromJson(cachedJson, type) ?: emptyList()
                if (cachedSongs.isNotEmpty()) {
                    _recentPlayedSongs.value = cachedSongs.take(20)
                    return
                }
            }
        } catch (_: Exception) { }

        // Fallback: search through albums by IDs
        val idsStr = settings.recentPlayIds.first()
        if (idsStr.isBlank()) return
        val ids = idsStr.split(",").filter { it.isNotEmpty() }
        if (ids.isEmpty()) return
        val recentSongs = mutableListOf<Song>()

        // First try: search through allSongs cache (fast, usually available)
        val allCached = _allSongs.value
        if (allCached.isNotEmpty()) {
            for (id in ids) {
                val found = allCached.find { it.id == id }
                if (found != null && recentSongs.none { it.id == found.id }) {
                    recentSongs.add(found)
                }
            }
        }

        // Second try: search through albums if allSongs cache didn't cover all IDs
        if (recentSongs.size < ids.size) {
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
        }

        val sorted = ids.mapNotNull { id -> recentSongs.find { it.id == id } }
        if (sorted.isNotEmpty()) {
            _recentPlayedSongs.value = sorted
            // Save to cache for future fast loading
            try {
                settings.saveCachedRecentSongsJson(Gson().toJson(sorted.take(50)))
            } catch (_: Exception) { }
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

    fun loadAllAlbums(callback: (List<Album>) -> Unit) {
        viewModelScope.launch {
            val allAlbums = mutableListOf<Album>()
            val seenIds = mutableSetOf<String>()
            val types = listOf("newest", "recent", "frequent", "random", "starred", "alphabeticalByName")
            for (type in types) {
                var offset = 0
                val pageSize = 500
                while (true) {
                    try {
                        val albums = repository.getAlbumList2(type, pageSize, offset).getOrNull() ?: break
                        for (album in albums) {
                            if (seenIds.add(album.id)) {
                                allAlbums.add(album)
                            }
                        }
                        if (albums.size < pageSize) break
                        offset += pageSize
                    } catch (_: Exception) { break }
                }
            }
            callback(allAlbums)
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
            // Cache the song object for recent plays
            addSongToRecentCache(song)
            // Refresh recent played songs
            loadRecentPlayedSongs()
            // Refresh cached songs after a delay (allow ExoPlayer to cache)
            kotlinx.coroutines.delay(3000)
            loadCachedSongs()
        }
    }

    private suspend fun addSongToRecentCache(song: Song) {
        try {
            val cachedJson = settings.cachedRecentSongsJson.first()
            val type = object : TypeToken<List<Song>>() {}.type
            val existing: List<Song> = if (cachedJson.isNotBlank()) {
                Gson().fromJson(cachedJson, type) ?: emptyList()
            } else emptyList()
            val updated = listOf(song) + existing.filter { it.id != song.id }
            settings.saveCachedRecentSongsJson(Gson().toJson(updated.take(50)))
        } catch (_: Exception) { }
    }

    fun refreshRandomAlbums() {
        viewModelScope.launch {
            repository.getRandomAlbums(10).onSuccess { _randomAlbums.value = it }
        }
    }

    fun refreshDailySongs() {
        viewModelScope.launch {
            repository.getRandomSongs(20).onSuccess {
                _dailySongs.value = it
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                settings.saveCachedDailySongs(Gson().toJson(it), today)
            }
        }
    }

    fun loadAllSongs(showToast: Boolean = false) {
        viewModelScope.launch {
            _allSongsLoading.value = true
            _allSongsLoadError.value = null

            // Step 1: Load cached songs first for instant display
            val cachedJson = settings.cachedAllSongsJson.first()
            if (cachedJson.isNotBlank()) {
                try {
                    val type = object : TypeToken<List<Song>>() {}.type
                    val cachedSongs: List<Song> = Gson().fromJson(cachedJson, type)
                    if (cachedSongs.isNotEmpty()) {
                        _allSongs.value = cachedSongs
                        _allSongsLoading.value = false
                    }
                } catch (_: Exception) { }
            }

            // Step 2: Fetch fresh data from server
            try {
                repository.getAllSongs().onSuccess { serverSongs ->
                    val cachedList = _allSongs.value
                    val cachedIds = cachedList.map { it.id }.toSet()
                    val newSongs = serverSongs.filter { it.id !in cachedIds }

                    if (newSongs.isNotEmpty()) {
                        // Merge: keep cached + add new songs (always grow, never shrink)
                        val merged = cachedList + newSongs
                        _allSongs.value = merged
                        if (showToast) _toastMessage.value = "发现 ${newSongs.size} 首新歌曲"
                        saveSongsToCache(merged)
                    } else if (serverSongs.size >= cachedList.size) {
                        // Server has more (or equal) songs — safe to update
                        _allSongs.value = serverSongs
                        saveSongsToCache(serverSongs)
                    }
                    // else: server returned fewer songs (non-deterministic search),
                    //       keep cached list — do NOT replace with smaller data
                    _allSongsLoading.value = false
                }.onFailure {
                    if (_allSongs.value.isEmpty()) {
                        // No cache and server failed
                        repository.getRandomSongs(500).onSuccess { songs ->
                            _allSongs.value = songs
                            saveSongsToCache(songs)
                        }.onFailure { e ->
                            _allSongsLoadError.value = e.message
                        }
                    }
                    _allSongsLoading.value = false
                }
            } catch (e: Exception) {
                if (_allSongs.value.isEmpty()) {
                    _allSongsLoadError.value = e.message
                }
                _allSongsLoading.value = false
            }
        }
    }

    private suspend fun saveSongsToCache(songs: List<Song>) {
        try {
            val json = Gson().toJson(songs)
            settings.saveCachedAllSongsJson(json)
        } catch (_: Exception) { }
    }

    fun addToPlaylist(playlistId: String, songId: String, playlistOwner: String = "") {
        viewModelScope.launch {
            // Check if this is someone else's playlist
            val currentUser = username.value
            if (playlistOwner.isNotBlank() && playlistOwner != currentUser) {
                _toastMessage.value = "不能添加歌曲到别人的歌单"
                return@launch
            }
            repository.addToPlaylist(playlistId, songId).onSuccess {
                _toastMessage.value = "已添加到歌单"
                // Refresh playlist detail if viewing
                repository.getPlaylist(playlistId).onSuccess { _currentPlaylist.value = it }
            }.onFailure {
                _toastMessage.value = "添加失败: ${it.message}"
            }
        }
    }

    fun removeFromPlaylist(playlistId: String, songIndex: Int) {
        viewModelScope.launch {
            repository.removeFromPlaylist(playlistId, songIndex).onSuccess {
                _toastMessage.value = "已从歌单移除"
                // Refresh playlist detail
                repository.getPlaylist(playlistId).onSuccess { _currentPlaylist.value = it }
                // Also refresh playlist list
                repository.getPlaylists().onSuccess { _playlists.value = it }
            }.onFailure {
                _toastMessage.value = "移除失败: ${it.message}"
            }
        }
    }

    fun createPlaylistAndAddSong(name: String, songId: String, isPublic: Boolean = false) {
        viewModelScope.launch {
            val idToSend = songId.ifBlank { null }
            repository.createPlaylist(name, idToSend, isPublic).onSuccess {
                _toastMessage.value = "歌单创建成功"
                // Refresh playlists after creating
                repository.getPlaylists().onSuccess { _playlists.value = it }
            }.onFailure {
                _toastMessage.value = "创建失败: ${it.message}"
            }
        }
    }

    // Timer countdown job
    private var countdownJob: kotlinx.coroutines.Job? = null

    fun setTimerWithCountdown(minutes: Int) {
        // Cancel any existing timer
        cancelTimerWithCountdown()
        // Clear timer expired flag for new timer
        playerManager.clearTimerExpired()
        // Also set alarm as backup (works even if app is killed)
        playerManager.setTimer(minutes)
        _timerRemainingSeconds.value = minutes * 60L
        val targetTime = System.currentTimeMillis() + minutes * 60 * 1000L
        countdownJob = viewModelScope.launch {
            // Use absolute time to avoid drift when app goes to background
            while (true) {
                kotlinx.coroutines.delay(500)
                val remaining = ((targetTime - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                _timerRemainingSeconds.value = remaining
                if (remaining <= 0) break
            }
            // Timer reached zero - force pause playback
            try {
                playerManager.forcePause()
                // If still playing after forcePause, toggle it
                kotlinx.coroutines.delay(300)
                if (playerManager.isPlaying.value) {
                    playerManager.togglePlayPause()
                }
                // Final force pause for reliability
                kotlinx.coroutines.delay(200)
                playerManager.forcePause()
            } catch (_: Exception) {
                try { playerManager.forcePause() } catch (_: Exception) {}
            }
        }
    }

    fun cancelTimerWithCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        playerManager.cancelTimer()
        _timerRemainingSeconds.value = 0
    }

    fun syncData() {
        viewModelScope.launch {
            _syncStatus.value = "同步中..."
            try {
                // Sync playlists
                _syncStatus.value = "同步歌单 (1/4)..."
                repository.getPlaylists().onSuccess { _playlists.value = it }

                // Sync radio stations
                repository.getInternetRadioStations().onSuccess { _radioStations.value = it }

                // Sync artists
                _syncStatus.value = "同步歌手 (2/4)..."
                repository.getArtists().onSuccess { _artists.value = it }

                // Sync starred
                _syncStatus.value = "同步收藏 (3/4)..."
                repository.getStarred().onSuccess { _starredSongs.value = it.songs }

                // Refresh home data (daily songs and random albums only refresh on user click "换一批")
                _syncStatus.value = "同步歌曲和专辑 (4/4)..."
                repository.getNewestAlbums(10).onSuccess { _newestAlbums.value = it }

                // Refresh server stats
                repository.getServerStats().onSuccess { _serverStats.value = it }

                // Refresh recent played
                loadRecentPlayedSongs()

                _syncStatus.value = "同步完成 ✓\n已同步: 歌单、歌手、收藏、专辑、歌曲、服务器统计"
            } catch (e: Exception) {
                _syncStatus.value = "同步失败: ${e.message}"
            }
            kotlinx.coroutines.delay(5000)
            _syncStatus.value = ""
        }
    }

    fun syncPlaylists() {
        viewModelScope.launch {
            repository.getPlaylists().onSuccess { _playlists.value = it }
            repository.getStarred().onSuccess { _starredSongs.value = it.songs }
        }
    }

    fun updatePlaylistPublic(playlistId: String, isPublic: Boolean) {
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
