package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.model.ApplicationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolHandshakeTest {

    @Test
    fun buildConnectLine_usesSimpleWireValue() {
        assertEquals(
            "GH:CONNECT,proto,simple",
            ProtocolHandshake.buildConnectLine(ApplicationId.GREENHOUSE, BluetoothProtocolMode.SIMPLE),
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
    fun parseAckAppId_mapsGhPrefix() {
        assertEquals(
            ApplicationId.GREENHOUSE,
            ProtocolHandshake.parseAckAppId(mapOf("app" to "GH")),
        )
    }

    @Test
    fun parseNakReason_appMismatch() {
        val failure = ProtocolHandshake.parseNakReason(
            mapOf("reason" to "app_mismatch", "expected" to "GH", "actual" to "RC"),
        )
        assertTrue(failure is HandshakeFailure.AppMismatch)
        val mismatch = failure as HandshakeFailure.AppMismatch
        assertEquals("GH", mismatch.device)
        assertEquals("RC", mismatch.requested)
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
}
