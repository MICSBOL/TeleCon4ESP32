package com.micsbol.telecon4esp32.domain.session

import com.micsbol.telecon4esp32.domain.model.PlotCalibration
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionCsvTest {

    @Test
    fun `header includes plot raw and stick columns`() {
        val header = SessionCsv.header()
        assertTrue(header.contains("plot0_raw,plot0"))
        assertTrue(header.contains("plot3_raw,plot3"))
        assertTrue(header.contains("plot7_raw,plot7"))
        assertTrue(header.contains("lx,ly,rx,ry,lk,rk,sw"))
        assertTrue(header.contains("left,right,analog,batt,led"))
    }

    @Test
    fun `row writes calibrated plot and stick values`() {
        val row = SessionCsv.row(
            timestampMs = 1_000L,
            elapsedMs = 500L,
            plotNormalized = listOf(1f, 0.5f, null, 0f),
            calibrations = listOf(
                PlotCalibration(offset = 0f, span = 3.3f, unit = "V"),
                PlotCalibration(),
                PlotCalibration(),
                PlotCalibration(),
            ),
            leftPanel = 12,
            rightPanel = 34,
            analog = 42,
            battery = 88,
            led = 0x0F,
            leftStickX = -0.5f,
            leftStickY = 1f,
            rightStickX = 0f,
            rightStickY = 0f,
            leftKnob = 0.5f,
            rightKnob = 0.25f,
            switches = listOf(true, false, false, false, false, false),
        )
        assertTrue(row.startsWith("1000,0.50000,255,3.30000,127,"))
        assertTrue(row.contains(",12,34,42,88,0F,"))
        assertTrue(row.contains(",01\n"))
    }
}
