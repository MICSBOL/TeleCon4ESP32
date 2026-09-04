package com.micsbol.telecon4esp32.domain.model

/**
 * Persisted drive-assist and stick-mode preferences for RC Vehicle Pro
 * (phone-side mapping only; independent of Control Panel [UserSettings]).
 */
data class RcVehicleProControlSettings(
    val leftStickMode: JoystickMode = DEFAULT_LEFT_STICK_MODE,
    val rightStickMode: JoystickMode = DEFAULT_RIGHT_STICK_MODE,
    val leftTrimX: Float = 0f,
    val leftTrimY: Float = 0f,
    val rightTrimX: Float = 0f,
    val rightTrimY: Float = 0f,
    /** 0…1 dual-rate scale for throttle after expo. */
    val throttleTravel: Float = 1f,
    /** 0…1 dual-rate scale for steering after expo. */
    val steerTravel: Float = 1f,
    val reverseThrottle: Boolean = false,
    val reverseSteer: Boolean = false,
    /** 0 = linear, 1 = strong expo near center. */
    val steerExpo: Float = 0.35f,
    val throttleExpo: Float = 0.15f,
    val deadzone: Float = 0.08f,
    /** Uniform scale for both stick containers (1 = layout max). */
    val stickGroupScale: Float = 1f,
    /** Uniform scale for the camera-pan knob card (1 = default size). */
    val cameraPanScale: Float = 1f,
) {
    companion object {
        val DEFAULT_LEFT_STICK_MODE = JoystickMode.VerticalHold(JoystickMode.DOWN)
        val DEFAULT_RIGHT_STICK_MODE = JoystickMode.HorizontalSpring(JoystickMode.CENTER)
        val DEFAULT = RcVehicleProControlSettings()
        val TRAVEL_PRESETS = listOf(0.50f, 0.75f, 1.00f)

        /**
         * Prefer the saved [JoystickMode] string. Older builds only stored hold flags;
         * map those to the previous car defaults.
         */
        fun leftStickModeFromPersisted(
            stored: String?,
            throttleHold: Boolean?,
        ): JoystickMode {
            if (!stored.isNullOrBlank()) return JoystickMode.fromString(stored)
            return if (throttleHold ?: true) {
                JoystickMode.VerticalHold(JoystickMode.DOWN)
            } else {
                JoystickMode.VerticalSpring(JoystickMode.CENTER)
            }
        }

        fun rightStickModeFromPersisted(
            stored: String?,
            steeringHold: Boolean?,
        ): JoystickMode {
            if (!stored.isNullOrBlank()) return JoystickMode.fromString(stored)
            return if (steeringHold == true) {
                JoystickMode.HorizontalHold(JoystickMode.CENTER)
            } else {
                JoystickMode.HorizontalSpring(JoystickMode.CENTER)
            }
        }
    }
}
