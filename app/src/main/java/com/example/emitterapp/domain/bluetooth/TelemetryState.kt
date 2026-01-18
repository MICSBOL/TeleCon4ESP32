package com.example.emitterapp.domain.bluetooth

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.StateFlow

data class PanelState(
    val leftValue: Int = 0,
    val rightValue: Int = 0,
    val leftOn: Boolean = false,
    val rightOn: Boolean = false,
    val leftColor: Color = Color.Green,
    val rightColor: Color = Color.Green
)

data class IndicatorState(
    val analogValue: Int = 0,
    val batteryLevel: Int = 0
)

data class PlotState(
    val series: List<PlotData> = emptyList()
)

data class TelemetryState(
    val panelState: StateFlow<PanelState>,
    val indicatorState: StateFlow<IndicatorState>,
    val plotState: StateFlow<PlotState>
)

data class PlotData(
    val dataPoints: List<Float> = emptyList(),
    val color: Color = Color.White
)