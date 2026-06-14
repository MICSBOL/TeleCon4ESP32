package com.micsbol.telecon4esp32.domain.model
data class RcState(
    val leftStickX: Int = 127,
    val leftStickY: Int = 127,
    val rightStickX: Int = 127,
    val rightStickY: Int = 127,
    val switch1: Boolean = false,
    val switch2: Boolean = false,
    val switch3: Boolean = false,
    val switch4: Boolean = false,
    val switch5: Boolean = false,
    val switch6: Boolean = false,
    val switch7: Boolean = false,
    val switch8: Boolean = false,
    val leftKnobValue: Int = 512,
    val rightKnobValue: Int = 512,
)
enum class ButtonEvent(val id: Byte) {
    CENTER_TOP_RIGHT(0x02),
    CENTER_TOP_LEFT(0x01),
    CENTER_BOTTOM_RIGHT(0x04),
    CENTER_BOTTOM_LEFT(0x03)
}
