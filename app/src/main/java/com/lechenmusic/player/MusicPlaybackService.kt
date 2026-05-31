package com.lechenmusic.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.session.MediaSession
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.session.MediaSessionService
import com.lechenmusic.MainActivity

/**
 * Foreground service for persistent music playback.
 * Uses platform MediaSession + MediaStyle for lock screen controls (HarmonyOS compatible).
 */
class MusicPlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "lechen_music_playback"
        const val NOTIFICATION_ID = 1001
        // Shared Media3 session for player integration
        var sharedMedia3Session: androidx.media3.session.MediaSession? = null
        // Shared platform MediaSession for lock screen controls (鸿蒙需要)
        var sharedMediaSession: MediaSession? = null
    }

    private var media3Session: androidx.media3.session.MediaSession? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        media3Session = sharedMedia3Session
        mediaSession = sharedMediaSession
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    fun refreshNotification() {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(NOTIFICATION_ID, buildNotification())
        } catch (_: Exception) {}
    }

    override fun onGetSession(controllerInfo: androidx.media3.session.MediaSession.ControllerInfo): androidx.media3.session.MediaSession? {
        return media3Session ?: sharedMedia3Session
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Don't stop when task is removed - keep playing
    }

    override fun onDestroy() {
        media3Session?.run {
            player.release()
            release()
        }
        media3Session = null
        mediaSession?.let {
            it.isActive = false
            it.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("悦音")
            .setContentText("正在播放音乐")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(true)

        // 使用平台原生 MediaStyle (鸿蒙系统需要 android.media.session.MediaStyle)
        val session = mediaSession ?: sharedMediaSession
        if (session != null) {
            builder.setStyle(
                android.media.session.MediaStyle(session)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "悦音播放控制"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
