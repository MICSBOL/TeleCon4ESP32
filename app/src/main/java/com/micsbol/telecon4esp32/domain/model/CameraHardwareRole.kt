package com.micsbol.telecon4esp32.domain.model

/**
 * How the user wires camera hardware for Control Panel / RC Vehicle settings.
 *
 * Maps onto [Esp32Board] plus the SoftAP camera overlay flag — no extra storage key.
 */
enum class CameraHardwareRole {
    /** Role B: one ESP32-CAM for HTTP video and SoftAP TCP control. */
    ONE_CAM,

    /** Role A: video-only CAM over SoftAP + DevKit Bluetooth control. */
    TWO_DEVICES,

    /** DevKit only. No SoftAP camera. */
    NO_CAM,
}

data class CameraHardwareRoleSelection(
    val board: Esp32Board,
    val useSoftApCamera: Boolean,
    val bluetoothControlOnly: Boolean,
)

fun resolveCameraHardwareRole(
    board: Esp32Board,
    useSoftApCamera: Boolean,
): CameraHardwareRole {
    if (board.normalizedControlBoard() == Esp32Board.CAM) {
        return CameraHardwareRole.ONE_CAM
    }
    if (useSoftApCamera || board.isKitBDual) {
        return CameraHardwareRole.TWO_DEVICES
    }
    return CameraHardwareRole.NO_CAM
}

fun CameraHardwareRole.toSelection(): CameraHardwareRoleSelection = when (this) {
    CameraHardwareRole.ONE_CAM -> CameraHardwareRoleSelection(
        board = Esp32Board.CAM,
        useSoftApCamera = false,
        bluetoothControlOnly = false,
    )
    CameraHardwareRole.TWO_DEVICES -> CameraHardwareRoleSelection(
        board = Esp32Board.DEV_KIT,
        useSoftApCamera = true,
        bluetoothControlOnly = true,
    )
    CameraHardwareRole.NO_CAM -> CameraHardwareRoleSelection(
        board = Esp32Board.DEV_KIT,
        useSoftApCamera = false,
        bluetoothControlOnly = false,
    )
}

val CameraHardwareRole.usesSoftApCamera: Boolean
    get() = this != CameraHardwareRole.NO_CAM

/**
 * Dual-board SoftAP video + DevKit Bluetooth (Role A) is available for Default
 * (Classic Simple) and Advanced (Classic Binary / BLE Binary).
 */
fun CameraHardwareRole.availableForUserType(userType: SettingsUserType): Boolean =
    this != CameraHardwareRole.TWO_DEVICES ||
        userType == SettingsUserType.NORMAL ||
        userType == SettingsUserType.ADVANCED

fun CameraHardwareRole.coerceForUserType(userType: SettingsUserType): CameraHardwareRole =
    if (availableForUserType(userType)) this else CameraHardwareRole.NO_CAM

/** Control Panel has no one-CAM video+control; map Role B to Role A overlay. */
fun CameraHardwareRole.coerceForApplication(applicationId: ApplicationId): CameraHardwareRole =
    if (this == CameraHardwareRole.ONE_CAM && !applicationId.supportsCamVideoControl()) {
        CameraHardwareRole.TWO_DEVICES
    } else {
        this
    }

/** Plots / Radar center modes keep hardware on No CAM for every user type. */
fun CameraHardwareRole.coerceForSettings(
    userType: SettingsUserType,
    centerCameraEnabled: Boolean,
): CameraHardwareRole {
    if (!centerCameraEnabled) return CameraHardwareRole.NO_CAM
    return coerceForUserType(userType)
}
