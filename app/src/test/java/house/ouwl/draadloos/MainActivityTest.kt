package house.ouwl.draadloos

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Looper
import android.os.SystemClock
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import house.ouwl.draadloos.playback.PlaybackService
import house.ouwl.draadloos.playback.retryExtras
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private var service: ServiceController<PlaybackService>? = null

    @After
    fun tearDown() {
        service?.destroy()
    }

    @Test
    fun asksForNotificationPermission_onAndroid13AndUp() {
        blockPlaybackService()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                assertThat(shadowOf(it).lastRequestedPermission?.requestedPermissions)
                    .asList().containsExactly(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @Test
    fun doesNotAskAgain_whenAlreadyGranted() {
        blockPlaybackService()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { assertThat(shadowOf(it).lastRequestedPermission).isNull() }
        }
    }

    @Test
    @Config(sdk = [32])
    fun doesNotAsk_beforeAndroid13() {
        blockPlaybackService()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { assertThat(shadowOf(it).lastRequestedPermission).isNull() }
        }
    }

    @Test
    fun buttonStaysDisabled_whenThePlayerCannotBeReached() {
        blockPlaybackService()

        ActivityScenario.launch(MainActivity::class.java).use {
            idle()
            compose.onNodeWithText("Speel").assertIsNotEnabled()
        }
    }

    @Test
    fun connectsToPlaybackService_andTogglesIt() {
        val playback = bindRealPlaybackService()

        ActivityScenario.launch(MainActivity::class.java).use {
            idle()
            compose.onNodeWithText("Speel").assertIsEnabled().performClick()
            idle()

            val player = playback.get().sessions.single().player
            assertThat(player.playWhenReady).isTrue()
        }
    }

    @Test
    fun showsTheWarning_whenTheServiceAnnouncesARetry() {
        val playback = bindRealPlaybackService()

        ActivityScenario.launch(MainActivity::class.java).use {
            idle()
            compose.onNodeWithText("Speel").performClick()
            idle()

            playback.get().sessions.single()
                .setSessionExtras(retryExtras(SystemClock.elapsedRealtime() + 60_000))
            idle()

            compose.onNodeWithText("Verbinding verloor").assertExists()
        }
    }

    @Test
    fun extrasFromAnotherController_areIgnored() {
        bindRealPlaybackService()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idle()
            compose.onNodeWithText("Speel").performClick()
            idle()
            // A controller the activity isn't using, as a released one would be.
            val other = MediaController.Builder(
                app,
                SessionToken(app, ComponentName(app, PlaybackService::class.java)),
            ).buildAsync()
            idle()

            scenario.onActivity {
                it.extrasListener.onExtrasChanged(
                    other.get(),
                    retryExtras(SystemClock.elapsedRealtime() + 60_000),
                )
            }
            idle()

            compose.onNodeWithText("Verbinding verloor").assertDoesNotExist()
            MediaController.releaseFuture(other)
        }
    }

    @Test
    fun releasesTheControllerWhenStopped() {
        val playback = bindRealPlaybackService()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            idle()
            assertThat(appControllers(playback)).isNotEmpty()

            scenario.moveToState(Lifecycle.State.CREATED)
            idle()

            assertThat(appControllers(playback)).isEmpty()
        }
    }

    /** Makes binding to PlaybackService fail, so the controller never connects. */
    private fun blockPlaybackService() {
        shadowOf(app).declareComponentUnbindable(ComponentName(app, PlaybackService::class.java))
    }

    /** Lets MainActivity's MediaController bind to a real, in-process PlaybackService. */
    private fun bindRealPlaybackService(): ServiceController<PlaybackService> {
        val intent = Intent(MediaSessionService.SERVICE_INTERFACE)
            .setComponent(ComponentName(app, PlaybackService::class.java))
        val controller = Robolectric.buildService(PlaybackService::class.java, intent).create()
        shadowOf(app).setComponentNameAndServiceForBindService(
            intent.component,
            controller.get().onBind(intent),
        )
        service = controller
        return controller
    }

    /** Controllers connected from the app itself, ignoring Media3's own notification controller. */
    private fun appControllers(playback: ServiceController<PlaybackService>) =
        playback.get().sessions.flatMap { session ->
            session.connectedControllers.filterNot(session::isMediaNotificationController)
        }

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()
    }
}
