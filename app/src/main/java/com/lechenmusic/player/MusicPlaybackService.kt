package com.lechenmusic.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaNotification
import com.google.common.collect.ImmutableList
import com.lechenmusic.MainActivity
import com.lechenmusic.R

/**
 * Foreground service that keeps the music playing when the app is in background.
 * Uses Media3's MediaNotification.Provider for proper lock screen integration.
 *
 * This approach ensures HarmonyOS (and all Android variants) recognize the notification
 * as a media notification linked to the MediaSession, enabling lock screen controls.
 */
@UnstableApi
class MusicPlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "lechen_music_playback"
        const val NOTIFICATION_ID = 1001
        // Shared MediaSession reference from MusicPlayerManager
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

    override fun onNotificationPosted(
        session: MediaSession,
        notification: MediaNotification,
        startForeground: Boolean
    ) {
        // Post the Media3-generated notification (properly linked to MediaSession)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(MusicPlayerManager.NOTIFICATION_ID, notification.notification)
        if (startForeground) {
            startForeground(MusicPlayerManager.NOTIFICATION_ID, notification.notification)
        }
    }

    override fun onNotificationCancelled(session: MediaSession, dismissedByUser: Boolean) {
        if (dismissedByUser) {
            session.player.pause()
        }
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
