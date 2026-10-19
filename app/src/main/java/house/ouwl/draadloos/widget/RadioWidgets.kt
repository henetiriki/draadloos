package house.ouwl.draadloos.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.currentState
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import house.ouwl.draadloos.MainActivity
import house.ouwl.draadloos.R
import house.ouwl.draadloos.playback.PlaybackService
import house.ouwl.draadloos.ui.Accent
import house.ouwl.draadloos.ui.Background
import house.ouwl.draadloos.ui.Live
import house.ouwl.draadloos.ui.OnBackground
import house.ouwl.draadloos.ui.RadioState
import house.ouwl.draadloos.ui.StopButton
import house.ouwl.draadloos.ui.TextMuted

/**
 * Marks the few lines that hand a widget's state and content to Glance's own runtime. That
 * runtime only works inside a session, which a Robolectric test can't host (a second update
 * deadlocks the test, and the app widget shadow fires fake broadcasts Glance can't finish).
 * Left out of the coverage report. The logic around them is tested: what a widget draws is its
 * `content` (composed in the tests with preferences handed in), and how state becomes
 * preferences and back is [WidgetPrefs].
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
internal annotation class SessionOnly

/** The wide widget: station art, name and what is on, and the toggle. Layout per docs/DESIGN.md. */
class RadioWideWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    internal val content: @Composable () -> Unit = { WideWidget(WidgetPrefs.read(currentState())) }

    @SessionOnly
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        updateAppWidgetState(context, id) { WidgetPrefs.write(it, RadioWidgetState.current) }
        provideContent(content)
    }
}

/** The 1x1 widget: just the toggle and the station name. */
class RadioSmallWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    internal val content: @Composable () -> Unit = { SmallWidget(WidgetPrefs.read(currentState())) }

    @SessionOnly
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        updateAppWidgetState(context, id) { WidgetPrefs.write(it, RadioWidgetState.current) }
        provideContent(content)
    }
}

class RadioWideWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RadioWideWidget()
}

class RadioSmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RadioSmallWidget()
}

/**
 * Sizes that follow the cell height Android gives the widget. The toggle never goes under
 * 48dp, and the art shrinks together with it.
 */
internal object WidgetMetrics {
    private val MIN_TOGGLE = 48.dp
    private val MAX_TOGGLE = 64.dp

    fun wideToggle(height: Dp): Dp = (height - 24.dp).coerceIn(MIN_TOGGLE, MAX_TOGGLE)

    fun art(toggle: Dp): Dp = (toggle - 8.dp).coerceAtMost(56.dp)

    // The small widget also holds the "RSG" label under the toggle.
    fun smallToggle(height: Dp): Dp = (height - 36.dp).coerceIn(MIN_TOGGLE, MAX_TOGGLE)
}

@Composable
internal fun WideWidget(state: WidgetState) {
    val toggle = WidgetMetrics.wideToggle(LocalSize.current.height)
    val playing = state.phase == RadioState.Playing
    Frame(playing) {
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.radio),
                contentDescription = null,
                modifier = GlanceModifier.size(WidgetMetrics.art(toggle)).cornerRadius(14.dp),
            )
            Spacer(GlanceModifier.width(14.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (playing) LiveDot(10.dp)
                    Text(
                        text = LocalContext.current.getString(R.string.station_name),
                        style = TextStyle(
                            color = ColorProvider(OnBackground),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                }
                Subtitle(state)
            }
            Spacer(GlanceModifier.width(12.dp))
            Toggle(state.on, toggle)
        }
    }
}

@Composable
internal fun SmallWidget(state: WidgetState) {
    val toggle = WidgetMetrics.smallToggle(LocalSize.current.height)
    Frame(state.phase == RadioState.Playing) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Toggle(state.on, toggle)
            Spacer(GlanceModifier.size(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.phase == RadioState.Playing) LiveDot(8.dp)
                Text(
                    text = LocalContext.current.getString(R.string.station_name),
                    style = TextStyle(
                        color = ColorProvider(OnBackground),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Black, rounded, with a 3dp yellow rim while playing. Tapping anywhere but the toggle opens the app. */
@Composable
@NonRestartableComposable
private fun Frame(playing: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(if (playing) Accent else Background)
            .cornerRadius(28.dp)
            .appWidgetBackground()
            .padding(if (playing) 3.dp else 0.dp)
            .clickable(actionStartActivity(Intent(LocalContext.current, MainActivity::class.java))),
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Background)
                .cornerRadius(if (playing) 25.dp else 28.dp),
        ) {
            content()
        }
    }
}

@Composable
@NonRestartableComposable
private fun LiveDot(size: Dp) {
    Box(GlanceModifier.size(size).cornerRadius(size / 2).background(Live)) {}
    Spacer(GlanceModifier.width(size * 0.8f))
}

@Composable
private fun Subtitle(state: WidgetState) {
    val context = LocalContext.current
    val song = state.song
    val (text, color) = when {
        state.phase == RadioState.Stopped -> context.getString(R.string.status_stopped) to TextMuted
        state.phase == RadioState.Connecting -> context.getString(R.string.status_connecting) to TextMuted
        song != null -> song.line to Accent
        else -> context.getString(R.string.status_live) to Accent
    }
    Text(text = text, style = TextStyle(color = ColorProvider(color), fontSize = 17.sp), maxLines = 1)
}

/** ▶ when the radio is off, ■ when it is on or connecting, so the user can always stop. */
@Composable
private fun Toggle(on: Boolean, size: Dp) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .size(size)
            .cornerRadius(size / 2)
            .background(if (on) StopButton else Accent)
            .clickable(toggleAction(context, on))
            .semantics {
                contentDescription = context.getString(
                    if (on) R.string.stop_description else R.string.play_description
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(if (on) R.drawable.ic_widget_stop else R.drawable.ic_widget_play),
            contentDescription = null,
            modifier = GlanceModifier.size(size * 0.45f),
        )
    }
}

/**
 * Android 12 and newer only let a foreground service start from a widget tap that is starting
 * playback. Stopping goes to the service that is already running, with a plain start.
 */
internal fun toggleAction(context: Context, on: Boolean): Action {
    val click = toggleClick(on)
    val intent = Intent(context, PlaybackService::class.java).setAction(click.action)
    return actionStartService(intent, isForegroundService = click.foreground)
}

/** What a tap on the toggle sends to the service, and whether it starts it as a foreground service. */
internal data class ToggleClick(val action: String, val foreground: Boolean)

internal fun toggleClick(on: Boolean): ToggleClick =
    if (on) ToggleClick(PlaybackService.ACTION_STOP, foreground = false)
    else ToggleClick(PlaybackService.ACTION_PLAY, foreground = true)
