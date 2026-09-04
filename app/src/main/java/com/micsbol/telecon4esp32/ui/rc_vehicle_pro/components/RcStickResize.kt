package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Hold anywhere on the stick card for [RcVehicleProLayout.STICK_RESIZE_HOLD_MS] to enter
 * resize immediately (no extra drag needed), then drag left to grow or right to shrink.
 * The white thumb is handled by the joystick and is not consumed here.
 */
internal fun Modifier.stickGroupResizeGesture(
    enabled: Boolean,
    resizeMode: Boolean,
    scale: Float,
    onResizeModeChange: (Boolean) -> Unit,
    onScaleChange: (Float) -> Unit,
    maxScale: Float = RcVehicleProLayout.STICK_GROUP_SCALE_MAX,
): Modifier = composed {
    val resizeModeState = rememberUpdatedState(resizeMode)
    val scaleState = rememberUpdatedState(scale)
    val onModeState = rememberUpdatedState(onResizeModeChange)
    val onScaleState = rememberUpdatedState(onScaleChange)
    val maxScaleState = rememberUpdatedState(maxScale)
    pointerInput(enabled) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = true)
            val slop = viewConfiguration.touchSlop
            val startX = down.position.x
            val startScale = scaleState.value
            val containerWidth = size.width.toFloat()
            val startedArmed = resizeModeState.value
            var armed = startedArmed
            var dragged = false
            var maxMove = 0f
            var stillPressed = true
            val pointerId = down.id

            if (!armed) {
                val timedOut = withTimeoutOrNull(RcVehicleProLayout.STICK_RESIZE_HOLD_MS) {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: run {
                            stillPressed = false
                            return@withTimeoutOrNull Unit
                        }
                        maxMove = maxOf(
                            maxMove,
                            (change.position - down.position).getDistance(),
                        )
                        if (!change.pressed) {
                            stillPressed = false
                            return@withTimeoutOrNull Unit
                        }
                        if (maxMove >= slop) return@withTimeoutOrNull Unit
                    }
                }
                if (timedOut == null && stillPressed && maxMove < slop) {
                    armed = true
                    onModeState.value(true)
                } else {
                    return@awaitEachGesture
                }
            }

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                maxMove = maxOf(maxMove, (change.position - down.position).getDistance())
                if (change.pressed) {
                    change.consume()
                    if (maxMove >= slop) {
                        dragged = true
                        onScaleState.value(
                            RcVehicleProLayout.stickGroupScaleFromDrag(
                                startScale = startScale,
                                startXPx = startX,
                                currentXPx = change.position.x,
                                containerWidthPx = containerWidth,
                                maxScale = maxScaleState.value,
                            ),
                        )
                    }
                } else {
                    if (startedArmed && !dragged && maxMove < slop) {
                        onModeState.value(false)
                    }
                    break
                }
            }
        }
    }
}
