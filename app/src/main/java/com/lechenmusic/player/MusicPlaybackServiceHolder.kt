package com.lechenmusic.player

/**
 * Simple holder to allow MusicPlaybackService to register itself
 * so MusicPlayerManager can pass the MediaSession to it.
 */
object MusicPlaybackServiceHolder {
    var service: MusicPlaybackService? = null
}
