package house.ouwl.draadloos.playback

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReconnectStateTest {

    @Test
    fun theRetryTime_survivesTheTripThroughTheExtras() {
        assertThat(retryExtras(123_456L).retryAt()).isEqualTo(123_456L)
    }

    @Test
    fun noRetry_meansNoExtra() {
        assertThat(retryExtras(null).retryAt()).isNull()
        assertThat(retryExtras(null).isEmpty).isTrue()
    }

    @Test
    fun extrasWithoutARetry_readAsNull() {
        assertThat(Bundle().retryAt()).isNull()
    }
}
