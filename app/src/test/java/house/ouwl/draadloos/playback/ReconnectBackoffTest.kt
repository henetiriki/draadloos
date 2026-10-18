package house.ouwl.draadloos.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReconnectBackoffTest {

    private val backoff = ReconnectBackoff()

    @Test
    fun delaysDoubleFromTwoSeconds() {
        val delays = List(4) { backoff.nextRetry().delayMs }

        assertThat(delays).containsExactly(2_000L, 4_000L, 8_000L, 16_000L).inOrder()
    }

    @Test
    fun delayIsCappedAtThirtySeconds() {
        repeat(4) { backoff.nextRetry().delayMs }

        assertThat(backoff.nextRetry().delayMs).isEqualTo(30_000L)
        assertThat(backoff.nextRetry().delayMs).isEqualTo(30_000L)
    }

    @Test
    fun staysCappedAfterManyFailures() {
        repeat(200) { backoff.nextRetry().delayMs }

        assertThat(backoff.nextRetry().delayMs).isEqualTo(30_000L)
    }

    @Test
    fun countsFailures() {
        assertThat(backoff.failures).isEqualTo(0)

        backoff.nextRetry().delayMs
        backoff.nextRetry().delayMs

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
        repeat(5) { backoff.nextRetry().delayMs }

        backoff.reset()

        assertThat(backoff.failures).isEqualTo(0)
        assertThat(backoff.nextRetry().delayMs).isEqualTo(2_000L)
    }
}
