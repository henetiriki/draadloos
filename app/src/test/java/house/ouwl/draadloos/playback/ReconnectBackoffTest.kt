package house.ouwl.draadloos.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReconnectBackoffTest {

    private val backoff = ReconnectBackoff()

    @Test
    fun delaysDoubleFromTwoSeconds() {
        val delays = List(4) { backoff.nextDelayMs() }

        assertThat(delays).containsExactly(2_000L, 4_000L, 8_000L, 16_000L).inOrder()
    }

    @Test
    fun delayIsCappedAtThirtySeconds() {
        repeat(4) { backoff.nextDelayMs() }

        assertThat(backoff.nextDelayMs()).isEqualTo(30_000L)
        assertThat(backoff.nextDelayMs()).isEqualTo(30_000L)
    }

    @Test
    fun staysCappedAfterManyFailures() {
        repeat(200) { backoff.nextDelayMs() }

        assertThat(backoff.nextDelayMs()).isEqualTo(30_000L)
    }

    @Test
    fun countsFailures() {
        assertThat(backoff.failures).isEqualTo(0)

        backoff.nextDelayMs()
        backoff.nextDelayMs()

        assertThat(backoff.failures).isEqualTo(2)
    }

    @Test
    fun nextRetry_givesTheDelayAndTheCountTogether() {
        assertThat(backoff.nextRetry()).isEqualTo(ReconnectBackoff.Retry(2_000L, 1))
        assertThat(backoff.nextRetry()).isEqualTo(ReconnectBackoff.Retry(4_000L, 2))
    }

    @Test
    fun nextRetry_startingOver_beginsAgainFromTwoSeconds() {
        repeat(4) { backoff.nextRetry() }

        assertThat(backoff.nextRetry(startOver = true)).isEqualTo(ReconnectBackoff.Retry(2_000L, 1))
    }

    @Test
    fun resetStartsOverFromTwoSeconds() {
        repeat(5) { backoff.nextDelayMs() }

        backoff.reset()

        assertThat(backoff.failures).isEqualTo(0)
        assertThat(backoff.nextDelayMs()).isEqualTo(2_000L)
    }
}
