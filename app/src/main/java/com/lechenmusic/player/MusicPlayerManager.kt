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
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.lechenmusic.MainActivity
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

enum class RepeatMode { OFF, ONE, ALL }

class MusicPlayerManager(private val context: Context) {
    private var player: ExoPlayer? = null
    private var repository: MusicRepository? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSession: MediaSession? = null
    private var isReleased = false

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
    private var receiverRegistered = false

    companion object {
        const val ACTION_STOP_PLAYBACK = "com.lechenmusic.STOP_PLAYBACK"
        const val CHANNEL_ID = "lechen_music_playback"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "MusicPlayerManager"
    }

    fun init(repo: MusicRepository) {
        repository = repo
        try {
            player = ExoPlayer.Builder(context)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build().apply {
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            try { _isPlaying.value = isPlaying } catch (_: Exception) {}
                        }
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            try {
                                if (playbackState == Player.STATE_READY) {
                                    _duration.value = this@apply.duration.coerceAtLeast(0)
                                }
                            } catch (_: Exception) {}
                        }
                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            try { updateCurrentFromPlayer() } catch (_: Exception) {}
                        }
                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(TAG, "Player error", error)
                            try { skipNext() } catch (_: Exception) {}
                        }
                    })
                }

            createNotificationChannel()
            val p = player
            if (p != null) {
                mediaSession = MediaSession.Builder(context, p)
                    .setCallback(object : MediaSession.Callback {})
                    .build()
            }

            // Start foreground service with a small delay to ensure Application.onCreate completes
            scope.launch {
                delay(200)
                startForegroundService()
            }

            alarmReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent?.action == ACTION_STOP_PLAYBACK) {
                        try { player?.pause() } catch (_: Exception) {}
                    }
                }
            }
            val filter = IntentFilter(ACTION_STOP_PLAYBACK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(alarmReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(alarmReceiver, filter)
            }
            receiverRegistered = true
        } catch (e: Exception) {
            Log.e(TAG, "Init failed", e)
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
            // Wait for service to be ready, then pass MediaSession
            scope.launch {
                var retries = 0
                while (MusicPlaybackServiceHolder.service == null && retries < 20) {
                    delay(100)
                    retries++
                }
                try {
                    val session = mediaSession
                    if (session != null) {
                        MusicPlaybackServiceHolder.service?.setMediaSession(session)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to set MediaSession on service", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForegroundService failed", e)
        }
    }

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "音乐播放",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "悦音播放控制"
                    setShowBadge(false)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            }
        } catch (_: Exception) {}
    }

    fun playSong(song: Song, songs: List<Song> = listOf(song)) {
        try {
            if (isReleased) return
            val p = player ?: return
            val repo = repository ?: return

            _playlist.value = songs
            val index = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            _currentIndex.value = index

            val mediaItems = songs.mapNotNull { s ->
                try {
                    val url = repo.getStreamUrl(s.id)
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
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to build media item for ${s.id}", e)
                    null
                }
            }

            if (mediaItems.isEmpty()) return

            p.setMediaItems(mediaItems, index.coerceIn(0, mediaItems.lastIndex), 0)
            p.prepare()
            p.play()

            _currentSong.value = song
            checkStarred(song.id)
        } catch (e: Exception) {
            Log.e(TAG, "playSong failed", e)
        }
    }

    fun togglePlayPause() {
        try {
            if (isReleased) return
            val p = player ?: return
            if (p.isPlaying) p.pause() else p.play()
        } catch (_: Exception) {}
    }

    fun forcePause() {
        try {
            if (isReleased) return
            player?.let { if (it.isPlaying) it.pause() }
        } catch (_: Exception) {}
    }

    fun skipNext() {
        try {
            if (isReleased) return
            val p = player ?: return
            val list = _playlist.value
            if (list.isEmpty()) return

            if (_shuffleMode.value) {
                val randomIndex = list.indices.random()
                p.seekTo(randomIndex, 0)
            } else if (p.hasNextMediaItem()) {
                p.seekToNext()
            } else if (_repeatMode.value == RepeatMode.ALL) {
                p.seekTo(0, 0)
            }
            updateCurrentFromPlayer()
        } catch (e: Exception) {
            Log.e(TAG, "skipNext failed", e)
        }
    }

    fun skipPrevious() {
        try {
            if (isReleased) return
            val p = player ?: return
            if (p.currentPosition > 3000) {
                p.seekTo(0)
            } else if (p.hasPreviousMediaItem()) {
                p.seekToPrevious()
            }
            updateCurrentFromPlayer()
        } catch (e: Exception) {
            Log.e(TAG, "skipPrevious failed", e)
        }
    }

    fun seekTo(position: Long) {
        try {
            if (isReleased) return
            player?.seekTo(position)
        } catch (_: Exception) {}
    }

    fun seekToProgress(progress: Float) {
        try {
            if (isReleased) return
            val p = player ?: return
            val pos = (p.duration * progress).toLong().coerceIn(0, p.duration.coerceAtLeast(0))
            p.seekTo(pos)
        } catch (_: Exception) {}
    }

    fun toggleShuffle() {
        try {
            _shuffleMode.value = !_shuffleMode.value
            if (!isReleased) player?.shuffleModeEnabled = _shuffleMode.value
        } catch (_: Exception) {}
    }

    fun toggleRepeat() {
        try {
            _repeatMode.value = when (_repeatMode.value) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            if (!isReleased) {
                player?.repeatMode = when (_repeatMode.value) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                }
            }
        } catch (_: Exception) {}
    }

    fun toggleStar() {
        val song = _currentSong.value ?: return
        val repo = repository ?: return
        scope.launch(Dispatchers.IO) {
            try {
                if (_isStarred.value) {
                    repo.unstar(song.id)
                } else {
                    repo.star(song.id)
                }
                _isStarred.value = !_isStarred.value
            } catch (e: Exception) {
                Log.e(TAG, "toggleStar failed", e)
            }
        }
    }

    private fun checkStarred(songId: String) {
        try {
            val song = _currentSong.value
            _isStarred.value = song?.isStarred == true
        } catch (_: Exception) {}
    }

    fun setTimer(minutes: Int) {
        try {
            cancelTimer()
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(ACTION_STOP_PLAYBACK)
            intent.setPackage(context.packageName)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerTime = System.currentTimeMillis() + minutes * 60 * 1000L
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (_: Exception) {}
    }

    fun cancelTimer() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(ACTION_STOP_PLAYBACK)
            intent.setPackage(context.packageName)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {}
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateCurrentFromPlayer() {
        try {
            if (isReleased) return
            val p = player ?: return
            val list = _playlist.value
            if (list.isEmpty()) return
            val index = p.currentMediaItemIndex
            _currentIndex.value = index
            if (index in list.indices) {
                _currentSong.value = list[index]
                checkStarred(list[index].id)
            }
        } catch (_: Exception) {}
    }

    fun updateProgress() {
        try {
            if (isReleased) return
            val p = player ?: return
            _currentPosition.value = p.currentPosition.coerceAtLeast(0)
            _duration.value = p.duration.coerceAtLeast(0)
            _progress.value = if (p.duration > 0) (p.currentPosition.toFloat() / p.duration).coerceIn(0f, 1f) else 0f
        } catch (_: Exception) {}
    }

    fun release() {
        if (isReleased) return
        isReleased = true
        try {
            // Unregister alarm receiver
            if (receiverRegistered) {
                alarmReceiver?.let {
                    try { context.unregisterReceiver(it) } catch (_: Exception) {}
                }
                receiverRegistered = false
                alarmReceiver = null
            }
            // Stop the service first (service will NOT release player)
            try {
                val intent = Intent(context, MusicPlaybackService::class.java)
                context.stopService(intent)
            } catch (_: Exception) {}
            // Release MediaSession and Player (owner = MusicPlayerManager)
            try { mediaSession?.release() } catch (_: Exception) {}
            mediaSession = null
            try { player?.release() } catch (_: Exception) {}
            player = null
        } catch (_: Exception) {}
    }
}
