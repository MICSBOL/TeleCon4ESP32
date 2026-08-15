package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.domain.model.usesCamera

/** Synthetic [RemoteDevice] for SoftAP TCP control (no Bluetooth address). */
data class Esp32SoftApDevice(
    override val name: String? = Esp32CameraDefaults.SOFTAP_SSID,
    override val address: String = defaultAddress(),
) : RemoteDevice {
    companion object {
        fun defaultAddress(
            host: String = Esp32CameraDefaults.DEFAULT_SOFTAP_HOST,
            port: Int = Esp32CameraDefaults.DEFAULT_CONTROL_PORT,
        ): String = "$host:$port"

        /** Default CAM SoftAP (`TeleCon-RC-CAM`). */
        val Default = Esp32SoftApDevice()

        /**
         * SoftAP identity for the active connection mode + board.
         *
         * Portable SoftAP base:
         * - CAM starter → starter SSID
         * - CAM + SoftAP Simple/Binary (or legacy Kit A) → main CAM SoftAP SSID
         * - DevKit SoftAP → `ESP32-TC-{PREFIX}-WiFi-Simple|Binary`
         */
        fun forConnectionMode(
            connectionMode: BluetoothConnectionMode,
            applicationId: ApplicationId = ApplicationId.CONTROL_PANEL,
            board: Esp32Board = Esp32Board.DEV_KIT,
        ): Esp32SoftApDevice {
            val camSoftAp = applicationId.usesCamera() && board == Esp32Board.CAM
            return when (connectionMode) {
                BluetoothConnectionMode.WIFI_CAM_STARTER -> Esp32SoftApDevice(
                    name = Esp32CameraDefaults.STARTER_SOFTAP_SSID,
                    address = defaultAddress(),
                )
                BluetoothConnectionMode.WIFI_SOFTAP -> Default
                BluetoothConnectionMode.WIFI_SIMPLE,
                BluetoothConnectionMode.WIFI_BINARY,
                -> {
                    if (camSoftAp) {
                        Default
                    } else {
                        val ssid = Esp32DevKitSoftApDefaults.softApSsid(
                            applicationId.protocolPrefix(),
                            connectionMode,
                        )
                        Esp32SoftApDevice(
                            name = ssid,
                            address = Esp32DevKitSoftApDefaults.controlAddress(),
                        )
                    }
                }
                else -> Default
            }
        }
    }
}
