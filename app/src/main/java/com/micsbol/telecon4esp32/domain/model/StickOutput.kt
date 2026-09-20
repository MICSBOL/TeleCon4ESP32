package com.micsbol.telecon4esp32.domain.model

import kotlin.math.roundToInt

/**
 * Stick command range shared by Bluetooth / Wi‑Fi and the plot overlay.
 *
 * Native travel is unsigned **0…[FULL_SCALE]**. Down / left is the range
 * start (0); up / right is the range end ([FULL_SCALE]). Rest only chooses
 * which end the stick sits at. Plot Y scales remap that 0…1 span.
 */
object StickOutput {
    const val FULL_SCALE = 4094
    const val HALF_SCALE = FULL_SCALE / 2
    const val CENTER = HALF_SCALE

    fun normalizedToUnsigned(normalized: Float): Int =
        (((normalized.coerceIn(-1f, 1f) + 1f) * FULL_SCALE) / 2f)
            .roundToInt()
            .coerceIn(0, FULL_SCALE)

    fun unsignedToNormalized(unsigned: Int): Float =
        (unsigned.coerceIn(0, FULL_SCALE) * 2f / FULL_SCALE) - 1f

    /** Percent command −100…100 used by Simple CTRL and [RcState] sticks. */
    fun normalizedToPercent(normalized: Float): Int =
        (normalized.coerceIn(-1f, 1f) * 100f).roundToInt().coerceIn(-100, 100)

    fun percentToUnsigned(percent: Int): Int =
        (((percent.coerceIn(-100, 100) + 100) * FULL_SCALE) / 200)
            .coerceIn(0, FULL_SCALE)

    /**
     * Maps a −1…1 stick onto the plot calibration. Native 0…4094 is the
     * 0…1 input; changing min/max only rescales the engineering readout.
     */
    fun toEngineering(normalized: Float, calibration: PlotCalibration): Float =
        calibration.toEngineering(normalizedToUnit(normalized))

    fun normalizedToUnit(normalized: Float): Float =
        ((normalized.coerceIn(-1f, 1f) + 1f) / 2f)

    /** Down / left → 0; up / right → 1. Rest does not invert the axis. */
    fun unitFromRest(value: Float, rest: Float): Float {
        // Rest is which physical end the stick sits at, not an analog invert.
        return normalizedToUnit(value)
    }

    fun engineeringFromUnit(unit: Float, yMin: Float, yMax: Float): Float {
        val span = (yMax - yMin).let { if (it > 0f) it else 1f }
        return yMin + unit.coerceIn(0f, 1f) * span
    }
}
