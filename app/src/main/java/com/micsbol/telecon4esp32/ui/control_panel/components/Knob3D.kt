package com.micsbol.telecon4esp32.ui.control_panel.components

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun Knob3D(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    onDoubleTap: (() -> Unit)? = null,
    contentDescription: String? = null,
) {
    // Remember the list so it is not allocated on every recomposition.
    val frames = remember {
        listOf(
            R.drawable.knob_01, R.drawable.knob_02, R.drawable.knob_03,
            R.drawable.knob_04, R.drawable.knob_05, R.drawable.knob_06,
            R.drawable.knob_07, R.drawable.knob_08, R.drawable.knob_09,
            R.drawable.knob_10, R.drawable.knob_11, R.drawable.knob_12,
            R.drawable.knob_13, R.drawable.knob_14, R.drawable.knob_15,
            R.drawable.knob_16, R.drawable.knob_17, R.drawable.knob_18,
            R.drawable.knob_19,
        )
    }

    val minAngle = -135f
    val maxAngle = 135f

    var rotationAngle by remember { mutableStateOf(minAngle + (value * (maxAngle - minAngle))) }
    var dragStartAngle by remember { mutableStateOf(0f) }
    var center by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    // Sync external value only when the user is not dragging (e.g. settings apply).
    LaunchedEffect(value) {
        if (!isDragging) {
            rotationAngle = minAngle + (value * (maxAngle - minAngle))
        }
    }

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)

    val frame = (value * (frames.size - 1)).roundToInt().coerceIn(0, frames.size - 1)

    Image(
        painter = painterResource(id = frames[frame]),
        contentDescription = contentDescription,
        modifier = modifier
            // fillMaxSize() lets the parent Box (size = knobSize) control the actual size.
            // The previous hardcoded size(200.dp) was overriding the parent constraint.
            .fillMaxSize()
            .onSizeChanged { newSize ->
                center = Offset(newSize.width / 2f, newSize.height / 2f)
            }
            .pointerInput(Unit) {
                var lastTapUptime = 0L
                var lastTapPosition = Offset.Zero
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val now = SystemClock.uptimeMillis()
                        val slop = viewConfiguration.touchSlop
                        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
                        val doubleTapMin = viewConfiguration.doubleTapMinTimeMillis
                        val doubleTapHandler = currentOnDoubleTap
                        val isDoubleTap = doubleTapHandler != null &&
                            now - lastTapUptime in doubleTapMin..doubleTapTimeout &&
                            (down.position - lastTapPosition).getDistance() <= slop * 2f
                        if (isDoubleTap) {
                            doubleTapHandler.invoke()
                            lastTapUptime = 0L
                            down.consume()
                            val tapPointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == tapPointerId }
                                    ?: break
                                change.consume()
                                if (!change.pressed) break
                            }
                            continue
                        }
                        lastTapUptime = now
                        lastTapPosition = down.position
                        isDragging = true
                        val startVector = down.position - center
                        dragStartAngle =
                            atan2(startVector.y, startVector.x) * 180 / Math.PI.toFloat()
                        down.consume()
                        val pointerId = down.id
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                    ?: break
                                change.consume()
                                if (!change.pressed) break
                                val dragVector = change.position - center
                                val currentDragAngle =
                                    atan2(dragVector.y, dragVector.x) * 180 / Math.PI.toFloat()
                                var angleDelta = currentDragAngle - dragStartAngle
                                if (angleDelta > 180) {
                                    angleDelta -= 360
                                } else if (angleDelta < -180) {
                                    angleDelta += 360
                                }
                                rotationAngle =
                                    (rotationAngle + angleDelta).coerceIn(minAngle, maxAngle)
                                val normalizedValue =
                                    (rotationAngle - minAngle) / (maxAngle - minAngle)
                                currentOnValueChange(normalizedValue)
                                dragStartAngle = currentDragAngle
                            }
                        } finally {
                            isDragging = false
                        }
                    }
                }
            },
    )
}

@Preview(showBackground = true)
@Composable
private fun Knob3DPreview() {
    var previewValue by remember { mutableStateOf(0.5f) }
    Box(
        modifier = Modifier.size(250.dp),
        contentAlignment = Alignment.Center
    ) {
        Knob3D(
            modifier = Modifier.size(200.dp),
            value = previewValue,
            onValueChange = { newValue ->
                previewValue = newValue
            }
        )
    }
}
