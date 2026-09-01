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
    fun `zero span parse falls back to default`() {
        assertEquals(255f, parseCalibrationFloat("", PlotCalibration.DEFAULT_SPAN), 0f)
        assertEquals(0f, parseCalibrationFloat("0", 0f), 0f)
    }
}
