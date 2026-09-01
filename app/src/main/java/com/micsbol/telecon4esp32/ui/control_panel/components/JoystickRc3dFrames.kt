package com.micsbol.telecon4esp32.ui.control_panel.components

import com.micsbol.telecon4esp32.domain.model.JoystickMode
import kotlin.math.roundToInt

internal const val JOYSTICK_RC3D_GRID = 13
internal const val JOYSTICK_RC3D_FRAME_COUNT = JOYSTICK_RC3D_GRID * JOYSTICK_RC3D_GRID

internal fun coerceJoystickRc3dFrame(frame: Int): Int =
    frame.coerceIn(0, JOYSTICK_RC3D_FRAME_COUNT - 1)

internal fun joystickRc3dFrameIndex(gridX: Int, gridY: Int): Int =
    gridY.coerceIn(0, JOYSTICK_RC3D_GRID - 1) * JOYSTICK_RC3D_GRID +
        gridX.coerceIn(0, JOYSTICK_RC3D_GRID - 1)

internal fun normalizedStickToFrame(x: Float, y: Float): Int {
    val gridX = ((x * 6f) + 6f).roundToInt()
    val gridY = (((-y) * 6f) + 6f).roundToInt()
    return joystickRc3dFrameIndex(gridX, gridY)
}

/**
 * Neighbor frame used for the spring-return wiggle. Always a valid 13×13 index
 * so the PNG lookup cannot crash composition and freeze the gimbal.
 */
internal fun joystickRc3dWiggleFrame(mode: JoystickMode, targetFrame: Int): Int {
    val target = coerceJoystickRc3dFrame(targetFrame)
    val gridX = target % JOYSTICK_RC3D_GRID
    val gridY = target / JOYSTICK_RC3D_GRID
    return when (mode) {
        is JoystickMode.VerticalSpring -> {
            val wiggleY = if (gridY > 0) gridY - 1 else gridY + 1
            joystickRc3dFrameIndex(gridX, wiggleY)
        }
        is JoystickMode.HorizontalSpring -> {
            val wiggleX = if (gridX > 0) gridX - 1 else gridX + 1
            joystickRc3dFrameIndex(wiggleX, gridY)
        }
        is JoystickMode.Spring -> {
            val wiggleX = if (gridX > 0) gridX - 1 else gridX + 1
            val wiggleY = if (gridY > 0) gridY - 1 else gridY + 1
            joystickRc3dFrameIndex(wiggleX, wiggleY)
        }
        else -> target
    }
}
