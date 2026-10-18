package house.ouwl.draadloos.playback

/**
 * Delays between reconnect attempts: 2s, 4s, 8s, 16s, then 30s. Call [reset] once the stream
 * plays again. ExoPlayer's playback thread asks for delays while the main thread resets, so
 * every method is synchronized.
 */
class ReconnectBackoff {

    /** One failed attempt: how long to wait, and how many failures in a row this makes. */
    data class Retry(val delayMs: Long, val failures: Int)

    /** Consecutive failures since the last [reset]. */
    var failures = 0
        @Synchronized get
        private set

    /**
     * Counts a failure. The delay and the count come from the same step, so a [reset] from
     * another thread can't land between them. With [startOver] the count begins again first:
     * a hiccup while audio still plays is not part of an outage.
     */
    @Synchronized
    fun nextRetry(startOver: Boolean = false): Retry {
        if (startOver) failures = 0
        // Past the cap the shift would only overflow, so stop growing it.
        val delay = minOf(BASE_DELAY_MS shl minOf(failures, MAX_SHIFT), MAX_DELAY_MS)
        failures++
        return Retry(delay, failures)
    }

    fun nextDelayMs(): Long = nextRetry().delayMs

    @Synchronized
    fun reset() {
        failures = 0
    }

    private companion object {
        const val BASE_DELAY_MS = 2_000L
        const val MAX_DELAY_MS = 30_000L
        const val MAX_SHIFT = 5
    }
}
