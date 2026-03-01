package com.example.emitterapp.domain.bluetooth

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.StateFlow

data class PanelState(
    val leftValue: Int = 0,
    val rightValue: Int = 0,
    val leftOn: Boolean = false,
    val rightOn: Boolean = false,
    val leftColor: Color = Color.Green,
    val rightColor: Color = Color.Green,
    val leftTitle: String =  "",
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
    val series: List<PlotData> = emptyList()
)

data class TelemetryState(
    val panelState: PanelState = PanelState(),
    val indicatorState: IndicatorState = IndicatorState(),
    val plotState: PlotState = PlotState()
)

data class PlotData(
    val name: String = "",
    val dataPoints: List<Float> = emptyList(),
    val color: Color = Color.White
)