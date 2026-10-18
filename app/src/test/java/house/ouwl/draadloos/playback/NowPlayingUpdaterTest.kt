package house.ouwl.draadloos.playback

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyHeaders
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class NowPlayingUpdaterTest {

    private val updater = NowPlayingUpdater("RSG", "Radio Sonder Grense")

    private fun item(title: String, artist: String) =
        MediaItem.Builder()
            .setMediaId("rsg")
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(title).setArtist(artist).setStation("RSG").build()
            )
            .build()

    private fun attached(playWhenReady: Boolean = true, current: MediaItem = item("RSG", "Radio Sonder Grense")) =
        FakeRadioPlayer(playWhenReady, Player.STATE_READY, current).also {
            updater.attach(it)
            it.commands.clear()
        }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun icy(title: String?) = Metadata(IcyInfo(ByteArray(0), title, null))

    @Test
    fun song_replacesTitleAndArtist_withoutRestartingTheStream() {
        val player = attached()

        updater.onMetadata(icy("Kobus Human - Môre"))

        assertThat(player.commands).containsExactly("replaceMediaItem:Môre/Kobus Human")
    }

    @Test
    fun song_keepsTheRestOfTheMetadata() {
        val player = attached()

        updater.onMetadata(icy("A - B"))

        assertThat(player.mediaMetadata.station.toString()).isEqualTo("RSG")
    }

    @Test
    fun titleOnly_keepsTheTagline() {
        val player = attached()

        updater.onMetadata(icy("Nuus om sewe"))

        assertThat(player.commands).containsExactly("replaceMediaItem:Nuus om sewe/Radio Sonder Grense")
    }

    @Test
    fun sameSongTwice_isReplacedOnce() {
        val player = attached()

        updater.onMetadata(icy("A - B"))
        updater.onMetadata(icy("A - B"))

        assertThat(player.commands).hasSize(1)
    }

    @Test
    fun nextSong_replacesThePrevious() {
        val player = attached()

        updater.onMetadata(icy("A - B"))
        updater.onMetadata(icy("C - D"))

        assertThat(player.commands).containsExactly(
            "replaceMediaItem:B/A",
            "replaceMediaItem:D/C",
        ).inOrder()
    }

    @Test
    fun streamNameOnly_goesBackToTheDefaults() {
        val player = attached(current = item("B", "A"))

        updater.onMetadata(icy("RSG"))

        assertThat(player.commands).containsExactly("replaceMediaItem:RSG/Radio Sonder Grense")
    }

    @Test
    fun missingTitle_whenAlreadyDefault_doesNothing() {
        val player = attached()

        updater.onMetadata(icy(null))

        assertThat(player.commands).isEmpty()
    }

    @Test
    fun emptyMetadata_isIgnored() {
        val player = attached()

        updater.onMetadata(Metadata())

        assertThat(player.commands).isEmpty()
    }

    @Test
    fun sameTitleWithADifferentArtist_isReplaced() {
        val player = attached(current = item("B", "Radio Sonder Grense"))

        updater.onMetadata(icy("A - B"))

        assertThat(player.commands).containsExactly("replaceMediaItem:B/A")
    }

    @Test
    fun withoutAnItem_doesNothing() {
        val player = FakeRadioPlayer(mediaItem = null)
        updater.attach(player)

        updater.onMetadata(icy("A - B"))

        assertThat(player.commands).isEmpty()
    }

    @Test
    fun metadataThatIsNotIcyInfo_isIgnored() {
        val player = attached()

        updater.onMetadata(Metadata(IcyHeaders(48_000, "Misc", "RSG", "http://www.rsg.co.za", false, 16_000)))

        assertThat(player.commands).isEmpty()
    }

    @Test
    fun stopping_putsTheDefaultsBack() {
        val player = attached(current = item("B", "A"))

        player.pause()
        idle()

        assertThat(player.commands).containsExactly("pause", "replaceMediaItem:RSG/Radio Sonder Grense").inOrder()
    }

    @Test
    fun stopping_whenAlreadyDefault_doesNothingExtra() {
        val player = attached()

        player.pause()
        idle()

        assertThat(player.commands).containsExactly("pause")
    }

    @Test
    fun starting_keepsTheCurrentItem() {
        val player = attached(playWhenReady = false, current = item("B", "A"))

        player.play()
        idle()

        assertThat(player.commands).containsExactly("play")
    }
}
