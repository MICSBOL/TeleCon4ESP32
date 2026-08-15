package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import kotlin.math.abs

/**
 * App-side stick deadzone (no ESP32 change required).
 * Values inside the zone become 0; the rest of the travel is rescaled to keep full range.
 */
object RcStickDeadzone {
    /** Fraction of stick travel ignored around center (0..1). */
    const val DEFAULT = 0.08f

    fun applyAxis(value: Float, deadzone: Float = DEFAULT): Float {
        val dz = deadzone.coerceIn(0f, 0.95f)
        if (dz <= 0f) return value.coerceIn(-1f, 1f)
        val magnitude = abs(value)
        if (magnitude <= dz) return 0f
        val sign = if (value >= 0f) 1f else -1f
        return (sign * ((magnitude - dz) / (1f - dz))).coerceIn(-1f, 1f)
    }

    fun apply(
        x: Float,
        y: Float,
        deadzone: Float = DEFAULT,
    ): Pair<Float, Float> = Pair(applyAxis(x, deadzone), applyAxis(y, deadzone))
}
