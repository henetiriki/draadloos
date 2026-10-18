package house.ouwl.draadloos.ui

import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import house.ouwl.draadloos.playback.FakeRadioPlayer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class RadioScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun withoutController_showsDisabledPlay() {
        compose.setContent { RadioScreen(controller = null, onToggle = {}) }

        compose.onNodeWithText("Speel").assertIsNotEnabled()
        compose.onNodeWithText("RSG").assertExists()
        compose.onNodeWithText("Radio Sonder Grense").assertExists()
    }

    @Test
    fun whenOff_showsPlay() {
        compose.setContent { RadioScreen(FakeRadioPlayer(false, Player.STATE_IDLE), onToggle = {}) }

        compose.onNodeWithText("Speel").assertIsEnabled()
    }

    @Test
    fun whenOn_showsStop() {
        compose.setContent { RadioScreen(FakeRadioPlayer(true, Player.STATE_READY), onToggle = {}) }

        compose.onNodeWithText("Stop").assertIsEnabled()
    }

    @Test
    fun tap_callsToggle() {
        var toggles = 0
        compose.setContent {
            RadioScreen(FakeRadioPlayer(false, Player.STATE_IDLE), onToggle = { toggles++ })
        }

        compose.onNodeWithText("Speel").performClick()

        assertThat(toggles).isEqualTo(1)
    }

    @Test
    fun followsPlayerStateChanges() {
        val player = FakeRadioPlayer(false, Player.STATE_IDLE)
        compose.setContent { RadioScreen(player, onToggle = {}) }

        player.moveTo(playWhenReady = true, playbackState = Player.STATE_READY)
        idleMainLooper()
        compose.onNodeWithText("Stop").assertExists()

        // A stream error drops to idle with playWhenReady still set. The radio keeps
        // reconnecting, so the button keeps offering Stop.
        player.moveTo(playbackState = Player.STATE_IDLE)
        idleMainLooper()
        compose.onNodeWithText("Stop").assertExists()

        player.moveTo(playWhenReady = false)
        idleMainLooper()
        compose.onNodeWithText("Speel").assertExists()
    }

    @Test
    fun stopsFollowingAReplacedController() {
        val old = FakeRadioPlayer(false, Player.STATE_IDLE)
        val new = FakeRadioPlayer(false, Player.STATE_IDLE)
        var controller by mutableStateOf<Player>(old)
        compose.setContent { RadioScreen(controller, onToggle = {}) }

        controller = new
        compose.waitForIdle()
        old.moveTo(playWhenReady = true, playbackState = Player.STATE_READY)
        idleMainLooper()

        compose.onNodeWithText("Speel").assertExists()
    }

    @Test
    fun showsTheSong_inPlaceOfTheTagline() {
        compose.setContent { RadioScreen(FakeRadioPlayer.showing("Kobus Human – Môre"), onToggle = {}) }

        compose.onNodeWithText("Kobus Human – Môre").assertExists()
        compose.onNodeWithText("Radio Sonder Grense").assertDoesNotExist()
    }

    @Test
    fun followsSongChanges_andReturnsToTheTagline() {
        val player = FakeRadioPlayer.showing(null)
        compose.setContent { RadioScreen(player, onToggle = {}) }
        compose.onNodeWithText("Radio Sonder Grense").assertExists()

        player.replaceMediaItem(0, FakeRadioPlayer.showing("Kobus Human – Môre").currentMediaItem!!)
        idleMainLooper()
        compose.onNodeWithText("Kobus Human – Môre").assertExists()

        player.replaceMediaItem(0, FakeRadioPlayer.showing(null).currentMediaItem!!)
        idleMainLooper()
        compose.onNodeWithText("Radio Sonder Grense").assertExists()
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }
}
