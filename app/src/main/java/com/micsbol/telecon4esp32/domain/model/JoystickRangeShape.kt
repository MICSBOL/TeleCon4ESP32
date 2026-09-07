package com.micsbol.telecon4esp32.domain.model

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/**
 * How a circular gimbal maps onto the −1…1 command square.
 *
 * [CIRCLE] keeps the physical disc: a 45° rim push is about (0.71, 0.71).
 * [SQUARE] scales that disc onto the command square so the same push is (1, 1).
 * Axes still reach (±1, 0) and (0, ±1). Single-axis sticks are unchanged.
 */
enum class JoystickRangeShape {
    CIRCLE,
    SQUARE,
    ;

    fun mapOutput(x: Float, y: Float): Pair<Float, Float> = when (this) {
        CIRCLE -> Pair(x.coerceIn(-1f, 1f), y.coerceIn(-1f, 1f))
        SQUARE -> circleToSquare(x, y)
    }

    /** Inverse of [mapOutput], for drawing the circular gimbal from stored commands. */
    fun toVisual(x: Float, y: Float): Pair<Float, Float> = when (this) {
        CIRCLE -> Pair(x.coerceIn(-1f, 1f), y.coerceIn(-1f, 1f))
        SQUARE -> squareToCircle(x, y)
    }

    companion object {
        fun fromStored(value: String?): JoystickRangeShape =
            entries.find { it.name == value } ?: CIRCLE
    }
}

internal fun circleToSquare(x: Float, y: Float): Pair<Float, Float> {
    val maxAbs = max(abs(x), abs(y))
    if (maxAbs < 1e-6f) return Pair(0f, 0f)
    val scale = hypot(x, y) / maxAbs
    return Pair(
        (x * scale).coerceIn(-1f, 1f),
        (y * scale).coerceIn(-1f, 1f),
    )
}

internal fun squareToCircle(x: Float, y: Float): Pair<Float, Float> {
    val radius = hypot(x, y)
    if (radius < 1e-6f) return Pair(0f, 0f)
    val scale = max(abs(x), abs(y)) / radius
    return Pair(
        (x * scale).coerceIn(-1f, 1f),
        (y * scale).coerceIn(-1f, 1f),
    )
}
