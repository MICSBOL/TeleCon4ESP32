package com.micsbol.telecon4esp32.ui.control_panel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class RadarSectorGeometryTest {

    @Test
    fun `180 sector starts at 9 o'clock and sweeps to 3 o'clock`() {
        val layout = computeRadarSectorLayout(200f, 120f, 180f, 0f)
        assertEquals(-180f, layout.canvasStartAngleDegrees, 0.01f)
        assertEquals(180f, layout.sweepAngleDegrees, 0.01f)
    }

    @Test
    fun `270 sector is centered on 12 o'clock`() {
        val layout = computeRadarSectorLayout(200f, 200f, 270f, 0f)
        assertEquals(-225f, layout.canvasStartAngleDegrees, 0.01f)
        assertEquals(270f, layout.sweepAngleDegrees, 0.01f)
    }

    @Test
    fun `180 sector uses full width and sits as an upward semicircle`() {
        val layout = computeRadarSectorLayout(200f, 120f, 180f, 10f)
        assertEquals(90f, layout.radius, 0.01f)
        assertEquals(100f, layout.originX, 0.01f)
        assertTrue(layout.originY > layout.radius)
    }

    @Test
    fun `270 sector reserves space below the horizon`() {
        val layout = computeRadarSectorLayout(200f, 200f, 270f, 0f)
        val belowFrac = sin(Math.toRadians(45.0)).toFloat()
        assertEquals(100f, layout.radius, 0.01f)
        val expectedOriginY = (200f - layout.radius * (1f + belowFrac)) / 2f + layout.radius
        assertEquals(expectedOriginY, layout.originY, 0.05f)
    }

    @Test
    fun `servo 0 and 1 map to the two extremes of the scan path`() {
        val span180 = 180f
        assertEquals(-90f, radarBearingFromServoSample(0f, span180), 0.01f)
        assertEquals(90f, radarBearingFromServoSample(1f, span180), 0.01f)
        assertEquals(0f, radarBearingFromServoSample(0.5f, span180), 0.01f)

        val span270 = 270f
        assertEquals(-135f, radarBearingFromServoSample(0f, span270), 0.01f)
        assertEquals(135f, radarBearingFromServoSample(1f, span270), 0.01f)
        assertEquals(0f, radarBearingFromServoSample(0.5f, span270), 0.01f)
    }

    @Test
    fun `normalized plot samples map into radar range`() {
        assertEquals(0.12f, radarRangeFraction(0f), 0.001f)
        assertEquals(0.5f, radarRangeFraction(0.5f), 0.001f)
        assertEquals(0.95f, radarRangeFraction(1f), 0.001f)
    }

    @Test
    fun `persistence keeps last point per bearing bin`() {
        val persistence = RadarPointPersistence(binDegrees = 2f)
        persistence.ingest(bearingFromNorth = 0.4f, rangeFraction = 0.3f, colorArgb = 1)
        persistence.ingest(bearingFromNorth = 10f, rangeFraction = 0.8f, colorArgb = 2)
        persistence.ingest(bearingFromNorth = 0.6f, rangeFraction = 0.5f, colorArgb = 3)
        val points = persistence.points()
        assertEquals(2, points.size)
        assertEquals(0.5f, points[0].rangeFraction, 0.001f)
        assertEquals(3, points[0].colorArgb)
        assertEquals(0.8f, points[1].rangeFraction, 0.001f)
    }
}
