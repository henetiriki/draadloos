package house.ouwl.draadloos.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import house.ouwl.draadloos.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val item = Station.mediaItem(context)

    @Test
    fun streamsTheTritonRedirect() {
        assertThat(item.localConfiguration!!.uri.toString()).isEqualTo(
            "https://playerservices.streamtheworld.com/api/livestream-redirect/RSGAAC.aac"
        )
        assertThat(item.mediaId).isEqualTo("rsg")
    }

    @Test
    fun fallsBackToTheHlsStream_withTheSameMetadata() {
        val fallback = Station.mediaItem(context, Station.FALLBACK_URL)

        assertThat(fallback.localConfiguration!!.uri.toString()).isEqualTo(
            "https://playerservices.streamtheworld.com/api/livestream-redirect/RSGAAC.m3u8"
        )
        assertThat(fallback.mediaId).isEqualTo("rsg")
        assertThat(fallback.mediaMetadata.title.toString()).isEqualTo("RSG")
    }

    @OptIn(UnstableApi::class)
    @Test
    fun hlsStreamHasAMediaSource() {
        // Fails if the HLS module is missing from the app.
        val source = DefaultMediaSourceFactory(context)
            .createMediaSource(Station.mediaItem(context, Station.FALLBACK_URL))

        assertThat(source).isInstanceOf(HlsMediaSource::class.java)
    }

    @Test
    fun describesTheStation() {
        val metadata = item.mediaMetadata
        assertThat(metadata.title.toString()).isEqualTo("RSG")
        assertThat(metadata.artist.toString()).isEqualTo("Radio Sonder Grense")
        assertThat(metadata.station.toString()).isEqualTo("RSG")
        assertThat(metadata.mediaType).isEqualTo(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
        assertThat(metadata.isPlayable).isTrue()
        assertThat(metadata.isBrowsable).isFalse()
    }

    @Test
    fun artworkIsReferencedByNameAndResolves() {
        val uri = item.mediaMetadata.artworkUri!!

        assertThat(uri.toString())
            .isEqualTo("android.resource://house.ouwl.draadloos/drawable/radio")
        // The name in the URI must map back to the bundled image.
        val (type, name) = uri.pathSegments
        assertThat(context.resources.getIdentifier(name, type, uri.authority))
            .isEqualTo(R.drawable.radio)
    }
}
