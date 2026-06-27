package com.micsbol.telecon4esp32.domain.bluetooth

/**
 * A decoded line-protocol message from the ESP32.
 *
 * Example wire form: `GH:DATA,temp,26.2,hum,68,fan,1`
 */
data class EspMessage(
    val app: String,
    val type: String,
    val values: Map<String, String>,
)
