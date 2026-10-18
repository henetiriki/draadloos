package house.ouwl.draadloos.playback

import androidx.media3.common.Player

/**
 * True while the radio is playing or connecting. A failed or ended stream drops back to
 * idle with playWhenReady still set, which must read as "off" so the button offers "Speel".
 */
val Player.isOn: Boolean
    get() = playWhenReady &&
        playbackState != Player.STATE_IDLE &&
        playbackState != Player.STATE_ENDED

/**
 * "artist – title" while a song is on, null otherwise. Between songs the session carries
 * the station's own defaults (title is the station name, artist is [tagline]), which are
 * not a song; a song without an artist also carries the tagline as its artist.
 */
fun Player.nowPlayingLine(tagline: String): String? {
    val title = mediaMetadata.title?.toString()
    if (title.isNullOrEmpty() || title == mediaMetadata.station?.toString()) return null
    val artist = mediaMetadata.artist?.toString()
    return if (artist.isNullOrEmpty() || artist == tagline) title else "$artist – $title"
}

fun Player.toggle() {
    if (isOn) {
        pause()
    } else {
        if (playbackState == Player.STATE_IDLE) prepare()
        play()
    }
}
