package house.ouwl.draadloos.playback

import android.os.Bundle

/**
 * Session extra holding when the next reconnect attempt starts, in
 * [android.os.SystemClock.elapsedRealtime] milliseconds. It is there only while the radio is
 * waiting to retry, so the app can tell "connection lost, trying again in N seconds" from
 * the first "connecting". [ReconnectingPlayer] hides the difference from the player's state.
 */
internal const val RETRY_AT_EXTRA = "house.ouwl.draadloos.RETRY_AT"

/** The session extras for a retry at [retryAt], or none while there is no retry pending. */
fun retryExtras(retryAt: Long?): Bundle =
    Bundle().apply { if (retryAt != null) putLong(RETRY_AT_EXTRA, retryAt) }

/** When the next reconnect starts, or null while none is pending. */
fun Bundle.retryAt(): Long? = if (containsKey(RETRY_AT_EXTRA)) getLong(RETRY_AT_EXTRA) else null
