package com.micsbol.telecon4esp32.domain.bluetooth

sealed interface ConnectionResult {
    /** RFCOMM socket is open; handshake may still be pending in [BluetoothViewModel]. */
    data object SocketEstablished : ConnectionResult

    data class SessionEstablished(val session: ActiveBluetoothSession) : ConnectionResult

    data class Error(val message: String) : ConnectionResult
}
