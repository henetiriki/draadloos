package house.ouwl.draadloos.playback

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

    private fun playerShowing(title: String?, artist: String?) = FakeRadioPlayer(
        mediaItem = MediaItem.Builder()
            .setMediaId("rsg")
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(title).setArtist(artist).setStation("RSG").build()
            )
            .build(),
    )

    
    fun nowPlayingLine_isArtistDashTitle() {
        assertThat(playerShowing("Môre", "Kobus Human").nowPlayingLine("Radio Sonder Grense"))
            .isEqualTo("Kobus Human – Môre")
    }

    
    fun nowPlayingLine_isJustTheTitle_whenTheArtistIsTheTagline() {
        assertThat(playerShowing("Nuus", "Radio Sonder Grense").nowPlayingLine("Radio Sonder Grense"))
            .isEqualTo("Nuus")
        assertThat(playerShowing("Nuus", null).nowPlayingLine("Radio Sonder Grense")).isEqualTo("Nuus")
    }

    
    fun nowPlayingLine_isNull_forTheStationDefaults() {
        assertThat(playerShowing("RSG", "Radio Sonder Grense").nowPlayingLine("Radio Sonder Grense")).isNull()
        assertThat(playerShowing(null, null).nowPlayingLine("Radio Sonder Grense")).isNull()
    }

    @Test
    fun nowPlayingLine_isNull_forAnEmptyTitle() {
        assertThat(playerShowing("", "Kobus Human").nowPlayingLine("Radio Sonder Grense")).isNull()
    }

    @Test
    fun nowPlayingLine_withoutAStationName_stillShowsTheSong() {
        val player = FakeRadioPlayer(
            mediaItem = MediaItem.Builder()
                .setMediaId("rsg")
                .setMediaMetadata(MediaMetadata.Builder().setTitle("Môre").setArtist("Kobus Human").build())
                .build(),
        )

        assertThat(player.nowPlayingLine("Radio Sonder Grense")).isEqualTo("Kobus Human – Môre")
    }

    @Test
    fun nowPlayingLine_isJustTheTitle_whenTheArtistIsEmpty() {
        assertThat(playerShowing("Nuus", "").nowPlayingLine("Radio Sonder Grense")).isEqualTo("Nuus")
    }
}
