package com.rhythmgame.game

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe audio sync manager. ExoPlayer is only accessed on the main thread.
 * Game thread reads cached position via atomic variable.
 */
class AudioSyncManager(context: Context) {
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var offsetMs: Long = 0

    @Volatile
    private var isReady = false

    // Cached position updated on main thread, read from any thread
    private val cachedPositionMs = AtomicLong(0L)
    private var positionUpdater: Runnable? = null
    private var startTimeMs: Long = 0L
    private var onPlaybackStarted: (() -> Unit)? = null
    // Cache the position when playback ends to prevent fluctuation
    private val endedPositionMs = AtomicLong(-1L)

    fun setAudioOffset(offset: Long) {
        offsetMs = offset
    }

    fun setVolume(volume: Float) {
        mainHandler.post {
            player.volume = volume.coerceIn(0f, 1f)
        }
    }

    fun loadAudio(uri: Uri) {
        mainHandler.post {
            val mediaItem = MediaItem.fromUri(uri)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        isReady = true
                    }
                }
            })
        }
    }

    /**
     * Start playback. If onStarted callback is provided, it will be called
     * once audio is actually playing (after buffering completes).
     */
    fun play(onStarted: (() -> Unit)? = null) {
        onPlaybackStarted = onStarted
        mainHandler.post {
            if (isReady) {
                player.play()
                startTimeMs = System.currentTimeMillis()
                startPositionUpdates()
                onPlaybackStarted?.invoke()
                onPlaybackStarted = null
            } else {
                // Not ready yet - wait for STATE_READY, then auto-play
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            player.removeListener(this)
                            isReady = true
                            player.play()
                            startTimeMs = System.currentTimeMillis()
                            startPositionUpdates()
                            onPlaybackStarted?.invoke()
                            onPlaybackStarted = null
                        }
                    }
                })
            }
        }
    }

    fun pause() {
        mainHandler.post {
            player.pause()
            stopPositionUpdates()
        }
    }

    fun resume() {
        mainHandler.post {
            player.play()
            startPositionUpdates()
        }
    }

    fun stop() {
        mainHandler.post {
            player.stop()
            stopPositionUpdates()
        }
    }

    fun seekTo(positionMs: Long) {
        mainHandler.post {
            player.seekTo(positionMs)
        }
    }

    /**
     * Thread-safe: can be called from game loop thread.
     * Returns cached position + offset. After playback ends, returns a stable cached value.
     */
    fun getCurrentPositionMs(): Long {
        val ended = endedPositionMs.get()
        if (ended >= 0) return ended
        return cachedPositionMs.get() + offsetMs
    }

    fun isPlaying(): Boolean = isReady

    fun release() {
        mainHandler.post {
            stopPositionUpdates()
            player.release()
        }
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdater = object : Runnable {
            override fun run() {
                if (player.isPlaying) {
                    cachedPositionMs.set(player.currentPosition)
                } else if (startTimeMs > 0L && player.playbackState == Player.STATE_ENDED) {
                    // Audio finished - cache the final position once and stop advancing
                    if (endedPositionMs.get() < 0) {
                        endedPositionMs.set(cachedPositionMs.get() + offsetMs)
                    }
                }
                mainHandler.postDelayed(this, 4) // ~250Hz update rate
            }
        }
        mainHandler.post(positionUpdater!!)
    }

    private fun stopPositionUpdates() {
        positionUpdater?.let { mainHandler.removeCallbacks(it) }
        positionUpdater = null
    }
}
