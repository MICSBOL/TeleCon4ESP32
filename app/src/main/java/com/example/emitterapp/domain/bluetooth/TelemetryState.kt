package com.example.emitterapp.domain.bluetooth

data class PanelState(
    val leftValue: Int = 0,
    val rightValue: Int = 0,
    val leftOn: Boolean = false,
    val rightOn: Boolean = false,
    val leftColorArgb: Int = 0xFF00FF00.toInt(),  // Green
    val rightColorArgb: Int = 0xFF00FF00.toInt(), // Green
    val leftTitle: String = "",
    val rightTitle: String = ""
)

data class IndicatorState(
    val analogValue: Int = 0,
    val batteryLevel: Int = 0,
    val ledValues: Byte = 0b00000000,
    val analogTitle: String = "",
    val batteryTitle: String = ""
)

data class PlotState(
    val series: List<PlotData> = emptyList(),
    /** Incremented on each plot packet so the UI can redraw scrolling history. */
    val revision: Long = 0L,
)

data class TelemetryState(
    val panelState: PanelState = PanelState(),
    val indicatorState: IndicatorState = IndicatorState(),
    val plotState: PlotState = PlotState()
)

data class PlotData(
    val name: String = "",
    val dataPoints: List<Float> = emptyList(),
    val colorArgb: Int = 0xFFFFFFFF.toInt() // White
)