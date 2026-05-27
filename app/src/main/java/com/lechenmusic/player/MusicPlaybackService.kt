package com.lechenmusic.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import com.lechenmusic.MainActivity

class MusicPlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "lechen_music_playback"
        const val NOTIFICATION_ID = 1001
        private const val TAG = "MusicPlaybackService"
    }

    private var mediaSession: MediaSession? = null
    private var notificationManager: NotificationManager? = null
    private var listener: Player.Listener? = null
    private var isForeground = false

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            notificationManager = getSystemService(NotificationManager::class.java)
            MusicPlaybackServiceHolder.service = this
            // Must call startForeground immediately on Android 12+
            val notification = buildDefaultNotification()
            startForeground(NOTIFICATION_ID, notification)
            isForeground = true
        } catch (e: Exception) {
            Log.e(TAG, "onCreate failed", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Ensure we stay foreground
        if (!isForeground) {
            try {
                val notification = buildDefaultNotification()
                startForeground(NOTIFICATION_ID, notification)
                isForeground = true
            } catch (_: Exception) {}
        }
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    fun setMediaSession(session: MediaSession) {
        try {
            // Remove old listener if any
            listener?.let { oldListener ->
                try { session.player.removeListener(oldListener) } catch (_: Exception) {}
            }

            mediaSession = session

            listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    try { updateNotification(session) } catch (_: Exception) {}
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    try { updateNotification(session) } catch (_: Exception) {}
                }
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    try { updateNotification(session) } catch (_: Exception) {}
                }
            }
            session.player.addListener(listener!!)
            updateNotification(session)
        } catch (e: Exception) {
            Log.e(TAG, "setMediaSession failed", e)
        }
    }

    private fun updateNotification(session: MediaSession) {
        try {
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

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title)
                .setContentText(artist)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(isPlaying)
                .setShowWhen(false)

            try {
                builder.setStyle(
                    MediaStyleNotificationHelper.MediaStyle(session)
                        .setShowActionsInCompactView(0, 1, 2)
                )
            } catch (_: Exception) {}

            val notification = builder.build()
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    private fun buildDefaultNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
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
            .setShowWhen(false)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Don't stop when task is removed - keep playing
    }

    override fun onDestroy() {
        try {
            MusicPlaybackServiceHolder.service = null
            // Remove listener only — do NOT release player or MediaSession here
            // Player and MediaSession are owned by MusicPlayerManager
            listener?.let { oldListener ->
                try { mediaSession?.player?.removeListener(oldListener) } catch (_: Exception) {}
            }
            listener = null
            mediaSession = null
        } catch (_: Exception) {}
        try { super.onDestroy() } catch (_: Exception) {}
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
                val manager = getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        } catch (_: Exception) {}
    }
}
