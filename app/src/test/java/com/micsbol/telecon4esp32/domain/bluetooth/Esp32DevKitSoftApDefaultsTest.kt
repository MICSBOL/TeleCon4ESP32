package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.model.ApplicationId
import org.junit.Assert.assertEquals
import org.junit.Test

class Esp32DevKitSoftApDefaultsTest {

    @Test
    fun softApSsid_controlPanelSimpleAndBinary() {
        assertEquals(
            "ESP32-TC-RC-WiFi-Simple",
            Esp32DevKitSoftApDefaults.softApSsid("RC", BluetoothConnectionMode.WIFI_SIMPLE),
        )
        assertEquals(
            "ESP32-TC-RC-WiFi-Binary",
            Esp32DevKitSoftApDefaults.softApSsid("RC", BluetoothConnectionMode.WIFI_BINARY),
        )
    }

    @Test
    fun softApDevice_forDevKitModes() {
        val simple = Esp32SoftApDevice.forConnectionMode(
            BluetoothConnectionMode.WIFI_SIMPLE,
            ApplicationId.CONTROL_PANEL,
        )
        assertEquals("ESP32-TC-RC-WiFi-Simple", simple.name)
        assertEquals("192.168.4.1:3333", simple.address)

        val binary = Esp32SoftApDevice.forConnectionMode(
            BluetoothConnectionMode.WIFI_BINARY,
            ApplicationId.CONTROL_PANEL,
        )
        assertEquals("ESP32-TC-RC-WiFi-Binary", binary.name)

        val cam = Esp32SoftApDevice.forConnectionMode(
            BluetoothConnectionMode.WIFI_BINARY,
            ApplicationId.RC_VEHICLE_PRO,
            com.micsbol.telecon4esp32.domain.model.Esp32Board.CAM,
        )
        assertEquals(Esp32SoftApDevice.Default.name, cam.name)

        val starter = Esp32SoftApDevice.forConnectionMode(
            BluetoothConnectionMode.WIFI_CAM_STARTER,
            ApplicationId.RC_VEHICLE_PRO,
        )
        assertEquals("TeleCon-RC-CAM-Starter", starter.name)

        val legacy = Esp32SoftApDevice.forConnectionMode(
            BluetoothConnectionMode.WIFI_SOFTAP,
            ApplicationId.RC_VEHICLE_PRO,
        )
        assertEquals(Esp32SoftApDevice.Default.name, legacy.name)
    }
}
