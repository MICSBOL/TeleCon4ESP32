package com.micsbol.telecon4esp32.ui.control_panel.components


import android.content.Context
import android.os.Build
import android.os.SystemClock
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import kotlin.coroutines.coroutineContext
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

@Composable
fun Joystick_RC3D(
    modifier: Modifier = Modifier,
    mode: JoystickMode = JoystickMode.Spring(),
    stickPosition: Pair<Float, Float> = Pair(0f, 0f),
    settingsSyncGeneration: Int = 0,
    onMove: (x: Float, y: Float) -> Unit,
    onDoubleTap: (() -> Unit)? = null,
    contentDescription: String? = null,
) {
    val frames = remember {
        (1..169).map {
            val number = it.toString().padStart(4, '0')
            val resourceName = "joy_rc_$number"
            R.drawable::class.java.getField(resourceName).getInt(null)
        }
    }

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

    var frame by remember {
        mutableStateOf(normalizedStickToFrame(stickPosition.first, stickPosition.second))
    }
    var isInteracting by remember { mutableStateOf(false) }

    LaunchedEffect(settingsSyncGeneration) {
        if (!isInteracting) {
            frame = normalizedStickToFrame(stickPosition.first, stickPosition.second)
        }
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
    val currentMode by rememberUpdatedState(mode)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentInitialNormalized by rememberUpdatedState(initialPositionNormalized)
    val currentInitialFrame by rememberUpdatedState(initialFrame)

    fun playReleaseAnimation(targetFrame: Int, joystickMode: JoystickMode) {
        releaseAnimationJob?.cancel()
        val safeTarget = coerceJoystickRc3dFrame(targetFrame)
        releaseAnimationJob = coroutineScope.launch {
            val thisJob = coroutineContext.job
            try {
                isInteracting = true
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
                    is JoystickMode.Spring,
                    is JoystickMode.VerticalSpring,
                    is JoystickMode.HorizontalSpring -> {
                        val wiggleFrame = joystickRc3dWiggleFrame(joystickMode, safeTarget)
                        delay(50)
                        frame = wiggleFrame
                        delay(60)
                        frame = safeTarget
                        delay(40)
                        frame = wiggleFrame
                        delay(50)
                        frame = safeTarget
                    }
                    else -> {
                        frame = safeTarget
                    }
                }
            } finally {
                if (releaseAnimationJob === thisJob) {
                    isInteracting = false
                }
            }
        }
    }

    val safeFrame = coerceJoystickRc3dFrame(frame)
    Image(
        painter = painterResource(id = frames[safeFrame]),
        contentDescription = contentDescription ?: "RC Joystick",
        modifier = modifier
            .clip(CircleShape)
            .padding(10.dp)
            .onSizeChanged { size ->
                center = Offset(size.width / 2f, size.height / 2f)
                dragRadius =
                    kotlin.math.min(size.width.toFloat(), size.height.toFloat()) / 2f
            }
            // Unit key: never restart the handler (same as Knob3D). Restarting
            // cancels the drag loop and the spring-return wiggle.
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
                        val liveFrame = coerceJoystickRc3dFrame(frame)
                        val currentGridX = liveFrame % JOYSTICK_RC3D_GRID
                        val currentGridY = liveFrame / JOYSTICK_RC3D_GRID
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
                        val downPosition = down.position
                        var maxMove = 0f

                        down.consume()
                        releaseAnimationJob?.cancel()
                        isInteracting = true
                        val dragPointerId = down.id
                        val gestureMode = currentMode
                        val emitMove = currentOnMove

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val dragEvent = event.changes.firstOrNull { it.id == dragPointerId }

                                if (dragEvent == null || !dragEvent.pressed) {
                                    when (gestureMode) {
                                        is JoystickMode.Spring,
                                        is JoystickMode.VerticalSpring,
                                        is JoystickMode.HorizontalSpring -> {
                                            emitMove(
                                                currentInitialNormalized.first,
                                                currentInitialNormalized.second
                                            )
                                            playReleaseAnimation(currentInitialFrame, gestureMode)
                                        }
                                        else -> {
                                            val gridX = coerceJoystickRc3dFrame(frame) % JOYSTICK_RC3D_GRID
                                            val gridY = coerceJoystickRc3dFrame(frame) / JOYSTICK_RC3D_GRID
                                            val finalNormX = (gridX - 6) / 6f
                                            val finalNormY = -(gridY - 6) / 6f
                                            emitMove(finalNormX, finalNormY)
                                            isInteracting = false
                                        }
                                    }
                                    break
                                }

                                maxMove = maxOf(
                                    maxMove,
                                    (dragEvent.position - downPosition).getDistance(),
                                )
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

                                when (gestureMode) {
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
                                emitMove(finalNormalizedX, finalNormalizedY)
                                val gridX = (normalizedX * 12).roundToInt().coerceIn(0, 12)
                                val gridY = (normalizedY * 12).roundToInt().coerceIn(0, 12)
                                val finalFrameIndex = joystickRc3dFrameIndex(gridX, gridY)
                                if (frame != finalFrameIndex) {
                                    frame = finalFrameIndex
                                }
                            }
                        } finally {
                            if (releaseAnimationJob?.isActive != true) {
                                isInteracting = false
                            }
                            val upTime = SystemClock.uptimeMillis()
                            if (
                                doubleTapHandler != null &&
                                maxMove < slop &&
                                upTime - now <= doubleTapTimeout
                            ) {
                                lastTapUptime = upTime
                                lastTapPosition = downPosition
                            } else {
                                lastTapUptime = 0L
                            }
                        }
                    }
                }
            }
    )
}