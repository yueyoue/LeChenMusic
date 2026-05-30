package com.lechenmusic.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.lechenmusic.MainActivity

/**
 * Foreground service that keeps the music playing when the app is in background.
 * Uses MediaSession for lock screen controls and notification integration.
 *
 * HarmonyOS fix: MediaSession is now set BEFORE super.onCreate() to ensure
 * the session is available when the system queries onGetSession().
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
        // Create notification channel first
        createNotificationChannel()
        // Get the shared MediaSession BEFORE calling super.onCreate()
        // This ensures onGetSession() returns a valid session immediately
        mediaSession = sharedMediaSession
        super.onCreate()
        // Start foreground with a default notification
        startForeground(NOTIFICATION_ID, buildDefaultNotification())
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

    private fun buildDefaultNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("悦音")
            .setContentText("正在播放音乐")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setCategory(android.app.Notification.CATEGORY_SERVICE)
            .build()
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
