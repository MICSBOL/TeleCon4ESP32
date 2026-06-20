package com.micsbol.telecon4esp32.ui.watertank

enum class TankChartPeriod {
    HOURS_24,
    DAYS_7,
    DAYS_30,
}

data class TankLevelChartData(
    val levelSeries: List<Float>,
    val timeLabels: List<String>,
    val averagePercent: Int,
    val minimumPercent: Int,
    val minimumLabel: String,
    val maximumPercent: Int,
    val maximumLabel: String,
) {
    companion object {
        fun mock(period: TankChartPeriod): TankLevelChartData = when (period) {
            TankChartPeriod.HOURS_24 -> TankLevelChartData(
                levelSeries = listOf(
                    72f, 71f, 70f, 69f, 68f, 67f, 66f, 65f, 64f, 63f,
                    62f, 61f, 60f, 59f, 58f, 59f, 60f, 62f, 65f, 68f,
                    70f, 72f, 73f, 74f, 74f,
                ),
                timeLabels = listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "24:00"),
                averagePercent = 66,
                minimumPercent = 58,
                minimumLabel = "04:00",
                maximumPercent = 74,
                maximumLabel = "Now",
            )
            TankChartPeriod.DAYS_7 -> TankLevelChartData(
                levelSeries = listOf(65f, 62f, 58f, 61f, 68f, 75f, 74f),
                timeLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                averagePercent = 71,
                minimumPercent = 58,
                minimumLabel = "Wed",
                maximumPercent = 89,
                maximumLabel = "Sat",
            )
            TankChartPeriod.DAYS_30 -> TankLevelChartData(
                levelSeries = listOf(
                    80f, 78f, 75f, 72f, 70f, 68f, 65f, 62f, 60f, 58f,
                    55f, 58f, 62f, 65f, 68f, 70f, 72f, 74f, 76f, 78f,
                    80f, 82f, 85f, 87f, 89f, 88f, 85f, 82f, 78f, 74f,
                ),
                timeLabels = listOf("W1", "W2", "W3", "W4"),
                averagePercent = 71,
                minimumPercent = 55,
                minimumLabel = "W2",
                maximumPercent = 89,
                maximumLabel = "W3",
            )
        }
    }
}

enum class TankStatus {
    NORMAL,
    LOW,
    CRITICAL,
}

data class WaterTankUiState(
    val isOnline: Boolean = true,
    val isConnected: Boolean = true,
    val deviceId: String = "ESP32 Sensor",
    val levelPercent: Int = 74,
    val capacityLiters: Int = 500,
    val updatedAgo: String = "2s",
    val lastUpdatedTime: String = "10:30 AM",
    val pumpOn: Boolean = false,
    val tankStatus: TankStatus = TankStatus.NORMAL,
    val chartPeriod: TankChartPeriod = TankChartPeriod.DAYS_7,
    val chartData: TankLevelChartData = TankLevelChartData.mock(TankChartPeriod.DAYS_7),
) {
    val volumeLiters: Int
        get() = (capacityLiters * levelPercent / 100f).toInt()
}
