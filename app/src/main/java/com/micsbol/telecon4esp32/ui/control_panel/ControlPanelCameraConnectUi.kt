package com.micsbol.telecon4esp32.ui.control_panel

/** Join / stream status for the Control Panel camera pane. */
sealed interface ControlPanelCameraConnectUi {
    data object Idle : ControlPanelCameraConnectUi
    data object Connecting : ControlPanelCameraConnectUi
    data class Ready(
        val ssid: String,
        val password: String,
    ) : ControlPanelCameraConnectUi
    data class NeedsWifi(
        val ssid: String,
        val password: String,
    ) : ControlPanelCameraConnectUi
    data class ControlWifiConflict(
        val ssid: String,
    ) : ControlPanelCameraConnectUi
    /** Settings CAM feature is No camera — do not join SoftAP or start HTTP video. */
    data object Disabled : ControlPanelCameraConnectUi
}
