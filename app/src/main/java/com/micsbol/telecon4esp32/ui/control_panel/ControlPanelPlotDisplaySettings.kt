package com.micsbol.telecon4esp32.ui.control_panel

import androidx.compose.runtime.saveable.Saver
import com.micsbol.telecon4esp32.domain.model.PlotGraphMode
import com.micsbol.telecon4esp32.domain.model.PlotLineStyle
import com.micsbol.telecon4esp32.domain.model.StickChannelLink
import com.micsbol.telecon4esp32.domain.model.UserSettings

private const val DISPLAY_SEPARATOR = "\u001e"

data class ControlPanelPlotDisplaySettings(
    val radarSettings: RadarDisplaySettings = RadarDisplaySettings(),
    val plotVisible: List<Boolean> = List(UserSettings.PLOT_LABEL_COUNT) { true },
    val graphModes: List<PlotGraphMode> = List(UserSettings.PLOT_LABEL_COUNT) { PlotGraphMode.CONTINUOUS },
    val lineStyles: List<PlotLineStyle> = List(UserSettings.PLOT_LABEL_COUNT) { PlotLineStyle.LINE },
    val plotOnTop: List<Boolean> = List(UserSettings.PLOT_LABEL_COUNT) { false },
    val leftStickLink: StickChannelLink = StickChannelLink.DEFAULT,
    val rightStickLink: StickChannelLink = StickChannelLink.DEFAULT,
) {
    fun encode(): String = listOf(
        radarSettings.encode(),
        encodeBooleanList(plotVisible, default = true),
        graphModes.joinToString(",") { it.name },
        lineStyles.joinToString(",") { it.name },
        encodeBooleanList(plotOnTop, default = false),
        leftStickLink.encode(),
        rightStickLink.encode(),
    ).joinToString(DISPLAY_SEPARATOR)

    companion object {
        val DEFAULT = ControlPanelPlotDisplaySettings()

        fun decode(encoded: String?): ControlPanelPlotDisplaySettings {
            if (encoded.isNullOrBlank()) return DEFAULT
            val parts = encoded.split(DISPLAY_SEPARATOR)
            return ControlPanelPlotDisplaySettings(
                radarSettings = RadarDisplaySettings.decode(parts.getOrNull(0)),
                plotVisible = decodeBooleanList(parts.getOrNull(1), default = true),
                graphModes = decodeGraphModes(parts.getOrNull(2)),
                lineStyles = decodeLineStyles(parts.getOrNull(3)),
                plotOnTop = decodeBooleanList(parts.getOrNull(4), default = false),
                leftStickLink = StickChannelLink.decode(parts.getOrNull(5)),
                rightStickLink = StickChannelLink.decode(parts.getOrNull(6)),
            )
        }
    }
}

internal val ControlPanelPlotDisplaySettingsSaver =
    Saver<ControlPanelPlotDisplaySettings, String>(
        save = { it.encode() },
        restore = { ControlPanelPlotDisplaySettings.decode(it) },
    )

private fun encodeBooleanList(values: List<Boolean>, default: Boolean): String =
    List(UserSettings.PLOT_LABEL_COUNT) { index ->
        if (values.getOrElse(index) { default }) "1" else "0"
    }.joinToString(",")

private fun decodeBooleanList(encoded: String?, default: Boolean): List<Boolean> {
    val parsed = encoded?.split(',').orEmpty().map { token -> token == "1" }
    return List(UserSettings.PLOT_LABEL_COUNT) { index ->
        parsed.getOrElse(index) { default }
    }
}

private fun decodeGraphModes(encoded: String?): List<PlotGraphMode> {
    val parsed = encoded?.split(',').orEmpty().map { token ->
        PlotGraphMode.entries.find { it.name == token } ?: PlotGraphMode.CONTINUOUS
    }
    return List(UserSettings.PLOT_LABEL_COUNT) { index ->
        parsed.getOrElse(index) { PlotGraphMode.CONTINUOUS }
    }
}

private fun decodeLineStyles(encoded: String?): List<PlotLineStyle> {
    val parsed = encoded?.split(',').orEmpty().map { token ->
        PlotLineStyle.entries.find { it.name == token } ?: PlotLineStyle.LINE
    }
    return List(UserSettings.PLOT_LABEL_COUNT) { index ->
        parsed.getOrElse(index) { PlotLineStyle.LINE }
    }
}
