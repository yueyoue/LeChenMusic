package com.lechenmusic.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground service that keeps the music playing when the app is in background.
 * Uses Media3's MediaSessionService for automatic notification and lock screen integration.
 *
 * Media3's MediaSessionService automatically:
 * - Creates a notification linked to the MediaSession
 * - Provides lock screen controls on all Android variants (including HarmonyOS)
 * - Handles foreground service lifecycle
 */
@UnstableApi
class MusicPlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "lechen_music_playback"
        var sharedMediaSession: MediaSession? = null
    }

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        createNotificationChannel()
        mediaSession = sharedMediaSession
        super.onCreate()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession ?: sharedMediaSession
    }

    fun setMediaSession(session: MediaSession) {
        mediaSession = session
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Don't stop when task is removed - keep playing
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
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
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
