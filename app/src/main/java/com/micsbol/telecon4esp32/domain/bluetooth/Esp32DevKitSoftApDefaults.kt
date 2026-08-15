package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults

/**
 * SoftAP identity for DevKit noCam Wi‑Fi Simple / Binary (Control Panel and other apps).
 * Host/port match Kit A CAM SoftAP TCP; SSIDs encode app token + mode.
 *
 * Do not confuse with [Esp32CameraDefaults.SOFTAP_SSID] (`TeleCon-RC-CAM`).
 */
object Esp32DevKitSoftApDefaults {
    const val DEFAULT_SOFTAP_HOST = Esp32CameraDefaults.DEFAULT_SOFTAP_HOST
    const val DEFAULT_CONTROL_PORT = Esp32CameraDefaults.DEFAULT_CONTROL_PORT
    const val SOFTAP_PASSWORD = Esp32CameraDefaults.SOFTAP_PASSWORD

    /** e.g. `ESP32-TC-RC-WiFi-Simple`, `ESP32-TC-GH-WiFi-Binary`. */
    fun softApSsid(appPrefix: String, mode: BluetoothConnectionMode): String {
        val token = appPrefix.trim().uppercase().ifBlank { "RC" }
        val suffix = when (mode) {
            BluetoothConnectionMode.WIFI_BINARY -> "WiFi-Binary"
            BluetoothConnectionMode.WIFI_SIMPLE,
            BluetoothConnectionMode.WIFI_SOFTAP,
            BluetoothConnectionMode.WIFI_CAM_STARTER,
            -> "WiFi-Simple"
            else -> "WiFi-Simple"
        }
        return "ESP32-TC-$token-$suffix"
    }

    fun controlAddress(
        host: String = DEFAULT_SOFTAP_HOST,
        port: Int = DEFAULT_CONTROL_PORT,
    ): String = "$host:$port"
}
