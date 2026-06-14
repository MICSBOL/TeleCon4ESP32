package com.micsbol.telecon4esp32.ui.control_panel.components

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun Switch3DButton(
    modifier: Modifier = Modifier,
    isOn: Boolean,
    onStateChange: (Boolean) -> Unit
) {
    // Remember the list so it is not allocated on every recomposition.
    val frames = remember {
        listOf(
            R.drawable.switch__01, R.drawable.switch__02, R.drawable.switch__03,
            R.drawable.switch__04, R.drawable.switch__05, R.drawable.switch__06,
            R.drawable.switch__07, R.drawable.switch__08, R.drawable.switch__09,
            R.drawable.switch__10,
        )
    }

    var frame by remember { mutableStateOf(if (isOn) 0 else frames.lastIndex) }
    var isBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    val mediaPlayer = remember {
        if (isInPreview) null else MediaPlayer.create(context, R.raw.click_sound)
    }

    DisposableEffect(Unit) {
        onDispose { mediaPlayer?.release() }
    }

    suspend fun animateToTarget(targetOn: Boolean) {
        if (isBusy) return
        isBusy = true
        val targetFrame = if (targetOn) 0 else frames.lastIndex
        val startFrame = frame
        if (startFrame != targetFrame) {
            val frameMs = 5L
            val totalMs = (kotlin.math.abs(targetFrame - startFrame) * frameMs).coerceAtLeast(frameMs)
            val startTime = withFrameMillis { it }
            while (true) {
                val elapsed = withFrameMillis { it } - startTime
                if (elapsed >= totalMs) break
                val t = (elapsed.toFloat() / totalMs).coerceIn(0f, 1f)
                frame = (startFrame + (targetFrame - startFrame) * t).roundToInt()
            }
            frame = targetFrame
        }
        isBusy = false
    }

    // External sync (e.g. settings apply) — skip while a local gesture animation runs.
    LaunchedEffect(isOn) {
        if (!isBusy) {
            val targetFrame = if (isOn) 0 else frames.lastIndex
            if (frame != targetFrame) animateToTarget(isOn)
        }
    }

    val currentIsBusy by rememberUpdatedState(isBusy)
    val currentOnStateChange by rememberUpdatedState(onStateChange)

    Image(
        painter = painterResource(id = frames[frame]),
        contentDescription = if (isOn) "On" else "Off",
        modifier = modifier
            .size(70.dp)
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    if (currentIsBusy) return@detectDragGestures
                    val verticalDrag = dragAmount.y
                    val currentlyOn = frame == 0
                    if (verticalDrag > 3 && currentlyOn) {
                        mediaPlayer?.safeStart()
                        currentOnStateChange(false)
                        scope.launch { animateToTarget(false) }
                    } else if (verticalDrag < -3 && !currentlyOn) {
                        mediaPlayer?.safeStart()
                        currentOnStateChange(true)
                        scope.launch { animateToTarget(true) }
                    }
                }
            }
    )
}

private fun MediaPlayer.safeStart(){
    if(this.isPlaying){
        this.stop()
        this.prepare()
    }
    this.start()
}