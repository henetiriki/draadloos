package house.ouwl.draadloos.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import house.ouwl.draadloos.MainActivity
import house.ouwl.draadloos.R

/**
 * Owns the player and the media session. Running as a mediaPlayback foreground service
 * keeps the radio going with the screen off and while other apps are in use.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var liveStream: LiveStream? = null

    override fun onCreate() {
        super.onCreate()
        val station = Station.mediaItem(this)
        val liveStream = LiveStream(
            station,
            Station.mediaItem(this, Station.FALLBACK_URL),
            onRetryAt = { retryAt -> mediaSession?.setSessionExtras(retryExtras(retryAt)) },
        )
        this.liveStream = liveStream
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this).setLoadErrorHandlingPolicy(liveStream.loadErrorPolicy)
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        // Not prepared here: the stream only connects once the user presses play.
        liveStream.attach(player)
        NowPlayingUpdater(getString(R.string.station_name), getString(R.string.station_tagline)).attach(player)

        mediaSession = MediaSession.Builder(this, ReconnectingPlayer(player))
            .setCallback(RadioSessionCallback(station))
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        liveStream?.release()
        liveStream = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
