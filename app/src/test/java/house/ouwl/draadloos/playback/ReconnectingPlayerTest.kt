package house.ouwl.draadloos.playback

import android.os.Looper
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class ReconnectingPlayerTest {

    private fun streamError() = PlaybackException("down", null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun wrap(playWhenReady: Boolean, state: Int): Pair<FakeRadioPlayer, ReconnectingPlayer> {
        val inner = FakeRadioPlayer(playWhenReady, state)
        val player = ReconnectingPlayer(inner)
        // Read the state once, so a later moveTo() is a change and not the starting state.
        assertThat(player.playbackState).isEqualTo(if (playWhenReady && state == Player.STATE_IDLE) Player.STATE_BUFFERING else state)
        return inner to player
    }

    @Test
    fun aDroppedStream_readsAsConnecting_whileTheRadioIsOn() {
        val (inner, player) = wrap(true, Player.STATE_READY)

        inner.moveTo(playbackState = Player.STATE_IDLE, error = streamError())
        idle()

        assertThat(player.playbackState).isEqualTo(Player.STATE_BUFFERING)
        assertThat(player.playerError).isNull()
        assertThat(player.playWhenReady).isTrue()
    }

    @Test
    fun anEndedStream_readsAsConnecting_whileTheRadioIsOn() {
        val (inner, player) = wrap(true, Player.STATE_READY)

        inner.moveTo(playbackState = Player.STATE_ENDED)
        idle()

        assertThat(player.playbackState).isEqualTo(Player.STATE_BUFFERING)
    }

    @Test
    fun whenStopped_theRealStateShows() {
        val (inner, player) = wrap(true, Player.STATE_READY)

        inner.moveTo(playWhenReady = false, playbackState = Player.STATE_IDLE)
        idle()

        assertThat(player.playbackState).isEqualTo(Player.STATE_IDLE)
        assertThat(player.playWhenReady).isFalse()
    }

    @Test
    fun otherStates_passThrough() {
        val (inner, player) = wrap(true, Player.STATE_BUFFERING)

        inner.moveTo(playbackState = Player.STATE_READY)
        idle()

        assertThat(player.playbackState).isEqualTo(Player.STATE_READY)
    }

    @Test
    fun commands_reachTheRealPlayer() {
        val (inner, player) = wrap(true, Player.STATE_READY)

        player.pause()
        idle()

        assertThat(inner.commands).containsExactly("pause")
    }
}
