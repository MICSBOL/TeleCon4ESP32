package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraHardwareRoleTest {

    @Test
    fun `cam board resolves to one device`() {
        assertEquals(
            CameraHardwareRole.ONE_CAM,
            resolveCameraHardwareRole(Esp32Board.CAM, useSoftApCamera = false),
        )
        assertEquals(
            CameraHardwareRole.ONE_CAM,
            resolveCameraHardwareRole(Esp32Board.CAM, useSoftApCamera = true),
        )
        assertEquals(Esp32Board.CAM, CameraHardwareRole.ONE_CAM.toSelection().board)
        assertFalse(CameraHardwareRole.ONE_CAM.toSelection().bluetoothControlOnly)
    }

    @Test
    fun `dev kit overlay resolves to two devices`() {
        assertEquals(
            CameraHardwareRole.TWO_DEVICES,
            resolveCameraHardwareRole(Esp32Board.DEV_KIT, useSoftApCamera = true),
        )
        assertEquals(
            CameraHardwareRole.TWO_DEVICES,
            resolveCameraHardwareRole(Esp32Board.CAM_AND_DEV_KIT, useSoftApCamera = false),
        )
        val selection = CameraHardwareRole.TWO_DEVICES.toSelection()
        assertEquals(Esp32Board.DEV_KIT, selection.board)
        assertTrue(selection.useSoftApCamera)
        assertTrue(selection.bluetoothControlOnly)
    }

    @Test
    fun `dev kit without overlay resolves to no cam`() {
        assertEquals(
            CameraHardwareRole.NO_CAM,
            resolveCameraHardwareRole(Esp32Board.DEV_KIT, useSoftApCamera = false),
        )
        val selection = CameraHardwareRole.NO_CAM.toSelection()
        assertEquals(Esp32Board.DEV_KIT, selection.board)
        assertFalse(selection.useSoftApCamera)
        assertFalse(selection.bluetoothControlOnly)
    }

    @Test
    fun `two devices is available for default and advanced`() {
        assertTrue(
            CameraHardwareRole.TWO_DEVICES.availableForUserType(SettingsUserType.NORMAL),
        )
        assertTrue(
            CameraHardwareRole.TWO_DEVICES.availableForUserType(SettingsUserType.ADVANCED),
        )
        assertEquals(
            CameraHardwareRole.TWO_DEVICES,
            CameraHardwareRole.TWO_DEVICES.coerceForUserType(SettingsUserType.NORMAL),
        )
        assertEquals(
            CameraHardwareRole.ONE_CAM,
            CameraHardwareRole.ONE_CAM.coerceForUserType(SettingsUserType.NORMAL),
        )
    }

    @Test
    fun `without center camera only no cam is kept`() {
        assertEquals(
            CameraHardwareRole.NO_CAM,
            CameraHardwareRole.ONE_CAM.coerceForSettings(
                userType = SettingsUserType.NORMAL,
                centerCameraEnabled = false,
            ),
        )
        assertEquals(
            CameraHardwareRole.NO_CAM,
            CameraHardwareRole.TWO_DEVICES.coerceForSettings(
                userType = SettingsUserType.ADVANCED,
                centerCameraEnabled = false,
            ),
        )
        assertEquals(
            CameraHardwareRole.ONE_CAM,
            CameraHardwareRole.ONE_CAM.coerceForSettings(
                userType = SettingsUserType.NORMAL,
                centerCameraEnabled = true,
            ),
        )
        assertEquals(
            CameraHardwareRole.TWO_DEVICES,
            CameraHardwareRole.TWO_DEVICES.coerceForSettings(
                userType = SettingsUserType.ADVANCED,
                centerCameraEnabled = true,
            ),
        )
        assertEquals(
            CameraHardwareRole.TWO_DEVICES,
            CameraHardwareRole.TWO_DEVICES.coerceForSettings(
                userType = SettingsUserType.NORMAL,
                centerCameraEnabled = true,
            ),
        )
    }
}
