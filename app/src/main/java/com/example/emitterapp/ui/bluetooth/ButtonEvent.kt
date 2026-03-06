package com.example.emitterapp.ui.bluetooth

enum class ButtonEvent(val id: Byte) {
    CENTER_TOP_RIGHT(0x02),
    CENTER_TOP_LEFT(0x01),
    CENTER_BOTTOM_RIGHT(0x04),
    CENTER_BOTTOM_LEFT(0x03)
}