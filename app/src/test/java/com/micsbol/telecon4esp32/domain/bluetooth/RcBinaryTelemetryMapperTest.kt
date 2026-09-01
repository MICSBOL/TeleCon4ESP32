package com.micsbol.telecon4esp32.domain.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Test

class RcBinaryTelemetryMapperTest {

    @Test
    fun `applyPlotConfigNames maps binary config list to plot series`() {
        val (updated, names) = RcBinaryTelemetryMapper.applyPlotConfigNames(
            names = listOf("Volts", "Amps", "RPM"),
            current = TelemetryState(),
        )

        assertEquals(listOf("Volts", "Amps", "RPM"), updated.plotState.series.map { it.name })
        assertEquals(mapOf(0 to "Volts", 1 to "Amps", 2 to "RPM"), names)
    }

    @Test
    fun `applyPlotSamples appends binary plot bytes as normalized points`() {
        val configured = RcBinaryTelemetryMapper.applyPlotConfigNames(
            names = listOf("Volts", "Amps"),
            current = TelemetryState(),
        )
        val afterSamples = RcBinaryTelemetryMapper.applyPlotSamples(
            normalizedSamples = listOf(128f / 255f, 64f / 255f),
            current = configured.first,
            plotNames = configured.second,
        )

        assertEquals(2, afterSamples.plotState.series.size)
        assertEquals(128f / 255f, afterSamples.plotState.series[0].dataPoints[0], 0.001f)
        assertEquals(64f / 255f, afterSamples.plotState.series[1].dataPoints[0], 0.001f)
        assertEquals(1L, afterSamples.plotState.revision)
    }

    @Test
    fun `applyPlotSamples grows sparse series like simple protocol`() {
        val afterFirst = RcBinaryTelemetryMapper.applyPlotSamples(
            normalizedSamples = listOf(1f),
            current = TelemetryState(),
            plotNames = emptyMap(),
        )
        val afterThird = RcBinaryTelemetryMapper.applyPlotSamples(
            normalizedSamples = listOf(0.5f, 0.25f, 0.75f),
            current = afterFirst,
            plotNames = mapOf(2 to "Temp"),
        )

        assertEquals(3, afterThird.plotState.series.size)
        assertEquals("CH2", afterThird.plotState.series[1].name)
        assertEquals("Temp", afterThird.plotState.series[2].name)
    }
}
