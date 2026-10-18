package house.ouwl.draadloos.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadioControlsTest {

    @Test
    fun isOn_whilePlayingOrConnecting() {
        assertThat(FakeRadioPlayer(true, Player.STATE_READY).isOn).isTrue()
        assertThat(FakeRadioPlayer(true, Player.STATE_BUFFERING).isOn).isTrue()
    }

    @Test
    fun isOff_whenFailedEndedOrPaused() {
        // A stream error leaves the player idle with playWhenReady still set.
        assertThat(FakeRadioPlayer(true, Player.STATE_IDLE).isOn).isFalse()
        assertThat(FakeRadioPlayer(true, Player.STATE_ENDED).isOn).isFalse()
        assertThat(FakeRadioPlayer(false, Player.STATE_READY).isOn).isFalse()
        assertThat(FakeRadioPlayer(false, Player.STATE_IDLE).isOn).isFalse()
    }

    @Test
    fun toggle_whenOn_pauses() {
        val player = FakeRadioPlayer(true, Player.STATE_READY)

        player.toggle()

        assertThat(player.commands).containsExactly("pause")
    }

    @Test
    fun toggle_whenIdle_preparesThenPlays() {
        val player = FakeRadioPlayer(false, Player.STATE_IDLE)

        player.toggle()

        assertThat(player.commands).containsExactly("prepare", "play").inOrder()
    }

    @Test
    fun toggle_afterStreamError_reconnects() {
        val player = FakeRadioPlayer(true, Player.STATE_IDLE)

        player.toggle()

        assertThat(player.commands).containsExactly("prepare", "play").inOrder()
    }

    @Test
    fun toggle_whenPausedButPrepared_onlyPlays() {
        val player = FakeRadioPlayer(false, Player.STATE_READY)

        player.toggle()

        assertThat(player.commands).containsExactly("play")
    }

    private fun playerShowing(line: String?) = FakeRadioPlayer(
        mediaItem = MediaItem.Builder()
            .setMediaId("rsg")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("RSG")
                    .setExtras(line?.let { Bundle().apply { putString(NOW_PLAYING_EXTRA, it) } })
                    .build()
            )
            .build(),
    )

    @Test
    fun nowPlayingLine_isTheMarkedSong() {
        assertThat(playerShowing("Kobus Human – Môre").nowPlayingLine()).isEqualTo("Kobus Human – Môre")
    }

    @Test
    fun nowPlayingLine_isNull_forTheStation() {
        assertThat(playerShowing(null).nowPlayingLine()).isNull()
    }

    @Test
    fun nowPlayingLine_ignoresTitleAndArtistText() {
        // A title that happens to differ from the station name is not a song unless marked.
        val player = FakeRadioPlayer(
            mediaItem = MediaItem.Builder()
                .setMediaId("rsg")
                .setMediaMetadata(MediaMetadata.Builder().setTitle("Môre").setArtist("Kobus Human").build())
                .build(),
        )

        assertThat(player.nowPlayingLine()).isNull()
    }
}
