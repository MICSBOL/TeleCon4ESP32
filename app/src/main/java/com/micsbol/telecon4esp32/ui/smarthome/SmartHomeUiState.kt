package com.micsbol.telecon4esp32.ui.smarthome

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MotionPhotosAuto
import androidx.compose.material.icons.filled.Outlet
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Window
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.smarthome.SmartHomeGlass
import com.micsbol.telecon4esp32.ui.theme.PlotYellow

enum class RoomStatusBadge {
    ON_COUNT,
    OFF,
    OPEN,
}

data class RoomDeviceUiModel(
    val id: String,
    val icon: ImageVector,
    val isLight: Boolean = false,
    val isOn: Boolean = false,
)

data class RoomUiModel(
    val id: String,
    @StringRes val nameRes: Int,
    val icon: ImageVector,
    @DrawableRes val imageOnRes: Int,
    @DrawableRes val imageOffRes: Int,
    val lightsOn: Boolean,
    val accentColor: Color,
    val statusBadge: RoomStatusBadge,
    val onCount: Int = 0,
    val totalDeviceCount: Int = 0,
    val devices: List<RoomDeviceUiModel> = emptyList(),
    val temperatureC: Float,
    @StringRes val alertTextRes: Int? = null,
) {
    @get:DrawableRes
    val displayImageRes: Int
        get() = if (lightsOn) imageOnRes else imageOffRes

    fun withToggledLight(): RoomUiModel {
        val newLightsOn = !lightsOn
        val lightDelta = if (newLightsOn) 1 else -1
        val newOnCount = (onCount + lightDelta).coerceIn(0, totalDeviceCount)
        val newBadge = when {
            statusBadge == RoomStatusBadge.OPEN -> RoomStatusBadge.OPEN
            newOnCount > 0 -> RoomStatusBadge.ON_COUNT
            else -> RoomStatusBadge.OFF
        }
        return copy(
            lightsOn = newLightsOn,
            onCount = newOnCount,
            statusBadge = newBadge,
            devices = devices.map { device ->
                if (device.isLight) device.copy(isOn = newLightsOn) else device
            },
        )
    }
}

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
        imageOnRes = R.drawable.smart_light_living_room_on,
        imageOffRes = R.drawable.smart_light_living_room_off,
        lightsOn = true,
        accentColor = SmartHomeGlass.AccentGreenBright,
        statusBadge = RoomStatusBadge.ON_COUNT,
        onCount = 2,
        totalDeviceCount = 5,
        devices = listOf(
            RoomDeviceUiModel(id = "thermostat", icon = Icons.Default.Thermostat, isOn = true),
            RoomDeviceUiModel(id = "light", icon = Icons.Default.Lightbulb, isLight = true, isOn = true),
            RoomDeviceUiModel(id = "ambience", icon = Icons.Default.LightMode, isOn = true),
            RoomDeviceUiModel(id = "outlet", icon = Icons.Default.Outlet),
        ),
        temperatureC = 21f,
        alertTextRes = R.string.smart_home_room_window_open,
    ),
    RoomUiModel(
        id = "kitchen",
        nameRes = R.string.smart_home_room_kitchen,
        icon = Icons.Default.Kitchen,
        imageOnRes = R.drawable.smart_light_kitchen_strip_on,
        imageOffRes = R.drawable.smart_light_kitchen_strip_off,
        lightsOn = true,
        accentColor = SmartHomeGlass.AccentWarm,
        statusBadge = RoomStatusBadge.ON_COUNT,
        onCount = 1,
        totalDeviceCount = 4,
        devices = listOf(
            RoomDeviceUiModel(id = "appliance", icon = Icons.Default.Kitchen),
            RoomDeviceUiModel(id = "light", icon = Icons.Default.Lightbulb, isLight = true, isOn = true),
            RoomDeviceUiModel(id = "thermostat", icon = Icons.Default.Thermostat),
            RoomDeviceUiModel(id = "water", icon = Icons.Default.WaterDrop),
        ),
        temperatureC = 23f,
    ),
    RoomUiModel(
        id = "bedroom",
        nameRes = R.string.smart_home_room_bedroom,
        icon = Icons.Default.Bed,
        imageOnRes = R.drawable.smart_light_bedroom_ceiling_on,
        imageOffRes = R.drawable.smart_light_bedroom_ceiling_off,
        lightsOn = false,
        accentColor = SmartHomeGlass.AccentGreenMuted,
        statusBadge = RoomStatusBadge.OFF,
        totalDeviceCount = 3,
        devices = listOf(
            RoomDeviceUiModel(id = "bed", icon = Icons.Default.Bed),
            RoomDeviceUiModel(id = "light", icon = Icons.Default.Lightbulb, isLight = true),
            RoomDeviceUiModel(id = "thermostat", icon = Icons.Default.Thermostat),
        ),
        temperatureC = 19f,
    ),
    RoomUiModel(
        id = "garage",
        nameRes = R.string.smart_home_room_garage,
        icon = Icons.Default.DirectionsCar,
        imageOnRes = R.drawable.smart_light_led_garage_on,
        imageOffRes = R.drawable.smart_light_led_garage_off,
        lightsOn = true,
        accentColor = SmartHomeGlass.AccentOrange,
        statusBadge = RoomStatusBadge.OPEN,
        onCount = 1,
        totalDeviceCount = 3,
        devices = listOf(
            RoomDeviceUiModel(id = "car", icon = Icons.Default.DirectionsCar),
            RoomDeviceUiModel(id = "light", icon = Icons.Default.Lightbulb, isLight = true, isOn = true),
            RoomDeviceUiModel(id = "lock", icon = Icons.Default.Lock),
            RoomDeviceUiModel(id = "motion", icon = Icons.Default.MotionPhotosAuto),
        ),
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
        iconTint = SmartHomeGlass.AccentGreenBright,
        statusColor = SmartHomeGlass.AccentGreenBright,
    ),
    SystemTileUiModel(
        id = "energy",
        titleRes = R.string.smart_home_system_energy,
        value = "1.8 kW",
        statusRes = R.string.smart_home_system_energy_now,
        icon = Icons.Default.Bolt,
        iconTint = PlotYellow,
        statusColor = SmartHomeGlass.ChartLine,
    ),
    SystemTileUiModel(
        id = "security",
        titleRes = R.string.smart_home_system_security,
        value = "",
        statusRes = R.string.smart_home_system_security_status,
        icon = Icons.Default.Lock,
        iconTint = SmartHomeGlass.AccentGreenBright,
        statusColor = SmartHomeGlass.AccentGreenBright,
    ),
    SystemTileUiModel(
        id = "water",
        titleRes = R.string.smart_home_system_water,
        value = "42 L",
        statusRes = R.string.smart_home_system_water_today,
        icon = Icons.Default.WaterDrop,
        iconTint = SmartHomeGlass.AccentWarm,
        statusColor = SmartHomeGlass.AccentWarm,
    ),
)

private fun defaultEvents(): List<RecentEventUiModel> = listOf(
    RecentEventUiModel(
        id = "door_locked",
        titleRes = R.string.smart_home_event_front_door_locked,
        time = "9:30 AM",
        icon = Icons.Default.Lock,
        iconTint = SmartHomeGlass.AccentGreenBright,
    ),
    RecentEventUiModel(
        id = "light_on",
        titleRes = R.string.smart_home_event_living_room_light_on,
        time = "9:15 AM",
        icon = Icons.Default.Lightbulb,
        iconTint = SmartHomeGlass.AccentWarm,
    ),
    RecentEventUiModel(
        id = "motion",
        titleRes = R.string.smart_home_event_motion_driveway,
        time = "8:47 AM",
        icon = Icons.Default.MotionPhotosAuto,
        iconTint = SmartHomeGlass.AccentGreenBright,
    ),
    RecentEventUiModel(
        id = "windows",
        titleRes = R.string.smart_home_event_windows_living_room,
        time = "8:20 AM",
        icon = Icons.Default.Window,
        iconTint = SmartHomeGlass.AccentOrange,
    ),
    RecentEventUiModel(
        id = "water",
        titleRes = R.string.smart_home_event_water_usage,
        time = "7:30 AM",
        icon = Icons.Default.WaterDrop,
        iconTint = SmartHomeGlass.AccentWarm,
    ),
)
