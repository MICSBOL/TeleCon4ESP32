package com.micsbol.telecon4esp32.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/** Match slow turntable pace (~18s / revolution with 36 frames). */
private const val HOLO_FRAME_DELAY_MS = 500L
/** Decode at half resolution to keep RAM modest. */
private const val HOLO_IN_SAMPLE_SIZE = 2
private const val HOLO_DEFAULT_FRAME_COUNT = 36

/**
 * Asset folder under `assets/` for each module's holographic turntable WebP pack.
 * Returns null when no flipbook pack is shipped for that module.
 */
fun ApplicationId.holoTurntableAssetDir(): String? = when (this) {
    ApplicationId.CONTROL_PANEL -> "holo_control_panel"
    ApplicationId.RC_VEHICLE_PRO -> "holo_rc_buggy_360"
}

/**
 * Slow looping holographic turntable driven by WebP frames in assets.
 *
 * Prefer this over ExoPlayer: sparse H.264 loops often flash a black surface between
 * frames or at the loop seam, even when the MP4 itself has no black content.
 */
@Composable
fun HoloTurntableFlipbook(
    assetDir: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    frameCount: Int? = null,
    frameDelayMs: Long = HOLO_FRAME_DELAY_MS,
) {
    val context = LocalContext.current
    val resolvedFrameCount = remember(assetDir, frameCount) {
        frameCount ?: context.assets.list(assetDir)
            ?.count { it.endsWith(".webp", ignoreCase = true) }
            ?.takeIf { it > 0 }
            ?: HOLO_DEFAULT_FRAME_COUNT
    }
    var frames by remember(assetDir) { mutableStateOf<List<Bitmap>>(emptyList()) }
    LaunchedEffect(assetDir, resolvedFrameCount) {
        val decoded = withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
                inSampleSize = HOLO_IN_SAMPLE_SIZE
            }
            List(resolvedFrameCount) { index ->
                val path = "$assetDir/${index.toString().padStart(4, '0')}.webp"
                context.assets.open(path).use { stream ->
                    requireNotNull(BitmapFactory.decodeStream(stream, null, opts)) {
                        "Missing hologram frame $path"
                    }
                }
            }
        }
        if (!isActive) {
            decoded.forEach { it.recycle() }
            return@LaunchedEffect
        }
        frames = decoded
    }
    DisposableEffect(frames) {
        val held = frames
        onDispose { held.forEach { it.recycle() } }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var screenStarted by remember {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> screenStarted = true
                Lifecycle.Event.ON_STOP -> screenStarted = false
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    var frameIndex by remember(assetDir) { mutableIntStateOf(0) }
    LaunchedEffect(frames, frameDelayMs, screenStarted) {
        if (!screenStarted || frames.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(frameDelayMs)
            frameIndex = (frameIndex + 1) % frames.size
        }
    }

    if (frames.isNotEmpty()) {
        Image(
            bitmap = frames[frameIndex.coerceIn(0, frames.lastIndex)].asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}

@Composable
fun HoloRcTurntableVideo(
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    HoloTurntableFlipbook(
        assetDir = ApplicationId.RC_VEHICLE_PRO.holoTurntableAssetDir()!!,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}
