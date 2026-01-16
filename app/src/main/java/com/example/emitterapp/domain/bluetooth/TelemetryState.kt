package com.example.emitterapp.domain.bluetooth

import androidx.compose.ui.graphics.Color

data class TelemetryState(
    val leftPanelValue: Int = 0,
    val rightPanelValue: Int = 0,
    val leftPanelOn: Boolean = false,
    val rightPanelOn: Boolean = false,
    val leftPanelColor: Color = Color.Green,
    val rightPanelColor: Color = Color.Green,
    val analogIndicatorValue: Int = 0,
    val batteryLevel: Int = 0,
    val plotSeries: List<PlotData> = emptyList()
)

data class PlotData(
    val dataPoints: List<Float> = emptyList(),
    val color: Color = Color.White
)