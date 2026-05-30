package com.lechenmusic.player

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
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

enum class RepeatMode { OFF, ONE, ALL }

class MusicPlayerManager(private val context: Context) {
    private var player: ExoPlayer? = null
    private var repository: MusicRepository? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSession: MediaSession? = null

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

    // Callback for when song auto-advances (for recent play tracking)
    var onSongAutoAdvanced: ((Song) -> Unit)? = null

    companion object {
        const val ACTION_STOP_PLAYBACK = "com.lechenmusic.STOP_PLAYBACK"
        const val CHANNEL_ID = "lechen_music_playback"
        const val NOTIFICATION_ID = 1001
    }

    fun init(repo: MusicRepository) {
        repository = repo
        player = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true // handle audio focus
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = duration
                        }
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        updateCurrentFromPlayer()
                        val song = _currentSong.value
                        if (song != null) {
                            scope.launch(Dispatchers.IO) {
                                try {
                                    repository?.scrobble(song.id)
                                } catch (_: Exception) {}
                            }
                            onSongAutoAdvanced?.invoke(song)
                        }
                    }
                    override fun onPlayerError(error: PlaybackException) {
                        skipNext()
                    }
                })
            }

        // Create MediaSession for lock screen controls
        // Media3 automatically routes play/pause/seek/skip commands to the player
        createNotificationChannel()
        mediaSession = MediaSession.Builder(context, player!!)
            .build()

        // Share MediaSession with the service
        MusicPlaybackService.sharedMediaSession = mediaSession

        // Start foreground service
        startForegroundService()

        // Register broadcast receiver for alarm-based timer
        alarmReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_STOP_PLAYBACK -> player?.pause()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_STOP_PLAYBACK)
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悦音播放控制"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
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
                        MediaMetadata.Builder()
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
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun forcePause() {
        try {
            player?.let {
                if (it.isPlaying) {
                    it.pause()
                }
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
                }
            } catch (_: Exception) {
            }
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
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
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
                    MediaMetadata.Builder()
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
            artist = "网络电台",
            album = "电台",
            duration = 0
        )
        _playlist.value = emptyList()
        _currentIndex.value = 0
        _isStarred.value = false
    }
}
