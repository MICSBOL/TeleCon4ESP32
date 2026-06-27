package com.micsbol.telecon4esp32.domain.bluetooth

/**
 * Maps simple-protocol [EspMessage] payloads into domain state objects.
 */
object SimpleProtocolTelemetryMapper {

    const val MAX_PLOT_POINTS = 100

    val DEFAULT_PLOT_COLORS_ARGB = listOf(
        0xFF00FFFF.toInt(), // Cyan
        0xFFFF0000.toInt(), // Red
        0xFF00FF00.toInt(), // Green
        0xFFFFFF00.toInt(), // Yellow
        0xFFFF00FF.toInt(), // Magenta
        0xFFFFFFFF.toInt(), // White
    )

    fun applyRcData(values: Map<String, String>, current: TelemetryState): TelemetryState {
        val leftValue = values["left"]?.toIntOrNull() ?: current.panelState.leftValue
        val rightValue = values["right"]?.toIntOrNull() ?: current.panelState.rightValue
        val leftOn = values["lo"]?.toBooleanLike() ?: current.panelState.leftOn
        val rightOn = values["ro"]?.toBooleanLike() ?: current.panelState.rightOn
        val leftGreen = values["lg"]?.toBooleanLike() ?: true
        val rightGreen = values["rg"]?.toBooleanLike() ?: true
        val analog = values["analog"]?.toIntOrNull() ?: current.indicatorState.analogValue
        val battery = values["batt"]?.toIntOrNull() ?: current.indicatorState.batteryLevel
        val ledValues = values["led"]?.toIntOrNull(16)?.toByte()
            ?: current.indicatorState.ledValues

        return current.copy(
            panelState = current.panelState.copy(
                leftValue = leftValue,
                rightValue = rightValue,
                leftOn = leftOn,
                rightOn = rightOn,
                leftColorArgb = if (leftGreen) 0xFF00FF00.toInt() else 0xFFFF0000.toInt(),
                rightColorArgb = if (rightGreen) 0xFF00FF00.toInt() else 0xFFFF0000.toInt(),
                leftTitle = values["lt"] ?: current.panelState.leftTitle,
                rightTitle = values["rt"] ?: current.panelState.rightTitle,
            ),
            indicatorState = current.indicatorState.copy(
                analogValue = analog,
                batteryLevel = battery,
                ledValues = ledValues,
                analogTitle = values["at"] ?: current.indicatorState.analogTitle,
                batteryTitle = values["bt"] ?: current.indicatorState.batteryTitle,
            ),
        )
    }

    /**
     * Applies `RC:PLOTCFG,n0,Name,n1,Name2,...` and returns updated telemetry plus name map.
     */
    fun applyRcPlotConfig(
        values: Map<String, String>,
        current: TelemetryState,
        plotColors: List<Int> = DEFAULT_PLOT_COLORS_ARGB,
    ): Pair<TelemetryState, Map<Int, String>> {
        val plotNames = parseIndexedEntries(values, keyPrefix = "n")
        if (plotNames.isEmpty()) return current to emptyMap()

        val orderedNames = plotNames.entries.sortedBy { it.key }.map { it.value }
        val updatedState = current.copy(
            plotState = current.plotState.copy(
                series = orderedNames.mapIndexed { index, name ->
                    val existing = current.plotState.series.getOrNull(index)
                    PlotData(
                        name = name,
                        dataPoints = existing?.dataPoints ?: emptyList(),
                        colorArgb = plotColors.getOrElse(index) { 0xFFFFFFFF.toInt() },
                    )
                },
            ),
        )
        return updatedState to plotNames
    }

    /**
     * Appends samples from `RC:PLOT,v0,128,v1,200,...`.
     * Values are 0–255 (same scale as the binary plot packet); stored as 0.0–1.0 floats.
     */
    fun applyRcPlot(
        values: Map<String, String>,
        current: TelemetryState,
        plotNames: Map<Int, String>,
        plotColors: List<Int> = DEFAULT_PLOT_COLORS_ARGB,
    ): TelemetryState {
        val samples = parseIndexedIntEntries(values, keyPrefix = "v")
        if (samples.isEmpty()) return current

        val updatedSeries = current.plotState.series.toMutableList()
        samples.forEach { (index, rawValue) ->
            val normalized = (rawValue.coerceIn(0, 255)) / 255f
            if (index < updatedSeries.size) {
                val oldPoints = updatedSeries[index].dataPoints.toMutableList()
                oldPoints.add(normalized)
                while (oldPoints.size > MAX_PLOT_POINTS) {
                    oldPoints.removeAt(0)
                }
                updatedSeries[index] = updatedSeries[index].copy(dataPoints = oldPoints.toList())
            } else {
                while (updatedSeries.size <= index) {
                    val seriesIndex = updatedSeries.size
                    updatedSeries.add(
                        PlotData(
                            name = plotNames[seriesIndex] ?: "Plot ${seriesIndex + 1}",
                            dataPoints = emptyList(),
                            colorArgb = plotColors.getOrElse(seriesIndex) { 0xFFFFFFFF.toInt() },
                        ),
                    )
                }
                updatedSeries[index] = updatedSeries[index].copy(dataPoints = listOf(normalized))
            }
        }

        return current.copy(
            plotState = current.plotState.copy(
                series = updatedSeries,
                revision = current.plotState.revision + 1,
            ),
        )
    }

    private fun parseIndexedEntries(values: Map<String, String>, keyPrefix: String): Map<Int, String> =
        values.mapNotNull { (key, value) ->
            if (!key.startsWith(keyPrefix)) return@mapNotNull null
            val index = key.removePrefix(keyPrefix).toIntOrNull() ?: return@mapNotNull null
            index to value
        }.toMap()

    private fun parseIndexedIntEntries(values: Map<String, String>, keyPrefix: String): Map<Int, Int> =
        values.mapNotNull { (key, value) ->
            if (!key.startsWith(keyPrefix)) return@mapNotNull null
            val index = key.removePrefix(keyPrefix).toIntOrNull() ?: return@mapNotNull null
            val intValue = value.toIntOrNull() ?: return@mapNotNull null
            index to intValue
        }.toMap()
}

private fun String.toBooleanLike(): Boolean = when (lowercase()) {
    "1", "true", "on", "yes" -> true
    "0", "false", "off", "no" -> false
    else -> toIntOrNull()?.let { it != 0 } ?: false
}
