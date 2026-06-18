package com.micsbol.telecon4esp32.ui.solarsystem

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.theme.PlotOrange
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.TechBlueBright
import com.micsbol.telecon4esp32.ui.theme.TechCyanBright

private val VoltageIconTint = Color(0xFFB39DDB)

data class SolarMetricUiModel(
    val id: String,
    @StringRes val labelRes: Int,
    val value: String,
    val iconType: SolarSystemIconType,
    val iconTint: Color,
    val valueColor: Color,
)

data class ProductionChartData(
    val powerSeries: List<Float>,
    val timeLabels: List<String>,
) {
    companion object {
        fun mock(): ProductionChartData = ProductionChartData(
            powerSeries = listOf(
                0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
                0.05f, 0.12f, 0.25f, 0.4f, 0.55f, 0.72f, 0.88f, 1.0f, 1.1f, 1.15f,
                1.18f, 1.2f, 1.15f, 1.05f, 0.95f, 0.82f, 0.68f, 0.52f, 0.38f, 0.25f,
                0.15f, 0.08f, 0.03f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
            ),
            timeLabels = listOf(
                "00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "24:00",
            ),
        )
    }
}

data class PowerFlowUiModel(
    val solarPowerW: Int,
    val batteryPowerW: Int,
    val loadPowerW: Int,
)

data class SolarSystemUiState(
    val energyBalanceW: Int = 120,
    val powerFlow: PowerFlowUiModel = PowerFlowUiModel(
        solarPowerW = 480,
        batteryPowerW = 170,
        loadPowerW = 310,
    ),
    val metrics: List<SolarMetricUiModel> = defaultMetrics(),
    val productionChart: ProductionChartData = ProductionChartData.mock(),
    val dailyEnergyKwh: Float = 4.6f,
    val dailyEnergyProgress: Float = 0.72f,
)

private val GridIconTint = Color(0xFFB0BEC5)

private fun defaultMetrics(): List<SolarMetricUiModel> = listOf(
    SolarMetricUiModel(
        id = "solar",
        labelRes = R.string.solar_system_metric_solar,
        value = "480W",
        iconType = SolarSystemIconType.SOLAR,
        iconTint = PlotOrange,
        valueColor = PlotOrange,
    ),
    SolarMetricUiModel(
        id = "load",
        labelRes = R.string.solar_system_metric_load,
        value = "310W",
        iconType = SolarSystemIconType.LOAD,
        iconTint = TechBlueBright,
        valueColor = TechBlueBright,
    ),
    SolarMetricUiModel(
        id = "battery",
        labelRes = R.string.solar_system_metric_battery,
        value = "86%",
        iconType = SolarSystemIconType.BATTERY,
        iconTint = StatusConnected,
        valueColor = StatusConnected,
    ),
    SolarMetricUiModel(
        id = "grid",
        labelRes = R.string.solar_system_metric_grid,
        value = "",
        iconType = SolarSystemIconType.GRID,
        iconTint = GridIconTint,
        valueColor = StatusConnected,
    ),
    SolarMetricUiModel(
        id = "voltage",
        labelRes = R.string.solar_system_metric_voltage,
        value = "24.8V",
        iconType = SolarSystemIconType.VOLTAGE,
        iconTint = VoltageIconTint,
        valueColor = VoltageIconTint,
    ),
    SolarMetricUiModel(
        id = "current",
        labelRes = R.string.solar_system_metric_current,
        value = "19.4A",
        iconType = SolarSystemIconType.CURRENT,
        iconTint = TechCyanBright,
        valueColor = TechCyanBright,
    ),
)
