package com.micsbol.telecon4esp32.domain.model

/**
 * Physical ESP32 board the user targets for an application.
 *
 * - [DEV_KIT] — ESP32 DevKit (WROOM-32); general-purpose sensors/actuators, no camera.
 * - [CAM] — ESP32-CAM (AI-Thinker); camera node, video streams over WiFi while
 *   controls use Bluetooth.
 */
enum class Esp32Board {
    DEV_KIT,
    CAM,
    ;

    companion object {
        fun fromStored(value: String?): Esp32Board =
            entries.firstOrNull { it.name == value } ?: DEV_KIT

        fun defaultFor(applicationId: ApplicationId): Esp32Board = DEV_KIT
    }
}

/** Applications that include a live camera view and can target the ESP32-CAM board. */
fun ApplicationId.usesCamera(): Boolean = when (this) {
    ApplicationId.RC_VEHICLE_PRO,
    ApplicationId.GREENHOUSE,
    ApplicationId.SMART_DOOR_LOCK,
    -> true
    else -> false
}
