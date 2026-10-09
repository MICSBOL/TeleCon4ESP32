package com.micsbol.telecon4esp32.ui.control_panel

/** Join / stream status for the Control Panel camera pane. */
sealed interface ControlPanelCameraConnectUi {
    data object Idle : ControlPanelCameraConnectUi
    data class Connecting(
        val ssid: String,
        val password: String,
    ) : ControlPanelCameraConnectUi
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
    /** Camera link is not being joined. */
    data object Disabled : ControlPanelCameraConnectUi
}
