package com.lechenmusic.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.lechenmusic.MainActivity

/**
 * Foreground service that keeps the music playing when the app is in background.
 * Provides lock screen notification with playback controls.
 */
class MusicPlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "lechen_music_playback"
        const val NOTIFICATION_ID = 1001
    }

    private var mediaSession: MediaSession? = null
    private var notificationManager: NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notificationManager = getSystemService(NotificationManager::class.java)
        MusicPlaybackServiceHolder.service = this
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    fun setMediaSession(session: MediaSession) {
        mediaSession = session
        // Add listener to update notification when playback state changes
        session.player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotification(session)
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                updateNotification(session)
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                updateNotification(session)
            }
        })
        updateNotification(session)
    }

    private fun updateNotification(session: MediaSession) {
        val player = session.player
        val mediaItem = player.currentMediaItem
        val title = mediaItem?.mediaMetadata?.title ?: "悦音"
        val artist = mediaItem?.mediaMetadata?.artist ?: "正在播放音乐"
        val isPlaying = player.isPlaying

        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification with MediaStyle for lock screen controls
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setShowWhen(false)

        // Apply MediaStyle for lock screen controls
        builder.setStyle(
            MediaStyleNotificationHelper.MediaStyle(session)
                .setShowActionsInCompactView(0, 1, 2)
        )

        val notification = builder.build()
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Don't stop when task is removed - keep playing
    }

    override fun onDestroy() {
        MusicPlaybackServiceHolder.service = null
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
