package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.protocolPrefix

/** CONNECT / ACK / NAK line helpers for application-scoped Bluetooth sessions. */
object ProtocolHandshake {

    const val CONNECT_TYPE = "CONNECT"
    const val ACK_TYPE = "ACK"
    const val NAK_TYPE = "NAK"

    fun buildConnectLine(
        appPrefix: String,
        protocolMode: BluetoothProtocolMode,
    ): String = LineProtocolCodec.encode(
        app = appPrefix,
        type = CONNECT_TYPE,
        pairs = mapOf("proto" to protocolMode.toWireValue()),
    )

    fun buildConnectLine(applicationId: ApplicationId, protocolMode: BluetoothProtocolMode): String =
        buildConnectLine(applicationId.protocolPrefix(), protocolMode)

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
        "GH" -> ApplicationId.GREENHOUSE
        "SP" -> ApplicationId.SOLAR_POWER
        "SH" -> ApplicationId.SMART_HOME
        "WT" -> ApplicationId.WATER_TANK
        "DL" -> ApplicationId.SMART_DOOR_LOCK
        "LT" -> ApplicationId.SMART_LIGHTING
        else -> null
    }

    private fun BluetoothProtocolMode.toWireValue(): String = when (this) {
        BluetoothProtocolMode.SIMPLE -> "simple"
        BluetoothProtocolMode.ADVANCED -> "binary"
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

    data class Unknown(val reason: String) : HandshakeFailure
}
