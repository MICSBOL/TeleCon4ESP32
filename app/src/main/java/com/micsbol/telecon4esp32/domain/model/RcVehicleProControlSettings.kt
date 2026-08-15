package com.micsbol.telecon4esp32.domain.model

/**
 * Persisted drive-assist preferences for RC Vehicle Pro (phone-side mapping only).
 */
data class RcVehicleProControlSettings(
    val throttleHold: Boolean = true,
    val steeringHold: Boolean = false,
    val steerTrim: Float = 0f,
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
) {
    companion object {
        val DEFAULT = RcVehicleProControlSettings()
        val TRAVEL_PRESETS = listOf(0.50f, 0.75f, 1.00f)
    }
}
