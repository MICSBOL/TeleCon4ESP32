package com.micsbol.telecon4esp32.ui.control_panel

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.atomic.AtomicInteger

/**
 * Control Panel and RC Vehicle Pro must never render in portrait.
 *
 * [ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE] is ignored on several OEMs
 * and, after an IME or dialog, can leave Samsung/Pixel in portrait when
 * auto-rotate is off. Fixed [ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE] always
 * rotates the HUD back to landscape.
 */
internal fun resolveHudLandscapeOrientation(
    @Suppress("UNUSED_PARAMETER") manufacturer: String,
    @Suppress("UNUSED_PARAMETER") brand: String = "",
    @Suppress("UNUSED_PARAMETER") currentlyPortrait: Boolean = false,
): Int = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

/**
 * Nested [HideHudSystemBars] callers (HUD → settings) must not restore bars
 * while another screen still wants them hidden.
 */
private val hudSystemBarsHideCount = AtomicInteger(0)

/**
 * Hides the status bar and navigation bar while a HUD is visible.
 * A swipe from the screen edge shows them briefly, then they hide again.
 */
@Composable
fun HideHudSystemBars() {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(activity, lifecycleOwner, view) {
        val window = activity?.window
        if (activity == null || window == null) {
            return@DisposableEffect onDispose {}
        }

        fun hideBars() {
            (activity as? ComponentActivity)?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            )
            WindowCompat.getInsetsController(window, view).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        hudSystemBarsHideCount.incrementAndGet()
        hideBars()
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hideBars()
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) hideBars()
        }
        val decorView = window.decorView
        if (decorView.viewTreeObserver.isAlive) {
            decorView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            if (decorView.viewTreeObserver.isAlive) {
                decorView.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            }
            if (hudSystemBarsHideCount.decrementAndGet() <= 0) {
                hudSystemBarsHideCount.set(0)
                WindowCompat.getInsetsController(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

/**
 * Landscape lock for Control Panel and RC Vehicle Pro HUDs.
 *
 * Plot / radar dialogs have text fields. The IME must not drop this lock:
 * disposing [LockScreenOrientation] restores the previous (portrait) activity
 * orientation, and several OEMs then stay portrait after the dialog closes.
 */
@Composable
fun LockHudLandscape() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val orientation = resolveHudLandscapeOrientation(
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        currentlyPortrait = configuration.orientation != Configuration.ORIENTATION_LANDSCAPE,
    )
    val orientationState = rememberUpdatedState(orientation)

    DisposableEffect(activity, lifecycleOwner) {
        if (activity == null) return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        val originalSoftInputMode = activity.window.attributes.softInputMode
        fun applyLock() {
            val wanted = orientationState.value
            if (activity.requestedOrientation != wanted) {
                activity.requestedOrientation = wanted
            }
        }
        applyLock()
        // IME resize can make height > width and report portrait. Keep the window size.
        activity.window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED,
        )

        val decorView = activity.window.decorView
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { _ ->
            applyLock()
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                applyLock()
            }
        }
        if (decorView.viewTreeObserver.isAlive) {
            decorView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            if (decorView.viewTreeObserver.isAlive) {
                decorView.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            }
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            activity.window.setSoftInputMode(originalSoftInputMode)
            activity.requestedOrientation = originalOrientation
        }
    }

    SideEffect {
        if (activity != null && activity.requestedOrientation != orientation) {
            activity.requestedOrientation = orientation
        }
    }
}

@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(activity, orientation, lifecycleOwner) {
        if (activity == null) return@DisposableEffect onDispose {}
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = orientation

        val decorView = activity.window.decorView
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus && activity.requestedOrientation != orientation) {
                activity.requestedOrientation = orientation
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                activity.requestedOrientation = orientation
            }
        }
        if (decorView.viewTreeObserver.isAlive) {
            decorView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            if (decorView.viewTreeObserver.isAlive) {
                decorView.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            }
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            // Only restore if we are still the ones holding this lock value.
            if (activity.requestedOrientation == orientation) {
                activity.requestedOrientation = originalOrientation
            }
        }
    }

    // Re-assert while visible — some OEMs reset orientation with configChanges.
    SideEffect {
        if (activity != null && activity.requestedOrientation != orientation) {
            activity.requestedOrientation = orientation
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
