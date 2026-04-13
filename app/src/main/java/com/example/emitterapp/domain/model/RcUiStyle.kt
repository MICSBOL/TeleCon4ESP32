package com.example.emitterapp.domain.model

enum class RcUiStyle {
    SCREEN_3D,
    SCREEN_LED;

    companion object {
        fun fromString(value: String?): RcUiStyle =
            entries.firstOrNull { it.name == value } ?: SCREEN_3D
    }
}
