package house.ouwl.draadloos.playback

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import house.ouwl.draadloos.R

/** The one station this app plays. */
object Station {
    /**
     * Triton/StreamTheWorld redirect. ExoPlayer follows it to a numbered live host;
     * never hardcode that host, as it changes between connections.
     */
    const val STREAM_URL =
        "https://playerservices.streamtheworld.com/api/livestream-redirect/RSGAAC.aac"

    private const val MEDIA_ID = "rsg"

    fun mediaItem(context: Context): MediaItem =
        MediaItem.Builder()
            .setMediaId(MEDIA_ID)
            .setUri(STREAM_URL)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(context.getString(R.string.station_name))
                    .setArtist(context.getString(R.string.station_tagline))
                    .setStation(context.getString(R.string.station_name))
                    .setArtworkUri(artworkUri(context))
                    .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()

    private fun artworkUri(context: Context): Uri =
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .appendPath(R.drawable.radio.toString())
            .build()
}
