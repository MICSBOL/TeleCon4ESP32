package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlotCalibrationTest {

    @Test
    fun `default calibration maps byte 128 to 128`() {
        val value = PlotCalibration.DEFAULT.toEngineering(128 / 255f)
        assertEquals(128f, value, 0.02f)
    }

    @Test
    fun `volts span maps mid scale to 1_65`() {
        val calibration = PlotCalibration(offset = 0f, span = 3.3f, unit = "V")
        assertEquals(1.65f, calibration.toEngineering(0.5f), 0.001f)
        assertEquals("1.65 V", calibration.formatEngineering(0.5f))
    }

    @Test
    fun `offset plus span maps full scale`() {
        val calibration = PlotCalibration(offset = -100f, span = 200f, unit = "")
        assertEquals(-100f, calibration.toEngineering(0f), 0.001f)
        assertEquals(100f, calibration.toEngineering(1f), 0.001f)
    }

    @Test
    fun `resolved y range uses offset as min and offset plus span as max`() {
        assertEquals(0f to 255f, PlotCalibration.DEFAULT.resolvedYRange())
        assertEquals(
            -20f to 80f,
            PlotCalibration(offset = -20f, span = 100f).resolvedYRange(),
        )
        assertEquals(5f to 6f, PlotCalibration(offset = 5f, span = 0f).resolvedYRange())
    }

    @Test
    fun `plot scale ticks include min mid and max`() {
        assertEquals(listOf(0f, 127.5f, 255f), plotScaleTicks(0f, 255f))
        assertEquals(listOf(-10f, 10f, 30f), plotScaleTicks(-10f, 30f))
        assertEquals(listOf(0f, 1f), plotScaleTicks(0f, 1f, count = 2))
    }

    @Test
    fun `zero line sits at the bottom when the range starts at zero`() {
        assertEquals(0f, PlotCalibration.DEFAULT.zeroLineNormalized())
        assertEquals(0f, PlotCalibration(offset = 0f, span = 3.3f).zeroLineNormalized())
    }

    @Test
    fun `zero line sits at mid scale for a bipolar range`() {
        assertEquals(
            0.5f,
            PlotCalibration(offset = -100f, span = 200f).zeroLineNormalized(),
        )
    }

    @Test
    fun `zero line sits at the top when the range ends at zero`() {
        assertEquals(1f, PlotCalibration(offset = -50f, span = 50f).zeroLineNormalized())
    }

    @Test
    fun `zero line is omitted when zero is outside the range`() {
        assertEquals(null, PlotCalibration(offset = 10f, span = 90f).zeroLineNormalized())
        assertEquals(null, PlotCalibration(offset = -50f, span = 40f).zeroLineNormalized())
    }
}
