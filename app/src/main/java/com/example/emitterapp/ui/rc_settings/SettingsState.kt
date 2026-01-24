package com.example.emitterapp.ui.rc_settings

import com.example.emitterapp.ui.rc_screen.components.JoystickMode

data class SettingsState(
    val leftStickMode: JoystickMode = JoystickMode.Spring(),
    val rightStickMode: JoystickMode = JoystickMode.Spring()
)