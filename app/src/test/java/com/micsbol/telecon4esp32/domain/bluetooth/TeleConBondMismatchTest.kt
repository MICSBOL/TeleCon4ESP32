package com.micsbol.telecon4esp32.domain.bluetooth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeleConBondMismatchTest {

    @Test
    fun staleBond_whenTeleConNameChanged() {
        assertTrue(
            TeleConBondMismatch.isStaleFirmwareBond(
                bondedName = "TeleCon-BLE-RC-V",
                advertisedName = "ESP32-TeleCon-GH",
            ),
        )
    }

    @Test
    fun notStale_whenNamesMatchIgnoringCase() {
        assertFalse(
            TeleConBondMismatch.isStaleFirmwareBond(
                bondedName = "ESP32-TeleCon-GH",
                advertisedName = "esp32-telecon-gh",
            ),
        )
    }

    @Test
    fun notStale_whenEitherNameBlank() {
        assertFalse(
            TeleConBondMismatch.isStaleFirmwareBond(
                bondedName = "ESP32-TeleCon-GH",
                advertisedName = null,
            ),
        )
        assertFalse(
            TeleConBondMismatch.isStaleFirmwareBond(
                bondedName = "  ",
                advertisedName = "ESP32-TeleCon-GH",
            ),
        )
    }

    @Test
    fun findStale_matchesSameAddressDifferentName() {
        val scanned = listOf(
            BluetoothDevice(name = "ESP32-TeleCon-GH", address = "AA:BB:CC:DD:EE:FF"),
        )
        val paired = listOf(
            BluetoothDevice(name = "TeleCon-BLE-RC-V", address = "aa:bb:cc:dd:ee:ff"),
        )
        assertNotNull(TeleConBondMismatch.findStaleFirmwareDevice(scanned, paired))
    }

    @Test
    fun findStale_nullWhenNoMismatch() {
        val devices = listOf(
            BluetoothDevice(name = "ESP32-TeleCon-GH", address = "AA:BB:CC:DD:EE:FF"),
        )
        assertNull(TeleConBondMismatch.findStaleFirmwareDevice(devices, devices))
    }
}
