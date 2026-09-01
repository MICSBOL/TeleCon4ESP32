package com.micsbol.telecon4esp32.domain.camera

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.CameraHardwareRole
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.toSelection
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
        assertFalse(CameraLinkProfile.WIFI_SOFTAP.isBluetoothControlWithCamera)
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
    fun `cam and dev kit board with bluetooth resolves to overlay profile`() {
        assertEquals(
            CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT,
            resolveCameraLinkProfile(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM_AND_DEV_KIT,
                BluetoothConnectionMode.BLE_BINARY,
            ),
        )
        assertEquals(
            CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT,
            resolveCameraLinkProfile(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM_AND_DEV_KIT,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
            ),
        )
        assertTrue(CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT.shouldStartCameraStream)
        assertFalse(CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT.autoConnectSoftApControlWhenCameraOnline)
        assertTrue(CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT.isBluetoothControlWithCamera)
        assertFalse(CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT.isSoftApControl)
    }

    @Test
    fun `role A overlay never auto-opens softap tcp for classic or ble`() {
        listOf(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            BluetoothConnectionMode.CLASSIC_BINARY,
            BluetoothConnectionMode.BLE_BINARY,
        ).forEach { mode ->
            val profile = resolveCameraLinkProfile(
                ApplicationId.CONTROL_PANEL,
                Esp32Board.DEV_KIT,
                mode,
                useSoftApCamera = true,
            )
            assertEquals(mode.name, CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT, profile)
            assertFalse(mode.name, profile.autoConnectSoftApControlWhenCameraOnline)
            assertFalse(mode.name, profile.isSoftApControl)
        }
        val roleB = resolveCameraLinkProfile(
            ApplicationId.CONTROL_PANEL,
            Esp32Board.CAM,
            BluetoothConnectionMode.WIFI_CAM_STARTER,
        )
        assertEquals(CameraLinkProfile.WIFI_SOFTAP, roleB)
        assertTrue(roleB.autoConnectSoftApControlWhenCameraOnline)
    }

    @Test
    fun `dev kit overlay off stays control only`() {
        assertEquals(
            CameraLinkProfile.CONTROL_ONLY,
            resolveCameraLinkProfile(
                ApplicationId.CONTROL_PANEL,
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                useSoftApCamera = false,
            ),
        )
    }

    @Test
    fun `dev kit overlay does not arm camera when wifi is the control link`() {
        assertEquals(
            CameraLinkProfile.CONTROL_ONLY,
            resolveCameraLinkProfile(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.WIFI_SIMPLE,
                useSoftApCamera = true,
            ),
        )
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
                ApplicationId.WATER_TANK,
                Esp32Board.CAM,
                BluetoothConnectionMode.BLE_BINARY,
            ),
        )
    }

    @Test
    fun `control panel cam board uses softap video plus tcp`() {
        assertEquals(
            CameraLinkProfile.WIFI_SOFTAP,
            resolveCameraLinkProfile(
                ApplicationId.CONTROL_PANEL,
                Esp32Board.CAM,
                BluetoothConnectionMode.WIFI_CAM_STARTER,
            ),
        )
    }

    @Test
    fun `control panel no cam does not start a camera stream`() {
        val noCam = CameraHardwareRole.NO_CAM.toSelection()
        val profile = resolveCameraLinkProfile(
            ApplicationId.CONTROL_PANEL,
            noCam.board,
            BluetoothConnectionMode.CLASSIC_BINARY,
            noCam.useSoftApCamera,
        )
        assertEquals(CameraLinkProfile.CONTROL_ONLY, profile)
        assertFalse(profile.shouldStartCameraStream)
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
        assertTrue(
            showSoftApPerformanceSettings(
                ApplicationId.CONTROL_PANEL,
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                useSoftApCamera = true,
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
