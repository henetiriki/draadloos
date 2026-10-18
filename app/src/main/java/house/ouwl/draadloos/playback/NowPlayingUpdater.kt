package house.ouwl.draadloos.playback

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.metadata.icy.IcyInfo

/**
 * Marks a media item's metadata as a song: its extras carry the text to show. Items without
 * it show the station's own name and tagline, so nothing has to guess from the strings.
 */
internal const val NOW_PLAYING_EXTRA = "house.ouwl.draadloos.NOW_PLAYING"

/**
 * Puts the song from the stream's ICY metadata on the session's media item, so the
 * notification, lock screen and app all show "artist – title". ExoPlayer prefers the media
 * item's own metadata over what the stream says, so the title has to be set on the item.
 * Replacing an item whose only change is its metadata keeps the stream connected.
 *
 * With no song, or whenever the stream stops or drops, the item goes back to the station's
 * [stationName] and [tagline], so a stale song never stays on the lock screen.
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
        show(IcyTitle.parse(icy.title, stationName))
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (!playWhenReady) show(null)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        // An error or a lost connection drops to idle, and the song is no longer live.
        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) show(null)
    }

    private fun show(song: NowPlaying?) {
        val item = player.currentMediaItem ?: return
        val metadata = item.mediaMetadata
        val title = song?.title ?: stationName
        val artist = song?.artist ?: tagline
        if (metadata.title == title && metadata.artist == artist &&
            metadata.extras?.getString(NOW_PLAYING_EXTRA) == song?.line
        ) return
        player.replaceMediaItem(
            player.currentMediaItemIndex,
            item.buildUpon()
                // HLS puts a live configuration on the item it plays. A replacement that differs
                // from the playing item in more than its metadata makes ExoPlayer rebuild the
                // stream, so go back to the unset configuration the station item has.
                .setLiveConfiguration(MediaItem.LiveConfiguration.UNSET)
                .setMediaMetadata(
                    metadata.buildUpon()
                        .setTitle(title)
                        .setArtist(artist)
                        .setExtras(song?.let { Bundle().apply { putString(NOW_PLAYING_EXTRA, it.line) } })
                        .build()
                )
                .build(),
        )
    }
}
