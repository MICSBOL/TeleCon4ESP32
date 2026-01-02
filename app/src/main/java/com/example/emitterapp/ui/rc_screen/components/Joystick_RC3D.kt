package com.example.emitterapp.ui.rc_screen.components


import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.emitterapp.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
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
@Composable
fun Joystick_RC3D(
    mode: JoystickMode = JoystickMode.Spring()
) {
    val frames = remember {
        (1..169).map {
            val number = it.toString().padStart(4, '0')
            val resourceName = "joy_rc_$number"
            Log.d("Joystick_RC3D", "resourceName: $resourceName")
            R.drawable::class.java.getField(resourceName).getInt(null)
        }
    }


    val centerFrameIndex = 84

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

    var frame by remember { mutableStateOf(initialFrame) }

    var center by remember { mutableStateOf(Offset.Zero) }
    var dragSquareSize by remember { mutableStateOf(0f) }

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

    fun onRelease(targetFrame: Int, joystickMode: JoystickMode) {
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
            when (mode) {
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
                    val wiggleFrame = centerFrameIndex - 14
                    delay(50)
                    frame = wiggleFrame
                    delay(60)
                    frame = centerFrameIndex
                    delay(40)
                    frame = wiggleFrame
                    delay(50)
                    frame = centerFrameIndex
                }

                else -> {}
            }
        }
    }

    var lastDragVector by remember { mutableStateOf(Offset.Zero) }

    Image(
        painter = painterResource(id = frames[frame]),
        contentDescription = "RC Joystick",
        modifier = Modifier
            .size(300.dp)
            .onSizeChanged { size ->
                center = Offset(size.width / 2f, size.height / 2f)
                dragSquareSize = size.width * 0.8f
            }
            .pointerInput(mode) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        down.consume()
                        releaseAnimationJob?.cancel()
                        val dragPointerId = down.id

                        lastDragVector = Offset.Zero

                        while (true) {
                            val event = awaitPointerEvent()
                            val dragEvent = event.changes.firstOrNull { it.id == dragPointerId }

                            if (dragEvent == null || !dragEvent.pressed) {
                                when (mode) {
                                    is JoystickMode.Spring,
                                    is JoystickMode.VerticalSpring,
                                    is JoystickMode.HorizontalSpring -> {
                                        Log.d("Joystick_RC3D", "Initial frame: $initialFrame")
                                        onRelease(initialFrame, mode)
                                    }

                                    else -> {}
                                }
                                break
                            }

                            var dragVector = dragEvent.position - center

                            when (mode) {
                                is JoystickMode.VerticalSpring,
                                is JoystickMode.VerticalHold -> {
                                    dragVector = Offset(0f, dragVector.y)
                                }

                                is JoystickMode.HorizontalSpring,
                                is JoystickMode.HorizontalHold -> {
                                    dragVector =
                                        Offset(dragVector.x, 0f)
                                }

                                else -> {}
                            }
                            lastDragVector = dragVector

//                            if (dragVector.x.absoluteValue < 15f && dragVector.y.absoluteValue < 15f) {
//                                if (frame != centerFrameIndex) {
//                                    frame = centerFrameIndex
//                                }
//                                continue
//                            }

                            val halfSquare = dragSquareSize / 2f
                            val clampedX = dragVector.x.coerceIn(-halfSquare, halfSquare)
                            val clampedY = dragVector.y.coerceIn(-halfSquare, halfSquare)

                            val normalizedX = (clampedX + halfSquare) / dragSquareSize
                            val normalizedY = (clampedY + halfSquare) / dragSquareSize

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