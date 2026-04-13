package com.example.emitterapp.domain.model

import com.example.emitterapp.domain.model.JoystickMode

data class UserSettings(
    val leftStickMode: JoystickMode = JoystickMode.Spring(),
    val rightStickMode: JoystickMode = JoystickMode.Spring(),
    val switchInitialStates: Map<Int, Boolean> = (0..5).associateWith { false },
    val leftKnobInitialValue: Float = 0.5f,
    val rightKnobInitialValue: Float = 0.5f,
    val rcUiStyle: RcUiStyle = RcUiStyle.SCREEN_3D
)