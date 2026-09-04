package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.protocolPrefix

/** CONNECT / ACK / NAK line helpers for application-scoped sessions. */
object ProtocolHandshake {

    const val CONNECT_TYPE = "CONNECT"
    const val ACK_TYPE = "ACK"
    const val NAK_TYPE = "NAK"

    fun buildConnectLine(
        appPrefix: String,
        connectionMode: BluetoothConnectionMode,
    ): String = LineProtocolCodec.encode(
        app = appPrefix,
        type = CONNECT_TYPE,
        pairs = mapOf("proto" to wireProto(connectionMode)),
    )

    fun buildConnectLine(
        applicationId: ApplicationId,
        connectionMode: BluetoothConnectionMode,
    ): String = buildConnectLine(applicationId.protocolPrefix(), connectionMode)

    /**
     * Convenience for Classic / BLE (and DevKit SoftAP when [transport] + [protocolMode]
     * already encode the mode). Prefer [buildConnectLine] with [BluetoothConnectionMode]
     * for SoftAP so CAM / DevKit SoftAP keep `simple` / `binary` (not legacy `wifi`).
     */
    fun buildConnectLine(
        appPrefix: String,
        protocolMode: BluetoothProtocolMode,
        transport: BluetoothTransportType = BluetoothTransportType.CLASSIC,
    ): String = buildConnectLine(
        appPrefix,
        BluetoothConnectionMode.from(transport, protocolMode),
    )

    fun buildConnectLine(
        applicationId: ApplicationId,
        protocolMode: BluetoothProtocolMode,
        transport: BluetoothTransportType = BluetoothTransportType.CLASSIC,
    ): String = buildConnectLine(applicationId.protocolPrefix(), protocolMode, transport)

    fun parseAckAppId(values: Map<String, String>): ApplicationId? =
        values["app"]?.let(::applicationIdFromWire)

    fun parseNakReason(values: Map<String, String>): HandshakeFailure? {
        val reason = values["reason"] ?: return HandshakeFailure.Unknown(values.toString())
        return when (reason) {
            // Firmware NAK convention: expected = device capability, actual = app CONNECT value.
            "app_mismatch" -> HandshakeFailure.AppMismatch(
                device = values["expected"],
                requested = values["actual"],
            )
            "proto_mismatch" -> HandshakeFailure.ProtocolMismatch(
                device = values["expected"],
                requested = values["actual"],
            )
            else -> HandshakeFailure.Unknown(reason)
        }
    }

    private fun applicationIdFromWire(value: String): ApplicationId? = when (value.uppercase()) {
        "RC" -> ApplicationId.CONTROL_PANEL
        else -> null
    }

    /**
     * Wire `proto` value for CONNECT.
     * SoftAP TCP uses the same `simple` / `binary` as Classic/BLE.
     * Legacy [BluetoothConnectionMode.WIFI_SOFTAP] keeps `wifi` for old firmware only.
     */
    fun wireProto(connectionMode: BluetoothConnectionMode): String = when (connectionMode) {
        BluetoothConnectionMode.WIFI_SOFTAP -> "wifi"
        BluetoothConnectionMode.CLASSIC_SIMPLE,
        BluetoothConnectionMode.WIFI_SIMPLE,
        BluetoothConnectionMode.WIFI_CAM_STARTER,
        -> "simple"
        BluetoothConnectionMode.CLASSIC_BINARY,
        BluetoothConnectionMode.BLE_BINARY,
        BluetoothConnectionMode.WIFI_BINARY,
        -> "binary"
    }
}

sealed interface HandshakeFailure {
    /**
     * @param device App prefix the ESP32 firmware serves (NAK `expected`).
     * @param requested App prefix from the phone CONNECT line (NAK `actual`).
     */
    data class AppMismatch(val device: String?, val requested: String?) : HandshakeFailure

    /**
     * @param device Protocol the ESP32 firmware supports (NAK `expected`, e.g. `simple`).
     * @param requested Protocol the phone asked for in CONNECT (NAK `actual`, e.g. `binary`).
     */
    data class ProtocolMismatch(val device: String?, val requested: String?) : HandshakeFailure

    /** ESP32 did not reply with ACK/NAK within the handshake window. */
    data object Timeout : HandshakeFailure

    data class Unknown(val reason: String) : HandshakeFailure
}
