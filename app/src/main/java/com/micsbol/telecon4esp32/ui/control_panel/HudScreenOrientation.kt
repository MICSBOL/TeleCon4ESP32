package com.micsbol.telecon4esp32.ui.control_panel

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
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

internal fun isHudSystemBarsHidden(): Boolean = hudSystemBarsHideCount.get() > 0

/**
 * Landscape HUDs that must draw into the cutout. Nested callers (RC Vehicle
 * locks orientation and also hides bars) share one restore.
 */
private val hudCutoutFillCount = AtomicInteger(0)
private var savedCutoutMode: Int = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
private var contentFillWatcher: View.OnLayoutChangeListener? = null

/** MIUI/HyperOS extra flags so the window may use the notch / status-bar side. */
private const val XIAOMI_NOTCH_EXTRA_FLAGS = 0x00000100 or 0x00000200 or 0x00000400

@Suppress("DEPRECATION")
private const val HUD_LAYOUT_FULLSCREEN_FLAGS =
    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

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
            activity.applyHudCutoutFill()
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            (activity as? ComponentActivity)?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            )
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        hudSystemBarsHideCount.incrementAndGet()
        activity.acquireHudCutoutFill()
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
            activity.releaseHudCutoutFill()
            if (hudSystemBarsHideCount.decrementAndGet() <= 0) {
                hudSystemBarsHideCount.set(0)
                window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                restoreVisibleSystemBars(activity, view)
            }
        }
    }

    SideEffect {
        if (activity != null && hudSystemBarsHideCount.get() > 0) {
            activity.applyHudCutoutFill()
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            activity.window.statusBarColor = Color.TRANSPARENT
            activity.window.navigationBarColor = Color.TRANSPARENT
            WindowCompat.getInsetsController(activity.window, view).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}

/**
 * Shows status and navigation bars in the default (non-overlay) mode so
 * portrait screens can pad below them after leaving an immersive HUD.
 */
@Composable
fun EnsureVisibleSystemBars() {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(activity, view, lifecycleOwner) {
        if (activity == null) return@DisposableEffect onDispose {}
        fun restore() {
            if (hudSystemBarsHideCount.get() > 0) return
            restoreVisibleSystemBars(activity, view)
        }
        restore()
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                restore()
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    SideEffect {
        if (activity != null && hudSystemBarsHideCount.get() <= 0) {
            restoreVisibleSystemBars(activity, view)
        }
    }
}

private fun restoreVisibleSystemBars(activity: Activity, view: View) {
    val window = activity.window
    (activity as? ComponentActivity)?.enableEdgeToEdge()
    WindowCompat.getInsetsController(window, view).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        show(WindowInsetsCompat.Type.systemBars())
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
        activity.acquireHudCutoutFill()
        // IME resize can make height > width and report portrait. Keep the window size.
        activity.window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED,
        )

        val decorView = activity.window.decorView
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { _ ->
            applyLock()
            activity.applyHudCutoutFill()
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                applyLock()
                activity.applyHudCutoutFill()
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
            activity.releaseHudCutoutFill()
            activity.window.setSoftInputMode(originalSoftInputMode)
            activity.requestedOrientation = originalOrientation
        }
    }

    SideEffect {
        if (activity != null && activity.requestedOrientation != orientation) {
            activity.requestedOrientation = orientation
        }
        if (hudCutoutFillCount.get() > 0) {
            activity?.applyHudCutoutFill()
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

/**
 * Lets a landscape HUD use the whole display, including the column where the
 * portrait status bar sits on Xiaomi/Poco and other notched or curved phones.
 */
private fun Activity.acquireHudCutoutFill() {
    if (hudCutoutFillCount.getAndIncrement() == 0 &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    ) {
        savedCutoutMode = window.attributes.layoutInDisplayCutoutMode
    }
    applyHudCutoutFill()
}

private fun Activity.releaseHudCutoutFill() {
    if (hudCutoutFillCount.decrementAndGet() > 0) return
    hudCutoutFillCount.set(0)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val params = window.attributes
        params.layoutInDisplayCutoutMode = savedCutoutMode
        window.attributes = params
    }
    window.clearXiaomiNotchFlags()
    @Suppress("DEPRECATION")
    window.decorView.systemUiVisibility =
        window.decorView.systemUiVisibility and HUD_LAYOUT_FULLSCREEN_FLAGS.inv()
    stopContentFill(window)
}

private fun Activity.applyHudCutoutFill() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        val params = window.attributes
        if (params.layoutInDisplayCutoutMode != mode) {
            params.layoutInDisplayCutoutMode = mode
            window.attributes = params
        }
    }
    window.addXiaomiNotchFlags()
    @Suppress("DEPRECATION")
    window.decorView.systemUiVisibility =
        window.decorView.systemUiVisibility or HUD_LAYOUT_FULLSCREEN_FLAGS
    ensureContentFillsDisplay(window)
}

private fun ensureContentFillsDisplay(window: Window) {
    val content = window.findViewById<View>(android.R.id.content) ?: return
    window.decorView.setPadding(0, 0, 0, 0)
    content.setPadding(0, 0, 0, 0)
    if (contentFillWatcher != null) return
    val watcher = View.OnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
        if (
            v.paddingLeft != 0 || v.paddingTop != 0 ||
            v.paddingRight != 0 || v.paddingBottom != 0
        ) {
            v.setPadding(0, 0, 0, 0)
        }
    }
    contentFillWatcher = watcher
    content.addOnLayoutChangeListener(watcher)
}

private fun stopContentFill(window: Window) {
    val content = window.findViewById<View>(android.R.id.content)
    contentFillWatcher?.let { watcher ->
        content?.removeOnLayoutChangeListener(watcher)
    }
    contentFillWatcher = null
}

/** No-op on stock Android. MIUI/HyperOS uses these flags to release the notch. */
private fun Window.addXiaomiNotchFlags() {
    try {
        val method = javaClass.getMethod("addExtraFlags", Int::class.javaPrimitiveType)
        method.invoke(this, XIAOMI_NOTCH_EXTRA_FLAGS)
    } catch (_: Exception) {
    }
}

private fun Window.clearXiaomiNotchFlags() {
    try {
        val method = javaClass.getMethod("clearExtraFlags", Int::class.javaPrimitiveType)
        method.invoke(this, XIAOMI_NOTCH_EXTRA_FLAGS)
    } catch (_: Exception) {
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
