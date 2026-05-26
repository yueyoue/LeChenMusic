package com.lechenmusic.player

import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

    private var timerRunnable: Runnable? = null
    private var timerEndTime: Long = 0

    fun init(repo: MusicRepository) {
        repository = repo
        player = ExoPlayer.Builder(context).build().apply {
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
                }
                override fun onPlayerError(error: PlaybackException) {
                    // Try next on error
                    skipNext()
                }
            })
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
        scope.launch(Dispatchers.IO) {
            if (_isStarred.value) {
                repo.unstar(song.id)
            } else {
                repo.star(song.id)
            }
            _isStarred.value = !_isStarred.value
        }
    }

    private fun checkStarred(songId: String) {
        // Check if song is starred by looking at the starred field
        val song = _currentSong.value
        _isStarred.value = song?.isStarred == true
    }

    fun setTimer(minutes: Int) {
        timerEndTime = System.currentTimeMillis() + minutes * 60 * 1000L
        // Use a simple coroutine-based timer
        scope.launch(Dispatchers.Main) {
            delay(minutes * 60 * 1000L)
            if (System.currentTimeMillis() >= timerEndTime) {
                player?.pause()
            }
        }
    }

    fun cancelTimer() {
        timerEndTime = 0
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
        player?.release()
        player = null
    }
}
