package house.ouwl.draadloos.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import house.ouwl.draadloos.playback.FakeRadioPlayer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * DESIGN.md: everything must still work at a 1.3x system font scale, with no clipping and no
 * overlap. On a phone-sized screen at that scale, the things the user needs have to be on
 * screen. The content area scrolls, so a long song may need a swipe, but the status and the
 * button never leave the screen.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w390dp-h844dp-port")
class FontScaleTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(player: Player?, retryAt: Long? = null) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                RadioScreen(player, retryAt = retryAt, onToggle = {}, clock = { 0L })
            }
        }
    }

    @Test
    fun stopped_atLargeFont() {
        show(FakeRadioPlayer(false, Player.STATE_IDLE))

        compose.onNodeWithText("RSG").assertIsDisplayed()
        compose.onNodeWithText("Radio Sonder Grense").assertIsDisplayed()
        compose.onNodeWithText("Gestop").assertIsDisplayed()
        compose.onNodeWithText("Speel").assertIsDisplayed()
    }

    @Test
    fun connecting_atLargeFont() {
        show(FakeRadioPlayer(true, Player.STATE_BUFFERING))

        compose.onNodeWithText("Koppel aan RSG…").assertIsDisplayed()
        compose.onNodeWithText("Stop").assertIsDisplayed()
    }

    @Test
    fun playingALongSong_atLargeFont() {
        val player = FakeRadioPlayer.showing(
            title = "Die allerlangste liedjie se naam wat eintlik op twee reëls moet pas",
            artist = "Kunstenaar met 'n baie lang naam en nog 'n bietjie meer",
        ).also { it.moveTo(true, Player.STATE_READY) }
        show(player)

        compose.onNodeWithText("Regstreeks").assertIsDisplayed()
        compose.onNodeWithText("Nou op die lug").assertIsDisplayed()
        compose.onNodeWithText("Stop").assertIsDisplayed()
    }

    @Test
    fun connectionLost_atLargeFont() {
        show(FakeRadioPlayer(true, Player.STATE_BUFFERING), retryAt = 8_000)

        compose.onNodeWithText("Verbinding verloor").assertIsDisplayed()
        compose.onNodeWithText("Moenie bekommer nie, ons probeer self weer.").assertIsDisplayed()
        compose.onNodeWithText("Probeer weer oor 8 sekondes…").assertIsDisplayed()
        compose.onNodeWithText("Stop").assertIsDisplayed()
    }
}
