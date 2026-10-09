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
    fun `control panel advanced requires unlock for free users`() {
        assertFalse(Entitlement.Free.canUseAdvancedProtocol(ApplicationId.CONTROL_PANEL))
        assertEquals(
            BluetoothProtocolMode.ADVANCED,
            Entitlement.Free.effectiveProtocolMode(
                ApplicationId.CONTROL_PANEL,
                BluetoothProtocolMode.ADVANCED,
            ),
        )
    }

    @Test
    fun `control panel extras grant does not unlock advanced protocol`() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS to FeatureGrant(
                    feature = PremiumFeature.CONTROL_PANEL_CENTER_EXTRAS,
                    option = CoinUnlockOption.HOURS_24,
                    expiresAtEpochMs = 4_000_000_000_000L,
                ),
            ),
        )
        assertTrue(
            Entitlement.Free.canUseControlPanelCenterExtras(
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
        assertFalse(
            Entitlement.Free.canUseAdvancedProtocol(
                ApplicationId.CONTROL_PANEL,
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
        assertEquals(
            BluetoothProtocolMode.ADVANCED,
            Entitlement.Free.effectiveProtocolMode(
                ApplicationId.CONTROL_PANEL,
                BluetoothProtocolMode.ADVANCED,
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
    }

    @Test
    fun `control panel advanced unlocks with advanced protocol grant`() {
        val wallet = CoinWalletState(
            grants = mapOf(
                PremiumFeature.ADVANCED_PROTOCOL to FeatureGrant(
                    feature = PremiumFeature.ADVANCED_PROTOCOL,
                    option = CoinUnlockOption.HOURS_24,
                    expiresAtEpochMs = 4_000_000_000_000L,
                ),
            ),
        )
        assertTrue(
            Entitlement.Free.canUseAdvancedProtocol(
                ApplicationId.CONTROL_PANEL,
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
        assertEquals(
            BluetoothProtocolMode.ADVANCED,
            Entitlement.Free.effectiveProtocolMode(
                ApplicationId.CONTROL_PANEL,
                BluetoothProtocolMode.ADVANCED,
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
        assertFalse(
            Entitlement.Free.canUseControlPanelCenterExtras(
                wallet = wallet,
                requiresCoinEntry = true,
            ),
        )
    }

    @Test
    fun `premium required for advanced rc vehicle mode`() {
        assertTrue(
            Entitlement.Free.effectiveProtocolMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothProtocolMode.ADVANCED,
            ).let { it == BluetoothProtocolMode.ADVANCED },
        )
        assertEquals(
            BluetoothProtocolMode.ADVANCED,
            Entitlement.Premium(PremiumSource.PURCHASE).effectiveProtocolMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothProtocolMode.ADVANCED,
                requiresCoinEntry = false,
            ),
        )
    }

    @Test
    fun `simple mode always allowed`() {
        assertEquals(
            BluetoothProtocolMode.SIMPLE,
            Entitlement.Free.effectiveProtocolMode(
                ApplicationId.RC_VEHICLE_PRO,
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
    fun `withdrawn wifi control modes are not selectable`() {
        assertFalse(
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
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Free.effectiveConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothTransportType.WIFI,
                BluetoothProtocolMode.ADVANCED,
                Esp32Board.CAM,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Premium(PremiumSource.PURCHASE).effectiveConnectionMode(
                ApplicationId.CONTROL_PANEL,
                BluetoothTransportType.WIFI,
                BluetoothProtocolMode.SIMPLE,
                Esp32Board.CAM,
            ),
        )
    }

    @Test
    fun `free users can select every bluetooth mode`() {
        assertTrue(
            Entitlement.Free.canUseConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
            ),
        )
        assertTrue(
            Entitlement.Free.canUseConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothConnectionMode.CLASSIC_BINARY,
            ),
        )
        assertTrue(
            Entitlement.Free.canUseConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothConnectionMode.BLE_BINARY,
            ),
        )
        assertFalse(
            Entitlement.Free.canUseConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothConnectionMode.WIFI_SIMPLE,
            ),
        )
    }

    @Test
    fun `effective connection mode keeps ble without a premium gate`() {
        assertEquals(
            BluetoothConnectionMode.BLE_BINARY,
            Entitlement.Free.effectiveConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothTransportType.BLE,
                BluetoothProtocolMode.ADVANCED,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.BLE_BINARY,
            Entitlement.Premium(PremiumSource.PURCHASE).effectiveConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothTransportType.BLE,
                BluetoothProtocolMode.ADVANCED,
            ),
        )
    }

    @Test
    fun `every board offers the three bluetooth modes on the list`() {
        assertEquals(
            listOf(
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                BluetoothConnectionMode.CLASSIC_BINARY,
                BluetoothConnectionMode.BLE_BINARY,
            ),
            ApplicationId.RC_VEHICLE_PRO.availableConnectionModes(Esp32Board.CAM),
        )
        assertTrue(
            ApplicationId.RC_VEHICLE_PRO.isConnectionModeAvailable(
                Esp32Board.CAM,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
            ),
        )
        assertEquals(
            listOf(
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                BluetoothConnectionMode.CLASSIC_BINARY,
                BluetoothConnectionMode.BLE_BINARY,
            ),
            ApplicationId.CONTROL_PANEL.availableConnectionModes(Esp32Board.CAM),
        )
        assertTrue(
            ApplicationId.CONTROL_PANEL.isConnectionModeAvailable(
                Esp32Board.CAM,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
            ),
        )
    }

    @Test
    fun `control panel default user type offers classic simple not wifi simple`() {
        assertEquals(
            listOf(
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                BluetoothConnectionMode.CLASSIC_BINARY,
                BluetoothConnectionMode.BLE_BINARY,
            ),
            ApplicationId.CONTROL_PANEL.availableConnectionModes(
                Esp32Board.DEV_KIT,
                SettingsUserType.NORMAL,
            ),
        )
        assertFalse(
            ApplicationId.CONTROL_PANEL.isConnectionModeAvailable(
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.WIFI_CAM_STARTER,
            ),
        )
        assertFalse(
            ApplicationId.CONTROL_PANEL.isConnectionModeAvailable(
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.WIFI_SIMPLE,
            ),
        )
    }

    @Test
    fun `dev kit offers the three bluetooth modes`() {
        assertEquals(
            listOf(
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                BluetoothConnectionMode.CLASSIC_BINARY,
                BluetoothConnectionMode.BLE_BINARY,
            ),
            ApplicationId.RC_VEHICLE_PRO.availableConnectionModes(Esp32Board.DEV_KIT),
        )
        assertEquals(
            listOf(
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                BluetoothConnectionMode.CLASSIC_BINARY,
                BluetoothConnectionMode.BLE_BINARY,
            ),
            ApplicationId.CONTROL_PANEL.availableConnectionModes(Esp32Board.DEV_KIT),
        )
    }

    @Test
    fun `classic modes stay classic on a stored cam board`() {
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Free.coerceConnectionModeForBoard(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_BINARY,
            Entitlement.Premium(PremiumSource.PURCHASE).coerceConnectionModeForBoard(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM,
                BluetoothConnectionMode.CLASSIC_BINARY,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Premium(PremiumSource.PURCHASE).coerceConnectionModeForBoard(
                ApplicationId.CONTROL_PANEL,
                Esp32Board.CAM,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
            ),
        )
    }

    @Test
    fun `coerce withdrawn wifi simple and cam starter to classic on dev kit`() {
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Premium(PremiumSource.PURCHASE).coerceConnectionModeForBoard(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.WIFI_SOFTAP,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Free.coerceConnectionModeForBoard(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.WIFI_CAM_STARTER,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Free.coerceConnectionModeForBoard(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.WIFI_SIMPLE,
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
    fun `live control icon follows stored classic simple even if transport is wifi`() {
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            liveControlConnectionMode(
                applicationId = ApplicationId.CONTROL_PANEL,
                board = Esp32Board.DEV_KIT,
                storedMode = BluetoothConnectionMode.CLASSIC_SIMPLE,
                transport = BluetoothTransportType.WIFI,
                protocol = BluetoothProtocolMode.SIMPLE,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            liveControlConnectionMode(
                applicationId = ApplicationId.CONTROL_PANEL,
                board = Esp32Board.DEV_KIT,
                storedMode = BluetoothConnectionMode.WIFI_SIMPLE,
                transport = BluetoothTransportType.WIFI,
                protocol = BluetoothProtocolMode.SIMPLE,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            liveControlConnectionMode(
                applicationId = ApplicationId.CONTROL_PANEL,
                board = Esp32Board.DEV_KIT,
                storedMode = BluetoothConnectionMode.WIFI_BINARY,
                transport = BluetoothTransportType.WIFI,
                protocol = BluetoothProtocolMode.ADVANCED,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            liveControlConnectionMode(
                applicationId = ApplicationId.RC_VEHICLE_PRO,
                board = Esp32Board.CAM,
                storedMode = BluetoothConnectionMode.WIFI_CAM_STARTER,
                transport = BluetoothTransportType.WIFI,
                protocol = BluetoothProtocolMode.SIMPLE,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.BLE_BINARY,
            liveControlConnectionMode(
                applicationId = ApplicationId.RC_VEHICLE_PRO,
                board = Esp32Board.DEV_KIT,
                storedMode = BluetoothConnectionMode.BLE_BINARY,
                transport = BluetoothTransportType.BLE,
                protocol = BluetoothProtocolMode.ADVANCED,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_BINARY,
            liveControlConnectionMode(
                applicationId = ApplicationId.CONTROL_PANEL,
                board = Esp32Board.DEV_KIT,
                storedMode = BluetoothConnectionMode.CLASSIC_BINARY,
                transport = BluetoothTransportType.CLASSIC,
                protocol = BluetoothProtocolMode.SIMPLE,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            liveControlConnectionMode(
                applicationId = ApplicationId.CONTROL_PANEL,
                board = Esp32Board.DEV_KIT,
                storedMode = null,
                transport = BluetoothTransportType.CLASSIC,
                protocol = BluetoothProtocolMode.ADVANCED,
            ),
        )
    }

    @Test
    fun `dev kit wifi respects protocol and premium`() {
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Free.effectiveConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothTransportType.WIFI,
                BluetoothProtocolMode.SIMPLE,
                Esp32Board.DEV_KIT,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Free.effectiveConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothTransportType.WIFI,
                BluetoothProtocolMode.ADVANCED,
                Esp32Board.DEV_KIT,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Premium(PremiumSource.PURCHASE).effectiveConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothTransportType.WIFI,
                BluetoothProtocolMode.ADVANCED,
                Esp32Board.DEV_KIT,
            ),
        )
    }

    @Test
    fun `every board offers the three bluetooth modes`() {
        val bluetooth = listOf(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            BluetoothConnectionMode.CLASSIC_BINARY,
            BluetoothConnectionMode.BLE_BINARY,
        )
        assertEquals(
            bluetooth,
            ApplicationId.CONTROL_PANEL.availableConnectionModes(
                Esp32Board.DEV_KIT,
                SettingsUserType.NORMAL,
            ),
        )
        assertEquals(
            bluetooth,
            ApplicationId.RC_VEHICLE_PRO.availableConnectionModes(
                Esp32Board.CAM,
                SettingsUserType.ADVANCED,
            ),
        )
        assertEquals(
            bluetooth,
            ApplicationId.RC_VEHICLE_PRO.connectionModesForFamily(
                Esp32Board.DEV_KIT,
                ConnectionLinkFamily.BLUETOOTH,
                SettingsUserType.NORMAL,
            ),
        )
        assertEquals(
            emptyList<BluetoothConnectionMode>(),
            ApplicationId.RC_VEHICLE_PRO.connectionModesForFamily(
                Esp32Board.DEV_KIT,
                ConnectionLinkFamily.WIFI,
                SettingsUserType.NORMAL,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            ApplicationId.RC_VEHICLE_PRO.preferredConnectionMode(
                Esp32Board.DEV_KIT,
                ConnectionLinkFamily.WIFI,
                SettingsUserType.NORMAL,
                canUseAdvanced = false,
            ),
        )
    }

    @Test
    fun `legacy dual board offers classic simple classic binary and ble binary`() {
        assertEquals(
            listOf(
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                BluetoothConnectionMode.CLASSIC_BINARY,
                BluetoothConnectionMode.BLE_BINARY,
            ),
            ApplicationId.RC_VEHICLE_PRO.availableConnectionModes(Esp32Board.CAM_AND_DEV_KIT),
        )
        assertEquals(
            listOf(
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                BluetoothConnectionMode.CLASSIC_BINARY,
                BluetoothConnectionMode.BLE_BINARY,
            ),
            ApplicationId.RC_VEHICLE_PRO.availableConnectionModes(
                Esp32Board.CAM_AND_DEV_KIT,
                SettingsUserType.ADVANCED,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Premium(PremiumSource.PURCHASE).coerceConnectionModeForBoard(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM_AND_DEV_KIT,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Premium(PremiumSource.PURCHASE).coerceConnectionModeForBoard(
                ApplicationId.RC_VEHICLE_PRO,
                Esp32Board.CAM_AND_DEV_KIT,
                BluetoothConnectionMode.WIFI_BINARY,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            ApplicationId.RC_VEHICLE_PRO.preferredConnectionMode(
                Esp32Board.CAM_AND_DEV_KIT,
                ConnectionLinkFamily.WIFI,
                SettingsUserType.ADVANCED,
                canUseAdvanced = true,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Premium(PremiumSource.PURCHASE).effectiveConnectionMode(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothTransportType.CLASSIC,
                BluetoothProtocolMode.SIMPLE,
                Esp32Board.CAM_AND_DEV_KIT,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.BLE_BINARY,
            Entitlement.Premium(PremiumSource.PURCHASE).effectiveConnectionMode(
                ApplicationId.CONTROL_PANEL,
                BluetoothTransportType.BLE,
                BluetoothProtocolMode.ADVANCED,
                Esp32Board.CAM_AND_DEV_KIT,
            ),
        )
    }

    @Test
    fun `softap camera overlay keeps classic simple classic binary and ble binary`() {
        val controlPanel = ApplicationId.CONTROL_PANEL.availableConnectionModes(Esp32Board.DEV_KIT)
        val dual = ApplicationId.CONTROL_PANEL.availableConnectionModes(Esp32Board.CAM_AND_DEV_KIT)
        val bluetoothModes = listOf(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            BluetoothConnectionMode.CLASSIC_BINARY,
            BluetoothConnectionMode.BLE_BINARY,
        )
        bluetoothModes.forEach { mode ->
            assertTrue(mode.name, mode in controlPanel)
            assertTrue("dual ${mode.name}", mode in dual)
        }
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            Entitlement.Premium(PremiumSource.PURCHASE).coerceConnectionModeForBoard(
                ApplicationId.CONTROL_PANEL,
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
            ),
        )
        assertEquals(
            BluetoothConnectionMode.CLASSIC_BINARY,
            Entitlement.Premium(PremiumSource.PURCHASE).coerceConnectionModeForBoard(
                ApplicationId.CONTROL_PANEL,
                Esp32Board.DEV_KIT,
                BluetoothConnectionMode.CLASSIC_BINARY,
            ),
        )
    }

    @Test
    fun `original default connection is classic simple on default board`() {
        ApplicationId.entries.forEach { app ->
            assertEquals(
                app.name,
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                app.originalDefaultConnectionMode(),
            )
            assertEquals(Esp32Board.DEV_KIT, Esp32Board.defaultFor(app))
        }
    }

    @Test
    fun `default two device overlay uses classic simple bluetooth`() {
        assertEquals(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            ApplicationId.CONTROL_PANEL.preferredConnectionMode(
                Esp32Board.DEV_KIT,
                ConnectionLinkFamily.BLUETOOTH,
                SettingsUserType.NORMAL,
                canUseAdvanced = false,
            ),
        )
        assertEquals(
            listOf(
                BluetoothConnectionMode.CLASSIC_SIMPLE,
                BluetoothConnectionMode.CLASSIC_BINARY,
                BluetoothConnectionMode.BLE_BINARY,
            ),
            ApplicationId.CONTROL_PANEL.connectionModesForFamily(
                Esp32Board.DEV_KIT,
                ConnectionLinkFamily.BLUETOOTH,
                SettingsUserType.NORMAL,
            ),
        )
    }
}
