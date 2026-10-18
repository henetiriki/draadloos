package house.ouwl.draadloos.playback

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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
 *  - so does a call or another app taking the audio for a while (not a short chime), once it gives it back;
 *  - a failed or dropped stream is retried forever with [ReconnectBackoff], always at the live edge;
 *  - after a few failures in a row it switches between the AAC stream and the HLS fallback,
 *    after the same backoff delay as any other retry;
 *  - while it waits to retry, [onRetryAt] gets the time the attempt starts (null once it starts,
 *    succeeds or the user stops), so the app can count down.
 *
 * Build the player with [loadErrorPolicy], then call [attach].
 */
@OptIn(UnstableApi::class)
class LiveStream(
    private val primary: MediaItem,
    private val fallback: MediaItem,
    private val backoff: ReconnectBackoff = ReconnectBackoff(),
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val onRetryAt: (Long?) -> Unit = {},
) : Player.Listener {

    private lateinit var player: Player
    private var usingFallback = false
    // When the audio was taken from us, or null while we have it.
    private var suppressedSince: Long? = null

    // Written on the main thread, read on ExoPlayer's playback thread, which may not touch the player.
    @Volatile
    private var ready = false

    /**
     * Retries every load error, however many and whatever they are, so the player stays
     * "connecting" through an outage instead of failing: a radio keeps trying. The default
     * policy gives up after a few tries.
     */
    val loadErrorPolicy: LoadErrorHandlingPolicy = object : DefaultLoadErrorHandlingPolicy() {
        override fun getMinimumLoadableRetryCount(dataType: Int): Int = MAX_RETRIES

        override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
            // A load that fails while audio is still playing (one lost segment, say) isn't an outage.
            val retry = backoff.nextRetry(startOver = ready)
            if (!ready) publishRetry(retry.delayMs)
            if (retry.failures % FAILURES_PER_SOURCE == 0) mainHandler.postDelayed(switchSource, retry.delayMs)
            return retry.delayMs
        }
    }

    fun attach(player: Player) {
        this.player = player
        ready = player.playbackState == Player.STATE_READY
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
            publishNoRetry()
            backoff.reset()
            suppressedSince = null
            player.stop()
            if (usingFallback) {
                usingFallback = false
                player.setMediaItem(primary)
            }
        }
    }

    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
        if (playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE) {
            // A call has the audio. The stream keeps buffering and would carry on from that
            // old audio afterwards, so remember to rejoin it live instead. Stopping now would
            // give the audio focus up, which ends the suppression straight away.
            if (suppressedSince == null) suppressedSince = SystemClock.elapsedRealtime()
        } else {
            val since = suppressedSince ?: return
            suppressedSince = null
            // A short chime resumes from the buffer; rebuffering would cost more silence than it saves.
            val long = SystemClock.elapsedRealtime() - since >= REJOIN_LIVE_AFTER_MS
            if (long && player.playWhenReady) reconnectNow()
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        ready = playbackState == Player.STATE_READY
        when (playbackState) {
            Player.STATE_READY -> {
                backoff.reset()
                publishNoRetry()
                // It recovered by itself, so a switch planned for the outage isn't needed.
                mainHandler.removeCallbacks(switchSource)
            }
            Player.STATE_ENDED -> reconnectLater()
        }
    }

    override fun onPlayerError(error: PlaybackException) = reconnectLater()

    private fun reconnectLater() {
        val retry = backoff.nextRetry()
        mainHandler.removeCallbacks(reconnect)
        mainHandler.removeCallbacks(switchSource)
        // Every third failure the retry is a switch to the other source.
        val next = if (retry.failures % FAILURES_PER_SOURCE == 0) switchSource else reconnect
        mainHandler.postDelayed(next, retry.delayMs)
        publishRetry(retry.delayMs)
    }

    // Always through the main handler: load errors arrive on ExoPlayer's playback thread, and
    // posting every update keeps them in order.
    private fun publishRetry(delayMs: Long) {
        val at = SystemClock.elapsedRealtime() + delayMs
        mainHandler.post { onRetryAt(at) }
    }

    private fun publishNoRetry() {
        mainHandler.post { onRetryAt(null) }
    }

    /** Drops the old connection and rejoins at the live edge, not where the player left off. */
    private fun reconnectNow() {
        player.stop()
        player.seekToDefaultPosition()
        player.prepare()
    }

    // Stored, not method references, so removeCallbacks can match them.
    private val reconnect = Runnable {
        publishNoRetry()
        val idle = player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED
        if (player.playWhenReady && idle) reconnectNow()
    }

    private val switchSource = Runnable {
        publishNoRetry()
        if (player.playWhenReady) {
            mainHandler.removeCallbacks(reconnect)
            usingFallback = !usingFallback
            player.setMediaItem(if (usingFallback) fallback else primary)
            player.prepare()
        }
    }

    private companion object {
        // Effectively forever: at the 30s cap this is about a year.
        const val MAX_RETRIES = 1_000_000
        const val FAILURES_PER_SOURCE = 3

        // Shorter than this, the buffered audio is still close enough to live.
        const val REJOIN_LIVE_AFTER_MS = 10_000L
    }
}
