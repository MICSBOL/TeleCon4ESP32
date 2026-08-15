package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

/**
 * Steering center trim for RC Vehicle Pro.
 * One tick = smallest channel step the app can send (−100…100 integers).
 */
object RcSteerTrim {
    /** Normalized stick step matching `(x * 100).toInt()` resolution. */
    const val STEP = 0.01f

    /** Max |trim| so ticks stay a fine adjust, not a second steering stick. */
    const val MAX = 0.25f

    fun nudge(current: Float, steps: Int): Float =
        (current + steps * STEP).coerceIn(-MAX, MAX)

    fun apply(stickX: Float, trim: Float): Float =
        (stickX + trim).coerceIn(-1f, 1f)

    /** Channel units shown in the HUD (−25…25). */
    fun toChannelUnits(trim: Float): Int = (trim * 100f).toInt()
}
