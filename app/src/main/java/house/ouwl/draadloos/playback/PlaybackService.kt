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
import house.ouwl.draadloos.widget.WidgetSync

/**
 * Owns the player and the media session. Running as a mediaPlayback foreground service
 * keeps the radio going with the screen off and while other apps are in use.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var liveStream: LiveStream? = null
    private var widgetSync: WidgetSync? = null

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

        // The widgets show what the session shows, so they listen to the session's own player.
        val sessionPlayer = ReconnectingPlayer(player)
        widgetSync = WidgetSync(this).also { it.attach(sessionPlayer) }

        mediaSession = MediaSession.Builder(this, sessionPlayer)
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

    /**
     * The widgets start and stop the radio with these. Play arrives through
     * startForegroundService, and Media3 moves the service to the foreground once playback begins.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> mediaSession?.player?.turnOn()
            ACTION_STOP -> mediaSession?.player?.turnOff()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        widgetSync?.release()
        widgetSync = null
        liveStream?.release()
        liveStream = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        /** Start (or reconnect to) the live stream. Sent with startForegroundService. */
        const val ACTION_PLAY = "house.ouwl.draadloos.action.PLAY"

        /** Stop the stream and release the connection. Sent with a plain startService. */
        const val ACTION_STOP = "house.ouwl.draadloos.action.STOP"
    }
}
