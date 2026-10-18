package house.ouwl.draadloos.ui

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** False when the user turned animations off (animator duration scale 0): show things static. */
@Composable
private fun animationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}

/** A ring with a quarter arc that goes round once a second. */
@Composable
internal fun Spinner() {
    val angle = if (animationsEnabled()) {
        rememberInfiniteTransition(label = "spinner").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1_000, easing = LinearEasing)),
            label = "angle",
        ).value
    } else {
        0f
    }
    Canvas(Modifier.size(24.dp)) {
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        val inset = stroke.width / 2
        val arcSize = Size(this.size.width - stroke.width, this.size.height - stroke.width)
        drawArc(PillBorder, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
        rotate(angle) {
            drawArc(Accent, -90f, 90f, false, Offset(inset, inset), arcSize, style = stroke)
        }
    }
}

/** The red "live" dot; its opacity goes 1 → 0.35 → 1 over 1.6 seconds. */
@Composable
internal fun LiveDot() {
    val alpha = if (animationsEnabled()) {
        rememberInfiniteTransition(label = "live").animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "alpha",
        ).value
    } else {
        1f
    }
    Box(Modifier.size(14.dp).alpha(alpha).background(Live, CircleShape))
}

/** ■ while the radio is on, ▶ while it is off, so the button never relies on colour alone. */
@Composable
internal fun ToggleIcon(on: Boolean, size: Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        if (on) {
            drawRoundRect(
                color,
                topLeft = Offset(this.size.width * 0.12f, this.size.height * 0.12f),
                size = Size(this.size.width * 0.76f, this.size.height * 0.76f),
                cornerRadius = CornerRadius(this.size.width * 0.12f),
            )
        } else {
            val play = Path().apply {
                moveTo(this@Canvas.size.width * 0.2f, this@Canvas.size.height * 0.08f)
                lineTo(this@Canvas.size.width * 0.92f, this@Canvas.size.height * 0.5f)
                lineTo(this@Canvas.size.width * 0.2f, this@Canvas.size.height * 0.92f)
                close()
            }
            drawPath(play, color)
        }
    }
}
