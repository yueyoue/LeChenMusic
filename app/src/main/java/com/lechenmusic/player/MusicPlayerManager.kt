package com.lechenmusic.player

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata as Media3Metadata
import androidx.media3.common.Player
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.OkHttpClient
import androidx.media3.session.MediaSession
import com.lechenmusic.MainActivity
import com.lechenmusic.R
import com.lechenmusic.data.model.Song
import com.lechenmusic.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.media3.common.PlaybackException
import kotlinx.coroutines.delay
import java.net.URL

enum class RepeatMode { OFF, ONE, ALL }

class MusicPlayerManager(private val context: Context) {
    private var player: ExoPlayer? = null
    private var repository: MusicRepository? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSession: MediaSession? = null
    private var mediaSessionCompat: MediaSessionCompat? = null

    // Music disk cache
    private var musicCache: SimpleCache? = null
    private var cacheDataSourceFactory: CacheDataSource.Factory? = null
    private var currentCacheSizeBytes: Long = 4L * 1024 * 1024 * 1024 // default 4GB

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isStarred = MutableStateFlow(false)
    val isStarred: StateFlow<Boolean> = _isStarred.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null
    private var alarmReceiver: BroadcastReceiver? = null

    // Timer stop flag: when true, player should not auto-resume
    @Volatile
    var timerExpired: Boolean = false
        private set

    var onSongAutoAdvanced: ((Song) -> Unit)? = null

    companion object {
        const val ACTION_STOP_PLAYBACK = "com.lechenmusic.STOP_PLAYBACK"
        const val ACTION_TOGGLE_FAVORITE = "com.lechenmusic.TOGGLE_FAVORITE"
        const val ACTION_PREV = "com.lechenmusic.PREV"
        const val ACTION_NEXT = "com.lechenmusic.NEXT"
        const val ACTION_PLAY_PAUSE = "com.lechenmusic.PLAY_PAUSE"
        const val CHANNEL_ID = "lechen_music_playback"
        const val NOTIFICATION_ID = 1001
    }

    fun init(repo: MusicRepository) {
        repository = repo
        initCache()
        player = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory!!))
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        // If timer expired and player auto-resumed (e.g. audio focus regain), force pause
                        if (isPlaying && timerExpired) {
                            player?.pause()
                            return
                        }
                        updateNotification()
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = duration
                        }
                        if (playbackState == Player.STATE_READY && _isPlaying.value) {
                            updateNotification()
                        }
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updateCurrentFromPlayer()
                        updateNotification()
                        val song = _currentSong.value
                        if (song != null) {
                            scope.launch(Dispatchers.IO) {
                                try { repository?.scrobble(song.id) } catch (_: Exception) {}
                            }
                            onSongAutoAdvanced?.invoke(song)
                        }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        skipNext()
                    }
                })
            }

        createNotificationChannel()

        // Media3 session for player integration
        mediaSession = MediaSession.Builder(context, player!!).build()

        // MediaSessionCompat for notification lock screen controls
        mediaSessionCompat = MediaSessionCompat(context, "LeChenMusicSession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { togglePlayPause() }
                override fun onPause() { togglePlayPause() }
                override fun onSkipToNext() { skipNext() }
                override fun onSkipToPrevious() { skipPrevious() }
                override fun onStop() { forcePause() }
            })
        }

        MusicPlaybackService.sharedMediaSession = mediaSession
        MusicPlaybackService.sharedSessionToken = mediaSessionCompat?.sessionToken

        startForegroundService()

        alarmReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_STOP_PLAYBACK -> {
                        timerExpired = true
                        player?.pause()
                    }
                    ACTION_TOGGLE_FAVORITE -> toggleStar()
                    ACTION_PREV -> skipPrevious()
                    ACTION_NEXT -> skipNext()
                    ACTION_PLAY_PAUSE -> togglePlayPause()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_STOP_PLAYBACK)
            addAction(ACTION_TOGGLE_FAVORITE)
            addAction(ACTION_PREV)
            addAction(ACTION_NEXT)
            addAction(ACTION_PLAY_PAUSE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(alarmReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(alarmReceiver, filter)
        }
    }

    private fun startForegroundService() {
        try {
            val intent = Intent(context, MusicPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) { }
    }

    private fun initCache() {
        val cacheDir = java.io.File(context.cacheDir, "music_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val evictor = LeastRecentlyUsedCacheEvictor(currentCacheSizeBytes)
        musicCache = SimpleCache(cacheDir, evictor, androidx.media3.database.StandaloneDatabaseProvider(context))
        val okHttpClient = OkHttpClient.Builder().build()
        val upstreamFactory = OkHttpDataSource.Factory(okHttpClient)
        cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(musicCache!!)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /** Update cache max size (called when user changes cache setting) */
    fun updateCacheSize(sizeGb: Int) {
        val newBytes = sizeGb.toLong() * 1024 * 1024 * 1024
        if (newBytes == currentCacheSizeBytes) return
        currentCacheSizeBytes = newBytes
        // Re-create cache with new size
        musicCache?.release()
        val cacheDir = java.io.File(context.cacheDir, "music_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(newBytes)
        musicCache = SimpleCache(cacheDir, evictor, androidx.media3.database.StandaloneDatabaseProvider(context))
        val okHttpClient = OkHttpClient.Builder().build()
        val upstreamFactory = OkHttpDataSource.Factory(okHttpClient)
        cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(musicCache!!)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /** Get current cache size in bytes */
    fun getCacheBytes(): Long {
        return try { musicCache?.cacheSpace ?: 0 } catch (_: Exception) { 0 }
    }

    /** Clear all cached music files */
    fun clearMusicCache() {
        try {
            musicCache?.let { cache ->
                val keys = cache.keys
                for (key in keys) {
                    cache.removeResource(key)
                }
            }
        } catch (_: Exception) {
            // Fallback: delete cache directory
            try {
                val cacheDir = java.io.File(context.cacheDir, "music_cache")
                if (cacheDir.exists()) {
                    cacheDir.deleteRecursively()
                    cacheDir.mkdirs()
                }
                // Re-init cache after clearing
                musicCache?.release()
                initCache()
            } catch (_: Exception) {}
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "悦音播放控制"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val song = _currentSong.value ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sessionCompat = mediaSessionCompat ?: return

        // Update MediaSessionCompat metadata (for lock screen display)
        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration * 1000L)
        sessionCompat.setMetadata(metadataBuilder.build())

        // Update playback state with current position
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SET_RATING
            )
            .setState(
                if (_isPlaying.value) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                player?.currentPosition ?: _currentPosition.value,
                1.0f
            )
        sessionCompat.setPlaybackState(stateBuilder.build())

        val openIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(ACTION_PREV).setPackage(context.packageName)
        val prevPending = PendingIntent.getBroadcast(
            context, 1, prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(ACTION_PLAY_PAUSE).setPackage(context.packageName)
        val playPausePending = PendingIntent.getBroadcast(
            context, 2, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = Intent(ACTION_NEXT).setPackage(context.packageName)
        val nextPending = PendingIntent.getBroadcast(
            context, 3, nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val favIntent = Intent(ACTION_TOGGLE_FAVORITE).setPackage(context.packageName)
        val favPending = PendingIntent.getBroadcast(
            context, 4, favIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scope.launch(Dispatchers.IO) {
            val albumArt = loadAlbumArt(song.coverArt)

            if (albumArt != null) {
                val metaWithArt = MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration * 1000L)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                    .build()
                sessionCompat.setMetadata(metaWithArt)
            }

            val playPauseIcon = if (_isPlaying.value) R.drawable.ic_notif_pause else R.drawable.ic_notif_play
            val favIcon = if (_isStarred.value) R.drawable.ic_notif_favorite else R.drawable.ic_notif_favorite_border

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setSubText(song.album)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(_isPlaying.value)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setLargeIcon(albumArt)
                .setStyle(
                    MediaStyle()
                        .setMediaSession(sessionCompat.sessionToken)
                        .setShowActionsInCompactView(0, 1, 2)
                )
                .addAction(R.drawable.ic_notif_prev, "上一曲", prevPending)
                .addAction(playPauseIcon, if (_isPlaying.value) "暂停" else "播放", playPausePending)
                .addAction(R.drawable.ic_notif_next, "下一曲", nextPending)
                .addAction(favIcon, if (_isStarred.value) "取消收藏" else "收藏", favPending)
                .build()

            nm.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun loadAlbumArt(coverArtId: String?): Bitmap? {
        if (coverArtId.isNullOrBlank()) return null
        return try {
            val repo = repository ?: return null
            val url = repo.getCoverArtUrl(coverArtId) ?: return null
            val connection = URL(url).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            val size = (128 * context.resources.displayMetrics.density).toInt()
            Bitmap.createScaledBitmap(bitmap, size, size, true)
        } catch (e: Exception) {
            null
        }
    }

    fun playSong(song: Song, songs: List<Song> = listOf(song)) {
        _playlist.value = songs
        val index = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        _currentIndex.value = index

        player?.apply {
            val mediaItems = songs.map { s ->
                val url = repository!!.getStreamUrl(s.id)
                MediaItem.Builder()
                    .setUri(url)
                    .setMediaId(s.id)
                    .setMediaMetadata(
                        Media3Metadata.Builder()
                            .setTitle(s.title)
                            .setArtist(s.artist)
                            .setAlbumTitle(s.album)
                            .build()
                    )
                    .build()
            }
            setMediaItems(mediaItems, index, 0)
            prepare()
            play()
        }
        _currentSong.value = song
        checkStarred(song.id)
        updateNotification()
    }

    fun togglePlayPause() {
        // User manually pressed play, clear timer expired flag
        timerExpired = false
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun forcePause() {
        try {
            player?.let {
                if (it.isPlaying) it.pause()
            }
        } catch (_: Exception) { }
    }

    fun skipNext() {
        player?.let {
            if (_shuffleMode.value) {
                val randomIndex = (_playlist.value.indices).random()
                it.seekTo(randomIndex, 0)
            } else if (it.hasNextMediaItem()) {
                it.seekToNext()
            } else if (_repeatMode.value == RepeatMode.ALL) {
                it.seekTo(0, 0)
            }
        }
        updateCurrentFromPlayer()
    }

    fun skipPrevious() {
        player?.let {
            if (it.currentPosition > 3000) {
                it.seekTo(0)
            } else if (it.hasPreviousMediaItem()) {
                it.seekToPrevious()
            }
        }
        updateCurrentFromPlayer()
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    fun seekToProgress(progress: Float) {
        player?.let {
            val pos = (it.duration * progress).toLong().coerceIn(0, it.duration)
            it.seekTo(pos)
        }
    }

    fun toggleShuffle() {
        _shuffleMode.value = !_shuffleMode.value
        player?.shuffleModeEnabled = _shuffleMode.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        player?.repeatMode = when (_repeatMode.value) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    fun toggleStar() {
        val song = _currentSong.value ?: return
        val repo = repository ?: return
        if (song.id.startsWith("radio_")) return
        scope.launch(Dispatchers.IO) {
            try {
                val result = if (_isStarred.value) {
                    repo.unstar(song.id)
                } else {
                    repo.star(song.id)
                }
                if (result.isSuccess) {
                    _isStarred.value = !_isStarred.value
                    updateNotification()
                }
            } catch (_: Exception) { }
        }
    }

    private fun checkStarred(songId: String) {
        val song = _currentSong.value
        _isStarred.value = song?.isStarred == true
    }

    fun setTimer(minutes: Int) {
        cancelTimer()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ACTION_STOP_PLAYBACK)
        intent.setPackage(context.packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = System.currentTimeMillis() + minutes * 60 * 1000L
        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }

    fun cancelTimer() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ACTION_STOP_PLAYBACK)
        intent.setPackage(context.packageName)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        timerJob?.cancel()
        timerJob = null
        timerExpired = false
    }

    fun clearTimerExpired() {
        timerExpired = false
    }

    private fun updateCurrentFromPlayer() {
        player?.let { p ->
            val index = p.currentMediaItemIndex
            _currentIndex.value = index
            if (index in _playlist.value.indices) {
                _currentSong.value = _playlist.value[index]
                checkStarred(_playlist.value[index].id)
            }
        }
    }

    fun updateProgress() {
        player?.let {
            _currentPosition.value = it.currentPosition
            _duration.value = it.duration.coerceAtLeast(0)
            _progress.value = if (it.duration > 0) it.currentPosition.toFloat() / it.duration else 0f
        }
    }

    fun release() {
        alarmReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) { }
        }
        mediaSessionCompat?.let {
            it.isActive = false
            it.release()
        }
        mediaSessionCompat = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        musicCache?.release()
        musicCache = null
        try {
            val intent = Intent(context, MusicPlaybackService::class.java)
            context.stopService(intent)
        } catch (_: Exception) { }
    }

    fun playRadioStation(station: com.lechenmusic.data.model.InternetRadioStation) {
        player?.apply {
            val mediaItem = MediaItem.Builder()
                .setUri(station.streamUrl)
                .setMediaId("radio_${station.id}")
                .setMediaMetadata(
                    Media3Metadata.Builder()
                        .setTitle(station.name)
                        .setArtist("电台")
                        .setAlbumTitle("网络电台")
                        .build()
                )
                .build()
            setMediaItem(mediaItem)
            prepare()
            play()
        }
        _currentSong.value = com.lechenmusic.data.model.Song(
            id = "radio_${station.id}",
            title = station.name,
            artist = "电台",
            album = "电台",
            duration = 0
        )
        _playlist.value = emptyList()
        _currentIndex.value = 0
        _isStarred.value = false
        updateNotification()
    }
}
