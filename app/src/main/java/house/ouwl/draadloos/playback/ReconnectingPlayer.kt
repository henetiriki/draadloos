package house.ouwl.draadloos.playback

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * The player the media session shows to the notification, the lock screen and the app.
 *
 * When the stream drops, ExoPlayer goes idle with playWhenReady still set while [LiveStream]
 * keeps retrying. An idle player makes the notification offer Play, and pressing that would
 * reconnect at once instead of stopping the retries. So while the radio is meant to be on,
 * this reports "connecting", with no error, and every surface offers Stop.
 *
 * [LiveStream] and [NowPlayingUpdater] listen to the real player ([wrapped]), which keeps the
 * true state.
 */
@OptIn(UnstableApi::class)
class ReconnectingPlayer(val wrapped: Player) : ForwardingSimpleBasePlayer(wrapped) {

    override fun getState(): State {
        val state = super.getState()
        val dropped = state.playWhenReady &&
            (state.playbackState == Player.STATE_IDLE || state.playbackState == Player.STATE_ENDED)
        if (!dropped) return state
        return state.buildUpon()
            .setPlaybackState(Player.STATE_BUFFERING)
            .setPlayerError(null)
            .build()
    }
}
