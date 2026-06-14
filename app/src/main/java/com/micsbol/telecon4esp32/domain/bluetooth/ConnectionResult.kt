package com.micsbol.telecon4esp32.domain.bluetooth

sealed interface ConnectionResult {
    object ConnectionEstablished : ConnectionResult
    data class Error(val message: String) : ConnectionResult
}
