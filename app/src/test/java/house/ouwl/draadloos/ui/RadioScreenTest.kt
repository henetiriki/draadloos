package house.ouwl.draadloos.ui

import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
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
    fun withoutController_showsDisabledPlayAndTheStopped() {
        compose.setContent { RadioScreen(controller = null, onToggle = {}) }

        compose.onNodeWithText("Speel").assertIsNotEnabled()
        compose.onNodeWithText("RSG").assertExists()
        compose.onNodeWithText("Radio Sonder Grense").assertExists()
        compose.onNodeWithText("Gestop").assertExists()
    }

    @Test
    fun whenOff_showsPlay() {
        compose.setContent { RadioScreen(FakeRadioPlayer(false, Player.STATE_IDLE), onToggle = {}) }

        compose.onNodeWithText("Speel").assertIsEnabled()
        compose.onNodeWithContentDescription("Speel RSG").assertExists()
        compose.onNodeWithText("Gestop").assertExists()
    }

    @Test
    fun whenConnecting_showsStopAndTheConnectingStatus() {
        compose.setContent { RadioScreen(FakeRadioPlayer(true, Player.STATE_BUFFERING), onToggle = {}) }

        compose.onNodeWithText("Stop").assertIsEnabled()
        compose.onNodeWithContentDescription("Stop RSG").assertExists()
        compose.onNodeWithText("Koppel aan RSG…").assertExists()
    }

    @Test
    fun whenPlaying_showsStopAndRegstreeks() {
        compose.setContent { RadioScreen(FakeRadioPlayer(true, Player.STATE_READY), onToggle = {}) }

        compose.onNodeWithText("Stop").assertIsEnabled()
        compose.onNodeWithText("Regstreeks").assertExists()
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
        compose.onNodeWithText("Regstreeks").assertExists()

        // A stream error drops to idle with playWhenReady still set. The radio keeps
        // reconnecting, so the button keeps offering Stop.
        player.moveTo(playbackState = Player.STATE_IDLE)
        idleMainLooper()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Stop").assertExists()
        compose.onNodeWithText("Koppel aan RSG…").assertExists()

        player.moveTo(playWhenReady = false)
        idleMainLooper()
        compose.onNodeWithText("Speel").assertExists()
        compose.onNodeWithText("Gestop").assertExists()
    }

    @Test
    fun aShortRebuffer_doesNotFlashTheConnectingStatus() {
        val player = FakeRadioPlayer(true, Player.STATE_READY)
        compose.setContent { RadioScreen(player, onToggle = {}) }

        player.moveTo(playbackState = Player.STATE_BUFFERING)
        idleMainLooper()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Regstreeks").assertExists()

        player.moveTo(playbackState = Player.STATE_READY)
        idleMainLooper()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Regstreeks").assertExists()
        compose.onNodeWithText("Koppel aan RSG…").assertDoesNotExist()
    }

    @Test
    fun aLongerRebuffer_showsTheConnectingStatus() {
        val player = FakeRadioPlayer(true, Player.STATE_READY)
        compose.setContent { RadioScreen(player, onToggle = {}) }

        player.moveTo(playbackState = Player.STATE_BUFFERING)
        idleMainLooper()
        compose.mainClock.advanceTimeBy(1_000)

        compose.onNodeWithText("Koppel aan RSG…").assertExists()
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
    fun playingASong_showsItInTheCard() {
        compose.setContent {
            RadioScreen(FakeRadioPlayer.showing("Môre", "Kobus Human").also { it.moveTo(true, Player.STATE_READY) }, onToggle = {})
        }

        compose.onNodeWithText("Nou op die lug").assertExists()
        compose.onNodeWithText("Kobus Human").assertExists()
        compose.onNodeWithText("Môre").assertExists()
        // The station block stays as it is: the song is in the card, not the tagline.
        compose.onNodeWithText("Radio Sonder Grense").assertExists()
    }

    @Test
    fun aTitleWithoutAnArtist_hidesTheArtistLine() {
        compose.setContent {
            RadioScreen(FakeRadioPlayer.showing("Nuus om sewe").also { it.moveTo(true, Player.STATE_READY) }, onToggle = {})
        }

        compose.onNodeWithText("Nuus om sewe").assertExists()
        compose.onNodeWithText("Nou op die lug").assertExists()
    }

    @Test
    fun withoutASong_hidesTheCard() {
        compose.setContent {
            RadioScreen(FakeRadioPlayer.showing(null).also { it.moveTo(true, Player.STATE_READY) }, onToggle = {})
        }

        compose.onNodeWithText("Regstreeks").assertExists()
        compose.onNodeWithText("Nou op die lug").assertDoesNotExist()
    }

    @Test
    fun aSongWhileStopped_isNotShown() {
        compose.setContent { RadioScreen(FakeRadioPlayer.showing("Môre", "Kobus Human"), onToggle = {}) }

        compose.onNodeWithText("Nou op die lug").assertDoesNotExist()
    }

    @Test
    fun followsSongChanges_andHidesTheCardAgain() {
        val player = FakeRadioPlayer.showing(null).also { it.moveTo(true, Player.STATE_READY) }
        compose.setContent { RadioScreen(player, onToggle = {}) }
        compose.onNodeWithText("Nou op die lug").assertDoesNotExist()

        player.replaceMediaItem(0, FakeRadioPlayer.showing("Môre", "Kobus Human").currentMediaItem!!)
        idleMainLooper()
        compose.onNodeWithText("Môre").assertExists()

        player.replaceMediaItem(0, FakeRadioPlayer.showing(null).currentMediaItem!!)
        idleMainLooper()
        compose.onNodeWithText("Nou op die lug").assertDoesNotExist()
    }

    @Test
    fun withAnimationsOff_theStatusStillShows() {
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val player = FakeRadioPlayer(true, Player.STATE_BUFFERING)
        compose.setContent { RadioScreen(player, onToggle = {}) }
        compose.onNodeWithText("Koppel aan RSG…").assertExists()

        player.moveTo(playbackState = Player.STATE_READY)
        idleMainLooper()

        compose.onNodeWithText("Regstreeks").assertExists()
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }
}
