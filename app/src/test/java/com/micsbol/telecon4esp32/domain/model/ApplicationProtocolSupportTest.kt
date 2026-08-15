package com.micsbol.telecon4esp32.domain.model

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionLinkFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplicationProtocolSupportTest {

    @Test
    fun `all applications support binary protocol`() {
        ApplicationId.entries.forEach { app ->
            assertTrue(app.name, app.supportsBinaryProtocol())
        }
    }

    @Test
    fun `control panel advanced is free`() {
        assertTrue(Entitlement.Free.canUseAdvancedProtocol(ApplicationId.CONTROL_PANEL))
        assertEquals(
            BluetoothProtocolMode.ADVANCED,
            Entitlement.Free.effectiveProtocolMode(
                ApplicationId.CONTROL_PANEL,
                BluetoothProtocolMode.ADVANCED,
            ),
        )
    }

    @Test
    fun `premium required for advanced greenhouse mode`() {
        assertFalse(
            Entitlement.Free.effectiveProtocolMode(
                ApplicationId.GREENHOUSE,
                BluetoothProtocolMode.ADVANCED,
            ).let { it == BluetoothProtocolMode.ADVANCED },
        )
        assertEquals(
            BluetoothProtocolMode.ADVANCED,
            Entitlement.Premium(PremiumSource.PURCHASE).effectiveProtocolMode(
                ApplicationId.GREENHOUSE,
                BluetoothProtocolMode.ADVANCED,
            ),
        )
    }

    @Test
    fun `simple mode always allowed`() {
        assertEquals(
            BluetoothProtocolMode.SIMPLE,
            Entitlement.Free.effectiveProtocolMode(
                ApplicationId.GREENHOUSE,
                BluetoothProtocolMode.SIMPLE,
            ),
        )
    }

    @Test
    fun `connection mode maps three options`() {
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            BluetoothConnectionMode.from(
                BluetoothTransportType.CLASSIC,
                BluetoothProtocolMode.SIMPLE,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_BINARY,
            BluetoothConnectionMode.from(
                BluetoothTransportType.CLASSIC,
                BluetoothProtocolMode.ADVANCED,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.BLE_BINARY,
            BluetoothConnectionMode.from(
                BluetoothTransportType.BLE,
                BluetoothProtocolMode.SIMPLE,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.BLE_BINARY,
            BluetoothConnectionMode.from(
                BluetoothTransportType.BLE,
                BluetoothProtocolMode.ADVANCED,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.WIFI_SIMPLE,
            BluetoothConnectionMode.from(
                BluetoothTransportType.WIFI,
                BluetoothProtocolMode.SIMPLE,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.WIFI_BINARY,
            BluetoothConnectionMode.from(
                BluetoothTransportType.WIFI,
                BluetoothProtocolMode.ADVANCED,
            ),
        )
    }

    @Test
    fun `cam softap starter is free softap binary needs premium`() {
        assertTrue(
            Entitlement.Free.canUseConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothConnectionMode.WIFI_CAM_STARTER,
            ),
        )
        assertFalse(
            Entitlement.Free.canUseConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothConnectionMode.WIFI_BINARY,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.WIFI_CAM_STARTER,
            Entitlement.Free.effectiveConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothTransportType.WIFI,
                BluetoothProtocolMode.ADVANCED,
                Esp32Board.CAM,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.WIFI_BINARY,
            Entitlement.Premium(PremiumSource.PURCHASE).effectiveConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothTransportType.WIFI,
                BluetoothProtocolMode.ADVANCED,
                Esp32Board.CAM,
            ),
        )
    }

    @Test
    fun `free users cannot select binary or ble for premium apps`() {
        assertTrue(
            Entitlement.Free.canUseConnectionMode(
                ApplicationId.GREENHOUSE,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
            ),
        )
        assertFalse(
            Entitlement.Free.canUseConnectionMode(
                ApplicationId.GREENHOUSE,
                BluetoothConnectionMode.CLASSIC_BINARY,
            ),
        )
        assertFalse(
            Entitlement.Free.canUseConnectionMode(
                ApplicationId.WATER_TANK,
                BluetoothConnectionMode.BLE_BINARY,
            ),
        )
    }

    @Test
    fun `effective connection mode falls back from ble without premium`() {
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Free.effectiveConnectionMode(
                ApplicationId.GREENHOUSE,
                BluetoothTransportType.BLE,
                BluetoothProtocolMode.ADVANCED,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.BLE_BINARY,
            Entitlement.Premium(PremiumSource.PURCHASE).effectiveConnectionMode(
                ApplicationId.GREENHOUSE,
                BluetoothTransportType.BLE,
                BluetoothProtocolMode.ADVANCED,
            ),
        )
    }

    @Test
    fun `cam board offers starter softap and softap binary`() {
        listOf(
            ApplicationId.RC_VEHICLE_PRO,
            ApplicationId.GREENHOUSE,
            ApplicationId.SMART_DOOR_LOCK,
        ).forEach { app ->
            assertEquals(
                app.name,
                listOf(
                    BluetoothConnectionMode.WIFI_CAM_STARTER,
                    BluetoothConnectionMode.WIFI_BINARY,
                ),
                app.availableConnectionModes(Esp32Board.CAM),
            )
            assertFalse(
                app.name,
                app.isConnectionModeAvailable(Esp32Board.CAM, BluetoothConnectionMode.CLASSIC_SIMPLE),
            )
            assertFalse(
                app.name,
                app.isConnectionModeAvailable(Esp32Board.CAM, BluetoothConnectionMode.CLASSIC_BINARY),
            )
        }
    }

    @Test
    fun `dev kit offers bluetooth and wifi simple binary`() {
        assertEquals(
            listOf(
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                BluetoothConnectionMode.CLASSIC_BINARY,
                BluetoothConnectionMode.BLE_BINARY,
                BluetoothConnectionMode.WIFI_SIMPLE,
                BluetoothConnectionMode.WIFI_BINARY,
            ),
            ApplicationId.RC_VEHICLE_PRO.availableConnectionModes(Esp32Board.DEV_KIT),
        )
        assertEquals(
            listOf(
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                BluetoothConnectionMode.CLASSIC_BINARY,
                BluetoothConnectionMode.BLE_BINARY,
                BluetoothConnectionMode.WIFI_SIMPLE,
                BluetoothConnectionMode.WIFI_BINARY,
            ),
            ApplicationId.CONTROL_PANEL.availableConnectionModes(Esp32Board.CAM),
        )
    }

    @Test
    fun `coerce to softap when switching to cam with classic selected`() {
        assertEquals(
            BluetoothConnectionMode.WIFI_CAM_STARTER,
            Entitlement.Free.coerceConnectionModeForBoard(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.WIFI_BINARY,
            Entitlement.Premium(PremiumSource.PURCHASE).coerceConnectionModeForBoard(
                ApplicationId.GREENHOUSE,
                Esp32Board.CAM,
                BluetoothConnectionMode.CLASSIC_BINARY,
            ),
        )
    }

    @Test
    fun `coerce to wifi simple when leaving cam softap for dev kit`() {
        assertEquals(
            BluetoothConnectionMode.WIFI_BINARY,
            Entitlement.Premium(PremiumSource.PURCHASE).coerceConnectionModeForBoard(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.WIFI_SOFTAP,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.WIFI_SIMPLE,
            Entitlement.Free.coerceConnectionModeForBoard(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.WIFI_CAM_STARTER,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.BLE_BINARY,
            Entitlement.Premium(PremiumSource.PURCHASE).coerceConnectionModeForBoard(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.BLE_BINARY,
            ),
        )
    }

    @Test
    fun `dev kit wifi respects protocol and premium`() {
        assertEquals(
            BluetoothConnectionMode.WIFI_SIMPLE,
            Entitlement.Free.effectiveConnectionMode(
                ApplicationId.GREENHOUSE,
                BluetoothTransportType.WIFI,
                BluetoothProtocolMode.SIMPLE,
                Esp32Board.DEV_KIT,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.WIFI_SIMPLE,
            Entitlement.Free.effectiveConnectionMode(
                ApplicationId.GREENHOUSE,
                BluetoothTransportType.WIFI,
                BluetoothProtocolMode.ADVANCED,
                Esp32Board.DEV_KIT,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.WIFI_BINARY,
            Entitlement.Premium(PremiumSource.PURCHASE).effectiveConnectionMode(
                ApplicationId.GREENHOUSE,
                BluetoothTransportType.WIFI,
                BluetoothProtocolMode.ADVANCED,
                Esp32Board.DEV_KIT,
            ),
        )
    }

    @Test
    fun `normal user type only offers starter simple modes`() {
        assertEquals(
            listOf(
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                BluetoothConnectionMode.WIFI_SIMPLE,
            ),
            ApplicationId.CONTROL_PANEL.availableConnectionModes(
                Esp32Board.DEV_KIT,
                SettingsUserType.NORMAL,
            ),
        )
        assertEquals(
            listOf(BluetoothConnectionMode.WIFI_CAM_STARTER),
            ApplicationId.RC_VEHICLE_PRO.availableConnectionModes(
                Esp32Board.CAM,
                SettingsUserType.NORMAL,
            ),
        )
        assertEquals(
            listOf(BluetoothConnectionMode.CLASSIC_SIMPLE),
            ApplicationId.GREENHOUSE.connectionModesForFamily(
                Esp32Board.DEV_KIT,
                ConnectionLinkFamily.BLUETOOTH,
                SettingsUserType.NORMAL,
            ),
        )
        assertEquals(
            listOf(BluetoothConnectionMode.WIFI_SIMPLE),
            ApplicationId.GREENHOUSE.connectionModesForFamily(
                Esp32Board.DEV_KIT,
                ConnectionLinkFamily.WIFI,
                SettingsUserType.NORMAL,
            ),
        )
    }

    @Test
    fun `advanced user type only offers binary protocols`() {
        assertEquals(
            listOf(
                BluetoothConnectionMode.CLASSIC_BINARY,
                BluetoothConnectionMode.BLE_BINARY,
                BluetoothConnectionMode.WIFI_BINARY,
            ),
            ApplicationId.CONTROL_PANEL.availableConnectionModes(
                Esp32Board.DEV_KIT,
                SettingsUserType.ADVANCED,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_BINARY,
            ApplicationId.GREENHOUSE.preferredConnectionMode(
                Esp32Board.DEV_KIT,
                ConnectionLinkFamily.BLUETOOTH,
                SettingsUserType.ADVANCED,
                canUseAdvanced = true,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.WIFI_BINARY,
            ApplicationId.GREENHOUSE.preferredConnectionMode(
                Esp32Board.DEV_KIT,
                ConnectionLinkFamily.WIFI,
                SettingsUserType.ADVANCED,
                canUseAdvanced = true,
            ),
        )
    }

    @Test
    fun `cam normal user type only offers starter softap`() {
        assertEquals(
            listOf(BluetoothConnectionMode.WIFI_CAM_STARTER),
            ApplicationId.RC_VEHICLE_PRO.availableConnectionModes(
                Esp32Board.CAM,
                SettingsUserType.NORMAL,
            ),
        )
        assertEquals(
            listOf(BluetoothConnectionMode.WIFI_BINARY),
            ApplicationId.RC_VEHICLE_PRO.availableConnectionModes(
                Esp32Board.CAM,
                SettingsUserType.ADVANCED,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.WIFI_CAM_STARTER,
            ApplicationId.GREENHOUSE.preferredConnectionMode(
                Esp32Board.CAM,
                ConnectionLinkFamily.WIFI,
                SettingsUserType.NORMAL,
                canUseAdvanced = true,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.WIFI_BINARY,
            ApplicationId.GREENHOUSE.preferredConnectionMode(
                Esp32Board.CAM,
                ConnectionLinkFamily.WIFI,
                SettingsUserType.ADVANCED,
                canUseAdvanced = true,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.WIFI_BINARY,
            ApplicationId.GREENHOUSE.preferredConnectionMode(
                Esp32Board.CAM,
                ConnectionLinkFamily.BLUETOOTH,
                SettingsUserType.ADVANCED,
                canUseAdvanced = true,
            ),
        )
    }

    @Test
    fun `kit B dual board offers ble binary only`() {
        assertEquals(
            listOf(BluetoothConnectionMode.BLE_BINARY),
            ApplicationId.RC_VEHICLE_PRO.availableConnectionModes(Esp32Board.CAM_AND_DEV_KIT),
        )
        assertEquals(
            listOf(BluetoothConnectionMode.BLE_BINARY),
            ApplicationId.RC_VEHICLE_PRO.availableConnectionModes(
                Esp32Board.CAM_AND_DEV_KIT,
                SettingsUserType.ADVANCED,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.BLE_BINARY,
            Entitlement.Premium(PremiumSource.PURCHASE).coerceConnectionModeForBoard(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM_AND_DEV_KIT,
                BluetoothConnectionMode.WIFI_BINARY,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.BLE_BINARY,
            ApplicationId.GREENHOUSE.preferredConnectionMode(
                Esp32Board.CAM_AND_DEV_KIT,
                ConnectionLinkFamily.WIFI,
                SettingsUserType.ADVANCED,
                canUseAdvanced = true,
            ),
        )
    }
}
