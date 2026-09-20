package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import com.micsbol.telecon4esp32.domain.model.JoystickAxis
import com.micsbol.telecon4esp32.domain.model.JoystickMode

/**
 * Phone-side axis trim for RC Vehicle Pro.
 * One tick = smallest channel step the app can send (−100…100 integers).
 */
object RcStickTrim {
    /** Normalized stick step matching `(x * 100).toInt()` resolution. */
    const val STEP = 0.01f

    /** Max |trim| so ticks stay a fine adjust, not a second stick. */
    const val MAX = 0.25f

    fun nudge(current: Float, steps: Int): Float =
        (current + steps * STEP).coerceIn(-MAX, MAX)

    fun apply(stick: Float, trim: Float): Float =
        (stick + trim).coerceIn(-1f, 1f)

    /** Channel units shown in the HUD (−25…25). */
    fun toChannelUnits(trim: Float): Int = (trim * 100f).toInt()

    /**
     * Visual rest offset for enabled axes. Spring still works in geometric
     * pad space; the thumb is drawn at raw + this offset so rest sits on trim.
     */
    fun visualOffset(mode: JoystickMode, trimX: Float, trimY: Float): Pair<Float, Float> {
        val x = if (mode.axis != JoystickAxis.VERTICAL) trimX else 0f
        val y = if (mode.axis != JoystickAxis.HORIZONTAL) trimY else 0f
        return Pair(x.coerceIn(-MAX, MAX), y.coerceIn(-MAX, MAX))
    }

    fun add(position: Pair<Float, Float>, offset: Pair<Float, Float>): Pair<Float, Float> =
        Pair(
            (position.first + offset.first).coerceIn(-1f, 1f),
            (position.second + offset.second).coerceIn(-1f, 1f),
        )

    fun subtract(position: Pair<Float, Float>, offset: Pair<Float, Float>): Pair<Float, Float> =
        Pair(
            (position.first - offset.first).coerceIn(-1f, 1f),
            (position.second - offset.second).coerceIn(-1f, 1f),
        )
}

/** Steering-only alias kept for existing tests and firmware-center save. */
object RcSteerTrim {
    const val STEP = RcStickTrim.STEP
    const val MAX = RcStickTrim.MAX

    fun nudge(current: Float, steps: Int): Float = RcStickTrim.nudge(current, steps)

    fun apply(stickX: Float, trim: Float): Float = RcStickTrim.apply(stickX, trim)

    fun toChannelUnits(trim: Float): Int = RcStickTrim.toChannelUnits(trim)
}
