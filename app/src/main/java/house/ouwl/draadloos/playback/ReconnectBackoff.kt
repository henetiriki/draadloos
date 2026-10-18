package house.ouwl.draadloos.playback

/**
 * Delays between reconnect attempts: 2s, 4s, 8s, 16s, then 30s. Call [reset] once the stream
 * plays again. ExoPlayer's playback thread asks for delays while the main thread resets, so
 * every method is synchronized.
 */
class ReconnectBackoff {

    /** Consecutive failures since the last [reset]. */
    var failures = 0
        @Synchronized get
        private set

    @Synchronized
    fun nextDelayMs(): Long {
        // Past the cap the shift would only overflow, so stop growing it.
        val delay = minOf(BASE_DELAY_MS shl minOf(failures, MAX_SHIFT), MAX_DELAY_MS)
        failures++
        return delay
    }

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
