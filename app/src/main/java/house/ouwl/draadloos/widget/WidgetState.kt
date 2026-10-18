package house.ouwl.draadloos.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.media3.common.Player
import house.ouwl.draadloos.playback.NowPlaying
import house.ouwl.draadloos.playback.nowPlaying
import house.ouwl.draadloos.ui.RadioState
import house.ouwl.draadloos.ui.radioState

/**
 * What the widgets show. [phase] is never [RadioState.Reconnecting]: a widget only tells
 * stopped, connecting and playing apart, and while connecting or reconnecting its button is
 * Stop, so the user can always stop.
 */
internal data class WidgetState(val phase: RadioState = RadioState.Stopped, val song: NowPlaying? = null) {
    /** True while the radio is meant to be on, so the toggle offers Stop. */
    val on: Boolean get() = phase != RadioState.Stopped
}

/** The widget state of this session: the song only counts while audio actually plays. */
internal fun Player.widgetState(): WidgetState {
    val phase = radioState()
    return WidgetState(phase, if (phase == RadioState.Playing) nowPlaying() else null)
}

/**
 * The state the widgets draw. The playback service keeps it up to date, and it starts as
 * stopped in every new process: if the process died, so did the service and the audio.
 */
internal object RadioWidgetState {

    @Volatile
    var current = WidgetState()
        private set

    /** Remembers [state] and redraws both widgets, unless nothing changed. */
    suspend fun update(context: Context, state: WidgetState) {
        if (state == current) return
        current = state
        RadioWideWidget().updateAll(context)
        RadioSmallWidget().updateAll(context)
    }
}
