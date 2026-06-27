package com.micsbol.telecon4esp32.ui.control_panel

import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.bluetooth.SimpleProtocolTelemetryMapper
import com.micsbol.telecon4esp32.domain.model.UserSettings

/**
 * Builds the plot legend series for the RC center display.
 * When telemetry has no points yet (e.g. Bluetooth disconnected), non-empty settings labels
 * are still shown with their color swatches.
 */
fun plotSeriesForDisplay(
    telemetrySeries: List<PlotData>,
    plotLabels: List<String>,
): List<PlotData> {
    val colors = SimpleProtocolTelemetryMapper.DEFAULT_PLOT_COLORS_ARGB
    return (0 until UserSettings.PLOT_LABEL_COUNT).mapNotNull { index ->
        val settingsLabel = plotLabels.getOrElse(index) { "" }
        val telemetry = telemetrySeries.getOrNull(index)
        when {
            telemetry != null -> telemetry.copy(
                name = telemetry.name.plotNameOrSettingsFallback(
                    index = index,
                    settingsLabel = settingsLabel,
                ),
            )
            settingsLabel.isNotBlank() -> PlotData(
                name = settingsLabel,
                dataPoints = emptyList(),
                colorArgb = colors.getOrElse(index) { 0xFFFFFFFF.toInt() },
            )
            else -> null
        }
    }
}

internal fun String.plotNameOrSettingsFallback(index: Int, settingsLabel: String): String {
    val defaultName = "Plot ${index + 1}"
    return when {
        isNotBlank() && this != defaultName -> this
        settingsLabel.isNotBlank() -> settingsLabel
        isNotBlank() -> this
        else -> settingsLabel
    }
}
