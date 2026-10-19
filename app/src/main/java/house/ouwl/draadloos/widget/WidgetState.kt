package house.ouwl.draadloos.widget

import android.content.Context
import android.os.Process
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
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
 * How a widget keeps its state: in its own Glance preferences. Glance draws a widget once and
 * afterwards redraws it only when its Glance state changes, so the state has to live there
 * (a plain variable, or Compose state, never redraws it).
 *
 * The preferences outlive the process, but the audio does not. Each write is stamped with the
 * process that made it, and state from any other process reads as stopped: if the process
 * died, or the phone rebooted, the service and its audio are gone.
 */
internal object WidgetPrefs {
    val PHASE = stringPreferencesKey("phase")
    val OWNER = longPreferencesKey("owner")
    val ARTIST = stringPreferencesKey("artist")
    val TITLE = stringPreferencesKey("title")

    // Unique to this process and this boot.
    private fun owner(): Long = Process.getStartElapsedRealtime()

    fun write(prefs: MutablePreferences, state: WidgetState) {
        prefs[PHASE] = state.phase.name
        prefs[OWNER] = owner()
        prefs.remove(ARTIST)
        prefs.remove(TITLE)
        val song = state.song ?: return
        prefs[TITLE] = song.title
        if (song.artist != null) prefs[ARTIST] = song.artist
    }

    fun read(prefs: Preferences): WidgetState {
        val phase = prefs[PHASE]
        if (phase == null || prefs[OWNER] != owner()) return WidgetState()
        return WidgetState(RadioState.valueOf(phase), prefs[TITLE]?.let { NowPlaying(prefs[ARTIST], it) })
    }
}

/**
 * The latest state the service has reported, for a widget that is drawn (or added) after the
 * change. It starts as stopped in every new process. [update] also writes it into every
 * installed widget's preferences and redraws them.
 */
internal object RadioWidgetState {

    @Volatile
    var current = WidgetState()
        private set

    suspend fun update(context: Context, state: WidgetState) {
        current = state
        redraw(context, RadioWideWidget())
        redraw(context, RadioSmallWidget())
    }

    @SessionOnly
    private suspend fun redraw(context: Context, widget: GlanceAppWidget) {
        for (id in GlanceAppWidgetManager(context).getGlanceIds(widget.javaClass)) {
            updateAppWidgetState(context, id) { WidgetPrefs.write(it, current) }
            widget.update(context, id)
        }
    }
}
