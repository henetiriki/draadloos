package house.ouwl.draadloos.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
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

/** The three things the screen can show for the session. Layout and colours follow docs/DESIGN.md. */
internal enum class RadioState { Stopped, Connecting, Playing }

internal fun Player.radioState(): RadioState = when {
    !isOn -> RadioState.Stopped
    isPlaying -> RadioState.Playing
    else -> RadioState.Connecting
}

/** A short rebuffer while audio is playing shouldn't flash "Koppel aan RSG…". */
private const val CONNECTING_DEBOUNCE_MS = 300L

@Composable
fun RadioScreen(controller: Player?, onToggle: () -> Unit) {
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

    var state by remember(controller) { mutableStateOf(target) }
    LaunchedEffect(target) {
        if (state == RadioState.Playing && target == RadioState.Connecting) delay(CONNECTING_DEBOUNCE_MS)
        state = target
    }

    val playing = state == RadioState.Playing
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
                    top = if (playing) 56.dp else 72.dp,
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
            StationArt(playing)
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.station_name),
                color = OnBackground,
                style = if (playing) Type.StationLargePlaying else Type.StationLarge,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.station_tagline),
                color = OnBackground,
                style = Type.StationSub,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            StatusPill(state)
            if (playing && nowPlaying != null) {
                Spacer(Modifier.height(24.dp))
                NowPlayingCard(nowPlaying!!)
            }
        }
        Spacer(Modifier.height(24.dp))
        ToggleButton(on = on, enabled = controller != null, onClick = onToggle)
    }
}

@Composable
private fun StationArt(playing: Boolean) {
    val size = if (playing) 132.dp else 168.dp
    val corner = if (playing) 28.dp else 32.dp
    Image(
        painter = painterResource(R.drawable.radio),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(Color.White),
    )
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
