package house.ouwl.draadloos.widget

import android.os.Looper
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import house.ouwl.draadloos.playback.FakeRadioPlayer
import house.ouwl.draadloos.playback.NowPlaying
import house.ouwl.draadloos.ui.RadioState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class WidgetStateTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @After
    fun resetState() = runBlocking { RadioWidgetState.update(context, WidgetState()) }

    private fun on(song: String? = null, artist: String? = null) =
        FakeRadioPlayer.showing(song, artist).also { it.moveTo(true, Player.STATE_READY) }

    @Test
    fun stopped_isOff() {
        val state = FakeRadioPlayer(false, Player.STATE_IDLE).widgetState()

        assertThat(state).isEqualTo(WidgetState(RadioState.Stopped))
        assertThat(state.on).isFalse()
    }

    @Test
    fun connecting_isOn_soTheToggleOffersStop() {
        val state = FakeRadioPlayer(true, Player.STATE_BUFFERING).widgetState()

        assertThat(state.phase).isEqualTo(RadioState.Connecting)
        assertThat(state.on).isTrue()
    }

    @Test
    fun playing_carriesTheSong() {
        val state = on("Môre", "Kobus Human").widgetState()

        assertThat(state).isEqualTo(WidgetState(RadioState.Playing, NowPlaying("Kobus Human", "Môre")))
        assertThat(state.on).isTrue()
    }

    @Test
    fun playingWithoutASong_hasNone() {
        assertThat(on().widgetState()).isEqualTo(WidgetState(RadioState.Playing, null))
    }

    @Test
    fun aSongIsDroppedWhileNotPlaying() {
        val player = FakeRadioPlayer.showing("Môre", "Kobus Human")
        player.moveTo(true, Player.STATE_BUFFERING)

        assertThat(player.widgetState().song).isNull()
    }

    @Test
    fun update_remembersTheState() = runBlocking {
        val playing = WidgetState(RadioState.Playing, null)

        RadioWidgetState.update(context, playing)

        assertThat(RadioWidgetState.current).isEqualTo(playing)
    }

    @Test
    fun update_withTheSameState_changesNothing() = runBlocking {
        RadioWidgetState.update(context, WidgetState(RadioState.Connecting))
        RadioWidgetState.update(context, WidgetState(RadioState.Connecting))

        assertThat(RadioWidgetState.current.phase).isEqualTo(RadioState.Connecting)
    }

    @Test
    fun syncFollowsThePlayer_andReleaseStopsIt() {
        val player = FakeRadioPlayer(false, Player.STATE_IDLE)
        val sync = WidgetSync(context)
        sync.attach(player)
        assertThat(RadioWidgetState.current.phase).isEqualTo(RadioState.Stopped)

        player.moveTo(playWhenReady = true, playbackState = Player.STATE_READY)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(RadioWidgetState.current.phase).isEqualTo(RadioState.Playing)

        sync.release()
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(RadioWidgetState.current.phase).isEqualTo(RadioState.Stopped)

        // Released: later changes are no longer followed.
        player.moveTo(playbackState = Player.STATE_BUFFERING)
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(RadioWidgetState.current.phase).isEqualTo(RadioState.Stopped)
    }
}
