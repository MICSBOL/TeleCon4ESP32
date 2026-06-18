package com.micsbol.telecon4esp32.ui.smarthome

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MotionPhotosAuto
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Window
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.theme.PlotOrange
import com.micsbol.telecon4esp32.ui.theme.PlotYellow
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.TechBlueBright
import com.micsbol.telecon4esp32.ui.theme.TechCyanBright

enum class RoomStatusBadge {
    ON_COUNT,
    OFF,
    OPEN,
}

data class RoomUiModel(
    val id: String,
    @StringRes val nameRes: Int,
    val icon: ImageVector,
    val accentColor: Color,
    val statusBadge: RoomStatusBadge,
    val onCount: Int = 0,
    val temperatureC: Float,
    @StringRes val alertTextRes: Int? = null,
)

data class SystemTileUiModel(
    val id: String,
    @StringRes val titleRes: Int,
    val value: String,
    @StringRes val statusRes: Int,
    val icon: ImageVector,
    val iconTint: Color,
    val statusColor: Color,
)

data class EnergyChartData(
    val powerSeries: List<Float>,
    val timeLabels: List<String>,
    val currentPowerKw: Float,
) {
    companion object {
        fun mock(): EnergyChartData = EnergyChartData(
            powerSeries = listOf(
                0.4f, 0.3f, 0.3f, 0.4f, 0.6f, 0.9f, 1.2f, 1.6f, 2.0f, 2.2f,
                2.4f, 2.1f, 1.9f, 1.8f, 1.7f, 1.6f, 1.5f, 1.4f, 1.2f, 1.0f,
                0.8f, 0.7f, 0.6f, 0.5f, 0.4f,
            ),
            timeLabels = listOf("00:00", "06:00", "12:00", "18:00", "24:00"),
            currentPowerKw = 1.8f,
        )
    }
}

data class RecentEventUiModel(
    val id: String,
    @StringRes val titleRes: Int,
    val time: String,
    val icon: ImageVector,
    val iconTint: Color,
)

data class SmartHomeUiState(
    val allSystemsNormal: Boolean = true,
    val rooms: List<RoomUiModel> = defaultRooms(),
    val systems: List<SystemTileUiModel> = defaultSystems(),
    val energyChart: EnergyChartData = EnergyChartData.mock(),
    val recentEvents: List<RecentEventUiModel> = defaultEvents(),
)

private fun defaultRooms(): List<RoomUiModel> = listOf(
    RoomUiModel(
        id = "living_room",
        nameRes = R.string.smart_home_room_living_room,
        icon = Icons.Default.Weekend,
        accentColor = TechBlueBright,
        statusBadge = RoomStatusBadge.ON_COUNT,
        onCount = 2,
        temperatureC = 21f,
        alertTextRes = R.string.smart_home_room_window_open,
    ),
    RoomUiModel(
        id = "kitchen",
        nameRes = R.string.smart_home_room_kitchen,
        icon = Icons.Default.Kitchen,
        accentColor = PlotYellow,
        statusBadge = RoomStatusBadge.ON_COUNT,
        onCount = 1,
        temperatureC = 23f,
    ),
    RoomUiModel(
        id = "bedroom",
        nameRes = R.string.smart_home_room_bedroom,
        icon = Icons.Default.Bed,
        accentColor = TechCyanBright,
        statusBadge = RoomStatusBadge.OFF,
        temperatureC = 19f,
    ),
    RoomUiModel(
        id = "garage",
        nameRes = R.string.smart_home_room_garage,
        icon = Icons.Default.DirectionsCar,
        accentColor = PlotOrange,
        statusBadge = RoomStatusBadge.OPEN,
        temperatureC = 18f,
        alertTextRes = R.string.smart_home_room_door_open,
    ),
)

private fun defaultSystems(): List<SystemTileUiModel> = listOf(
    SystemTileUiModel(
        id = "climate",
        titleRes = R.string.smart_home_system_climate,
        value = "22°C",
        statusRes = R.string.smart_home_system_climate_status,
        icon = Icons.Default.Thermostat,
        iconTint = StatusConnected,
        statusColor = StatusConnected,
    ),
    SystemTileUiModel(
        id = "energy",
        titleRes = R.string.smart_home_system_energy,
        value = "1.8 kW",
        statusRes = R.string.smart_home_system_energy_now,
        icon = Icons.Default.Bolt,
        iconTint = PlotYellow,
        statusColor = PlotOrange,
    ),
    SystemTileUiModel(
        id = "security",
        titleRes = R.string.smart_home_system_security,
        value = "",
        statusRes = R.string.smart_home_system_security_status,
        icon = Icons.Default.Lock,
        iconTint = StatusConnected,
        statusColor = StatusConnected,
    ),
    SystemTileUiModel(
        id = "water",
        titleRes = R.string.smart_home_system_water,
        value = "42 L",
        statusRes = R.string.smart_home_system_water_today,
        icon = Icons.Default.WaterDrop,
        iconTint = TechCyanBright,
        statusColor = TechCyanBright,
    ),
)

private fun defaultEvents(): List<RecentEventUiModel> = listOf(
    RecentEventUiModel(
        id = "door_locked",
        titleRes = R.string.smart_home_event_front_door_locked,
        time = "9:30 AM",
        icon = Icons.Default.Lock,
        iconTint = StatusConnected,
    ),
    RecentEventUiModel(
        id = "light_on",
        titleRes = R.string.smart_home_event_living_room_light_on,
        time = "9:15 AM",
        icon = Icons.Default.Lightbulb,
        iconTint = PlotYellow,
    ),
    RecentEventUiModel(
        id = "motion",
        titleRes = R.string.smart_home_event_motion_driveway,
        time = "8:47 AM",
        icon = Icons.Default.MotionPhotosAuto,
        iconTint = TechBlueBright,
    ),
    RecentEventUiModel(
        id = "windows",
        titleRes = R.string.smart_home_event_windows_living_room,
        time = "8:20 AM",
        icon = Icons.Default.Window,
        iconTint = TechCyanBright,
    ),
    RecentEventUiModel(
        id = "water",
        titleRes = R.string.smart_home_event_water_usage,
        time = "7:30 AM",
        icon = Icons.Default.WaterDrop,
        iconTint = TechCyanBright,
    ),
)
