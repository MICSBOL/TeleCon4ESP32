package com.micsbol.telecon4esp32.ui.smarthome

import androidx.annotation.StringRes
import com.micsbol.telecon4esp32.R

data class SmartHomePinAssignment(
    @StringRes val elementLabelRes: Int,
    val gpio: Int,
    @StringRes val hardwareRes: Int,
)

data class SmartHomePinSection(
    @StringRes val sectionTitleRes: Int,
    val assignments: List<SmartHomePinAssignment>,
)

object SmartHomePinMap {
    fun sections(): List<SmartHomePinSection> = listOf(
        SmartHomePinSection(
            sectionTitleRes = R.string.smart_home_pin_section_topbar,
            assignments = listOf(
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_topbar_status,
                    gpio = 2,
                    hardwareRes = R.string.smart_home_pin_hw_status_led,
                ),
            ),
        ),
        SmartHomePinSection(
            sectionTitleRes = R.string.smart_home_section_rooms,
            assignments = listOf(
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_living_light,
                    gpio = 16,
                    hardwareRes = R.string.smart_home_pin_hw_relay,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_living_ambience,
                    gpio = 17,
                    hardwareRes = R.string.smart_home_pin_hw_relay,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_living_outlet,
                    gpio = 15,
                    hardwareRes = R.string.smart_home_pin_hw_relay,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_living_thermostat,
                    gpio = 4,
                    hardwareRes = R.string.smart_home_pin_hw_dht22,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_living_window,
                    gpio = 32,
                    hardwareRes = R.string.smart_home_pin_hw_reed,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_kitchen_light,
                    gpio = 19,
                    hardwareRes = R.string.smart_home_pin_hw_relay,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_kitchen_appliance,
                    gpio = 23,
                    hardwareRes = R.string.smart_home_pin_hw_relay,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_kitchen_thermostat,
                    gpio = 5,
                    hardwareRes = R.string.smart_home_pin_hw_dht22,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_kitchen_water,
                    gpio = 14,
                    hardwareRes = R.string.smart_home_pin_hw_valve,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_bedroom_light,
                    gpio = 18,
                    hardwareRes = R.string.smart_home_pin_hw_relay,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_bedroom_thermostat,
                    gpio = 12,
                    hardwareRes = R.string.smart_home_pin_hw_dht22,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_garage_light,
                    gpio = 13,
                    hardwareRes = R.string.smart_home_pin_hw_relay,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_garage_door,
                    gpio = 33,
                    hardwareRes = R.string.smart_home_pin_hw_reed,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_garage_motion,
                    gpio = 25,
                    hardwareRes = R.string.smart_home_pin_hw_pir,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_garage_lock,
                    gpio = 26,
                    hardwareRes = R.string.smart_home_pin_hw_lock_relay,
                ),
            ),
        ),
        SmartHomePinSection(
            sectionTitleRes = R.string.smart_home_section_systems,
            assignments = listOf(
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_system_climate,
                    gpio = 4,
                    hardwareRes = R.string.smart_home_pin_hw_dht22_avg,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_system_energy,
                    gpio = 21,
                    hardwareRes = R.string.smart_home_pin_hw_ina219_sda,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_energy_scl,
                    gpio = 22,
                    hardwareRes = R.string.smart_home_pin_hw_ina219_scl,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_system_security,
                    gpio = 32,
                    hardwareRes = R.string.smart_home_pin_hw_security_inputs,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_system_water,
                    gpio = 27,
                    hardwareRes = R.string.smart_home_pin_hw_flow_pulse,
                ),
            ),
        ),
        SmartHomePinSection(
            sectionTitleRes = R.string.smart_home_section_energy_usage,
            assignments = listOf(
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_pin_energy_chart,
                    gpio = 21,
                    hardwareRes = R.string.smart_home_pin_hw_ina219_power,
                ),
            ),
        ),
        SmartHomePinSection(
            sectionTitleRes = R.string.smart_home_section_recent_events,
            assignments = listOf(
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_event_front_door_locked,
                    gpio = 26,
                    hardwareRes = R.string.smart_home_pin_hw_lock_relay,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_event_living_room_light_on,
                    gpio = 16,
                    hardwareRes = R.string.smart_home_pin_hw_relay,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_event_motion_driveway,
                    gpio = 25,
                    hardwareRes = R.string.smart_home_pin_hw_pir,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_event_windows_living_room,
                    gpio = 32,
                    hardwareRes = R.string.smart_home_pin_hw_reed,
                ),
                SmartHomePinAssignment(
                    elementLabelRes = R.string.smart_home_event_water_usage,
                    gpio = 27,
                    hardwareRes = R.string.smart_home_pin_hw_flow_pulse,
                ),
            ),
        ),
    )
}
