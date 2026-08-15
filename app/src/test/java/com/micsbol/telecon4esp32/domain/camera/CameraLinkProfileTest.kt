package com.micsbol.telecon4esp32.domain.camera

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraLinkProfileTest {

    @Test
    fun `cam wifi binary resolves to softap video plus tcp profile`() {
        assertEquals(
            CameraLinkProfile.WIFI_SOFTAP,
            resolveCameraLinkProfile(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM,
                BluetoothConnectionMode.WIFI_BINARY,
            ),
        )
        assertTrue(CameraLinkProfile.WIFI_SOFTAP.shouldStartCameraStream)
        assertTrue(CameraLinkProfile.WIFI_SOFTAP.autoConnectSoftApControlWhenCameraOnline)
        assertTrue(CameraLinkProfile.WIFI_SOFTAP.isSoftApControl)
        assertFalse(CameraLinkProfile.WIFI_SOFTAP.isBleControlWithCamera)
    }

    @Test
    fun `cam starter softap uses same softap video plus tcp profile`() {
        assertEquals(
            CameraLinkProfile.WIFI_SOFTAP,
            resolveCameraLinkProfile(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM,
                BluetoothConnectionMode.WIFI_CAM_STARTER,
            ),
        )
    }

    @Test
    fun `legacy wifi softap still maps to softap video plus tcp`() {
        assertEquals(
            CameraLinkProfile.WIFI_SOFTAP,
            resolveCameraLinkProfile(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM,
                BluetoothConnectionMode.WIFI_SOFTAP,
            ),
        )
    }

    @Test
    fun `cam ble binary on single cam coerces to softap not dual radio`() {
        assertEquals(
            CameraLinkProfile.WIFI_SOFTAP,
            resolveCameraLinkProfile(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM,
                BluetoothConnectionMode.BLE_BINARY,
            ),
        )
    }

    @Test
    fun `cam and dev kit board resolves to kit B profile`() {
        assertEquals(
            CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE,
            resolveCameraLinkProfile(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM_AND_DEV_KIT,
                BluetoothConnectionMode.BLE_BINARY,
            ),
        )
        assertTrue(CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE.shouldStartCameraStream)
        assertFalse(CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE.autoConnectSoftApControlWhenCameraOnline)
        assertTrue(CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE.isBleControlWithCamera)
        assertFalse(CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE.isSoftApControl)
    }

    @Test
    fun `stale classic on cam coerces to softap video not dual radio`() {
        assertEquals(
            CameraLinkProfile.WIFI_SOFTAP,
            resolveCameraLinkProfile(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
            ),
        )
        assertEquals(
            CameraLinkProfile.WIFI_SOFTAP,
            resolveCameraLinkProfile(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM,
                BluetoothConnectionMode.CLASSIC_BINARY,
            ),
        )
    }

    @Test
    fun `dev kit is control only even with wifi transport`() {
        assertEquals(
            CameraLinkProfile.CONTROL_ONLY,
            resolveCameraLinkProfile(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.DEV_KIT,
                BluetoothTransportType.WIFI,
            ),
        )
        assertFalse(CameraLinkProfile.CONTROL_ONLY.shouldStartCameraStream)
    }

    @Test
    fun `greenhouse and door lock share portable softap cam profiles`() {
        listOf(ApplicationId.GREENHOUSE, ApplicationId.SMART_DOOR_LOCK).forEach { app ->
            assertEquals(
                app.name,
                CameraLinkProfile.WIFI_SOFTAP,
                resolveCameraLinkProfile(app, Esp32Board.CAM, BluetoothTransportType.WIFI),
            )
            assertEquals(
                app.name,
                CameraLinkProfile.WIFI_SOFTAP,
                resolveCameraLinkProfile(app, Esp32Board.CAM, BluetoothConnectionMode.WIFI_BINARY),
            )
        }
    }

    @Test
    fun `non camera apps stay control only on cam board`() {
        assertEquals(
            CameraLinkProfile.CONTROL_ONLY,
            resolveCameraLinkProfile(
                ApplicationId.CONTROL_PANEL,
                Esp32Board.CAM,
                BluetoothConnectionMode.BLE_BINARY,
            ),
        )
    }

    @Test
    fun `softap performance settings show for cam softap tcp video`() {
        assertTrue(
            showSoftApPerformanceSettings(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM,
                BluetoothConnectionMode.WIFI_BINARY,
            ),
        )
        assertTrue(
            showSoftApPerformanceSettings(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM,
                BluetoothConnectionMode.WIFI_CAM_STARTER,
            ),
        )
        // Stale BLE on single CAM still maps to SoftAP TCP+video (Kit A path).
        assertTrue(
            showSoftApPerformanceSettings(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM,
                BluetoothConnectionMode.BLE_BINARY,
            ),
        )
        assertTrue(
            showSoftApPerformanceSettings(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM_AND_DEV_KIT,
                BluetoothConnectionMode.BLE_BINARY,
            ),
        )
    }

    @Test
    fun `softap performance settings hidden without softap camera`() {
        assertFalse(
            showSoftApPerformanceSettings(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.BLE_BINARY,
            ),
        )
        assertFalse(
            showSoftApPerformanceSettings(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.WIFI_BINARY,
            ),
        )
        assertFalse(
            showSoftApPerformanceSettings(
                ApplicationId.GREENHOUSE,
                Esp32Board.CAM,
                BluetoothConnectionMode.WIFI_BINARY,
            ),
        )
    }
}
