package com.micsbol.telecon4esp32.ui.greenhouse

data class EnvironmentalChartData(
    val tempSeries: List<Float>,
    val humiditySeries: List<Float>,
    val vpdSeries: List<Float>,
    val timeLabels: List<String>,
) {
    companion object {
        fun mock(): EnvironmentalChartData = EnvironmentalChartData(
            tempSeries = listOf(
                18f, 17.5f, 17f, 16.8f, 17.2f, 19f, 22f, 25f, 27f, 28.5f,
                29f, 28f, 27.5f, 27f, 26.5f, 26f, 25.5f, 24f, 22f, 20f,
                19f, 18.5f, 18f, 17.5f, 17f,
            ),
            humiditySeries = listOf(
                72f, 74f, 76f, 78f, 80f, 78f, 72f, 65f, 60f, 58f,
                55f, 56f, 58f, 60f, 62f, 64f, 66f, 68f, 70f, 72f,
                73f, 74f, 75f, 74f, 72f,
            ),
            vpdSeries = listOf(
                0.4f, 0.35f, 0.3f, 0.28f, 0.3f, 0.45f, 0.7f, 1.0f, 1.3f, 1.5f,
                1.6f, 1.4f, 1.2f, 1.1f, 1.0f, 0.95f, 0.9f, 0.85f, 0.7f, 0.55f,
                0.45f, 0.4f, 0.38f, 0.35f, 0.32f,
            ),
            timeLabels = listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "24:00"),
        )
    }
}

data class GreenhouseUiState(
    val isOnline: Boolean = false,
    val isAutoMode: Boolean = true,
    val deviceId: String = "ESP32-GH01",
    val updatedAgo: String = "—",
    val lastTelemetryAtMs: Long = 0L,
    val temperatureC: Float = 26.2f,
    val humidityPercent: Int = 68,
    val vpdKpa: Float = 1.1f,
    val isStable: Boolean = true,
    val soilPercent: Int = 42,
    val soilTargetMin: Int = 40,
    val soilTargetMax: Int = 55,
    val lightLux: Float = 12_400f,
    val lightsOn: Boolean = false,
    val deltaTempC: Float = 3.2f,
    val ventOpenPercent: Int = 40,
    val tankPercent: Int = 78,
    val lastIrrigationAgo: String = "6h",
    val chartData: EnvironmentalChartData = EnvironmentalChartData.mock(),
    val fanOn: Boolean = true,
    val heaterOn: Boolean = false,
    val pumpOn: Boolean = false,
    val targetTempC: Int = 24,
    val targetHumidityPercent: Int = 65,
)
