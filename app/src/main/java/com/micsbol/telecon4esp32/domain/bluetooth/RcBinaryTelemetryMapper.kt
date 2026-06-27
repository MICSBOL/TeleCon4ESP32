package com.micsbol.telecon4esp32.domain.bluetooth

/**
 * Adapts legacy binary RC telemetry packets (0xCC 0x33 plot, 0xCC 0x44 config)
 * to the same [TelemetryState] shape used by the simple line protocol and UI.
 *
 * ESP32 firmware is unchanged: plot packets still send sequential 0–255 bytes and
 * config section 0x01 still sends an ordered name list.
 */
object RcBinaryTelemetryMapper {

    fun applyPlotConfigNames(
        names: List<String>,
        current: TelemetryState,
        plotColors: List<Int> = SimpleProtocolTelemetryMapper.DEFAULT_PLOT_COLORS_ARGB,
    ): Pair<TelemetryState, Map<Int, String>> {
        if (names.isEmpty()) return current to emptyMap()
        val values = names.mapIndexed { index, name -> "n$index" to name }.toMap()
        return SimpleProtocolTelemetryMapper.applyRcPlotConfig(values, current, plotColors)
    }

    /**
     * @param normalizedSamples Values already scaled to 0.0–1.0 (as parsed from binary plot bytes).
     */
    fun applyPlotSamples(
        normalizedSamples: List<Float>,
        current: TelemetryState,
        plotNames: Map<Int, String>,
        plotColors: List<Int> = SimpleProtocolTelemetryMapper.DEFAULT_PLOT_COLORS_ARGB,
    ): TelemetryState {
        if (normalizedSamples.isEmpty()) return current
        val values = normalizedSamples.mapIndexed { index, sample ->
            val raw = (sample.coerceIn(0f, 1f) * 255f).toInt().coerceIn(0, 255)
            "v$index" to raw.toString()
        }.toMap()
        return SimpleProtocolTelemetryMapper.applyRcPlot(values, current, plotNames, plotColors)
    }
}
