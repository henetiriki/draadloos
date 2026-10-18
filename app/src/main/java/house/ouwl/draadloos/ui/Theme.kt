package house.ouwl.draadloos.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import house.ouwl.draadloos.R

// Design tokens from docs/DESIGN.md. Dark only, on purpose: high contrast is the point.
internal val Background = Color(0xFF000000)
internal val OnBackground = Color(0xFFFFFFFF)
internal val TextMuted = Color(0xFFD4D4D4)
internal val TextSubtle = Color(0xFFC8C8C8)
internal val Accent = Color(0xFFF7D046)
/** The Speel button while the player isn't connected yet: dimmed, so it doesn't look pressable. */
internal val AccentDisabled = Accent.copy(alpha = 0.4f)
internal val StopButton =Color(0xFFFFFFFF)
internal val OnButton = Color(0xFF000000)
internal val Live = Color(0xFFFF5A4E)
internal val Divider = Color(0xFF3A3A3A)
internal val PillBorder = Color(0xFF4A4A4A)
internal val PillFill = Color(0xFF1F1F1F)
internal val IdleIcon = Color(0xFFA8A8A8)

/** Atkinson Hyperlegible, bundled in res/font/ (SIL Open Font License). */
internal val Atkinson = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Atkinson, fontSize = size.sp, fontWeight = weight)

internal object Type {
    val ButtonLabel = style(64, FontWeight.Bold)
    val StationLarge = style(64, FontWeight.Bold)
    val StationLargePlaying = style(56, FontWeight.Bold)
    val StationSub = style(24)
    val Status = style(22)
    val StatusBold = style(22, FontWeight.Bold)
    val NowPlayingArtist = style(28, FontWeight.Bold)
    val NowPlayingTitle = style(24)
    val Label = style(18)
    val WarningTitle = style(26, FontWeight.Bold)
    val WarningBody = style(20)
}
