package com.micsbol.telecon4esp32.ui.control_panel

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

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
    fun encode(): String = listOf(
        scanSpan.name,
        beamWidth.name,
        angleSeriesIndex.toString(),
        rangeSeriesIndex.toString(),
        if (showGrid) "1" else "0",
        if (showSweepTrail) "1" else "0",
        if (showHistory) "1" else "0",
        if (keepLastPoints) "1" else "0",
        rangeRingCount.toString(),
    ).joinToString(",")

    companion object {
        const val MIN_RANGE_RINGS = 2
        const val MAX_RANGE_RINGS = 6
        const val HISTORY_SAMPLE_COUNT = 8
        val SERIES_COUNT: Int get() = com.micsbol.telecon4esp32.domain.model.TelemetryChannel.U8_SOURCES.size

        fun decode(encoded: String?): RadarDisplaySettings {
            if (encoded.isNullOrBlank()) return RadarDisplaySettings()
            val parts = encoded.split(',')
            val defaults = RadarDisplaySettings()
            return RadarDisplaySettings(
                scanSpan = RadarScanSpan.entries.find { it.name == parts.getOrNull(0) }
                    ?: RadarScanSpan.DEGREES_180,
                beamWidth = RadarBeamWidth.entries.find { it.name == parts.getOrNull(1) }
                    ?: RadarBeamWidth.MEDIUM,
                angleSeriesIndex = parts.getOrNull(2)?.toIntOrNull() ?: 0,
                rangeSeriesIndex = parts.getOrNull(3)?.toIntOrNull() ?: 1,
                showGrid = parts.getOrNull(4) != "0",
                showSweepTrail = parts.getOrNull(5) != "0",
                showHistory = parts.getOrNull(6) != "0",
                keepLastPoints = parts.getOrNull(7) == "1",
                rangeRingCount = parts.getOrNull(8)?.toIntOrNull() ?: defaults.rangeRingCount,
            )
        }
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

/**
 * Horizon of the RC HUD perspective floor, measured from the top of the plot.
 * The radar camera elevation is derived from this so the disc sits in that 3D space.
 */
const val RADAR_PLOT_HORIZON_FRACTION = 3f / 4f

private const val RADAR_CAMERA_DISTANCE_RADII = 2.55f
private const val RADAR_LOOK_AT_FORWARD = 0.16f
private const val RADAR_HOVER_HEIGHT = 0.16f
private const val MIN_RADAR_ELEVATION_DEGREES = 42f
private const val MAX_RADAR_ELEVATION_DEGREES = 54f

data class RadarCanvasPoint(val x: Float, val y: Float)

/**
 * Front-upper view of a ground-plane radar. +Z is north (ahead), +X is east.
 * [project] maps a polar sample onto the canvas after perspective divide.
 */
class RadarViewProjection internal constructor(
    val originX: Float,
    val originY: Float,
    val elevationDegrees: Float,
    private val mapWorld: (Float, Float, Float) -> RadarCanvasPoint,
    private val worldDepthScale: (Float, Float, Float) -> Float,
    private val hoverY: Float,
) {
    fun project(bearingFromNorth: Float, rangeFraction: Float): RadarCanvasPoint {
        val (x, z) = radarPlanePoint(bearingFromNorth, rangeFraction)
        return mapWorld(x, hoverY, z)
    }

    fun projectFloor(worldX: Float, worldZ: Float): RadarCanvasPoint =
        mapWorld(worldX, 0f, worldZ)

    fun depthScale(bearingFromNorth: Float, rangeFraction: Float): Float {
        val (x, z) = radarPlanePoint(bearingFromNorth, rangeFraction)
        return worldDepthScale(x, hoverY, z)
    }
}

/**
 * Elevation of the virtual camera above the radar plane, from the plot's floor
 * band (horizon at [RADAR_PLOT_HORIZON_FRACTION]) and aspect. Clamped so the
 * disc stays readable instead of flattening into an edge-on line.
 */
fun radarCameraElevationDegrees(width: Float, height: Float): Float {
    val w = width.coerceAtLeast(1f)
    val h = height.coerceAtLeast(1f)
    val groundBand = h * (1f - RADAR_PLOT_HORIZON_FRACTION)
    val fromFloor = Math.toDegrees(atan2(groundBand.toDouble(), (w * 0.5).toDouble()))
    val fromFill = Math.toDegrees(atan2(h.toDouble(), w.toDouble()))
    return (fromFloor * 0.35 + fromFill * 0.65).toFloat()
        .coerceIn(MIN_RADAR_ELEVATION_DEGREES, MAX_RADAR_ELEVATION_DEGREES)
}

/** Unit-disc point on the ground: x = east, z = north. */
fun radarPlanePoint(bearingFromNorth: Float, rangeFraction: Float): Pair<Float, Float> {
    val rad = Math.toRadians(bearingFromNorth.toDouble())
    val r = rangeFraction.coerceAtLeast(0f)
    return sin(rad).toFloat() * r to cos(rad).toFloat() * r
}

/**
 * Fits a north-centered ground sector into [width] x [height], viewed from
 * front-upper so the near rim is larger than the far (north) rim.
 */
fun computeRadarViewProjection(
    width: Float,
    height: Float,
    spanDegrees: Float,
    padding: Float,
): RadarViewProjection {
    val span = spanDegrees.coerceIn(
        RadarScanSpan.DEGREES_180.degrees,
        RadarScanSpan.DEGREES_270.degrees,
    )
    val availableW = (width - 2f * padding).coerceAtLeast(1f)
    val availableH = (height - 2f * padding).coerceAtLeast(1f)
    val elevation = radarCameraElevationDegrees(width, height)
    val elevRad = Math.toRadians(elevation.toDouble()).toFloat()
    val dist = RADAR_CAMERA_DISTANCE_RADII
    val cam = Vec3(0f, dist * sin(elevRad), -dist * cos(elevRad))
    val lookAt = Vec3(0f, 0f, RADAR_LOOK_AT_FORWARD)
    val forward = (lookAt - cam).normalized()
    val worldUp = Vec3(0f, 1f, 0f)
    val right = worldUp.cross(forward).normalized()
    val up = forward.cross(right).normalized()

    fun projectNdc(worldX: Float, worldY: Float, worldZ: Float): Pair<Float, Float> {
        val toPoint = Vec3(worldX, worldY, worldZ) - cam
        val cx = toPoint.dot(right)
        val cy = toPoint.dot(up)
        val cz = toPoint.dot(forward).coerceAtLeast(0.08f)
        return (cx / cz) to (cy / cz)
    }

    fun depthScale(worldX: Float, worldY: Float, worldZ: Float): Float {
        val distPoint = (Vec3(worldX, worldY, worldZ) - cam).length()
        val distOrigin = (Vec3(0f, worldY, 0f) - cam).length().coerceAtLeast(1e-4f)
        return (distOrigin / distPoint).coerceIn(0.55f, 1.55f)
    }

    val hoverY = RADAR_HOVER_HEIGHT
    val steps = 48
    val ndcs = ArrayList<Pair<Float, Float>>(steps + 2)
    ndcs.add(projectNdc(0f, hoverY, 0f))
    for (i in 0..steps) {
        val bearing = -span / 2f + span * i / steps
        val (wx, wz) = radarPlanePoint(bearing, 1f)
        ndcs.add(projectNdc(wx, hoverY, wz))
    }
    var minX = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    ndcs.forEach { (x, y) ->
        minX = min(minX, x)
        maxX = max(maxX, x)
        minY = min(minY, y)
        maxY = max(maxY, y)
    }
    val ndcW = (maxX - minX).coerceAtLeast(1e-4f)
    val ndcH = (maxY - minY).coerceAtLeast(1e-4f)
    val scale = min(availableW / ndcW, availableH / ndcH)
    val x0 = padding + (availableW - ndcW * scale) / 2f
    val y0 = padding + (availableH - ndcH * scale) / 2f

    fun mapWorld(worldX: Float, worldY: Float, worldZ: Float): RadarCanvasPoint {
        val (ndcX, ndcY) = projectNdc(worldX, worldY, worldZ)
        return RadarCanvasPoint(
            x = x0 + (ndcX - minX) * scale,
            y = y0 + (maxY - ndcY) * scale,
        )
    }

    val origin = mapWorld(0f, hoverY, 0f)
    return RadarViewProjection(
        originX = origin.x,
        originY = origin.y,
        elevationDegrees = elevation,
        mapWorld = ::mapWorld,
        worldDepthScale = ::depthScale,
        hoverY = hoverY,
    )
}

private data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vec3 {
        val len = length().coerceAtLeast(1e-6f)
        return Vec3(x / len, y / len, z / len)
    }
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
