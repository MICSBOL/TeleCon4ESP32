package com.micsbol.telecon4esp32.domain.model

import com.micsbol.telecon4esp32.domain.model.JoystickMode

data class UserSettings(
    val leftStickMode: JoystickMode = JoystickMode.Spring(),
    val rightStickMode: JoystickMode = JoystickMode.Spring(),
    val switchInitialStates: Map<Int, Boolean> = (0..5).associateWith { false },
    val leftKnobInitialValue: Float = 0.5f,
    val rightKnobInitialValue: Float = 0.5f
)