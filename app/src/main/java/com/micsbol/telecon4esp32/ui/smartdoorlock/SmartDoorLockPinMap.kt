package com.micsbol.telecon4esp32.ui.smartdoorlock

import androidx.annotation.StringRes
import com.micsbol.telecon4esp32.R

data class SmartDoorLockPinAssignment(
    @StringRes val elementLabelRes: Int,
    val gpio: Int,
    @StringRes val hardwareRes: Int,
)

data class SmartDoorLockPinSection(
    @StringRes val sectionTitleRes: Int,
    val assignments: List<SmartDoorLockPinAssignment>,
)

object SmartDoorLockPinMap {
    fun sections(): List<SmartDoorLockPinSection> = listOf(
        SmartDoorLockPinSection(
            sectionTitleRes = R.string.smart_door_lock_pin_section_topbar,
            assignments = listOf(
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_pin_topbar_connection,
                    gpio = 2,
                    hardwareRes = R.string.smart_door_lock_pin_hw_status_led,
                ),
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_pin_topbar_call_timer,
                    gpio = 0,
                    hardwareRes = R.string.smart_door_lock_pin_hw_app_only,
                ),
            ),
        ),
        SmartDoorLockPinSection(
            sectionTitleRes = R.string.smart_door_lock_screen_title,
            assignments = listOf(
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_pin_wifi_signal,
                    gpio = 0,
                    hardwareRes = R.string.smart_door_lock_pin_hw_wifi_rssi,
                ),
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_pin_esp32_online,
                    gpio = 2,
                    hardwareRes = R.string.smart_door_lock_pin_hw_status_led,
                ),
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_pin_live_video,
                    gpio = 0,
                    hardwareRes = R.string.smart_door_lock_pin_hw_esp32_cam,
                ),
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_pin_door_state,
                    gpio = 14,
                    hardwareRes = R.string.smart_door_lock_pin_hw_reed_switch,
                ),
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_pin_swipe_unlock,
                    gpio = 12,
                    hardwareRes = R.string.smart_door_lock_pin_hw_lock_relay,
                ),
            ),
        ),
        SmartDoorLockPinSection(
            sectionTitleRes = R.string.smart_door_lock_pin_section_quick_actions,
            assignments = listOf(
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_send_unlock_short,
                    gpio = 12,
                    hardwareRes = R.string.smart_door_lock_pin_hw_lock_relay,
                ),
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_send_lock_short,
                    gpio = 12,
                    hardwareRes = R.string.smart_door_lock_pin_hw_lock_relay,
                ),
            ),
        ),
        SmartDoorLockPinSection(
            sectionTitleRes = R.string.smart_door_lock_pin_section_media,
            assignments = listOf(
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_pin_mic,
                    gpio = 4,
                    hardwareRes = R.string.smart_door_lock_pin_hw_i2s_mic,
                ),
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_pin_speaker,
                    gpio = 25,
                    hardwareRes = R.string.smart_door_lock_pin_hw_i2s_amp,
                ),
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_pin_camera_toggle,
                    gpio = 0,
                    hardwareRes = R.string.smart_door_lock_pin_hw_esp32_cam,
                ),
            ),
        ),
        SmartDoorLockPinSection(
            sectionTitleRes = R.string.smart_door_lock_pin_section_relay,
            assignments = listOf(
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_pin_relay_state,
                    gpio = 12,
                    hardwareRes = R.string.smart_door_lock_pin_hw_lock_relay,
                ),
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_pin_relay_pulse,
                    gpio = 12,
                    hardwareRes = R.string.smart_door_lock_pin_hw_lock_relay,
                ),
            ),
        ),
        SmartDoorLockPinSection(
            sectionTitleRes = R.string.smart_door_lock_pin_section_peripherals,
            assignments = listOf(
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_pin_doorbell_button,
                    gpio = 13,
                    hardwareRes = R.string.smart_door_lock_pin_hw_doorbell_button,
                ),
                SmartDoorLockPinAssignment(
                    elementLabelRes = R.string.smart_door_lock_pin_strike_relay,
                    gpio = 15,
                    hardwareRes = R.string.smart_door_lock_pin_hw_strike_relay,
                ),
            ),
        ),
    )
}
