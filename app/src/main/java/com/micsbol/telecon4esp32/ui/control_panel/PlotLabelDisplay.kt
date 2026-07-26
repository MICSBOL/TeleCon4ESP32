package com.micsbol.telecon4esp32.ui.control_panel

import com.micsbol.telecon4esp32.domain.bluetooth.PlotData
import com.micsbol.telecon4esp32.domain.bluetooth.SimpleProtocolTelemetryMapper
import com.micsbol.telecon4esp32.domain.model.UserSettings

/**
 * Builds exactly [UserSettings.PLOT_LABEL_COUNT] (4) series for the RC center display
 * (two traces in the top pane, two in the bottom).
 *
 * Live telemetry fills data points when present; otherwise the slot keeps its settings
 * label (or a default `Plot N` name) so both panes stay allocated.
 */
fun plotSeriesForDisplay(
    telemetrySeries: List<PlotData>,
    plotLabels: List<String>,
): List<PlotData> {
    val colors = SimpleProtocolTelemetryMapper.DEFAULT_PLOT_COLORS_ARGB
    return List(UserSettings.PLOT_LABEL_COUNT) { index ->
        val settingsLabel = plotLabels.getOrElse(index) { "" }
        val telemetry = telemetrySeries.getOrNull(index)
        if (telemetry != null) {
            telemetry.copy(
                name = telemetry.name.plotNameOrSettingsFallback(
                    index = index,
                    settingsLabel = settingsLabel,
                ),
            )
        } else {
            PlotData(
                name = settingsLabel.ifBlank { "Plot ${index + 1}" },
                dataPoints = emptyList(),
                colorArgb = colors.getOrElse(index) { 0xFFFFFFFF.toInt() },
            )
        }
    }
}

internal fun String.plotNameOrSettingsFallback(index: Int, settingsLabel: String): String {
    val defaultName = "Plot ${index + 1}"
    return when {
        isNotBlank() && this != defaultName -> this
        settingsLabel.isNotBlank() -> settingsLabel
        isNotBlank() -> this
        else -> settingsLabel.ifBlank { defaultName }
    }
}
