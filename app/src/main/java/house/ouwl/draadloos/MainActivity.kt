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
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import house.ouwl.draadloos.playback.PlaybackService
import house.ouwl.draadloos.ui.RadioScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var controller by mutableStateOf<MediaController?>(null)
    private var connectJob: Job? = null

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askForNotificationPermission()
        setContent {
            RadioScreen(
                controller = controller,
                onToggle = { controller?.toggle() },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        connectJob = lifecycleScope.launch {
            controller = MediaController.Builder(this@MainActivity, token).buildAsync().await()
        }
    }

    override fun onStop() {
        connectJob?.cancel()
        controller?.release()
        controller = null
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

private fun Player.toggle() {
    if (playWhenReady) {
        pause()
    } else {
        if (playbackState == Player.STATE_IDLE) prepare()
        play()
    }
}
