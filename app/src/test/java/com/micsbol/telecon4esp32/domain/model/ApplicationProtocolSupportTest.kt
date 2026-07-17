package com.micsbol.telecon4esp32.domain.model

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
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
    fun `connection mode maps transport and protocol`() {
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
    }

    @Test
    fun `free users cannot select binary connection modes for premium apps`() {
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
}
