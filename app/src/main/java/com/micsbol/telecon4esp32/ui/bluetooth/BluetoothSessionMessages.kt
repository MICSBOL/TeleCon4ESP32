package com.micsbol.telecon4esp32.ui.bluetooth

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectFailure
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.HandshakeFailure

@Composable
fun handshakeFailureTitle(failure: HandshakeFailure): String = when (failure) {
    is HandshakeFailure.AppMismatch ->
        stringResource(R.string.bluetooth_handshake_app_mismatch_title)
    is HandshakeFailure.ProtocolMismatch ->
        stringResource(R.string.bluetooth_handshake_proto_mismatch_title)
    HandshakeFailure.Timeout ->
        stringResource(R.string.bluetooth_handshake_timeout_title)
    is HandshakeFailure.Unknown ->
        stringResource(R.string.bluetooth_handshake_unknown_title)
}

@Composable
fun handshakeFailureMessage(failure: HandshakeFailure): String = when (failure) {
    is HandshakeFailure.AppMismatch -> stringResource(
        R.string.bluetooth_handshake_app_mismatch,
        handshakeAppLabel(failure.requested),
        handshakeAppLabel(failure.device),
    )
    is HandshakeFailure.ProtocolMismatch -> stringResource(
        R.string.bluetooth_handshake_proto_mismatch,
        handshakeProtocolLabel(failure.requested),
        handshakeProtocolLabel(failure.device),
    )
    HandshakeFailure.Timeout ->
        stringResource(R.string.bluetooth_handshake_timeout)
    is HandshakeFailure.Unknown -> stringResource(
        R.string.bluetooth_handshake_unknown,
        failure.reason,
    )
}

@Composable
fun connectFailureTitle(failure: BluetoothConnectFailure): String = when (failure) {
    BluetoothConnectFailure.MissingSessionContext ->
        stringResource(R.string.bluetooth_connection_failed_title)
    BluetoothConnectFailure.StaleFirmwareBond ->
        stringResource(R.string.bluetooth_stale_firmware_bond_title)
    is BluetoothConnectFailure.ClassicLinkFailed ->
        stringResource(R.string.bluetooth_classic_link_failed_title)
    is BluetoothConnectFailure.BleLinkFailed ->
        stringResource(R.string.bluetooth_ble_link_failed_title)
    is BluetoothConnectFailure.WifiSoftApLinkFailed ->
        stringResource(R.string.bluetooth_wifi_softap_link_failed_title)
    is BluetoothConnectFailure.Generic ->
        stringResource(R.string.bluetooth_connection_failed_title)
}

@Composable
fun connectFailureMessage(failure: BluetoothConnectFailure): String = when (failure) {
    BluetoothConnectFailure.MissingSessionContext ->
        stringResource(R.string.bluetooth_missing_session_context)
    BluetoothConnectFailure.StaleFirmwareBond ->
        stringResource(R.string.bluetooth_stale_firmware_bond_body)
    is BluetoothConnectFailure.ClassicLinkFailed ->
        stringResource(R.string.bluetooth_classic_link_failed_body)
    is BluetoothConnectFailure.BleLinkFailed ->
        stringResource(R.string.bluetooth_ble_link_failed_body)
    is BluetoothConnectFailure.WifiSoftApLinkFailed -> {
        val ssid = failure.softApSsid.orEmpty()
        val isCamSoftAp = failure.connectionMode.isCamSoftApControl ||
            ssid.contains("TeleCon-RC-CAM", ignoreCase = true)
        stringResource(
            if (isCamSoftAp) {
                R.string.bluetooth_wifi_softap_link_failed_body
            } else {
                R.string.bluetooth_wifi_devkit_softap_link_failed_body
            },
        )
    }
    is BluetoothConnectFailure.Generic -> stringResource(
        R.string.bluetooth_connection_failed_body,
        failure.technicalDetail,
    )
}

@Composable
fun protocolModeLabel(mode: BluetoothProtocolMode): String =
    when (mode) {
        BluetoothProtocolMode.SIMPLE ->
            stringResource(R.string.bluetooth_session_protocol_simple)
        BluetoothProtocolMode.ADVANCED ->
            stringResource(R.string.bluetooth_session_protocol_advanced)
    }

@Composable
private fun handshakeProtocolLabel(wireValue: String?): String {
    val normalized = wireValue?.trim()?.lowercase().orEmpty()
    return when (normalized) {
        "simple", "text" -> stringResource(R.string.bluetooth_session_protocol_simple)
        "binary", "advanced" -> stringResource(R.string.bluetooth_session_protocol_advanced)
        "wifi" -> stringResource(R.string.bluetooth_session_protocol_wifi)
        "" -> stringResource(R.string.bluetooth_handshake_value_unknown)
        else -> wireValue!!.trim()
    }
}

@Composable
private fun handshakeAppLabel(wireValue: String?): String {
    val normalized = wireValue?.trim()?.uppercase().orEmpty()
    return when (normalized) {
        "RC" -> stringResource(R.string.bluetooth_firmware_app_rc)
        "" -> stringResource(R.string.bluetooth_handshake_value_unknown)
        else -> wireValue!!.trim()
    }
}
