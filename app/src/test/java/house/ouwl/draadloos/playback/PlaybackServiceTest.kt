package house.ouwl.draadloos.playback

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import house.ouwl.draadloos.MainActivity
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController

@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class PlaybackServiceTest {

    private val controller: ServiceController<PlaybackService> =
        Robolectric.buildService(PlaybackService::class.java).create()
    private val service = controller.get()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        if (session() != null) controller.destroy()
    }

    @Test
    fun createsSessionWithStationLoadedButNotConnected() {
        val player = session()!!.player

        assertThat(player.currentMediaItem!!.mediaId).isEqualTo("rsg")
        // The stream only connects once the user presses play.
        assertThat(player.playbackState).isEqualTo(Player.STATE_IDLE)
        assertThat(player.playWhenReady).isFalse()
    }

    @Test
    fun playerIsConfiguredForMusic() {
        val player = session()!!.player

        assertThat(player.audioAttributes.usage).isEqualTo(C.USAGE_MEDIA)
        assertThat(player.audioAttributes.contentType).isEqualTo(C.AUDIO_CONTENT_TYPE_MUSIC)
    }

    @Test
    fun pausingStopsTheStream() {
        val player = session()!!.player
        player.prepare()
        player.play()

        player.pause()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(player.playbackState).isEqualTo(Player.STATE_IDLE)
        assertThat(player.playWhenReady).isFalse()
    }

    @Test
    fun stopping_clearsAStaleSong() {
        val player = session()!!.player
        val station = player.currentMediaItem!!
        player.replaceMediaItem(
            0,
            station.buildUpon()
                .setMediaMetadata(station.mediaMetadata.buildUpon().setTitle("Môre").setArtist("Kobus Human").build())
                .build(),
        )
        player.play()

        player.pause()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(player.mediaMetadata.title.toString()).isEqualTo("RSG")
        assertThat(player.mediaMetadata.artist.toString()).isEqualTo("Radio Sonder Grense")
    }

    @Test
    fun widgetPlay_startsTheRadio() {
        val player = session()!!.player

        service.onStartCommand(Intent(PlaybackService.ACTION_PLAY), 0, 1)

        assertThat(player.playWhenReady).isTrue()
    }

    @Test
    fun widgetPlay_whenAlreadyOn_doesNothingExtra() {
        val player = session()!!.player
        service.onStartCommand(Intent(PlaybackService.ACTION_PLAY), 0, 1)

        service.onStartCommand(Intent(PlaybackService.ACTION_PLAY), 0, 2)

        assertThat(player.playWhenReady).isTrue()
    }

    @Test
    fun widgetStop_stopsTheRadio() {
        val player = session()!!.player
        service.onStartCommand(Intent(PlaybackService.ACTION_PLAY), 0, 1)

        service.onStartCommand(Intent(PlaybackService.ACTION_STOP), 0, 2)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(player.playWhenReady).isFalse()
        assertThat(player.playbackState).isEqualTo(Player.STATE_IDLE)
    }

    @Test
    fun otherIntents_leaveTheRadioAlone() {
        val player = session()!!.player

        service.onStartCommand(Intent("something.else"), 0, 1)
        service.onStartCommand(null, 0, 2)

        assertThat(player.playWhenReady).isFalse()
    }

    @Test
    fun mediaButtonReceiverIsDeclared() {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).setPackage("house.ouwl.draadloos")
        val receivers = context.packageManager.queryBroadcastReceivers(intent, 0)

        assertThat(receivers.map { it.activityInfo.name })
            .containsExactly("androidx.media3.session.MediaButtonReceiver")
    }

    @Test
    fun tappingTheNotificationOpensTheApp() {
        val intent = shadowOf(session()!!.sessionActivity).savedIntent

        assertThat(intent.component!!.className).isEqualTo(MainActivity::class.java.name)
    }

    @Test
    fun destroyReleasesPlayerAndSession() {
        val player = (session()!!.player as ReconnectingPlayer).wrapped as ExoPlayer

        controller.destroy()

        assertThat(player.isReleased).isTrue()
        assertThat(session()).isNull()
    }

    private fun session(): MediaSession? =
        service.onGetSession(
            MediaSession.ControllerInfo.createTestOnlyControllerInfo(
                "house.ouwl.draadloos",
                /* pid = */ 0,
                /* uid = */ 0,
                MediaLibraryInfo.VERSION_INT,
                /* interfaceVersion = */ 0,
                /* trusted = */ true,
                Bundle.EMPTY,
                /* isPackageNameVerified = */ true,
            )
        )
}
