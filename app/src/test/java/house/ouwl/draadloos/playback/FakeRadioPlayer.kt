package house.ouwl.draadloos.playback

import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * A player whose state tests set directly. Records the commands it receives so tests can
 * check what [toggle] and the UI asked for.
 */
class FakeRadioPlayer(
    private var playWhenReady: Boolean = false,
    private var playbackState: Int = Player.STATE_IDLE,
) : SimpleBasePlayer(Looper.getMainLooper()) {

    val commands = mutableListOf<String>()

    override fun getState(): State =
        State.Builder()
            .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
            .setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(playbackState)
            // A non-idle state needs a playlist.
            .setPlaylist(listOf(MediaItemData.Builder("rsg").build()))
            .build()

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        commands += if (playWhenReady) "play" else "pause"
        this.playWhenReady = playWhenReady
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        commands += "prepare"
        playbackState = Player.STATE_BUFFERING
        return Futures.immediateVoidFuture()
    }

    /** Moves the player to a new state on its own, as a stream error or end would. */
    fun moveTo(playWhenReady: Boolean = this.playWhenReady, playbackState: Int = this.playbackState) {
        this.playWhenReady = playWhenReady
        this.playbackState = playbackState
        invalidateState()
    }
}
