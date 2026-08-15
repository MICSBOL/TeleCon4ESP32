package com.micsbol.telecon4esp32.domain.model

/**
 * Greenhouse camera pan/tilt mapping for ESP32-CAM gimbal servos.
 *
 * Wire values are 0…100 with [CENTER] as mechanical front/level.
 * Suggested free CAM pins: pan → GPIO 13, tilt → GPIO 12.
 */
object GhCameraGimbal {
    const val MIN = 0
    const val MAX = 100
    const val CENTER = 50

    fun clamp(value: Int): Int = value.coerceIn(MIN, MAX)

    /** Normalized stick X/Y (−1…1) → 0…100 with 0 at center. */
    fun fromNormalizedAxis(normalized: Float): Int =
        clamp(((normalized.coerceIn(-1f, 1f) + 1f) * 0.5f * MAX).toInt())

    fun toNormalizedAxis(percent: Int): Float =
        (clamp(percent) - CENTER) / CENTER.toFloat()
}
