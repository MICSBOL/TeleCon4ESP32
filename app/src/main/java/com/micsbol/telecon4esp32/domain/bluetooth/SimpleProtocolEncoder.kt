package com.micsbol.telecon4esp32.domain.bluetooth

import com.micsbol.telecon4esp32.domain.model.ButtonEvent
import com.micsbol.telecon4esp32.domain.model.RcState

/**
 * Builds outbound simple-protocol lines for the RC control panel.
 *
 * Inbound RC telemetry types handled by the app (ESP32 → phone):
 * - `RC:DATA` — panels, indicators, LEDs
 * - `RC:PLOTCFG` — plot series names (`n0`, `n1`, …)
 * - `RC:PLOT` — plot samples (`v0`, `v1`, … as 0–255)
 */
object SimpleProtocolEncoder {

    const val RC_APP = "RC"

    fun buildRcCtrlLine(state: RcState): String = LineProtocolCodec.encode(
        app = RC_APP,
        type = "CTRL",
        pairs = mapOf(
            "lx" to state.leftStickX,
            "ly" to state.leftStickY,
            "rx" to state.rightStickX,
            "ry" to state.rightStickY,
            "lk" to state.leftKnobValue,
            "rk" to state.rightKnobValue,
            "sw" to buildSwitchHex(state),
        ),
    )

    fun buildRcButtonLine(event: ButtonEvent): String = LineProtocolCodec.encode(
        app = RC_APP,
        type = "BTN",
        pairs = mapOf("id" to (event.id.toInt() and 0xFF)),
    )

    fun buildSetLine(appPrefix: String, pairs: Map<String, Any>): String =
        LineProtocolCodec.encode(appPrefix, "SET", pairs)
}

private fun buildSwitchHex(state: RcState): String {
    var switches = 0
    if (state.switch1) switches = switches or (1 shl 0)
    if (state.switch2) switches = switches or (1 shl 1)
    if (state.switch3) switches = switches or (1 shl 2)
    if (state.switch4) switches = switches or (1 shl 3)
    if (state.switch5) switches = switches or (1 shl 4)
    if (state.switch6) switches = switches or (1 shl 5)
    if (state.switch7) switches = switches or (1 shl 6)
    if (state.switch8) switches = switches or (1 shl 7)
    return switches.toString(16).uppercase().padStart(2, '0')
}
