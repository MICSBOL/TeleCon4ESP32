package com.micsbol.telecon4esp32.domain.model

/**
 * RC Vehicle camera pan (`rk` / right knob) mapping for ESP32.
 * Raw range is 0..1023; [FRONT_RAW] points the camera straight ahead.
 */
object RcCameraPan {
    const val FRONT_RAW = 511
    const val MAX_RAW = 1023

    /** Normalized 0..1 value that encodes to [FRONT_RAW] via [rawFromNormalized]. */
    const val FRONT_NORMALIZED = FRONT_RAW / MAX_RAW.toFloat()

    fun rawFromNormalized(normalized: Float): Int =
        (normalized * MAX_RAW).toInt().coerceIn(0, MAX_RAW)
}
