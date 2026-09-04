package com.micsbol.telecon4esp32.domain.model

/**
 * Physical ESP32 board setup the user targets for an application.
 *
 * - [DEV_KIT] — Role A control board. Bluetooth or DevKit SoftAP Wi‑Fi.
 *   Optional SoftAP HTTP video is a separate overlay setting, not this enum.
 * - [CAM] — Role B: one ESP32-CAM. SoftAP starter (video + TCP) or Advanced SoftAP Binary
 *   ([com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile.WIFI_SOFTAP]).
 * - [CAM_AND_DEV_KIT] — legacy stored value for Role A dual-board setups. Normalized to
 *   [DEV_KIT] plus SoftAP camera overlay (Classic, Classic Binary, or BLE control).
 */
enum class Esp32Board {
    DEV_KIT,
    CAM,
    /** Legacy dual-board storage: CAM SoftAP video + DevKit Bluetooth control. */
    CAM_AND_DEV_KIT,
    ;

    /** SoftAP HTTP camera is expected on this board value (single CAM or legacy dual). */
    val usesSoftApCamera: Boolean
        get() = this == CAM || this == CAM_AND_DEV_KIT

    /** Legacy dual-board stored value (CAM video + DevKit Bluetooth). */
    val isKitBDual: Boolean
        get() = this == CAM_AND_DEV_KIT

    /**
     * Board used for control-link pickers. Legacy [CAM_AND_DEV_KIT] is DevKit control
     * plus an orthogonal SoftAP camera overlay.
     */
    fun normalizedControlBoard(): Esp32Board =
        if (this == CAM_AND_DEV_KIT) DEV_KIT else this

    companion object {
        fun fromStored(value: String?): Esp32Board =
            entries.firstOrNull { it.name == value } ?: DEV_KIT

        fun defaultFor(applicationId: ApplicationId): Esp32Board = DEV_KIT
    }
}

/**
 * Applications that include a live camera view and can target the ESP32-CAM board
 * or an optional SoftAP camera overlay on DevKit Bluetooth control.
 */
fun ApplicationId.usesCamera(): Boolean = true

/** SoftAP camera overlay toggle is shown for DevKit control (not single-CAM SoftAP kits). */
fun showSoftApCameraOverlaySetting(
    applicationId: ApplicationId,
    board: Esp32Board,
): Boolean = applicationId.usesCamera() && board.normalizedControlBoard() != Esp32Board.CAM
