package house.ouwl.draadloos.playback

import android.content.Context
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class RadioSessionCallbackTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val station = Station.mediaItem(context)
    private val callback = RadioSessionCallback(station)
    private val session = MediaSession.Builder(context, FakeRadioPlayer()).build()
    private val controller = MediaSession.ControllerInfo.createTestOnlyControllerInfo(
        "house.ouwl.draadloos",
        /* pid = */ 0,
        /* uid = */ 0,
        MediaLibraryInfo.VERSION_INT,
        /* interfaceVersion = */ 0,
        /* trusted = */ true,
        Bundle.EMPTY,
        /* isPackageNameVerified = */ true,
    )

    @After
    fun tearDown() {
        session.release()
    }

    @Test
    fun connectingControllers_cannotSeekAnywhere() {
        val commands = callback.onConnectAsync(session, controller).get().availablePlayerCommands

        listOf(
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_MEDIA_ITEM,
            Player.COMMAND_SEEK_BACK,
            Player.COMMAND_SEEK_FORWARD,
            Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        ).forEach { assertThat(commands.contains(it)).isFalse() }
    }

    @Test
    fun connectingControllers_canStillPlayPauseAndPrepare() {
        val commands = callback.onConnectAsync(session, controller).get().availablePlayerCommands

        assertThat(commands.contains(Player.COMMAND_PLAY_PAUSE)).isTrue()
        assertThat(commands.contains(Player.COMMAND_PREPARE)).isTrue()
        assertThat(commands.contains(Player.COMMAND_STOP)).isTrue()
    }

    @Test
    fun resumption_offersTheStationFromTheStart() {
        val result = callback.onPlaybackResumption(session, controller, /* isForPlayback = */ true).get()

        assertThat(result.mediaItems).containsExactly(station)
        assertThat(result.startIndex).isEqualTo(0)
        assertThat(result.startPositionMs).isEqualTo(C.TIME_UNSET)
    }
}
