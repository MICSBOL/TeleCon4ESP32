package com.example.emitterapp.domain.model

enum class RcUiStyle {
    SCREEN_3D,
    SCREEN_LED;

    fun toRoute(): String = when (this) {
        SCREEN_3D  -> "rc_screen"
        SCREEN_LED -> "rc_screen_led_style"
    }

    companion object {
        fun fromString(value: String?): RcUiStyle =
            entries.firstOrNull { it.name == value } ?: SCREEN_3D
    }
}

