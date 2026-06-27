package com.micsbol.telecon4esp32.domain.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleProtocolTelemetryMapperTest {

    @Test
    fun `applyRcData updates panel and indicator fields`() {
        val updated = SimpleProtocolTelemetryMapper.applyRcData(
            values = mapOf(
                "left" to "1200",
                "right" to "3400",
                "lo" to "1",
                "ro" to "0",
                "lg" to "0",
                "rg" to "1",
                "analog" to "42",
                "batt" to "88",
                "led" to "0F",
            ),
            current = TelemetryState(),
        )

        assertEquals(1200, updated.panelState.leftValue)
        assertEquals(3400, updated.panelState.rightValue)
        assertTrue(updated.panelState.leftOn)
        assertFalse(updated.panelState.rightOn)
        assertEquals(42, updated.indicatorState.analogValue)
        assertEquals(88, updated.indicatorState.batteryLevel)
        assertEquals(0x0F.toByte(), updated.indicatorState.ledValues)
    }

    @Test
    fun `applyRcPlotConfig sets series names`() {
        val updated = SimpleProtocolTelemetryMapper.applyRcPlotConfig(
            values = mapOf("n0" to "RPM", "n1" to "Speed"),
            current = TelemetryState(),
        )

        assertEquals("RPM", updated.first.plotState.series[0].name)
        assertEquals("Speed", updated.first.plotState.series[1].name)
        assertEquals(mapOf(0 to "RPM", 1 to "Speed"), updated.second)
    }

    @Test
    fun `applyRcPlot appends normalized samples`() {
        val configured = SimpleProtocolTelemetryMapper.applyRcPlotConfig(
            values = mapOf("n0" to "RPM"),
            current = TelemetryState(),
        )
        val afterFirst = SimpleProtocolTelemetryMapper.applyRcPlot(
            values = mapOf("v0" to "255"),
            current = configured.first,
            plotNames = configured.second,
        )
        val afterSecond = SimpleProtocolTelemetryMapper.applyRcPlot(
            values = mapOf("v0" to "0"),
            current = afterFirst,
            plotNames = configured.second,
        )

        assertEquals(listOf(1f, 0f), afterSecond.plotState.series[0].dataPoints)
        assertEquals(2L, afterSecond.plotState.revision)
    }
}
