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

fun Player.toggle() {
    if (isOn) {
        pause()
    } else {
        if (playbackState == Player.STATE_IDLE) prepare()
        play()
    }
}
