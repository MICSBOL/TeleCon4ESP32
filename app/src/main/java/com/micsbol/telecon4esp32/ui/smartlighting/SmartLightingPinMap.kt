package com.micsbol.telecon4esp32.ui.smartlighting

import androidx.annotation.StringRes
import com.micsbol.telecon4esp32.R

data class SmartLightingPinAssignment(
    @StringRes val elementLabelRes: Int,
    val gpio: Int,
    @StringRes val hardwareRes: Int,
)

data class SmartLightingPinSection(
    @StringRes val sectionTitleRes: Int,
    val assignments: List<SmartLightingPinAssignment>,
)

object SmartLightingPinMap {
    fun sections(): List<SmartLightingPinSection> = listOf(
        SmartLightingPinSection(
            sectionTitleRes = R.string.smart_lighting_pin_section_topbar,
            assignments = listOf(
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_pin_topbar_status,
                    gpio = 2,
                    hardwareRes = R.string.smart_lighting_pin_hw_status_led,
                ),
            ),
        ),
        SmartLightingPinSection(
            sectionTitleRes = R.string.smart_lighting_hero_active_lights,
            assignments = listOf(
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_pin_hero_count,
                    gpio = 0,
                    hardwareRes = R.string.smart_lighting_pin_hw_derived_count,
                ),
            ),
        ),
        SmartLightingPinSection(
            sectionTitleRes = R.string.smart_lighting_turn_all_on,
            assignments = listOf(
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_pin_all_lights,
                    gpio = 4,
                    hardwareRes = R.string.smart_lighting_pin_hw_master_relay,
                ),
            ),
        ),
        SmartLightingPinSection(
            sectionTitleRes = R.string.smart_lighting_section_connected,
            assignments = listOf(
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_device_living_room_dimmer,
                    gpio = 16,
                    hardwareRes = R.string.smart_lighting_pin_hw_pwm_dimmer,
                ),
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_device_kitchen_strip,
                    gpio = 17,
                    hardwareRes = R.string.smart_lighting_pin_hw_pwm_strip,
                ),
            ),
        ),
        SmartLightingPinSection(
            sectionTitleRes = R.string.smart_lighting_section_bedroom,
            assignments = listOf(
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_device_bedroom_lamp,
                    gpio = 18,
                    hardwareRes = R.string.smart_lighting_pin_hw_relay,
                ),
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_device_bedroom_ceiling,
                    gpio = 19,
                    hardwareRes = R.string.smart_lighting_pin_hw_relay,
                ),
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_device_bedroom_closet,
                    gpio = 21,
                    hardwareRes = R.string.smart_lighting_pin_hw_pwm_strip,
                ),
            ),
        ),
        SmartLightingPinSection(
            sectionTitleRes = R.string.smart_lighting_section_common_areas,
            assignments = listOf(
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_device_led_hallway,
                    gpio = 22,
                    hardwareRes = R.string.smart_lighting_pin_hw_pwm_strip,
                ),
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_device_led_dining_room,
                    gpio = 23,
                    hardwareRes = R.string.smart_lighting_pin_hw_pwm_strip,
                ),
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_device_led_staircase,
                    gpio = 25,
                    hardwareRes = R.string.smart_lighting_pin_hw_pwm_strip,
                ),
            ),
        ),
        SmartLightingPinSection(
            sectionTitleRes = R.string.smart_lighting_section_rooms,
            assignments = listOf(
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_device_led_bathroom,
                    gpio = 26,
                    hardwareRes = R.string.smart_lighting_pin_hw_relay,
                ),
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_device_led_office,
                    gpio = 13,
                    hardwareRes = R.string.smart_lighting_pin_hw_pwm_strip,
                ),
            ),
        ),
        SmartLightingPinSection(
            sectionTitleRes = R.string.smart_lighting_section_utility_outdoor,
            assignments = listOf(
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_device_led_garage,
                    gpio = 14,
                    hardwareRes = R.string.smart_lighting_pin_hw_relay,
                ),
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_device_led_patio,
                    gpio = 15,
                    hardwareRes = R.string.smart_lighting_pin_hw_relay,
                ),
            ),
        ),
        SmartLightingPinSection(
            sectionTitleRes = R.string.smart_lighting_section_settings,
            assignments = listOf(
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_setting_auto_off_away,
                    gpio = 32,
                    hardwareRes = R.string.smart_lighting_pin_hw_pir,
                ),
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_setting_motion_activation,
                    gpio = 32,
                    hardwareRes = R.string.smart_lighting_pin_hw_pir,
                ),
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_setting_sunset_sync,
                    gpio = 34,
                    hardwareRes = R.string.smart_lighting_pin_hw_ldr,
                ),
            ),
        ),
        SmartLightingPinSection(
            sectionTitleRes = R.string.smart_lighting_section_add_device,
            assignments = listOf(
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_add_pair_bluetooth,
                    gpio = 5,
                    hardwareRes = R.string.smart_lighting_pin_hw_pair_button,
                ),
                SmartLightingPinAssignment(
                    elementLabelRes = R.string.smart_lighting_add_scan_qr,
                    gpio = 0,
                    hardwareRes = R.string.smart_lighting_pin_hw_ble_only,
                ),
            ),
        ),
    )
}
