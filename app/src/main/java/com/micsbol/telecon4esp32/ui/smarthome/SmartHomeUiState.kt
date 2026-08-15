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
import com.micsbol.telecon4esp32.domain.bluetooth.sh.SmartHomeProtocol
import com.micsbol.telecon4esp32.ui.theme.PlotYellow

enum class RoomStatusBadge {
    ON_COUNT,
    OFF,
    OPEN,
}

data class RoomDeviceUiModel(
    val id: String,
    val icon: ImageVector,
    /** Main ceiling/strip light that drives the room photo on/off art. */
    val isLight: Boolean = false,
    /** Relay / lock the user can toggle over Bluetooth. */
    val isControllable: Boolean = false,
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

    fun withToggledDevice(deviceId: String): RoomUiModel {
        val device = devices.firstOrNull { it.id == deviceId && it.isControllable } ?: return this
        val nextOn = !device.isOn
        val newDevices = devices.map { candidate ->
            if (candidate.id == deviceId && candidate.isControllable) {
                candidate.copy(isOn = nextOn)
            } else {
                candidate
            }
        }
        return copyFromDevices(newDevices)
    }

    fun withDeviceStates(states: Map<String, Boolean>): RoomUiModel {
        if (states.isEmpty()) return this
        val newDevices = devices.map { device ->
            val next = states[device.id]
            if (next != null && device.isControllable) device.copy(isOn = next) else device
        }
        return copyFromDevices(newDevices)
    }

    fun withAllControllable(on: Boolean, predicate: (RoomDeviceUiModel) -> Boolean = { true }): RoomUiModel {
        val newDevices = devices.map { device ->
            if (device.isControllable && predicate(device)) device.copy(isOn = on) else device
        }
        return copyFromDevices(newDevices)
    }

    private fun copyFromDevices(newDevices: List<RoomDeviceUiModel>): RoomUiModel {
        val controllable = newDevices.filter { it.isControllable }
        val newOnCount = controllable.count { it.isOn }
        val newTotal = controllable.size
        val newLightsOn = newDevices.any { it.isLight && it.isOn }
        val newBadge = when {
            statusBadge == RoomStatusBadge.OPEN -> RoomStatusBadge.OPEN
            newOnCount > 0 -> RoomStatusBadge.ON_COUNT
            else -> RoomStatusBadge.OFF
        }
        return copy(
            devices = newDevices,
            lightsOn = newLightsOn,
            onCount = newOnCount,
            totalDeviceCount = newTotal,
            statusBadge = newBadge,
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

        fun empty(): EnergyChartData = EnergyChartData(
            powerSeries = listOf(0f, 0f),
            timeLabels = listOf("00:00", "24:00"),
            currentPowerKw = 0f,
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
    val isOnline: Boolean = false,
    val deviceId: String = "ESP32-SH01",
    val updatedAgo: String = "—",
    val lastTelemetryAtMs: Long = 0L,
    val allSystemsNormal: Boolean = true,
    val rooms: List<RoomUiModel> = defaultRooms(),
    val systems: List<SystemTileUiModel> = defaultSystems(),
    val energyChart: EnergyChartData = EnergyChartData.mock(),
    val recentEvents: List<RecentEventUiModel> = defaultEvents(),
)

private fun defaultRooms(): List<RoomUiModel> = listOf(
    RoomUiModel(
        id = SmartHomeProtocol.ROOM_LIVING,
        nameRes = R.string.smart_home_room_living_room,
        icon = Icons.Default.Weekend,
        imageOnRes = R.drawable.smart_light_living_room_on,
        imageOffRes = R.drawable.smart_light_living_room_off,
        lightsOn = true,
        accentColor = SmartHomeGlass.AccentGreenBright,
        statusBadge = RoomStatusBadge.ON_COUNT,
        onCount = 2,
        totalDeviceCount = 3,
        devices = listOf(
            RoomDeviceUiModel(id = "thermostat", icon = Icons.Default.Thermostat, isOn = true),
            RoomDeviceUiModel(
                id = SmartHomeProtocol.DEVICE_LIGHT,
                icon = Icons.Default.Lightbulb,
                isLight = true,
                isControllable = true,
                isOn = true,
            ),
            RoomDeviceUiModel(
                id = SmartHomeProtocol.DEVICE_AMBIENCE,
                icon = Icons.Default.LightMode,
                isControllable = true,
                isOn = true,
            ),
            RoomDeviceUiModel(
                id = SmartHomeProtocol.DEVICE_OUTLET,
                icon = Icons.Default.Outlet,
                isControllable = true,
            ),
        ),
        temperatureC = 21f,
        alertTextRes = R.string.smart_home_room_window_open,
    ),
    RoomUiModel(
        id = SmartHomeProtocol.ROOM_KITCHEN,
        nameRes = R.string.smart_home_room_kitchen,
        icon = Icons.Default.Kitchen,
        imageOnRes = R.drawable.smart_light_kitchen_strip_on,
        imageOffRes = R.drawable.smart_light_kitchen_strip_off,
        lightsOn = true,
        accentColor = SmartHomeGlass.AccentWarm,
        statusBadge = RoomStatusBadge.ON_COUNT,
        onCount = 1,
        totalDeviceCount = 3,
        devices = listOf(
            RoomDeviceUiModel(
                id = SmartHomeProtocol.DEVICE_APPLIANCE,
                icon = Icons.Default.Kitchen,
                isControllable = true,
            ),
            RoomDeviceUiModel(
                id = SmartHomeProtocol.DEVICE_LIGHT,
                icon = Icons.Default.Lightbulb,
                isLight = true,
                isControllable = true,
                isOn = true,
            ),
            RoomDeviceUiModel(id = "thermostat", icon = Icons.Default.Thermostat),
            RoomDeviceUiModel(
                id = SmartHomeProtocol.DEVICE_WATER,
                icon = Icons.Default.WaterDrop,
                isControllable = true,
            ),
        ),
        temperatureC = 23f,
    ),
    RoomUiModel(
        id = SmartHomeProtocol.ROOM_BEDROOM,
        nameRes = R.string.smart_home_room_bedroom,
        icon = Icons.Default.Bed,
        imageOnRes = R.drawable.smart_light_bedroom_ceiling_on,
        imageOffRes = R.drawable.smart_light_bedroom_ceiling_off,
        lightsOn = false,
        accentColor = SmartHomeGlass.AccentGreenMuted,
        statusBadge = RoomStatusBadge.OFF,
        totalDeviceCount = 1,
        devices = listOf(
            RoomDeviceUiModel(id = "bed", icon = Icons.Default.Bed),
            RoomDeviceUiModel(
                id = SmartHomeProtocol.DEVICE_LIGHT,
                icon = Icons.Default.Lightbulb,
                isLight = true,
                isControllable = true,
            ),
            RoomDeviceUiModel(id = "thermostat", icon = Icons.Default.Thermostat),
        ),
        temperatureC = 19f,
    ),
    RoomUiModel(
        id = SmartHomeProtocol.ROOM_GARAGE,
        nameRes = R.string.smart_home_room_garage,
        icon = Icons.Default.DirectionsCar,
        imageOnRes = R.drawable.smart_light_led_garage_on,
        imageOffRes = R.drawable.smart_light_led_garage_off,
        lightsOn = true,
        accentColor = SmartHomeGlass.AccentOrange,
        statusBadge = RoomStatusBadge.OPEN,
        onCount = 1,
        totalDeviceCount = 2,
        devices = listOf(
            RoomDeviceUiModel(id = "car", icon = Icons.Default.DirectionsCar),
            RoomDeviceUiModel(
                id = SmartHomeProtocol.DEVICE_LIGHT,
                icon = Icons.Default.Lightbulb,
                isLight = true,
                isControllable = true,
                isOn = true,
            ),
            RoomDeviceUiModel(
                id = SmartHomeProtocol.DEVICE_LOCK,
                icon = Icons.Default.Lock,
                isControllable = true,
            ),
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
