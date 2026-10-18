package house.ouwl.draadloos.playback

import android.net.Uri
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.io.FileNotFoundException
import java.io.IOException
import java.time.Duration
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class LiveStreamTest {

    private val backoff = ReconnectBackoff()
    private val retryTimes = mutableListOf<Long?>()
    private val liveStream = LiveStream(
        MediaItem.fromUri("aac"),
        MediaItem.fromUri("hls"),
        backoff,
        onRetryAt = { retryTimes += it },
    )
    private val policy = liveStream.loadErrorPolicy

    private fun attached(playWhenReady: Boolean, state: Int): FakeRadioPlayer =
        FakeRadioPlayer(playWhenReady, state).also {
            liveStream.attach(it)
            it.commands.clear()
        }

    private fun pass(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun loadFails(error: IOException = IOException("down")): Long =
        policy.getRetryDelayMsFor(
            LoadErrorInfo(
                LoadEventInfo(
                    /* loadTaskId = */ 0,
                    DataSpec(Uri.parse("aac")),
                    Uri.parse("aac"),
                    emptyMap(),
                    /* elapsedRealtimeMs = */ 0,
                    /* loadDurationMs = */ 0,
                    /* bytesLoaded = */ 0,
                ),
                MediaLoadData(C.DATA_TYPE_MEDIA),
                error,
                /* errorCount= */ 1,
            )
        )

    private fun streamError() = PlaybackException("down", null, PlaybackException.ERROR_CODE_IO_UNSPECIFIED)

    @Test
    fun attach_loadsTheAacStream_withoutConnecting() {
        val player = FakeRadioPlayer()

        liveStream.attach(player)

        assertThat(player.commands).containsExactly("setMediaItem:aac")
    }

    @Test
    fun pause_stopsTheStream() {
        val player = attached(true, Player.STATE_READY)

        player.pause()
        pass(0)

        assertThat(player.commands).containsExactly("pause", "stop").inOrder()
    }

    @Test
    fun pause_afterFallback_goesBackToAac() {
        val player = attached(true, Player.STATE_BUFFERING)
        repeat(3) { loadFails() }
        pass(8_000)
        assertThat(player.commands).containsExactly("setMediaItem:hls", "prepare").inOrder()
        player.commands.clear()

        player.pause()
        pass(0)

        assertThat(player.commands).containsExactly("pause", "stop", "setMediaItem:aac").inOrder()
    }

    @Test
    fun pause_startsTheBackoffOver() {
        val player = attached(true, Player.STATE_BUFFERING)
        loadFails()
        loadFails()

        player.pause()
        pass(0)

        assertThat(backoff.failures).isEqualTo(0)
    }

    @Test
    fun pause_cancelsAPendingReconnect() {
        val player = attached(true, Player.STATE_IDLE)
        player.moveTo(error = streamError())
        player.commands.clear()

        player.pause()
        pass(60_000)

        assertThat(player.commands).containsExactly("pause", "stop").inOrder()
    }

    @Test
    fun callEnding_rejoinsTheStreamLive() {
        val player = attached(true, Player.STATE_READY)

        player.moveTo(suppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS)
        assertThat(player.commands).isEmpty()
        pass(10_000)
        player.moveTo(suppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_NONE)

        assertThat(player.commands).containsExactly("stop", "seekToDefaultPosition", "prepare").inOrder()
    }

    @Test
    fun aShortChime_resumesFromTheBuffer() {
        val player = attached(true, Player.STATE_READY)

        player.moveTo(suppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS)
        pass(9_999)
        player.moveTo(suppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_NONE)

        assertThat(player.commands).isEmpty()
    }

    @Test
    fun suppressionCountsFromWhenItFirstStarted() {
        val player = attached(true, Player.STATE_READY)

        player.moveTo(suppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS)
        pass(6_000)
        // Another reason replacing the first one is still the same gap in the audio.
        player.moveTo(suppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_UNSUITABLE_AUDIO_OUTPUT)
        pass(6_000)
        player.moveTo(suppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_NONE)

        assertThat(player.commands).containsExactly("stop", "seekToDefaultPosition", "prepare").inOrder()
    }

    @Test
    fun callEnding_afterTheUserStopped_doesNothing() {
        val player = attached(true, Player.STATE_READY)
        player.moveTo(suppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS)

        player.pause()
        pass(0)
        player.commands.clear()
        player.moveTo(suppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_NONE)

        assertThat(player.commands).isEmpty()
    }

    @Test
    fun suppressionEndingWithoutOneStarting_doesNothing() {
        val player = attached(true, Player.STATE_READY)

        player.moveTo(suppressionReason = Player.PLAYBACK_SUPPRESSION_REASON_NONE)

        assertThat(player.commands).isEmpty()
    }

    @Test
    fun playingByRequest_doesNothingExtra() {
        val player = attached(false, Player.STATE_IDLE)

        player.moveTo(playWhenReady = true, playWhenReadyReason = Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)

        assertThat(player.commands).isEmpty()
    }

    @Test
    fun playingAgain_resetsTheBackoff() {
        val player = attached(true, Player.STATE_BUFFERING)
        loadFails()
        loadFails()

        player.moveTo(playbackState = Player.STATE_READY)

        assertThat(backoff.failures).isEqualTo(0)
    }

    @Test
    fun bufferingDoesNotResetTheBackoff() {
        val player = attached(true, Player.STATE_READY)
        loadFails()

        player.moveTo(playbackState = Player.STATE_BUFFERING)

        assertThat(backoff.failures).isEqualTo(1)
    }

    @Test
    fun streamError_reconnectsAfterTheBackoffDelay() {
        val player = attached(true, Player.STATE_READY)

        player.moveTo(playbackState = Player.STATE_IDLE, error = streamError())
        pass(1_999)
        assertThat(player.commands).isEmpty()
        pass(1)

        assertThat(player.commands).containsExactly("stop", "seekToDefaultPosition", "prepare").inOrder()
    }

    @Test
    fun streamEnding_reconnectsAfterTheBackoffDelay() {
        val player = attached(true, Player.STATE_READY)

        player.moveTo(playbackState = Player.STATE_ENDED)
        pass(2_000)

        assertThat(player.commands).containsExactly("stop", "seekToDefaultPosition", "prepare").inOrder()
    }

    @Test
    fun repeatedErrors_backOffLonger() {
        val player = attached(true, Player.STATE_READY)
        player.moveTo(playbackState = Player.STATE_IDLE, error = streamError())
        pass(2_000)
        player.moveTo(playbackState = Player.STATE_IDLE, error = streamError())
        player.commands.clear()

        pass(3_999)
        assertThat(player.commands).isEmpty()
        pass(1)

        assertThat(player.commands).containsExactly("stop", "seekToDefaultPosition", "prepare").inOrder()
    }

    @Test
    fun reconnect_isSkipped_whenTheUserAlreadyRestarted() {
        val player = attached(true, Player.STATE_READY)
        player.moveTo(playbackState = Player.STATE_IDLE, error = streamError())
        player.prepare()
        player.commands.clear()

        pass(2_000)

        assertThat(player.commands).isEmpty()
    }

    @Test
    fun loadErrors_areRetriedWithBackoff_neverGivenUpOn() {
        assertThat(loadFails()).isEqualTo(2_000)
        assertThat(loadFails()).isEqualTo(4_000)
        assertThat(loadFails()).isEqualTo(8_000)
        assertThat(loadFails()).isEqualTo(16_000)
        assertThat(loadFails()).isEqualTo(30_000)
    }

    @Test
    fun errorsTheDefaultPolicyTreatsAsFatal_stillRetry() {
        assertThat(loadFails(FileNotFoundException("404"))).isEqualTo(2_000)
        assertThat(policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE))
            .isAtLeast(1_000_000)
    }

    @Test
    fun threeFailuresInARow_switchToHls_andSixSwitchBack() {
        val player = attached(true, Player.STATE_BUFFERING)

        repeat(2) { loadFails() }
        pass(60_000)
        assertThat(player.commands).isEmpty()

        loadFails()
        pass(7_999)
        assertThat(player.commands).isEmpty()
        pass(1)
        assertThat(player.commands).containsExactly("setMediaItem:hls", "prepare").inOrder()
        player.commands.clear()

        repeat(3) { loadFails() }
        pass(29_999)
        assertThat(player.commands).isEmpty()
        pass(1)
        assertThat(player.commands).containsExactly("setMediaItem:aac", "prepare").inOrder()
    }

    @Test
    fun noSwitch_whenTheUserHasStopped() {
        val player = attached(false, Player.STATE_IDLE)

        repeat(3) { loadFails() }
        pass(60_000)

        assertThat(player.commands).isEmpty()
    }

    @Test
    fun release_dropsPendingWork() {
        val player = attached(true, Player.STATE_READY)
        player.moveTo(playbackState = Player.STATE_IDLE, error = streamError())

        liveStream.release()
        pass(60_000)

        assertThat(player.commands).isEmpty()
    }

    @Test
    fun aLoadFailingWhileAudioPlays_isNotAnOutage() {
        val player = attached(true, Player.STATE_READY)

        repeat(5) { loadFails() }
        pass(60_000)

        assertThat(backoff.failures).isEqualTo(1)
        assertThat(player.commands).isEmpty()
    }

    @Test
    fun switchingSource_followsTheBackoffSchedule_andSkipsTheReconnect() {
        val player = attached(true, Player.STATE_READY)
        repeat(2) {
            player.moveTo(playbackState = Player.STATE_IDLE, error = streamError())
            pass(if (it == 0) 2_000 else 4_000)
        }
        player.commands.clear()

        player.moveTo(playbackState = Player.STATE_IDLE, error = streamError())
        pass(7_999)
        assertThat(player.commands).isEmpty()
        pass(1)

        // The third failure switches to HLS instead of reconnecting to the same stream.
        assertThat(player.commands).containsExactly("setMediaItem:hls", "prepare").inOrder()
    }

    @Test
    fun recovering_cancelsAPlannedSwitch() {
        val player = attached(true, Player.STATE_BUFFERING)
        repeat(3) { loadFails() }

        player.moveTo(playbackState = Player.STATE_READY)
        pass(60_000)

        assertThat(player.commands).isEmpty()
    }

    @Test
    fun streamError_announcesWhenTheRetryStarts() {
        val player = attached(true, Player.STATE_READY)

        player.moveTo(playbackState = Player.STATE_IDLE, error = streamError())
        pass(0)

        assertThat(retryTimes).containsExactly(SystemClock.elapsedRealtime() + 2_000)
    }

    @Test
    fun theRetryStarting_clearsTheAnnouncement() {
        val player = attached(true, Player.STATE_READY)
        player.moveTo(playbackState = Player.STATE_IDLE, error = streamError())

        pass(2_000)

        assertThat(retryTimes.last()).isNull()
        assertThat(retryTimes).hasSize(2)
    }

    @Test
    fun aSourceSwitchStarting_clearsTheAnnouncement() {
        attached(true, Player.STATE_BUFFERING)
        repeat(3) { loadFails() }
        pass(0)
        assertThat(retryTimes.last()).isNotNull()

        pass(8_000)

        assertThat(retryTimes.last()).isNull()
    }

    @Test
    fun loadErrors_announceTheirRetry_whileConnecting() {
        attached(true, Player.STATE_BUFFERING)

        loadFails()
        loadFails()
        pass(0)

        assertThat(retryTimes).containsExactly(
            SystemClock.elapsedRealtime() + 2_000,
            SystemClock.elapsedRealtime() + 4_000,
        ).inOrder()
    }

    @Test
    fun aLoadFailingWhileAudioPlays_isNotAnnounced() {
        attached(true, Player.STATE_READY)

        loadFails()
        pass(0)

        assertThat(retryTimes).isEmpty()
    }

    @Test
    fun playingAgain_clearsTheAnnouncement() {
        val player = attached(true, Player.STATE_BUFFERING)
        loadFails()

        player.moveTo(playbackState = Player.STATE_READY)
        pass(0)

        assertThat(retryTimes.last()).isNull()
    }

    @Test
    fun stopping_clearsTheAnnouncement() {
        val player = attached(true, Player.STATE_IDLE)
        player.moveTo(error = streamError())
        pass(0)
        assertThat(retryTimes.last()).isNotNull()

        player.pause()
        pass(0)

        assertThat(retryTimes.last()).isNull()
    }
}
