package house.ouwl.draadloos.ui

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import house.ouwl.draadloos.R
import house.ouwl.draadloos.playback.NowPlaying
import house.ouwl.draadloos.playback.isOn
import house.ouwl.draadloos.playback.nowPlaying
import kotlinx.coroutines.delay

/** What the screen can show for the session. Layout and colours follow docs/DESIGN.md. */
internal enum class RadioState { Stopped, Connecting, Reconnecting, Playing }

/** Stopped, playing, or waiting for audio. [RadioState.Reconnecting] needs the retry time too. */
internal fun Player.radioState(): RadioState = when {
    !isOn -> RadioState.Stopped
    isPlaying -> RadioState.Playing
    else -> RadioState.Connecting
}

/** A short rebuffer while audio is playing shouldn't flash "Koppel aan RSG…". */
private const val CONNECTING_DEBOUNCE_MS = 300L

/**
 * [retryAt] is when the radio retries after losing the stream, in elapsedRealtime milliseconds
 * (null when it isn't waiting to retry). While that time is ahead the screen shows the
 * "Verbinding verloor" panel and counts down to it.
 */
@Composable
fun RadioScreen(
    controller: Player?,
    retryAt: Long?,
    onToggle: () -> Unit,
    clock: () -> Long = SystemClock::elapsedRealtime,
) {
    var target by remember(controller) { mutableStateOf(controller?.radioState() ?: RadioState.Stopped) }
    var nowPlaying by remember(controller) { mutableStateOf(controller?.nowPlaying()) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                target = player.radioState()
                nowPlaying = player.nowPlaying()
            }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }

    // Ticks once a second while a retry is ahead, which also moves the countdown on.
    var now by remember { mutableLongStateOf(clock()) }
    LaunchedEffect(retryAt) {
        now = clock()
        while (retryAt != null && now < retryAt) {
            delay(1_000)
            now = clock()
        }
    }
    val remainingMs = if (retryAt == null) 0 else retryAt - now
    val waiting = remainingMs > 0
    val wanted = if (target == RadioState.Connecting && waiting) RadioState.Reconnecting else target

    var state by remember(controller) { mutableStateOf(wanted) }
    LaunchedEffect(wanted) {
        val dropped = wanted == RadioState.Connecting || wanted == RadioState.Reconnecting
        if (state == RadioState.Playing && dropped) delay(CONNECTING_DEBOUNCE_MS)
        state = wanted
    }

    val compact = state == RadioState.Playing || state == RadioState.Reconnecting
    val on = state != RadioState.Stopped

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .safeDrawingPadding()
            .padding(
                PaddingValues(
                    start = 24.dp,
                    end = 24.dp,
                    top = if (compact) 56.dp else 72.dp,
                    bottom = 40.dp,
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            StationArt(compact, dimmed = state == RadioState.Reconnecting)
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.station_name),
                color = OnBackground,
                style = if (compact) Type.StationLargePlaying else Type.StationLarge,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.station_tagline),
                color = OnBackground,
                style = Type.StationSub,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            if (state == RadioState.Reconnecting) {
                WarningPanel()
                Spacer(Modifier.height(16.dp))
                // Rounded up, so the last second reads 1 and never 0.
                RetryCountdown(((remainingMs + 999) / 1_000).toInt().coerceAtLeast(1))
            } else {
                StatusPill(state)
                if (state == RadioState.Playing && nowPlaying != null) {
                    Spacer(Modifier.height(24.dp))
                    NowPlayingCard(nowPlaying!!)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        ToggleButton(on = on, enabled = controller != null, onClick = onToggle)
    }
}

@Composable
private fun StationArt(compact: Boolean, dimmed: Boolean) {
    val size = if (compact) 132.dp else 168.dp
    val corner = if (compact) 28.dp else 32.dp
    Image(
        painter = painterResource(R.drawable.radio),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .alpha(if (dimmed) 0.55f else 1f)
            .clip(RoundedCornerShape(corner))
            .background(Color.White),
    )
}

@Composable
private fun WarningPanel() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .background(Accent, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WifiOffIcon(size = 36.dp, color = OnButton)
        Column {
            Text(stringResource(R.string.warning_title), color = OnButton, style = Type.WarningTitle)
            Text(stringResource(R.string.warning_body), color = OnButton, style = Type.WarningBody)
        }
    }
}

@Composable
private fun RetryCountdown(seconds: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spinner()
        Text(
            text = pluralStringResource(R.plurals.retry_countdown, seconds, seconds),
            color = TextMuted,
            style = Type.WarningBody,
        )
    }
}

@Composable
private fun StatusPill(state: RadioState) {
    val modifier = if (state == RadioState.Stopped) {
        Modifier.border(2.dp, PillBorder, CircleShape)
    } else if (state == RadioState.Connecting) {
        Modifier.border(2.dp, Accent, CircleShape)
    } else {
        Modifier.background(PillFill, CircleShape)
    }
    Row(
        modifier = modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state == RadioState.Stopped) {
            Box(Modifier.size(14.dp).background(IdleIcon, RoundedCornerShape(3.dp)))
            Text(stringResource(R.string.status_stopped), color = TextMuted, style = Type.Status)
        } else if (state == RadioState.Connecting) {
            Spinner()
            Text(stringResource(R.string.status_connecting), color = OnBackground, style = Type.Status)
        } else {
            LiveDot()
            Text(stringResource(R.string.status_live), color = OnBackground, style = Type.StatusBold)
        }
    }
}

@Composable
private fun NowPlayingCard(song: NowPlaying) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, Divider, RoundedCornerShape(24.dp))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.now_playing_label), color = TextSubtle, style = Type.Label)
        if (song.artist != null) {
            FittingText(song.artist, OnBackground, Type.NowPlayingArtist)
        }
        FittingText(song.title, Accent, Type.NowPlayingTitle)
    }
}

/** Up to two lines with an ellipsis; the size shrinks first so long names still fit. */
@Composable
private fun FittingText(text: String, color: Color, style: TextStyle) {
    Text(
        text = text,
        color = color,
        style = style,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(
            minFontSize = (style.fontSize.value * 0.6f).sp,
            maxFontSize = style.fontSize,
        ),
    )
}

@Composable
private fun ToggleButton(on: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val description = stringResource(if (on) R.string.stop_description else R.string.play_description)
    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (on) StopButton else Accent,
            contentColor = OnButton,
            disabledContainerColor = Accent,
            disabledContentColor = OnButton,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics { contentDescription = description },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToggleIcon(on = on, size = 56.dp, color = OnButton)
            Spacer(Modifier.width(20.dp))
            Text(
                text = stringResource(if (on) R.string.stop else R.string.play),
                style = Type.ButtonLabel,
                maxLines = 1,
            )
        }
    }
}
