package com.micsbol.telecon4esp32.ui.watertank

import androidx.annotation.StringRes
import com.micsbol.telecon4esp32.R

data class WaterTankPinAssignment(
    @StringRes val elementLabelRes: Int,
    val gpio: Int,
    @StringRes val hardwareRes: Int,
)

data class WaterTankPinSection(
    @StringRes val sectionTitleRes: Int,
    val assignments: List<WaterTankPinAssignment>,
)

object WaterTankPinMap {
    fun sections(): List<WaterTankPinSection> = listOf(
        WaterTankPinSection(
            sectionTitleRes = R.string.water_tank_pin_section_topbar,
            assignments = listOf(
                WaterTankPinAssignment(
                    elementLabelRes = R.string.water_tank_pin_topbar_connection,
                    gpio = 2,
                    hardwareRes = R.string.water_tank_pin_hw_status_led,
                ),
            ),
        ),
        WaterTankPinSection(
            sectionTitleRes = R.string.water_tank_current_level,
            assignments = listOf(
                WaterTankPinAssignment(
                    elementLabelRes = R.string.water_tank_pin_level_percent,
                    gpio = 27,
                    hardwareRes = R.string.water_tank_pin_hw_ultrasonic_trig,
                ),
                WaterTankPinAssignment(
                    elementLabelRes = R.string.water_tank_pin_level_echo,
                    gpio = 33,
                    hardwareRes = R.string.water_tank_pin_hw_ultrasonic_echo,
                ),
                WaterTankPinAssignment(
                    elementLabelRes = R.string.water_tank_pin_level_analog,
                    gpio = 34,
                    hardwareRes = R.string.water_tank_pin_hw_analog_level,
                ),
            ),
        ),
        WaterTankPinSection(
            sectionTitleRes = R.string.water_tank_pin_section_metrics,
            assignments = listOf(
                WaterTankPinAssignment(
                    elementLabelRes = R.string.water_tank_volume,
                    gpio = 27,
                    hardwareRes = R.string.water_tank_pin_hw_level_derived,
                ),
                WaterTankPinAssignment(
                    elementLabelRes = R.string.water_tank_capacity,
                    gpio = 0,
                    hardwareRes = R.string.water_tank_pin_hw_nvs_capacity,
                ),
            ),
        ),
        WaterTankPinSection(
            sectionTitleRes = R.string.water_tank_pin_section_status,
            assignments = listOf(
                WaterTankPinAssignment(
                    elementLabelRes = R.string.water_tank_status_low,
                    gpio = 35,
                    hardwareRes = R.string.water_tank_pin_hw_float_switch,
                ),
                WaterTankPinAssignment(
                    elementLabelRes = R.string.water_tank_status_critical,
                    gpio = 36,
                    hardwareRes = R.string.water_tank_pin_hw_float_switch,
                ),
                WaterTankPinAssignment(
                    elementLabelRes = R.string.water_tank_pump_on,
                    gpio = 16,
                    hardwareRes = R.string.water_tank_pin_hw_pump_relay,
                ),
            ),
        ),
        WaterTankPinSection(
            sectionTitleRes = R.string.water_tank_chart_title,
            assignments = listOf(
                WaterTankPinAssignment(
                    elementLabelRes = R.string.water_tank_pin_chart_history,
                    gpio = 27,
                    hardwareRes = R.string.water_tank_pin_hw_level_source,
                ),
                WaterTankPinAssignment(
                    elementLabelRes = R.string.water_tank_stat_average,
                    gpio = 0,
                    hardwareRes = R.string.water_tank_pin_hw_nvs_history,
                ),
            ),
        ),
        WaterTankPinSection(
            sectionTitleRes = R.string.water_tank_pin_section_footer,
            assignments = listOf(
                WaterTankPinAssignment(
                    elementLabelRes = R.string.water_tank_sensor_online,
                    gpio = 27,
                    hardwareRes = R.string.water_tank_pin_hw_level_source,
                ),
            ),
        ),
    )
}
