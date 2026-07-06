package com.micsbol.telecon4esp32.ui.solarsystem

enum class SolarChartPeriod {
    DAY,
    WEEK,
}

enum class SolarSystemStatus {
    NORMAL,
    FAULT,
}

enum class SolarGridMode {
    IDLE,
    EXPORT,
    IMPORT,
}

data class SolarLiveTelemetry(
    val solarW: Int = 480,
    val loadW: Int = 310,
    val batteryW: Int = 170,
    val gridW: Int = 120,
    val batteryPercent: Int = 89,
    val voltage: Float = 24.8f,
    val current: Float = 19.4f,
)

data class SolarEnergyTotals(
    val todayKwh: Float = 725f,
    val monthKwh: Float = 523f,
    val totalKwh: Float = 1800f,
    val consumedWeekKwh: Float = 472f,
    val consumedDayKwh: Float = 68f,
)

data class SolarEnergyBreakdown(
    val producedKwh: Float = 492f,
    val exportedKwh: Float = 183f,
    val batteryUsedKwh: Float = 72f,
)

data class SolarEnergyDistribution(
    val toHomeKwh: Float = 23f,
    val toBatteryKwh: Float = 14f,
    val toGridKwh: Float = 16f,
)

data class SolarBatteryInfo(
    val capacityKwh: Float = 2000f,
    val chargeEtaMinutes: Int = 272,
    val totalChargedKwh: Float = 112.9f,
    val lowBatteryThreshold: Int = 20,
)

data class SolarDiyControls(
    val inverterOn: Boolean = true,
    val gridMode: SolarGridMode = SolarGridMode.EXPORT,
    val panelEfficiencyPercent: Int = 92,
    val maxSolarW: Int = 1200,
    val faultCode: Int = 0,
)

data class ConsumptionChartData(
    val groupLabels: List<String>,
    val producedSeries: List<Float>,
    val consumedSeries: List<Float>,
    val barsPerGroup: Int,
) {
    companion object {
        const val WEEK_BARS_PER_GROUP = 3
        const val DAY_BARS_PER_GROUP = 3

        fun labelsFor(period: SolarChartPeriod): List<String> = when (period) {
            SolarChartPeriod.WEEK -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            SolarChartPeriod.DAY -> listOf("00", "03", "06", "09", "12", "15", "18", "21")
        }

        fun barsPerGroupFor(period: SolarChartPeriod): Int = when (period) {
            SolarChartPeriod.WEEK -> WEEK_BARS_PER_GROUP
            SolarChartPeriod.DAY -> DAY_BARS_PER_GROUP
        }

        fun mock(period: SolarChartPeriod = SolarChartPeriod.WEEK): ConsumptionChartData {
            val labels = labelsFor(period)
            val barsPerGroup = barsPerGroupFor(period)
            return when (period) {
                SolarChartPeriod.WEEK -> ConsumptionChartData(
                    groupLabels = labels,
                    producedSeries = listOf(
                        6f, 14f, 9f, 8f, 18f, 11f, 7f, 16f, 10f, 9f, 20f, 13f,
                        10f, 22f, 14f, 7f, 15f, 9f, 5f, 12f, 8f,
                    ),
                    consumedSeries = listOf(
                        12f, 7f, 9f, 14f, 8f, 11f, 16f, 9f, 12f, 11f, 7f, 10f,
                        15f, 8f, 11f, 13f, 6f, 9f, 10f, 5f, 7f,
                    ),
                    barsPerGroup = barsPerGroup,
                )
                SolarChartPeriod.DAY -> ConsumptionChartData(
                    groupLabels = labels,
                    producedSeries = listOf(
                        0f, 0f, 0f, 2f, 5f, 8f, 10f, 12f, 10f, 14f, 18f, 22f,
                        20f, 16f, 12f, 10f, 8f, 5f, 3f, 1f, 0f, 0f, 0f, 0f,
                    ),
                    consumedSeries = listOf(
                        4f, 3f, 2f, 4f, 3f, 4f, 5f, 4f, 6f, 6f, 5f, 4f,
                        5f, 6f, 7f, 8f, 7f, 6f, 5f, 4f, 3f, 2f, 2f, 1f,
                    ),
                    barsPerGroup = barsPerGroup,
                )
            }
        }
    }
}

data class ProductionChartData(
    val powerSeries: List<Float>,
    val timeLabels: List<String>,
) {
    companion object {
        fun mock(): ProductionChartData = ProductionChartData(
            powerSeries = listOf(
                150f, 180f, 220f, 280f, 350f, 410f, 450f, 420f, 380f, 320f,
                260f, 210f, 180f, 160f,
            ),
            timeLabels = listOf("06:00", "09:00", "12:00", "15:00", "18:00"),
        )
    }
}

data class SolarSystemUiState(
    val isConnected: Boolean = false,
    val isOnline: Boolean = false,
    val deviceId: String = "ESP32-SP01",
    val panelName: String = "LinCore",
    val updatedAgo: String = "",
    val lastTelemetryAtMs: Long = 0L,
    val status: SolarSystemStatus = SolarSystemStatus.NORMAL,
    val panelCount: Int = 4,
    val live: SolarLiveTelemetry = SolarLiveTelemetry(),
    val totals: SolarEnergyTotals = SolarEnergyTotals(),
    val breakdown: SolarEnergyBreakdown = SolarEnergyBreakdown(),
    val distribution: SolarEnergyDistribution = SolarEnergyDistribution(),
    val batteryInfo: SolarBatteryInfo = SolarBatteryInfo(),
    val diy: SolarDiyControls = SolarDiyControls(),
    val consumptionChart: ConsumptionChartData = ConsumptionChartData.mock(),
    val productionChart: ProductionChartData = ProductionChartData.mock(),
    val chartPeriod: SolarChartPeriod = SolarChartPeriod.WEEK,
) {
    val lowBatteryWarning: Boolean
        get() = live.batteryPercent <= batteryInfo.lowBatteryThreshold

    val hasFault: Boolean
        get() = status == SolarSystemStatus.FAULT || diy.faultCode != 0
}
