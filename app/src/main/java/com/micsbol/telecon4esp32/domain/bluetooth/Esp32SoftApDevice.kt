package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults

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

        val Default = Esp32SoftApDevice()
    }
}
