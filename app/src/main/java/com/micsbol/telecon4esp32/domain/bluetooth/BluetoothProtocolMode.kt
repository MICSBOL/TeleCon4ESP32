package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.model.ApplicationId

/**
 * How the app exchanges data with the ESP32 for a given application profile.
 *
 * - [SIMPLE] — human-readable line protocol (`APP:TYPE,key,value,...\\n`)
 * - [ADVANCED] — legacy binary packets (RC control panel only)
 */
enum class BluetoothProtocolMode {
    SIMPLE,
    ADVANCED,
    ;

    companion object {
        fun fromStored(value: String?): BluetoothProtocolMode =
            entries.firstOrNull { it.name == value } ?: SIMPLE

        fun defaultFor(applicationId: ApplicationId): BluetoothProtocolMode = when (applicationId) {
            ApplicationId.CONTROL_PANEL -> ADVANCED
            else -> SIMPLE
        }
    }
}
