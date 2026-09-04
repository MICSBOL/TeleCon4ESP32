package com.micsbol.telecon4esp32.domain.bluetooth

/**
 * A decoded line-protocol message from the ESP32.
 *
 * Example wire form: `RC:DATA,batt,82,left,150`
 */
data class EspMessage(
    val app: String,
    val type: String,
    val values: Map<String, String>,
)
