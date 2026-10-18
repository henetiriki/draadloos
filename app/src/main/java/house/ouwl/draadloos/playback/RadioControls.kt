package house.ouwl.draadloos.playback

import androidx.media3.common.Player

/**
 * True while the radio is meant to be playing: playing, connecting, or reconnecting after the
 * stream dropped (playWhenReady stays set and the retries carry on). The button then offers
 * "Stop", which is what ends the retries.
 */
val Player.isOn: Boolean
    get() = playWhenReady

/** "artist – title" while a song is on, null when the session shows the station itself. */
fun Player.nowPlayingLine(): String? = mediaMetadata.extras?.getString(NOW_PLAYING_EXTRA)

/** The song on air, split into artist and title, or null when the session shows the station itself. */
fun Player.nowPlaying(): NowPlaying? {
    val extras = mediaMetadata.extras ?: return null
    if (!extras.containsKey(NOW_PLAYING_EXTRA)) return null
    return NowPlaying(extras.getString(NOW_PLAYING_ARTIST_EXTRA), mediaMetadata.title.toString())
}

fun Player.toggle() {
    if (isOn) {
        pause()
    } else {
        if (playbackState == Player.STATE_IDLE) prepare()
        play()
    }
}
