package house.ouwl.draadloos.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import house.ouwl.draadloos.MainActivity
import house.ouwl.draadloos.playback.NowPlaying
import house.ouwl.draadloos.playback.PlaybackService
import house.ouwl.draadloos.ui.RadioState
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadioWidgetsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val wideSize = DpSize(300.dp, 96.dp)
    private val smallSize = DpSize(96.dp, 96.dp)

    private val stopped = WidgetState(RadioState.Stopped)
    private val connecting = WidgetState(RadioState.Connecting)
    private val liveNoSong = WidgetState(RadioState.Playing)
    private val liveWithSong = WidgetState(RadioState.Playing, NowPlaying("Kobus Human", "Môre"))

    private fun wide(state: WidgetState, block: GlanceAppWidgetUnitTest.() -> Unit) =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(wideSize)
            provideComposable { WideWidget(state) }
            block()
        }

    private fun small(state: WidgetState, block: GlanceAppWidgetUnitTest.() -> Unit) =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(smallSize)
            provideComposable { SmallWidget(state) }
            block()
        }

    @Test
    fun wide_stopped_showsGestop_andOffersPlay() = wide(stopped) {
        onNode(hasText("RSG")).assertExists()
        onNode(hasText("Gestop")).assertExists()
        onNode(hasContentDescription("Speel RSG")).assertExists()
        onNode(hasContentDescription("Stop RSG")).assertDoesNotExist()
    }

    @Test
    fun wide_connecting_offersStop() = wide(connecting) {
        onNode(hasText("Koppel aan RSG…")).assertExists()
        onNode(hasContentDescription("Stop RSG")).assertExists()
    }

    @Test
    fun wide_playing_showsTheSong() = wide(liveWithSong) {
        onNode(hasText("Kobus Human – Môre")).assertExists()
        onNode(hasContentDescription("Stop RSG")).assertExists()
    }

    @Test
    fun wide_playingWithoutASong_saysRegstreeks() = wide(liveNoSong) {
        onNode(hasText("Regstreeks")).assertExists()
    }

    @Test
    fun tappingPlay_startsTheServiceInTheForeground() {
        assertThat(toggleClick(on = false)).isEqualTo(ToggleClick(PlaybackService.ACTION_PLAY, foreground = true))
    }

    @Test
    fun tappingStop_usesAPlainServiceStart() {
        assertThat(toggleClick(on = true)).isEqualTo(ToggleClick(PlaybackService.ACTION_STOP, foreground = false))
    }

    @Test
    fun theToggleAction_isBuiltForTheService() {
        // Building the action is what a tap hands to the system; it must not throw.
        assertThat(toggleAction(context, on = false)).isNotNull()
        assertThat(toggleAction(context, on = true)).isNotNull()
    }

    @Test
    fun wide_tappingElsewhere_opensTheApp() = wide(stopped) {
        onNode(hasStartActivityClickAction(Intent(context, MainActivity::class.java))).assertExists()
    }

    @Test
    fun small_stopped_offersPlay() = small(stopped) {
        onNode(hasText("RSG")).assertExists()
        onNode(hasContentDescription("Speel RSG")).assertExists()
    }

    @Test
    fun small_playing_offersStop() = small(liveWithSong) {
        onNode(hasContentDescription("Stop RSG")).assertExists()
    }

    @Test
    fun small_connecting_offersStop() = small(connecting) {
        onNode(hasContentDescription("Stop RSG")).assertExists()
    }

    @Test
    fun small_tappingElsewhere_opensTheApp() = small(stopped) {
        onNode(hasStartActivityClickAction(Intent(context, MainActivity::class.java))).assertExists()
    }

    @Test
    fun theToggle_neverGoesUnder48dp_andTheArtShrinksWithIt() {
        assertThat(WidgetMetrics.wideToggle(40.dp)).isEqualTo(48.dp)
        assertThat(WidgetMetrics.wideToggle(96.dp)).isEqualTo(64.dp.coerceAtMost(72.dp))
        assertThat(WidgetMetrics.wideToggle(200.dp)).isEqualTo(64.dp)
        assertThat(WidgetMetrics.smallToggle(60.dp)).isEqualTo(48.dp)
        assertThat(WidgetMetrics.smallToggle(120.dp)).isEqualTo(64.dp)
        assertThat(WidgetMetrics.art(48.dp)).isEqualTo(40.dp)
        assertThat(WidgetMetrics.art(64.dp)).isEqualTo(56.dp)
    }

    // What the widgets hand to Glance is their `content`: it draws the state the service keeps.

    private fun withState(state: WidgetState, block: () -> Unit) {
        runBlocking { RadioWidgetState.update(context, state) }
        try {
            block()
        } finally {
            runBlocking { RadioWidgetState.update(context, WidgetState()) }
        }
    }

    @Test
    fun wideContent_drawsTheCurrentState() = withState(liveWithSong) {
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(wideSize)
            provideComposable { RadioWideWidget().content() }

            onNode(hasText("Kobus Human – Môre")).assertExists()
            onNode(hasContentDescription("Stop RSG")).assertExists()
        }
    }

    @Test
    fun smallContent_drawsTheCurrentState() = withState(connecting) {
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(smallSize)
            provideComposable { RadioSmallWidget().content() }

            onNode(hasText("RSG")).assertExists()
            onNode(hasContentDescription("Stop RSG")).assertExists()
        }
    }
}
