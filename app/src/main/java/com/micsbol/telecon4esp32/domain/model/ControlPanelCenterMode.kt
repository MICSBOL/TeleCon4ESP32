package com.micsbol.telecon4esp32.domain.model

/**
 * What the Control Panel center pane shows.
 * [PLOTS] is always free; [CAMERA] and [RADAR] each need their own unlock.
 */
enum class ControlPanelCenterMode {
    PLOTS,
    CAMERA,
    RADAR,
    ;

    val premiumFeature: PremiumFeature?
        get() = when (this) {
            PLOTS -> null
            CAMERA -> PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS
            RADAR -> PremiumFeature.CONTROL_PANEL_RADAR
        }

    val isFree: Boolean
        get() = premiumFeature == null

    companion object {
        fun fromStored(value: String?): ControlPanelCenterMode =
            entries.firstOrNull { it.name == value } ?: PLOTS
    }
}

fun ControlPanelCenterMode.isUnlocked(
    cameraUnlocked: Boolean,
    radarUnlocked: Boolean,
): Boolean = when (this) {
    ControlPanelCenterMode.PLOTS -> true
    ControlPanelCenterMode.CAMERA -> cameraUnlocked
    ControlPanelCenterMode.RADAR -> radarUnlocked
}

/** CAM hardware role picker is for Control Panel after Camera/Radar extras are unlocked. */
fun ApplicationId.showsCameraHardwareRoleSettings(
    centerExtrasUnlocked: Boolean,
): Boolean = this == ApplicationId.CONTROL_PANEL && centerExtrasUnlocked

/**
 * CAM hardware picker is shown only while the Control Panel center pane is Camera.
 */
fun ApplicationId.showsCameraHardwareRolePicker(
    centerExtrasUnlocked: Boolean,
    centerCameraEnabled: Boolean,
): Boolean = showsCameraHardwareRoleSettings(centerExtrasUnlocked) && centerCameraEnabled

/** One CAM / two-device roles are only offered while the center pane is Camera. */
fun ControlPanelCenterMode.enablesCameraHardwareRoles(
    extrasUnlocked: Boolean,
): Boolean = extrasUnlocked && this == ControlPanelCenterMode.CAMERA

/**
 * Stored CAM board / overlay is ignored unless Control Panel extras are unlocked
 * and the center pane is Camera. Otherwise settings stay on No CAM (DevKit).
 */
fun ApplicationId.effectiveCameraHardwareRole(
    stored: CameraHardwareRole,
    userType: SettingsUserType,
    centerExtrasUnlocked: Boolean,
    centerCameraEnabled: Boolean,
): CameraHardwareRole {
    if (!showsCameraHardwareRoleSettings(centerExtrasUnlocked)) {
        return CameraHardwareRole.NO_CAM
    }
    return stored.coerceForSettings(
        userType = userType,
        centerCameraEnabled = centerCameraEnabled,
    )
}
