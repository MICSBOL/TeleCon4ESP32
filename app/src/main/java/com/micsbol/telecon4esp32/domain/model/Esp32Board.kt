package com.micsbol.telecon4esp32.domain.model

/**
 * Physical ESP32 board setup the user targets for an application.
 *
 * - [DEV_KIT] — ESP32 DevKit (WROOM-32); Bluetooth and SoftAP Wi‑Fi control only
 *   ([com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile.CONTROL_ONLY]).
 * - [CAM] — one ESP32-CAM. Normal SoftAP starter or Advanced Kit A SoftAP Binary
 *   ([com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile.WIFI_SOFTAP]).
 * - [CAM_AND_DEV_KIT] — Advanced Kit B: SoftAP video on ESP32-CAM + BLE Binary on DevKit
 *   ([com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE]).
 */
enum class Esp32Board {
    DEV_KIT,
    CAM,
    /** Kit B dual-board: CAM SoftAP video + DevKit BLE control. */
    CAM_AND_DEV_KIT,
    ;

    /** SoftAP HTTP camera is expected (single CAM or Kit B video half). */
    val usesSoftApCamera: Boolean
        get() = this == CAM || this == CAM_AND_DEV_KIT

    /** Advanced Kit B dual-board setup. */
    val isKitBDual: Boolean
        get() = this == CAM_AND_DEV_KIT

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
