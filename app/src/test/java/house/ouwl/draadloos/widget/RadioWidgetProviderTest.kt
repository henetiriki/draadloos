package house.ouwl.draadloos.widget

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadioWidgetProviderTest {

    @Test
    fun theReceivers_provideTheirWidgets() {
        assertThat(RadioWideWidgetReceiver().glanceAppWidget).isInstanceOf(RadioWideWidget::class.java)
        assertThat(RadioSmallWidgetReceiver().glanceAppWidget).isInstanceOf(RadioSmallWidget::class.java)
    }
}
