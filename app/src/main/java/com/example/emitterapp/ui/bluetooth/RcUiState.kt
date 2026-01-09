package com.example.emitterapp.ui.bluetooth

data class RcUiState(
    val leftStickX: Int = 127,
    val leftStickY: Int = 127,
    val rightStickX: Int = 127,
    val rightStickY: Int = 127,
    val leftKnob: Int = 0,
    val rightKnob: Int = 0,
    val switch1: Boolean = false,
    val switch2: Boolean = false,
    val switch3: Boolean = false,
    val switch4: Boolean = false,
    val switch5: Boolean = false,
    val switch6: Boolean = false,
    val switch7: Boolean = false,
    val switch8: Boolean = false,
    val leftKnobValue: Int = 512 ,
    val rightKnobValue: Int = 512,
)