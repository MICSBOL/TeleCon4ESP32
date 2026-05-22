package com.micsbol.emitterapp.ui.rc_screen.components


import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.micsbol.emitterapp.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.micsbol.emitterapp.domain.model.JoystickMode
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A highly customizable and realistic 3D RC-style joystick component.
 *
 * This composable simulates a physical transmitter gimbal using a sequence of pre-rendered
 * PNG images. It supports various operational modes, initial positions, and provides
 * haptic and visual feedback for a rich user experience. The joystick's position is
 * determined by mapping the user's drag gesture within a defined square area to a
 * 13x13 grid, which corresponds to one of the 169 provided image frames.
 *
 * The component is designed to be highly reusable, with behavior defined by the [JoystickMode]
 * sealed class.
 *
 * ### Usage Example:
 * A standard, spring-loaded joystick that returns to the center
 * Joystick_RC3D(mode = JoystickMode.Spring(initialPosition = JoystickMode.CENTER))
 *
 * A non-centering throttle stick that holds its vertical position
 * Joystick_RC3D(mode = JoystickMode.VerticalHold(initialPosition = JoystickMode.DOWN))
 *
 * @param mode The operational mode of the joystick, defined by the [JoystickMode] sealed class.
 *             This parameter dictates the joystick's movement constraints (e.g., vertical-only,
 *             full range) and its release behavior (spring-to-center or hold-position).
 *             The data class associated with each mode also defines its initial position.
 *             Defaults to a standard spring-loaded joystick centered at (6, 6).
 */

private fun normalizedStickToFrame(x: Float, y: Float): Int {
    val gridX = ((x * 6f) + 6f).roundToInt().coerceIn(0, 12)
    val gridY = (((-y) * 6f) + 6f).roundToInt().coerceIn(0, 12)
    return gridY * 13 + gridX
}

@Composable
fun Joystick_RC3D(
    modifier: Modifier = Modifier,
    mode: JoystickMode = JoystickMode.Spring(),
    stickPosition: Pair<Float, Float> = Pair(0f, 0f),
    settingsSyncGeneration: Int = 0,
    onMove: (x: Float, y: Float) -> Unit
) {
    val frames = remember {
        (1..169).map {
            val number = it.toString().padStart(4, '0')
            val resourceName = "joy_rc_$number"
            R.drawable::class.java.getField(resourceName).getInt(null)
        }
    }

    val centerFrameIndex = 84

    val initialPositionNormalized = remember(mode) {
        val initialGridPos = when (mode) {
            is JoystickMode.Spring -> mode.initialPosition
            is JoystickMode.Hold -> mode.initialPosition
            is JoystickMode.VerticalSpring -> mode.initialPosition
            is JoystickMode.VerticalHold -> mode.initialPosition
            is JoystickMode.HorizontalSpring -> mode.initialPosition
            is JoystickMode.HorizontalHold -> mode.initialPosition
        }
        val (gridX, gridY) = initialGridPos
        val initialX = (gridX - 6) / 6f
        val initialY = -(gridY - 6) / 6f
        Pair(initialX, initialY)
    }

    val initialFrame = remember(mode) {

        val initialPosition = when (mode) {
            is JoystickMode.Spring -> mode.initialPosition
            is JoystickMode.Hold -> mode.initialPosition
            is JoystickMode.VerticalSpring -> mode.initialPosition
            is JoystickMode.VerticalHold -> mode.initialPosition
            is JoystickMode.HorizontalSpring -> mode.initialPosition
            is JoystickMode.HorizontalHold -> mode.initialPosition
        }

        val (gridX, gridY) = initialPosition
        val clampedX = gridX.coerceIn(0, 12)
        val clampedY = gridY.coerceIn(0, 12)
        clampedY * 13 + clampedX
    }

    var frame by remember(settingsSyncGeneration) {
        mutableStateOf(normalizedStickToFrame(stickPosition.first, stickPosition.second))
    }

    LaunchedEffect(settingsSyncGeneration) {
        frame = normalizedStickToFrame(stickPosition.first, stickPosition.second)
    }

    var center by remember { mutableStateOf(Offset.Zero) }
    var dragRadius by remember { mutableStateOf(0f) }

    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var releaseAnimationJob by remember { mutableStateOf<Job?>(null) }

    fun playReleaseAnimation(targetFrame: Int, joystickMode: JoystickMode) {

        releaseAnimationJob?.cancel()
        releaseAnimationJob = coroutineScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 50, 30, 40, 30, 20)
                val amplitudes = intArrayOf(0, 180, 0, 120, 0, 70)
                val vibrationEffect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
            when (joystickMode) {
                is JoystickMode.VerticalSpring -> {
                    val wiggleFrame = if (targetFrame > 13) {
                        targetFrame - 13
                    } else {
                        targetFrame + 13
                    }
                    delay(50)
                    frame = wiggleFrame
                    delay(60)
                    frame = targetFrame
                    delay(40)
                    frame = wiggleFrame
                    delay(50)
                    frame = targetFrame
                }

                is JoystickMode.HorizontalSpring -> {
                    val wiggleFrame = if ((targetFrame % 10) == 0) {
                        targetFrame - 1
                    } else {
                        targetFrame + 1
                    }
                    delay(50)
                    frame = wiggleFrame
                    delay(60)
                    frame = targetFrame
                    delay(40)
                    frame = wiggleFrame
                    delay(50)
                    frame = targetFrame
                }

                is JoystickMode.Spring -> {
                    val wiggleFrame = targetFrame - 14
                    delay(50)
                    frame = wiggleFrame
                    delay(60)
                    frame = targetFrame
                    delay(40)
                    frame = wiggleFrame
                    delay(50)
                    frame = targetFrame
                }

                else -> {}
            }
        }
    }

    Image(
        painter = painterResource(id = frames[frame]),
        contentDescription = "RC Joystick",
        modifier = modifier
            .clip(CircleShape)
            .padding(10.dp)
            .onSizeChanged { size ->
                center = Offset(size.width / 2f, size.height / 2f)
                dragRadius =
                    kotlin.math.min(size.width.toFloat(), size.height.toFloat()) / 2f
            }
            .pointerInput(mode) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val currentGridX = frame % 13
                        val currentGridY = frame / 13
                        val normalizedX_minus1_to_1 = (currentGridX - 6) / 6f
                        val normalizedY_minus1_to_1 = -(currentGridY - 6) / 6f
                        val stickBaseX = normalizedX_minus1_to_1 * dragRadius
                        val stickBaseY = -normalizedY_minus1_to_1 * dragRadius
                        val stickBasePosition = Offset(stickBaseX, stickBaseY) + center

                        val touchRadius = size.width * 0.35f
                        val distanceToBase = sqrt(
                            (down.position.x - stickBasePosition.x).pow(2) + (down.position.y - stickBasePosition.y).pow(2)
                        )

                        if (distanceToBase > touchRadius) {
                            continue
                        }

                        val touchOffsetFromStick = down.position - stickBasePosition

                        down.consume()
                        releaseAnimationJob?.cancel()
                        val dragPointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val dragEvent = event.changes.firstOrNull { it.id == dragPointerId }

                            if (dragEvent == null || !dragEvent.pressed) {
                                when (mode) {
                                    is JoystickMode.Spring,
                                    is JoystickMode.VerticalSpring,
                                    is JoystickMode.HorizontalSpring -> {
                                        onMove(
                                            initialPositionNormalized.first,
                                            initialPositionNormalized.second
                                        )
                                        playReleaseAnimation(initialFrame, mode)
                                    }
                                    else -> {
                                        val gridX = frame % 13
                                        val gridY = frame / 13
                                        val finalNormX = (gridX - 6) / 6f
                                        val finalNormY = -(gridY - 6) / 6f
                                        onMove(finalNormX, finalNormY)
                                    }
                                }
                                break
                            }

                            var dragVector = (dragEvent.position - touchOffsetFromStick) - center
                            val dragDistance = sqrt(dragVector.x.pow(2) + dragVector.y.pow(2))
                            var clampedVector = dragVector

                            if (dragDistance > dragRadius) {
                                val angle = atan2(dragVector.y, dragVector.x)
                                clampedVector = Offset(
                                    dragRadius * cos(angle),
                                    dragRadius * sin(angle)
                                )
                            }

                            when (mode) {
                                is JoystickMode.VerticalSpring,
                                is JoystickMode.VerticalHold -> {
                                    clampedVector = Offset(0f, clampedVector.y)
                                }
                                is JoystickMode.HorizontalSpring,
                                is JoystickMode.HorizontalHold -> {
                                    clampedVector = Offset(clampedVector.x, 0f)
                                }
                                else -> {}
                            }

                            val normalizedX = (clampedVector.x + dragRadius) / (dragRadius * 2f)
                            val normalizedY = (clampedVector.y + dragRadius) / (dragRadius * 2f)
                            val finalNormalizedX = normalizedX * 2f - 1f
                            val finalNormalizedY = -(normalizedY * 2f - 1f)
                            onMove(finalNormalizedX, finalNormalizedY)
                            val gridX = (normalizedX * 12).roundToInt().coerceIn(0, 12)
                            val gridY = (normalizedY * 12).roundToInt().coerceIn(0, 12)
                            val finalFrameIndex = gridY * 13 + gridX
                            if (frame != finalFrameIndex) {
                                frame = finalFrameIndex
                            }
                        }
                    }
                }
            }
    )
}