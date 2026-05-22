package com.micsbol.emitterapp.domain.bluetooth

sealed interface ConnectionResult {
    object ConnectionEstablished : ConnectionResult
    data class Error(val message: String) : ConnectionResult
}
