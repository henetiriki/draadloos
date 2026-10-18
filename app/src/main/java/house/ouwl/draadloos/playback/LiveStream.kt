package house.ouwl.draadloos.playback

import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

/**
 * Makes a [Player] behave like a radio rather than a podcast:
 *  - pausing stops the player and drops the connection, so playing again joins the live edge;
 *  - a failed or dropped stream is retried forever with [ReconnectBackoff];
 *  - after a few failures in a row it switches between the AAC stream and the HLS fallback.
 *
 * Build the player with [loadErrorPolicy], then call [attach].
 */
@OptIn(UnstableApi::class)
class LiveStream(
    private val primary: MediaItem,
    private val fallback: MediaItem,
    private val backoff: ReconnectBackoff = ReconnectBackoff(),
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
) : Player.Listener {

    private lateinit var player: Player
    private var usingFallback = false

    /**
     * Retries every load error, however many, so the player stays "connecting" through an
     * outage instead of failing. The default policy gives up after a few tries.
     */
    val loadErrorPolicy: LoadErrorHandlingPolicy = object : DefaultLoadErrorHandlingPolicy() {
        override fun getMinimumLoadableRetryCount(dataType: Int): Int = MAX_RETRIES

        override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long =
            recordFailure()
    }

    fun attach(player: Player) {
        this.player = player
        player.setMediaItem(primary)
        player.addListener(this)
    }

    fun release() {
        mainHandler.removeCallbacksAndMessages(null)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (!playWhenReady) {
            // Pause means stop: close the connection, and start over on the AAC stream.
            mainHandler.removeCallbacksAndMessages(null)
            backoff.reset()
            player.stop()
            if (usingFallback) {
                usingFallback = false
                player.setMediaItem(primary)
            }
        } else if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS &&
            player.playbackState == Player.STATE_IDLE
        ) {
            // Focus came back after a pause that stopped the stream: reconnect live.
            player.prepare()
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> backoff.reset()
            Player.STATE_ENDED -> reconnectLater()
        }
    }

    override fun onPlayerError(error: PlaybackException) = reconnectLater()

    private fun reconnectLater() {
        mainHandler.removeCallbacks(reconnect)
        mainHandler.postDelayed(reconnect, recordFailure())
    }

    // Stored, not method references, so removeCallbacks can match them.
    private val reconnect = Runnable {
        val idle = player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED
        if (player.playWhenReady && idle) {
            player.stop()
            player.prepare()
        }
    }

    private val switchSource = Runnable {
        if (player.playWhenReady) {
            usingFallback = !usingFallback
            player.setMediaItem(if (usingFallback) fallback else primary)
            player.prepare()
        }
    }

    /** Counts a failure and returns how long to wait before trying again. */
    private fun recordFailure(): Long {
        val delay = backoff.nextDelayMs()
        if (backoff.failures % FAILURES_PER_SOURCE == 0) mainHandler.post(switchSource)
        return delay
    }

    private companion object {
        // Effectively forever: at the 30s cap this is about a year.
        const val MAX_RETRIES = 1_000_000
        const val FAILURES_PER_SOURCE = 3
    }
}
