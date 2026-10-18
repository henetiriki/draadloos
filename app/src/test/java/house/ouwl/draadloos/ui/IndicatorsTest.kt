package house.ouwl.draadloos.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

// Drawing only runs when the screen is rendered, so these tests capture an image.
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IndicatorsTest {

    @get:Rule
    val compose = createComposeRule()

    private fun animationsOff() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private fun drawAll() {
        compose.setContent {
            Column {
                Spinner()
                LiveDot()
                ToggleIcon(on = false, size = 56.dp, color = Color.Black)
                ToggleIcon(on = true, size = 56.dp, color = Color.Black)
                WifiOffIcon(size = 36.dp, color = Color.Black)
            }
        }
        val image = compose.onRoot().captureToImage()
        assertThat(image.width).isGreaterThan(0)
    }

    @Test
    fun draws_withAnimations() = drawAll()

    @Test
    fun draws_withAnimationsOff() {
        animationsOff()
        drawAll()
    }
}
