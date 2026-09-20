package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.model.ApplicationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolHandshakeTest {

    @Test
    fun buildConnectLine_usesSimpleWireValue() {
        assertEquals(
            "RC:CONNECT,proto,simple",
            ProtocolHandshake.buildConnectLine(ApplicationId.CONTROL_PANEL, BluetoothProtocolMode.SIMPLE),
        )
    }

    @Test
    fun buildConnectLine_usesBinaryWireValue() {
        assertEquals(
            "RC:CONNECT,proto,binary",
            ProtocolHandshake.buildConnectLine(ApplicationId.CONTROL_PANEL, BluetoothProtocolMode.ADVANCED),
        )
    }

    @Test
    fun buildConnectLine_legacyCamSoftApUsesWifiWireValue() {
        assertEquals(
            "RC:CONNECT,proto,wifi",
            ProtocolHandshake.buildConnectLine(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothConnectionMode.WIFI_SOFTAP,
            ),
        )
    }

    @Test
    fun buildConnectLine_camSoftApBinaryUsesBinaryWireValue() {
        assertEquals(
            "RC:CONNECT,proto,binary",
            ProtocolHandshake.buildConnectLine(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothConnectionMode.WIFI_BINARY,
            ),
        )
    }

    @Test
    fun buildConnectLine_camStarterUsesSimpleWireValue() {
        assertEquals(
            "RC:CONNECT,proto,simple",
            ProtocolHandshake.buildConnectLine(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothConnectionMode.WIFI_CAM_STARTER,
            ),
        )
    }

    @Test
    fun buildConnectLine_legacyDevKitWifiSimpleEnumStillUsesSimpleWireValue() {
        assertEquals(
            "RC:CONNECT,proto,simple",
            ProtocolHandshake.buildConnectLine(
                ApplicationId.CONTROL_PANEL,
                BluetoothConnectionMode.WIFI_SIMPLE,
            ),
        )
    }

    @Test
    fun buildConnectLine_devKitWifiBinaryUsesBinaryWireValue() {
        assertEquals(
            "RC:CONNECT,proto,binary",
            ProtocolHandshake.buildConnectLine(
                ApplicationId.CONTROL_PANEL,
                BluetoothConnectionMode.WIFI_BINARY,
            ),
        )
    }

    @Test
    fun buildConnectLine_wifiTransportPlusSimpleDefaultsToDevKitSimpleNotCamWifi() {
        // Ambiguous transport+mode maps to the withdrawn WIFI_SIMPLE enum; CAM Simple is WIFI_CAM_STARTER.
        assertEquals(
            "RC:CONNECT,proto,simple",
            ProtocolHandshake.buildConnectLine(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothProtocolMode.SIMPLE,
                BluetoothTransportType.WIFI,
            ),
        )
    }

    @Test
    fun buildConnectLine_classicKeepsSimpleWireValue() {
        assertEquals(
            "RC:CONNECT,proto,simple",
            ProtocolHandshake.buildConnectLine(
                ApplicationId.RC_VEHICLE_PRO,
                BluetoothProtocolMode.SIMPLE,
                BluetoothTransportType.CLASSIC,
            ),
        )
    }

    @Test
    fun parseAckAppId_mapsRcPrefix() {
        assertEquals(
            ApplicationId.CONTROL_PANEL,
            ProtocolHandshake.parseAckAppId(mapOf("app" to "RC")),
        )
    }

    @Test
    fun parseNakReason_appMismatch() {
        val failure = ProtocolHandshake.parseNakReason(
            mapOf("reason" to "app_mismatch", "expected" to "RC", "actual" to "XX"),
        )
        assertTrue(failure is HandshakeFailure.AppMismatch)
        val mismatch = failure as HandshakeFailure.AppMismatch
        assertEquals("RC", mismatch.device)
        assertEquals("XX", mismatch.requested)
    }

    @Test
    fun parseNakReason_protoMismatch_mapsFirmwareAsDevice() {
        val failure = ProtocolHandshake.parseNakReason(
            mapOf("reason" to "proto_mismatch", "expected" to "simple", "actual" to "binary"),
        )
        assertTrue(failure is HandshakeFailure.ProtocolMismatch)
        val mismatch = failure as HandshakeFailure.ProtocolMismatch
        // Firmware NAK: expected = device protocol, actual = app CONNECT proto.
        assertEquals("simple", mismatch.device)
        assertEquals("binary", mismatch.requested)
    }

    @Test
    fun parseNakReason_protoMismatch_wifi() {
        val failure = ProtocolHandshake.parseNakReason(
            mapOf("reason" to "proto_mismatch", "expected" to "wifi", "actual" to "simple"),
        )
        assertTrue(failure is HandshakeFailure.ProtocolMismatch)
        val mismatch = failure as HandshakeFailure.ProtocolMismatch
        assertEquals("wifi", mismatch.device)
        assertEquals("simple", mismatch.requested)
    }
}
