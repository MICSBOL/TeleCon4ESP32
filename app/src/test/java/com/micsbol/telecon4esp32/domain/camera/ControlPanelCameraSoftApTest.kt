package com.micsbol.telecon4esp32.domain.camera

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlPanelCameraSoftApTest {

    @Test
    fun `dev kit bluetooth uses starter cam ssid`() {
        val target = resolveCameraVideoSoftApTarget(
            Esp32Board.DEV_KIT,
            BluetoothConnectionMode.CLASSIC_SIMPLE,
        )
        assertEquals(Esp32CameraDefaults.STARTER_SOFTAP_SSID, target.ssid)
        assertEquals(Esp32CameraDefaults.SOFTAP_PASSWORD, target.password)
    }

    @Test
    fun `cam starter firmware uses starter ssid`() {
        assertEquals(
            Esp32CameraDefaults.STARTER_SOFTAP_SSID,
            resolveCameraVideoSoftApTarget(
                Esp32Board.CAM,
                BluetoothConnectionMode.WIFI_CAM_STARTER,
            ).ssid,
        )
    }

    @Test
    fun `cam advanced softap uses kit a ssid`() {
        assertEquals(
            Esp32CameraDefaults.SOFTAP_SSID,
            resolveCameraVideoSoftApTarget(
                Esp32Board.CAM,
                BluetoothConnectionMode.WIFI_BINARY,
            ).ssid,
        )
    }

    @Test
    fun `dev kit wifi control conflicts with cam video softap`() {
        assertTrue(
            cameraVideoSoftApConflictsWithControlLink(
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.WIFI_SIMPLE,
            ),
        )
        assertFalse(
            cameraVideoSoftApConflictsWithControlLink(
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
            ),
        )
        assertFalse(
            cameraVideoSoftApConflictsWithControlLink(
                Esp32Board.CAM,
                BluetoothConnectionMode.WIFI_CAM_STARTER,
            ),
        )
    }

    @Test
    fun `camera pane session-arms overlay profile without stored overlay`() {
        val profile = resolveSessionCameraLinkProfile(
            ApplicationId.CONTROL_PANEL,
            Esp32Board.DEV_KIT,
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            useSoftApCamera = false,
        )
        assertEquals(CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT, profile)
        assertTrue(profile.shouldStartCameraStream)
        assertFalse(profile.autoConnectSoftApControlWhenCameraOnline)
    }

    @Test
    fun `camera pane keeps stored overlay profile`() {
        assertEquals(
            CameraLinkProfile.WIFI_CAMERA_DEVKIT_BT,
            resolveSessionCameraLinkProfile(
                ApplicationId.CONTROL_PANEL,
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.BLE_BINARY,
                useSoftApCamera = true,
            ),
        )
    }

    @Test
    fun `camera pane on cam board stays softap video plus tcp profile`() {
        val profile = resolveSessionCameraLinkProfile(
            ApplicationId.CONTROL_PANEL,
            Esp32Board.CAM,
            BluetoothConnectionMode.WIFI_CAM_STARTER,
            useSoftApCamera = false,
        )
        assertEquals(CameraLinkProfile.WIFI_SOFTAP, profile)
        assertTrue(profile.autoConnectSoftApControlWhenCameraOnline)
    }
}
