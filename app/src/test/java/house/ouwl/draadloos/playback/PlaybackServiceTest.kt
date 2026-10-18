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
        val player = session()!!.player as ExoPlayer

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
        val player = session()!!.player as ExoPlayer

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
