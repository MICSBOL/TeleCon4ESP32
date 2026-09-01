package com.micsbol.telecon4esp32.ui.control_panel

import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

enum class RadarScanSpan(val degrees: Float) {
    DEGREES_180(180f),
    DEGREES_270(270f),
}

enum class RadarBeamWidth(val halfAngleDegrees: Float) {
    NARROW(14f),
    MEDIUM(28f),
    WIDE(42f),
}

data class RadarDisplaySettings(
    val scanSpan: RadarScanSpan = RadarScanSpan.DEGREES_180,
    val beamWidth: RadarBeamWidth = RadarBeamWidth.MEDIUM,
    val angleSeriesIndex: Int = 0,
    val rangeSeriesIndex: Int = 1,
    val showGrid: Boolean = true,
    val showSweepTrail: Boolean = true,
    val showHistory: Boolean = true,
    val keepLastPoints: Boolean = false,
    val rangeRingCount: Int = 4,
) {
    companion object {
        const val MIN_RANGE_RINGS = 2
        const val MAX_RANGE_RINGS = 6
        const val HISTORY_SAMPLE_COUNT = 8
        val SERIES_COUNT: Int get() = com.micsbol.telecon4esp32.domain.model.TelemetryChannel.U8_SOURCES.size
    }
}

data class RadarSectorLayout(
    val originX: Float,
    val originY: Float,
    val radius: Float,
    /** Canvas `drawArc` degrees: 0 = 3 o'clock, positive clockwise. */
    val canvasStartAngleDegrees: Float,
    val sweepAngleDegrees: Float,
)

/**
 * Fits a north-centered sector (12 o'clock) into [width] x [height].
 * 180° is an upward semicircle; 270° also covers 45° below the horizon.
 */
fun computeRadarSectorLayout(
    width: Float,
    height: Float,
    spanDegrees: Float,
    padding: Float,
): RadarSectorLayout {
    val span = spanDegrees.coerceIn(
        RadarScanSpan.DEGREES_180.degrees,
        RadarScanSpan.DEGREES_270.degrees,
    )
    val halfBelowHorizon = ((span / 2f) - 90f).coerceAtLeast(0f)
    val belowFrac = sin(Math.toRadians(halfBelowHorizon.toDouble())).toFloat()
    val availableW = (width - 2f * padding).coerceAtLeast(1f)
    val availableH = (height - 2f * padding).coerceAtLeast(1f)
    val radius = min(availableW / 2f, availableH / (1f + belowFrac)).coerceAtLeast(1f)
    val sectorHeight = radius * (1f + belowFrac)
    val top = padding + (availableH - sectorHeight) / 2f
    return RadarSectorLayout(
        originX = width / 2f,
        originY = top + radius,
        radius = radius,
        canvasStartAngleDegrees = -90f - span / 2f,
        sweepAngleDegrees = span,
    )
}

fun radarSeriesIndex(index: Int): Int =
    index.coerceIn(0, RadarDisplaySettings.SERIES_COUNT - 1)

/** Maps a 0–1 (or -1–1) plot sample onto 0–1 servo travel. */
fun radarServoProgress(sample: Float): Float {
    val t = if (sample in 0f..1f) sample else (sample + 1f) / 2f
    return t.coerceIn(0f, 1f)
}

/**
 * Servo plot sample → bearing from 12 o'clock.
 * 0 is the left extreme of [spanDegrees], 1 is the right extreme, 0.5 is north.
 */
fun radarBearingFromServoSample(sample: Float, spanDegrees: Float): Float {
    return -spanDegrees / 2f + radarServoProgress(sample) * spanDegrees
}

fun radarRangeFraction(sample: Float): Float {
    val normalized = if (sample in 0f..1f) sample else (sample + 1f) / 2f
    return normalized.coerceIn(0.12f, 0.95f)
}

fun radarGridBearingsFromNorth(spanDegrees: Float, stepDegrees: Float = 45f): List<Float> {
    val half = spanDegrees / 2f
    val bearings = mutableListOf<Float>()
    var angle = -half
    while (angle <= half + 0.01f) {
        bearings.add(angle)
        angle += stepDegrees
    }
    return bearings
}

data class RadarPersistedPoint(
    val bearingFromNorth: Float,
    val rangeFraction: Float,
    val colorArgb: Int,
)

/**
 * Keeps the last sample in each bearing bin so a sweep paints a lasting scan picture.
 * Live mode ignores this buffer.
 */
class RadarPointPersistence(
    private val binDegrees: Float = 2f,
) {
    private val bins = linkedMapOf<Int, RadarPersistedPoint>()

    fun ingest(bearingFromNorth: Float, rangeFraction: Float, colorArgb: Int) {
        if (binDegrees <= 0f) return
        val key = (bearingFromNorth / binDegrees).roundToInt()
        bins[key] = RadarPersistedPoint(
            bearingFromNorth = bearingFromNorth,
            rangeFraction = rangeFraction,
            colorArgb = colorArgb,
        )
    }

    fun points(): List<RadarPersistedPoint> = bins.values.toList()

    fun clear() {
        bins.clear()
    }
}
