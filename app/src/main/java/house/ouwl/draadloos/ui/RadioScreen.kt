package house.ouwl.draadloos.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import house.ouwl.draadloos.R
import house.ouwl.draadloos.playback.isOn

// High contrast for an older user: black background, bright yellow and white.
private val Background = Color(0xFF000000)
private val Foreground = Color(0xFFFFFFFF)
private val PlayColour = Color(0xFFFFD000)
private val StopColour = Color(0xFFFF5A3C)

@Composable
fun RadioScreen(controller: Player?, onToggle: () -> Unit) {
    // Mirrors the session: "on" means playing or connecting; a failed stream reads as off.
    var isOn by remember(controller) { mutableStateOf(controller?.isOn == true) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                isOn = player.isOn
            }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.radio),
            contentDescription = null,
            modifier = Modifier.size(160.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.station_name),
            color = Foreground,
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.station_tagline),
            color = Foreground,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onToggle,
            enabled = controller != null,
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isOn) StopColour else PlayColour,
                contentColor = Background,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        ) {
            Text(
                text = stringResource(if (isOn) R.string.stop else R.string.play),
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
