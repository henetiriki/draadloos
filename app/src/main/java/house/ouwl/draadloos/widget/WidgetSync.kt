package house.ouwl.draadloos.widget

import android.content.Context
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Keeps the home-screen widgets in step with the session: every change of what the radio is
 * doing, or of the song, redraws them. [release] puts them back to stopped.
 */
class WidgetSync(private val context: Context) : Player.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: Player

    fun attach(player: Player) {
        this.player = player
        player.addListener(this)
        push(player.widgetState())
    }

    override fun onEvents(player: Player, events: Player.Events) = push(player.widgetState())

    fun release() {
        player.removeListener(this)
        // The service is going away, and so is the audio. The scope ends once the widgets are redrawn.
        scope.launch { RadioWidgetState.update(context, WidgetState()) }
            .invokeOnCompletion { scope.cancel() }
    }

    private fun push(state: WidgetState) {
        scope.launch { RadioWidgetState.update(context, state) }
    }
}
