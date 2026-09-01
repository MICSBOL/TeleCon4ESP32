package com.micsbol.telecon4esp32.ui.codes

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeAssetModelsTest {

    @Test
    fun `role A classic simple lists video only cam and not wifi simple`() {
        val ids = controlPanelCodePackageIds(
            Esp32Board.DEV_KIT,
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            useSoftApCamera = true,
        )
        assertTrue(ControlPanelCodePackageId.DEVKIT_CLASSIC in ids)
        assertTrue(ControlPanelCodePackageId.CAM_SOFTAP_VIDEO in ids)
        assertFalse(ControlPanelCodePackageId.CAM_WIFI_SIMPLE in ids)
    }

    @Test
    fun `role A classic binary lists video only cam and not wifi simple`() {
        val ids = controlPanelCodePackageIds(
            Esp32Board.DEV_KIT,
            BluetoothConnectionMode.CLASSIC_BINARY,
            useSoftApCamera = true,
        )
        assertTrue(ControlPanelCodePackageId.DEVKIT_CLASSIC in ids)
        assertTrue(ControlPanelCodePackageId.CAM_SOFTAP_VIDEO in ids)
        assertFalse(ControlPanelCodePackageId.CAM_WIFI_SIMPLE in ids)
    }

    @Test
    fun `role A ble binary lists video only cam and not wifi simple`() {
        val ids = controlPanelCodePackageIds(
            Esp32Board.DEV_KIT,
            BluetoothConnectionMode.BLE_BINARY,
            useSoftApCamera = true,
        )
        assertTrue(ControlPanelCodePackageId.DEVKIT_BLE in ids)
        assertTrue(ControlPanelCodePackageId.CAM_SOFTAP_VIDEO in ids)
        assertFalse(ControlPanelCodePackageId.CAM_WIFI_SIMPLE in ids)
    }

    @Test
    fun `dev kit without overlay does not list cam sketches`() {
        val ids = controlPanelCodePackageIds(
            Esp32Board.DEV_KIT,
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            useSoftApCamera = false,
        )
        assertTrue(ControlPanelCodePackageId.DEVKIT_CLASSIC in ids)
        assertFalse(ControlPanelCodePackageId.CAM_SOFTAP_VIDEO in ids)
        assertFalse(ControlPanelCodePackageId.CAM_WIFI_SIMPLE in ids)
    }

    @Test
    fun `role B cam starter lists wifi simple only`() {
        val ids = controlPanelCodePackageIds(
            Esp32Board.CAM,
            BluetoothConnectionMode.WIFI_CAM_STARTER,
        )
        assertEquals(listOf(ControlPanelCodePackageId.CAM_WIFI_SIMPLE), ids)
        assertFalse(ControlPanelCodePackageId.CAM_SOFTAP_VIDEO in ids)
        assertFalse(ControlPanelCodePackageId.DEVKIT_CLASSIC in ids)
    }
}
