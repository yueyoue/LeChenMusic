package com.lechenmusic.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.media3.common.Player
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Foreground service for persistent music playback.
 *
 * Uses Media3's DefaultMediaNotificationProvider to create the notification.
 * This uses the native Media3 MediaSession.Token (not MediaSessionCompat),
 * which ensures compatibility with both standard Android and HarmonyOS.
 *
 * Lock screen controls are handled by:
 * - Media3 MediaSession (primary, used by the notification and system)
 * - MediaSessionCompat (secondary, for metadata on some Android versions)
 */
class MusicPlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "lechen_music_playback"
        const val NOTIFICATION_ID = 1001
        // Shared MediaSession reference from MusicPlayerManager
        var sharedMediaSession: MediaSession? = null
    }

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Use Media3's built-in notification provider.
        // This creates notifications using the Media3 session token directly,
        // which is compatible with HarmonyOS lock screen controls.
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setNotificationId(NOTIFICATION_ID)
            .setChannelId(CHANNEL_ID)
            .build()
        setMediaNotificationProvider(notificationProvider)

        // Ensure the notification channel exists
        createNotificationChannel()

        // Start as foreground service immediately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            // Create a temporary notification to start foreground
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悦音播放控制"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession ?: sharedMediaSession
    }

    fun setMediaSession(session: MediaSession) {
        mediaSession = session
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Don't stop when task is removed - keep playing
        val player = mediaSession?.player ?: sharedMediaSession?.player
        if (player == null || !player.playWhenReady) {
            stopSelf()
        }
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
