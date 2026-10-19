package house.ouwl.draadloos.ui

import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
        compose.setContent { RadioScreen(controller = null, retryAt = null, onToggle = {}) }

        compose.onNodeWithText("Speel").assertIsNotEnabled()
        compose.onNodeWithText("RSG").assertExists()
        compose.onNodeWithText("Radio Sonder Grense").assertExists()
        compose.onNodeWithText("Gestop").assertExists()
    }

    @Test
    fun whenOff_showsPlay() {
        compose.setContent { RadioScreen(FakeRadioPlayer(false, Player.STATE_IDLE), retryAt = null, onToggle = {}) }

        compose.onNodeWithText("Speel").assertIsEnabled()
        compose.onNodeWithContentDescription("Speel RSG").assertExists()
        compose.onNodeWithText("Gestop").assertExists()
    }

    @Test
    fun whenConnecting_showsStopAndTheConnectingStatus() {
        compose.setContent { RadioScreen(FakeRadioPlayer(true, Player.STATE_BUFFERING), retryAt = null, onToggle = {}) }

        compose.onNodeWithText("Stop").assertIsEnabled()
        compose.onNodeWithContentDescription("Stop RSG").assertExists()
        compose.onNodeWithText("Koppel aan RSG…").assertExists()
    }

    @Test
    fun whenPlaying_showsStopAndRegstreeks() {
        compose.setContent { RadioScreen(FakeRadioPlayer(true, Player.STATE_READY), retryAt = null, onToggle = {}) }

        compose.onNodeWithText("Stop").assertIsEnabled()
        compose.onNodeWithText("Regstreeks").assertExists()
    }

    @Test
    fun tap_callsToggle() {
        var toggles = 0
        compose.setContent {
            RadioScreen(FakeRadioPlayer(false, Player.STATE_IDLE), retryAt = null, onToggle = { toggles++ })
        }

        compose.onNodeWithText("Speel").performClick()

        assertThat(toggles).isEqualTo(1)
    }

    @Test
    fun followsPlayerStateChanges() {
        val player = FakeRadioPlayer(false, Player.STATE_IDLE)
        compose.setContent { RadioScreen(player, retryAt = null, onToggle = {}) }

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
        compose.setContent { RadioScreen(player, retryAt = null, onToggle = {}) }

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
        compose.setContent { RadioScreen(player, retryAt = null, onToggle = {}) }

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
        compose.setContent { RadioScreen(controller, retryAt = null, onToggle = {}) }

        controller = new
        compose.waitForIdle()
        old.moveTo(playWhenReady = true, playbackState = Player.STATE_READY)
        idleMainLooper()

        compose.onNodeWithText("Speel").assertExists()
    }

    @Test
    fun playingASong_showsItInTheCard() {
        compose.setContent {
            RadioScreen(FakeRadioPlayer.showing("Môre", "Kobus Human").also { it.moveTo(true, Player.STATE_READY) }, retryAt = null, onToggle = {})
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
            RadioScreen(FakeRadioPlayer.showing("Nuus om sewe").also { it.moveTo(true, Player.STATE_READY) }, retryAt = null, onToggle = {})
        }

        compose.onNodeWithText("Nuus om sewe").assertExists()
        compose.onNodeWithText("Nou op die lug").assertExists()
    }

    @Test
    fun withoutASong_hidesTheCard() {
        compose.setContent {
            RadioScreen(FakeRadioPlayer.showing(null).also { it.moveTo(true, Player.STATE_READY) }, retryAt = null, onToggle = {})
        }

        compose.onNodeWithText("Regstreeks").assertExists()
        compose.onNodeWithText("Nou op die lug").assertDoesNotExist()
    }

    @Test
    fun aSongWhileStopped_isNotShown() {
        compose.setContent { RadioScreen(FakeRadioPlayer.showing("Môre", "Kobus Human"), retryAt = null, onToggle = {}) }

        compose.onNodeWithText("Nou op die lug").assertDoesNotExist()
    }

    @Test
    fun followsSongChanges_andHidesTheCardAgain() {
        val player = FakeRadioPlayer.showing(null).also { it.moveTo(true, Player.STATE_READY) }
        compose.setContent { RadioScreen(player, retryAt = null, onToggle = {}) }
        compose.onNodeWithText("Nou op die lug").assertDoesNotExist()

        player.replaceMediaItem(0, FakeRadioPlayer.showing("Môre", "Kobus Human").currentMediaItem!!)
        idleMainLooper()
        compose.onNodeWithText("Môre").assertExists()

        player.replaceMediaItem(0, FakeRadioPlayer.showing(null).currentMediaItem!!)
        idleMainLooper()
        compose.onNodeWithText("Nou op die lug").assertDoesNotExist()
    }

    @Test
    fun stoppingWhilePlaying_showsSpeelAtOnce() {
        val player = FakeRadioPlayer(true, Player.STATE_READY)
        compose.mainClock.autoAdvance = false
        compose.setContent { RadioScreen(player, retryAt = null, onToggle = {}, clock = { clockMs }) }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Stop").assertExists()

        player.moveTo(playWhenReady = false)
        shadowOf(Looper.getMainLooper()).idle()
        compose.mainClock.advanceTimeByFrame()

        // No grace period: Stop must not linger on a radio that has stopped.
        compose.onNodeWithText("Speel").assertExists()
        compose.onNodeWithText("Gestop").assertExists()
    }

    @Test
    fun aNewRetryTime_isCountedFromTheCurrentClock() {
        var retryAt by mutableStateOf<Long?>(null)
        compose.setContent {
            RadioScreen(FakeRadioPlayer(true, Player.STATE_BUFFERING), retryAt = retryAt, onToggle = {}, clock = { clockMs })
        }

        // Time has moved on with nothing ticking, then a retry time arrives.
        clockMs = 50_000
        retryAt = 52_000
        compose.waitForIdle()

        compose.onNodeWithText("Probeer weer oor 2 sekondes…").assertExists()
    }

    @Test
    fun aNewTitleByTheSameArtist_updatesTheCard() {
        val player = FakeRadioPlayer.showing("Môre", "Kobus Human").also { it.moveTo(true, Player.STATE_READY) }
        compose.setContent { RadioScreen(player, retryAt = null, onToggle = {}) }
        compose.onNodeWithText("Môre").assertExists()

        player.replaceMediaItem(0, FakeRadioPlayer.showing("Hartseer", "Kobus Human").currentMediaItem!!)
        idleMainLooper()

        compose.onNodeWithText("Hartseer").assertExists()
        compose.onNodeWithText("Môre").assertDoesNotExist()
        compose.onNodeWithText("Kobus Human").assertExists()
    }

    @Test
    fun aNewArtistWithTheSameTitle_updatesTheCard() {
        val player = FakeRadioPlayer.showing("Môre", "Kobus Human").also { it.moveTo(true, Player.STATE_READY) }
        compose.setContent { RadioScreen(player, retryAt = null, onToggle = {}) }

        player.replaceMediaItem(0, FakeRadioPlayer.showing("Môre", "Juanita du Plessis").currentMediaItem!!)
        idleMainLooper()

        compose.onNodeWithText("Juanita du Plessis").assertExists()
        compose.onNodeWithText("Kobus Human").assertDoesNotExist()
    }

    private fun artWidth(): Float {
        val bounds = compose.onNodeWithTag("station_art").getUnclippedBoundsInRoot()
        return (bounds.right - bounds.left).value
    }

    @Test
    fun theArtShrinksSmoothly_whenPlayingStarts_andGrowsBackOnStop() {
        val player = FakeRadioPlayer(false, Player.STATE_IDLE)
        compose.mainClock.autoAdvance = false
        compose.setContent { RadioScreen(player, retryAt = null, onToggle = {}, clock = { clockMs }) }
        compose.mainClock.advanceTimeByFrame()
        assertThat(artWidth()).isWithin(0.5f).of(168f)

        player.moveTo(playWhenReady = true, playbackState = Player.STATE_READY)
        shadowOf(Looper.getMainLooper()).idle()
        compose.mainClock.advanceTimeBy(100)
        assertThat(artWidth()).isGreaterThan(133f)
        assertThat(artWidth()).isLessThan(167f)
        compose.mainClock.advanceTimeBy(1_000)
        assertThat(artWidth()).isWithin(0.5f).of(132f)

        player.moveTo(playWhenReady = false)
        shadowOf(Looper.getMainLooper()).idle()
        compose.mainClock.advanceTimeBy(100)
        assertThat(artWidth()).isGreaterThan(133f)
        assertThat(artWidth()).isLessThan(167f)
        compose.mainClock.advanceTimeBy(1_000)
        assertThat(artWidth()).isWithin(0.5f).of(168f)
    }

    @Test
    fun withAnimationsOff_theArtResizesAtOnce() {
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val player = FakeRadioPlayer(false, Player.STATE_IDLE)
        compose.mainClock.autoAdvance = false
        compose.setContent { RadioScreen(player, retryAt = null, onToggle = {}, clock = { clockMs }) }
        compose.mainClock.advanceTimeByFrame()

        player.moveTo(playWhenReady = true, playbackState = Player.STATE_READY)
        shadowOf(Looper.getMainLooper()).idle()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()

        assertThat(artWidth()).isWithin(0.5f).of(132f)
    }

    @Test
    fun withAnimationsOff_theStatusStillShows() {
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val player = FakeRadioPlayer(true, Player.STATE_BUFFERING)
        compose.setContent { RadioScreen(player, retryAt = null, onToggle = {}) }
        compose.onNodeWithText("Koppel aan RSG…").assertExists()

        player.moveTo(playbackState = Player.STATE_READY)
        idleMainLooper()

        compose.onNodeWithText("Regstreeks").assertExists()
    }

    // Reconnecting: the clock is faked, so a test moves time on by changing it and advancing
    // Compose's own clock by the same amount.

    private var clockMs = 10_000L

    private fun passTime(ms: Long) {
        clockMs += ms
        compose.mainClock.advanceTimeBy(ms)
    }

    @Test
    fun whenTheStreamIsLost_showsTheWarningAndTheCountdown() {
        compose.setContent {
            RadioScreen(FakeRadioPlayer(true, Player.STATE_BUFFERING), retryAt = 15_000, onToggle = {}, clock = { clockMs })
        }

        compose.onNodeWithText("Verbinding verloor").assertExists()
        compose.onNodeWithText("Moenie bekommer nie, ons probeer self weer.").assertExists()
        compose.onNodeWithText("Probeer weer oor 5 sekondes…").assertExists()
        compose.onNodeWithText("Koppel aan RSG…").assertDoesNotExist()
        compose.onNodeWithText("Stop").assertIsEnabled()
    }

    @Test
    fun theCountdownGoesDownEverySecond_thenReadsOneSecond() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            RadioScreen(FakeRadioPlayer(true, Player.STATE_BUFFERING), retryAt = 12_000, onToggle = {}, clock = { clockMs })
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Probeer weer oor 2 sekondes…").assertExists()

        passTime(1_000)

        compose.onNodeWithText("Probeer weer oor 1 sekonde…").assertExists()
    }

    @Test
    fun whenTheRetryStarts_theWarningGivesWayToConnecting() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            RadioScreen(FakeRadioPlayer(true, Player.STATE_BUFFERING), retryAt = 12_000, onToggle = {}, clock = { clockMs })
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Verbinding verloor").assertExists()

        passTime(2_000)

        compose.onNodeWithText("Verbinding verloor").assertDoesNotExist()
        compose.onNodeWithText("Koppel aan RSG…").assertExists()
    }

    @Test
    fun aRetryTimeThatHasPassed_isNotAWarning() {
        compose.setContent {
            RadioScreen(FakeRadioPlayer(true, Player.STATE_BUFFERING), retryAt = 9_000, onToggle = {}, clock = { clockMs })
        }

        compose.onNodeWithText("Verbinding verloor").assertDoesNotExist()
        compose.onNodeWithText("Koppel aan RSG…").assertExists()
    }

    @Test
    fun aRetryTime_isIgnoredWhileAudioPlays() {
        compose.setContent {
            RadioScreen(FakeRadioPlayer(true, Player.STATE_READY), retryAt = 15_000, onToggle = {}, clock = { clockMs })
        }

        compose.onNodeWithText("Verbinding verloor").assertDoesNotExist()
        compose.onNodeWithText("Regstreeks").assertExists()
    }

    @Test
    fun aRetryTime_isIgnoredOnceStopped() {
        compose.setContent {
            RadioScreen(FakeRadioPlayer(false, Player.STATE_IDLE), retryAt = 15_000, onToggle = {}, clock = { clockMs })
        }

        compose.onNodeWithText("Verbinding verloor").assertDoesNotExist()
        compose.onNodeWithText("Gestop").assertExists()
    }

    @Test
    fun followsTheRetryTimeComingAndGoing() {
        var retryAt by mutableStateOf<Long?>(null)
        compose.setContent {
            RadioScreen(FakeRadioPlayer(true, Player.STATE_BUFFERING), retryAt = retryAt, onToggle = {}, clock = { clockMs })
        }
        compose.onNodeWithText("Koppel aan RSG…").assertExists()

        retryAt = 20_000
        compose.waitForIdle()
        compose.onNodeWithText("Verbinding verloor").assertExists()

        retryAt = null
        compose.waitForIdle()
        compose.onNodeWithText("Koppel aan RSG…").assertExists()
    }

    @Test
    fun aDropWhilePlaying_waitsBeforeShowingTheWarning() {
        val player = FakeRadioPlayer(true, Player.STATE_READY)
        compose.mainClock.autoAdvance = false
        compose.setContent { RadioScreen(player, retryAt = 20_000, onToggle = {}, clock = { clockMs }) }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Regstreeks").assertExists()

        player.moveTo(playbackState = Player.STATE_BUFFERING)
        shadowOf(Looper.getMainLooper()).idle()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Verbinding verloor").assertDoesNotExist()

        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithText("Verbinding verloor").assertExists()
    }

    private fun idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }
}
