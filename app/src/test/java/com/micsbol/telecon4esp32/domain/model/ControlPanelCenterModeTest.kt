package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlPanelCenterModeTest {

    @Test
    fun `terminal is free and the other center views unlock independently`() {
        assertTrue(ControlPanelCenterMode.TERMINAL.isFree)
        assertFalse(ControlPanelCenterMode.PLOTS.isFree)
        assertFalse(ControlPanelCenterMode.STICK.isFree)
        assertEquals(
            PremiumFeature.CONTROL_PANEL_PLOTS,
            ControlPanelCenterMode.PLOTS.premiumFeature,
        )
        assertEquals(
            PremiumFeature.CONTROL_PANEL_STICK,
            ControlPanelCenterMode.STICK.premiumFeature,
        )
        assertEquals(
            PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS,
            ControlPanelCenterMode.CAMERA.premiumFeature,
        )
        assertEquals(
            PremiumFeature.CONTROL_PANEL_RADAR,
            ControlPanelCenterMode.RADAR.premiumFeature,
        )
        assertTrue(
            ControlPanelCenterMode.TERMINAL.isUnlocked(
                cameraUnlocked = false,
                radarUnlocked = false,
                stickUnlocked = false,
                plotsUnlocked = false,
            ),
        )
        assertFalse(
            ControlPanelCenterMode.PLOTS.isUnlocked(
                cameraUnlocked = false,
                radarUnlocked = false,
                stickUnlocked = false,
                plotsUnlocked = false,
            ),
        )
        assertTrue(
            ControlPanelCenterMode.PLOTS.isUnlocked(
                cameraUnlocked = false,
                radarUnlocked = false,
                stickUnlocked = false,
                plotsUnlocked = true,
            ),
        )
        assertFalse(
            ControlPanelCenterMode.STICK.isUnlocked(
                cameraUnlocked = true,
                radarUnlocked = true,
                stickUnlocked = false,
                plotsUnlocked = true,
            ),
        )
        assertTrue(
            ControlPanelCenterMode.STICK.isUnlocked(
                cameraUnlocked = false,
                radarUnlocked = false,
                stickUnlocked = true,
                plotsUnlocked = false,
            ),
        )
        assertFalse(
            ControlPanelCenterMode.CAMERA.isUnlocked(
                cameraUnlocked = false,
                radarUnlocked = true,
                stickUnlocked = true,
                plotsUnlocked = true,
            ),
        )
        assertTrue(
            ControlPanelCenterMode.CAMERA.isUnlocked(
                cameraUnlocked = true,
                radarUnlocked = false,
                stickUnlocked = false,
                plotsUnlocked = false,
            ),
        )
        assertFalse(
            ControlPanelCenterMode.RADAR.isUnlocked(
                cameraUnlocked = true,
                radarUnlocked = false,
                stickUnlocked = true,
                plotsUnlocked = true,
            ),
        )
        assertTrue(
            ControlPanelCenterMode.RADAR.isUnlocked(
                cameraUnlocked = false,
                radarUnlocked = true,
                stickUnlocked = false,
                plotsUnlocked = false,
            ),
        )
    }

    @Test
    fun `fromStored defaults to terminal`() {
        assertEquals(ControlPanelCenterMode.TERMINAL, ControlPanelCenterMode.fromStored(null))
        assertEquals(ControlPanelCenterMode.TERMINAL, ControlPanelCenterMode.fromStored("NOPE"))
        assertEquals(
            ControlPanelCenterMode.CAMERA,
            ControlPanelCenterMode.fromStored("CAMERA"),
        )
        assertEquals(
            ControlPanelCenterMode.STICK,
            ControlPanelCenterMode.fromStored("STICK"),
        )
    }

    @Test
    fun `camera hardware settings when Control Panel extras are unlocked`() {
        assertTrue(
            ApplicationId.CONTROL_PANEL.showsCameraHardwareRoleSettings(
                centerExtrasUnlocked = true,
            ),
        )
        assertFalse(
            ApplicationId.CONTROL_PANEL.showsCameraHardwareRoleSettings(
                centerExtrasUnlocked = false,
            ),
        )
        assertFalse(
            ApplicationId.RC_VEHICLE_PRO.showsCameraHardwareRoleSettings(
                centerExtrasUnlocked = true,
            ),
        )
    }

    @Test
    fun `camera hardware roles only when center pane is camera`() {
        assertFalse(
            ControlPanelCenterMode.TERMINAL.enablesCameraHardwareRoles(extrasUnlocked = true),
        )
        assertFalse(
            ControlPanelCenterMode.PLOTS.enablesCameraHardwareRoles(extrasUnlocked = true),
        )
        assertFalse(
            ControlPanelCenterMode.STICK.enablesCameraHardwareRoles(extrasUnlocked = true),
        )
        assertFalse(
            ControlPanelCenterMode.RADAR.enablesCameraHardwareRoles(extrasUnlocked = true),
        )
        assertFalse(
            ControlPanelCenterMode.CAMERA.enablesCameraHardwareRoles(extrasUnlocked = false),
        )
        assertTrue(
            ControlPanelCenterMode.CAMERA.enablesCameraHardwareRoles(extrasUnlocked = true),
        )
    }

    @Test
    fun `hides cam picker when center camera is off`() {
        assertFalse(
            ApplicationId.CONTROL_PANEL.showsCameraHardwareRolePicker(
                centerExtrasUnlocked = true,
                centerCameraEnabled = false,
            ),
        )
        assertTrue(
            ApplicationId.CONTROL_PANEL.showsCameraHardwareRolePicker(
                centerExtrasUnlocked = true,
                centerCameraEnabled = true,
            ),
        )
        assertFalse(
            ApplicationId.CONTROL_PANEL.showsCameraHardwareRolePicker(
                centerExtrasUnlocked = false,
                centerCameraEnabled = true,
            ),
        )
    }

    @Test
    fun `locked extras ignore stored cam board`() {
        assertEquals(
            CameraHardwareRole.NO_CAM,
            ApplicationId.CONTROL_PANEL.effectiveCameraHardwareRole(
                stored = CameraHardwareRole.ONE_CAM,
                userType = SettingsUserType.NORMAL,
                centerExtrasUnlocked = false,
                centerCameraEnabled = false,
            ),
        )
        assertEquals(
            CameraHardwareRole.NO_CAM,
            ApplicationId.CONTROL_PANEL.effectiveCameraHardwareRole(
                stored = CameraHardwareRole.ONE_CAM,
                userType = SettingsUserType.NORMAL,
                centerExtrasUnlocked = false,
                centerCameraEnabled = true,
            ),
        )
        assertEquals(
            CameraHardwareRole.TWO_DEVICES,
            ApplicationId.CONTROL_PANEL.effectiveCameraHardwareRole(
                stored = CameraHardwareRole.ONE_CAM,
                userType = SettingsUserType.NORMAL,
                centerExtrasUnlocked = true,
                centerCameraEnabled = true,
            ),
        )
        assertEquals(
            CameraHardwareRole.TWO_DEVICES,
            ApplicationId.CONTROL_PANEL.effectiveCameraHardwareRole(
                stored = CameraHardwareRole.TWO_DEVICES,
                userType = SettingsUserType.NORMAL,
                centerExtrasUnlocked = true,
                centerCameraEnabled = true,
            ),
        )
        assertEquals(
            CameraHardwareRole.NO_CAM,
            ApplicationId.CONTROL_PANEL.effectiveCameraHardwareRole(
                stored = CameraHardwareRole.ONE_CAM,
                userType = SettingsUserType.NORMAL,
                centerExtrasUnlocked = true,
                centerCameraEnabled = false,
            ),
        )
    }
}
