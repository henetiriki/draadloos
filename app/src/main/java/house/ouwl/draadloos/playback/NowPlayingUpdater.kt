package house.ouwl.draadloos.playback

import androidx.annotation.OptIn
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyInfo

/**
 * Puts the song from the stream's ICY metadata on the session's media item, so the
 * notification, lock screen and app all show "artist – title". ExoPlayer prefers the media
 * item's own metadata over what the stream says, so the title has to be set on the item.
 * Replacing an item whose only change is its metadata keeps the stream connected.
 *
 * With no song (or after Stop) the item goes back to the station's [stationName] and [tagline].
 */
@OptIn(UnstableApi::class)
class NowPlayingUpdater(
    private val stationName: String,
    private val tagline: String,
) : Player.Listener {

    private lateinit var player: Player

    fun attach(player: Player) {
        this.player = player
        player.addListener(this)
    }

    override fun onMetadata(metadata: Metadata) {
        val icy = (0 until metadata.length()).firstNotNullOfOrNull { metadata[it] as? IcyInfo } ?: return
        val song = IcyTitle.parse(icy.title, stationName)
        show(song?.title ?: stationName, song?.artist ?: tagline)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        // Stopping must not leave the last song on the lock screen.
        if (!playWhenReady) show(stationName, tagline)
    }

    private fun show(title: String, artist: String) {
        val item = player.currentMediaItem ?: return
        if (item.mediaMetadata.title == title && item.mediaMetadata.artist == artist) return
        player.replaceMediaItem(
            player.currentMediaItemIndex,
            item.buildUpon()
                .setMediaMetadata(item.mediaMetadata.buildUpon().setTitle(title).setArtist(artist).build())
                .build(),
        )
    }
}
