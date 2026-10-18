package house.ouwl.draadloos.playback

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * A player whose state tests set directly. Records the commands it receives so tests can
 * check what [toggle], the UI and [LiveStream] asked for.
 */
class FakeRadioPlayer(
    private var playWhenReady: Boolean = false,
    private var playbackState: Int = Player.STATE_IDLE,
) : SimpleBasePlayer(Looper.getMainLooper()) {

    val commands = mutableListOf<String>()

    private var playWhenReadyReason = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
    private var error: PlaybackException? = null
    private var mediaItem: MediaItem = MediaItem.Builder().setMediaId("rsg").build()

    override fun getState(): State =
        State.Builder()
            .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
            .setPlayWhenReady(playWhenReady, playWhenReadyReason)
            .setPlaybackState(playbackState)
            .setPlayerError(error)
            // A non-idle state needs a playlist.
            .setPlaylist(listOf(MediaItemData.Builder("rsg").setMediaItem(mediaItem).build()))
            .build()

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        commands += if (playWhenReady) "play" else "pause"
        this.playWhenReady = playWhenReady
        playWhenReadyReason = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        commands += "prepare"
        playbackState = Player.STATE_BUFFERING
        error = null
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        commands += "stop"
        playbackState = Player.STATE_IDLE
        return Futures.immediateVoidFuture()
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long,
    ): ListenableFuture<*> {
        mediaItem = mediaItems.single()
        commands += "setMediaItem:${mediaItem.localConfiguration?.uri}"
        return Futures.immediateVoidFuture()
    }

    /** Moves the player to a new state on its own, as a stream error, end or focus change would. */
    fun moveTo(
        playWhenReady: Boolean = this.playWhenReady,
        playbackState: Int = this.playbackState,
        playWhenReadyReason: Int = this.playWhenReadyReason,
        error: PlaybackException? = null,
    ) {
        this.playWhenReady = playWhenReady
        this.playbackState = playbackState
        this.playWhenReadyReason = playWhenReadyReason
        this.error = error
        invalidateState()
    }
}
