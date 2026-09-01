package com.micsbol.telecon4esp32.ui.control_panel

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Control Panel and RC Vehicle Pro must never render in portrait.
 *
 * [ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE] is ignored on several OEMs
 * (Huawei/EMUI, Honor, some Xiaomi builds) when auto-rotate is off or the phone
 * is held portrait — the HUD then lays out as a narrow portrait column.
 * Fixed [ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE] forces the window to rotate.
 */
internal fun resolveHudLandscapeOrientation(
    manufacturer: String,
    brand: String = "",
    currentlyPortrait: Boolean = false,
): Int {
    if (currentlyPortrait) return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    val token = "$manufacturer $brand".lowercase()
    val oemIgnoresSensorLandscape = OEM_SENSOR_LANDSCAPE_UNRELIABLE.any { token.contains(it) }
    return if (oemIgnoresSensorLandscape) {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    } else {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
}

private val OEM_SENSOR_LANDSCAPE_UNRELIABLE = listOf(
    "huawei",
    "honor",
    "xiaomi",
    "redmi",
    "poco",
    "oppo",
    "vivo",
    "realme",
    "oneplus",
    "tecno",
    "infinix",
)

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
            WindowCompat.getInsetsController(window, view)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

/** Landscape lock for Control Panel and RC Vehicle Pro HUDs. */
@Composable
fun LockHudLandscape() {
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    if (imeBottom > 0) {
        // SwiftKey/Gboard on Huawei draws sideways if the window stays landscape
        // while the phone is held portrait. Release the lock until the IME hides.
        return
    }
    val configuration = LocalConfiguration.current
    var forceFixedLandscape by remember {
        mutableStateOf(
            resolveHudLandscapeOrientation(
                manufacturer = Build.MANUFACTURER,
                brand = Build.BRAND,
                currentlyPortrait = false,
            ) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        )
    }
    LaunchedEffect(configuration.orientation) {
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            forceFixedLandscape = true
        }
    }
    val orientation = if (forceFixedLandscape) {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    } else {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    LockScreenOrientation(orientation)
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
