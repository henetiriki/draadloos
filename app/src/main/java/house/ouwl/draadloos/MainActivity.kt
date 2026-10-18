package house.ouwl.draadloos

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import house.ouwl.draadloos.playback.PlaybackService
import house.ouwl.draadloos.playback.retryAt
import house.ouwl.draadloos.playback.toggle
import house.ouwl.draadloos.ui.RadioScreen

class MainActivity : ComponentActivity() {

    private var controller by mutableStateOf<MediaController?>(null)
    private var retryAt by mutableStateOf<Long?>(null)
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askForNotificationPermission()
        setContent {
            RadioScreen(
                controller = controller,
                retryAt = retryAt,
                onToggle = { controller?.toggle() },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token)
            .setListener(object : MediaController.Listener {
                // The service sets these while it waits to reconnect.
                override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
                    retryAt = extras.retryAt()
                }
            })
            .buildAsync()
        controllerFuture = future
        future.addListener(
            {
                if (controllerFuture === future) {
                    controller = runCatching { future.get() }.getOrNull()
                    retryAt = controller?.let { it.sessionExtras.retryAt() }
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    override fun onStop() {
        // Releases the controller, or cancels the connection if it hasn't finished yet.
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        controller = null
        retryAt = null
        super.onStop()
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
