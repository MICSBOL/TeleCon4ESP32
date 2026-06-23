package com.micsbol.telecon4esp32.ui.smartlighting

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.theme.TechBlueBright
import com.micsbol.telecon4esp32.ui.theme.TechCyanBright

enum class LightingDeviceCategory {
    GENERAL,
    BEDROOM,
    COMMON_AREAS,
    ROOMS,
    UTILITY_OUTDOOR,
}

fun LightingDeviceCategory.sectionTitleRes(): Int = when (this) {
    LightingDeviceCategory.GENERAL -> R.string.smart_lighting_section_connected
    LightingDeviceCategory.BEDROOM -> R.string.smart_lighting_section_bedroom
    LightingDeviceCategory.COMMON_AREAS -> R.string.smart_lighting_section_common_areas
    LightingDeviceCategory.ROOMS -> R.string.smart_lighting_section_rooms
    LightingDeviceCategory.UTILITY_OUTDOOR -> R.string.smart_lighting_section_utility_outdoor
}

val lightingDeviceSectionOrder: List<LightingDeviceCategory> = listOf(
    LightingDeviceCategory.GENERAL,
    LightingDeviceCategory.BEDROOM,
    LightingDeviceCategory.COMMON_AREAS,
    LightingDeviceCategory.ROOMS,
    LightingDeviceCategory.UTILITY_OUTDOOR,
)

data class ConnectedLightingDeviceUiModel(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val typeRes: Int,
    val category: LightingDeviceCategory = LightingDeviceCategory.GENERAL,
    val isOnline: Boolean,
    val isOn: Boolean,
    val signalPercent: Int,
    @DrawableRes val thumbnailOnRes: Int,
    @DrawableRes val thumbnailOffRes: Int,
)

data class AddLightingDeviceOptionUiModel(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
    val accentColor: Color,
)

enum class LightingSettingToggleColor {
    GREEN,
    BLUE,
}

data class LightingSettingUiModel(
    val id: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val isEnabled: Boolean,
    val toggleColor: LightingSettingToggleColor = LightingSettingToggleColor.GREEN,
)

data class SmartLightingUiState(
    val connectedDevices: List<ConnectedLightingDeviceUiModel> = defaultConnectedDevices(),
    val addDeviceOptions: List<AddLightingDeviceOptionUiModel> = defaultAddDeviceOptions(),
    val settings: List<LightingSettingUiModel> = defaultSettings(),
) {
    val allDevicesOn: Boolean
        get() = connectedDevices.isNotEmpty() && connectedDevices.all { it.isOn }
}

private fun defaultConnectedDevices(): List<ConnectedLightingDeviceUiModel> = listOf(
    ConnectedLightingDeviceUiModel(
        id = "living_room_dimmer",
        nameRes = R.string.smart_lighting_device_living_room_dimmer,
        typeRes = R.string.smart_lighting_device_type_smart_bulb,
        isOnline = true,
        isOn = true,
        signalPercent = 98,
        thumbnailOnRes = R.drawable.smart_light_living_room_on,
        thumbnailOffRes = R.drawable.smart_light_living_room_off,
    ),
    ConnectedLightingDeviceUiModel(
        id = "kitchen_strip",
        nameRes = R.string.smart_lighting_device_kitchen_strip,
        typeRes = R.string.smart_lighting_device_type_led_strip,
        isOnline = true,
        isOn = true,
        signalPercent = 92,
        thumbnailOnRes = R.drawable.smart_light_kitchen_strip_on,
        thumbnailOffRes = R.drawable.smart_light_kitchen_strip_off,
    ),
    ConnectedLightingDeviceUiModel(
        id = "bedroom_lamp",
        nameRes = R.string.smart_lighting_device_bedroom_lamp,
        typeRes = R.string.smart_lighting_device_type_smart_bulb,
        category = LightingDeviceCategory.BEDROOM,
        isOnline = true,
        isOn = false,
        signalPercent = 85,
        thumbnailOnRes = R.drawable.smart_light_bedroom_lamp_on,
        thumbnailOffRes = R.drawable.smart_light_bedroom_lamp_off,
    ),
    ConnectedLightingDeviceUiModel(
        id = "bedroom_ceiling",
        nameRes = R.string.smart_lighting_device_bedroom_ceiling,
        typeRes = R.string.smart_lighting_device_type_ceiling_light,
        category = LightingDeviceCategory.BEDROOM,
        isOnline = true,
        isOn = true,
        signalPercent = 88,
        thumbnailOnRes = R.drawable.smart_light_bedroom_ceiling_on,
        thumbnailOffRes = R.drawable.smart_light_bedroom_ceiling_off,
    ),
    ConnectedLightingDeviceUiModel(
        id = "bedroom_closet",
        nameRes = R.string.smart_lighting_device_bedroom_closet,
        typeRes = R.string.smart_lighting_device_type_led_strip,
        category = LightingDeviceCategory.BEDROOM,
        isOnline = true,
        isOn = false,
        signalPercent = 81,
        thumbnailOnRes = R.drawable.smart_light_bedroom_closet_on,
        thumbnailOffRes = R.drawable.smart_light_bedroom_closet_off,
    ),
    ConnectedLightingDeviceUiModel(
        id = "led_hallway",
        nameRes = R.string.smart_lighting_device_led_hallway,
        typeRes = R.string.smart_lighting_device_type_led_strip,
        category = LightingDeviceCategory.COMMON_AREAS,
        isOnline = true,
        isOn = true,
        signalPercent = 94,
        thumbnailOnRes = R.drawable.smart_light_led_hallway_on,
        thumbnailOffRes = R.drawable.smart_light_led_hallway_off,
    ),
    ConnectedLightingDeviceUiModel(
        id = "led_dining_room",
        nameRes = R.string.smart_lighting_device_led_dining_room,
        typeRes = R.string.smart_lighting_device_type_led_strip,
        category = LightingDeviceCategory.COMMON_AREAS,
        isOnline = true,
        isOn = false,
        signalPercent = 90,
        thumbnailOnRes = R.drawable.smart_light_led_dining_room_on,
        thumbnailOffRes = R.drawable.smart_light_led_dining_room_off,
    ),
    ConnectedLightingDeviceUiModel(
        id = "led_staircase",
        nameRes = R.string.smart_lighting_device_led_staircase,
        typeRes = R.string.smart_lighting_device_type_led_strip,
        category = LightingDeviceCategory.COMMON_AREAS,
        isOnline = true,
        isOn = true,
        signalPercent = 87,
        thumbnailOnRes = R.drawable.smart_light_led_staircase_on,
        thumbnailOffRes = R.drawable.smart_light_led_staircase_off,
    ),
    ConnectedLightingDeviceUiModel(
        id = "led_bathroom",
        nameRes = R.string.smart_lighting_device_led_bathroom,
        typeRes = R.string.smart_lighting_device_type_led_strip,
        category = LightingDeviceCategory.ROOMS,
        isOnline = true,
        isOn = true,
        signalPercent = 91,
        thumbnailOnRes = R.drawable.smart_light_led_bathroom_on,
        thumbnailOffRes = R.drawable.smart_light_led_bathroom_off,
    ),
    ConnectedLightingDeviceUiModel(
        id = "led_office",
        nameRes = R.string.smart_lighting_device_led_office,
        typeRes = R.string.smart_lighting_device_type_led_strip,
        category = LightingDeviceCategory.ROOMS,
        isOnline = true,
        isOn = false,
        signalPercent = 89,
        thumbnailOnRes = R.drawable.smart_light_led_office_on,
        thumbnailOffRes = R.drawable.smart_light_led_office_off,
    ),
    ConnectedLightingDeviceUiModel(
        id = "led_garage",
        nameRes = R.string.smart_lighting_device_led_garage,
        typeRes = R.string.smart_lighting_device_type_led_strip,
        category = LightingDeviceCategory.UTILITY_OUTDOOR,
        isOnline = true,
        isOn = true,
        signalPercent = 83,
        thumbnailOnRes = R.drawable.smart_light_led_garage_on,
        thumbnailOffRes = R.drawable.smart_light_led_garage_off,
    ),
    ConnectedLightingDeviceUiModel(
        id = "led_patio",
        nameRes = R.string.smart_lighting_device_led_patio,
        typeRes = R.string.smart_lighting_device_type_led_strip,
        category = LightingDeviceCategory.UTILITY_OUTDOOR,
        isOnline = true,
        isOn = false,
        signalPercent = 79,
        thumbnailOnRes = R.drawable.smart_light_led_patio_on,
        thumbnailOffRes = R.drawable.smart_light_led_patio_off,
    ),
)

private fun defaultAddDeviceOptions(): List<AddLightingDeviceOptionUiModel> = listOf(
    AddLightingDeviceOptionUiModel(
        id = "smart_bulb",
        titleRes = R.string.smart_lighting_add_smart_bulb,
        subtitleRes = R.string.smart_lighting_add_pair_bluetooth,
        icon = Icons.Default.Lightbulb,
        accentColor = TechBlueBright,
    ),
    AddLightingDeviceOptionUiModel(
        id = "led_strip",
        titleRes = R.string.smart_lighting_add_led_strip,
        subtitleRes = R.string.smart_lighting_add_scan_qr,
        icon = Icons.Default.LightMode,
        accentColor = TechCyanBright,
    ),
)

private fun defaultSettings(): List<LightingSettingUiModel> = listOf(
    LightingSettingUiModel(
        id = "auto_off_away",
        titleRes = R.string.smart_lighting_setting_auto_off_away,
        icon = Icons.Default.Home,
        isEnabled = true,
    ),
    LightingSettingUiModel(
        id = "motion_activation",
        titleRes = R.string.smart_lighting_setting_motion_activation,
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        isEnabled = true,
    ),
    LightingSettingUiModel(
        id = "sunset_sync",
        titleRes = R.string.smart_lighting_setting_sunset_sync,
        icon = Icons.Default.WbTwilight,
        isEnabled = true,
        toggleColor = LightingSettingToggleColor.BLUE,
    ),
)
