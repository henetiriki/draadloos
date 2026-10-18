package house.ouwl.draadloos.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Session rules for a live radio: nobody can seek, and a media button press (or the system's
 * "resume" card after a reboot) brings RSG back.
 */
@OptIn(UnstableApi::class)
class RadioSessionCallback(private val station: MediaItem) : MediaSession.Callback {

    override fun onConnectAsync(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): ListenableFuture<MediaSession.ConnectionResult> {
        // Start from the defaults, which depend on whether the controller is trusted.
        val defaults = MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller).build()
        return Futures.immediateFuture(
            MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
                .setAvailablePlayerCommands(
                    defaults.availablePlayerCommands.buildUpon().removeAll(*SEEK_COMMANDS).build()
                )
                .build()
        )
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        isForPlayback: Boolean,
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
        Futures.immediateFuture(
            MediaSession.MediaItemsWithStartPosition(
                ImmutableList.of(station),
                /* startIndex = */ 0,
                C.TIME_UNSET,
            )
        )
}

private val SEEK_COMMANDS = intArrayOf(
    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
    Player.COMMAND_SEEK_TO_PREVIOUS,
    Player.COMMAND_SEEK_TO_NEXT,
    Player.COMMAND_SEEK_TO_MEDIA_ITEM,
    Player.COMMAND_SEEK_BACK,
    Player.COMMAND_SEEK_FORWARD,
    Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
)
