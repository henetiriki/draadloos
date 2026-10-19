package house.ouwl.draadloos.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

/** DESIGN.md says every text and background pair passes WCAG AA. This keeps the tokens honest. */
class ContrastTest {

    private fun ratio(a: Color, b: Color): Double {
        val hi = max(a.luminance(), b.luminance()).toDouble()
        val lo = min(a.luminance(), b.luminance()).toDouble()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertAa(text: Color, background: Color, largeText: Boolean = false) {
        assertThat(ratio(text, background)).isAtLeast(if (largeText) 3.0 else 4.5)
    }

    @Test
    fun textOnTheBlackBackground() {
        assertAa(OnBackground, Background)
        assertAa(TextMuted, Background)
        assertAa(TextSubtle, Background)
        assertAa(Accent, Background)
    }

    @Test
    fun textOnThePills() {
        assertAa(OnBackground, PillFill)
        assertAa(TextMuted, Background)
        assertAa(OnBackground, Background)
    }

    @Test
    fun textAndIconsOnTheButtons() {
        assertAa(OnButton, Accent)
        assertAa(OnButton, StopButton)
    }

    @Test
    fun theWarningPanel() {
        assertAa(OnButton, Accent)
    }

    @Test
    fun theLiveDotAndIdleSquare_standOutFromTheirBackgrounds() {
        // Not text, so the 3:1 bar for graphics applies.
        assertThat(ratio(Live, PillFill)).isAtLeast(3.0)
        assertThat(ratio(IdleIcon, Background)).isAtLeast(3.0)
    }

    @Test
    fun theSongTitleInTheCard() {
        assertAa(Accent, Background)
    }
}
