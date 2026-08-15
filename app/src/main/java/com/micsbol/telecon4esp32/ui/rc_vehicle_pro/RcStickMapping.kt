package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import com.micsbol.telecon4esp32.domain.model.RcVehicleProControlSettings
import kotlin.math.abs

/**
 * Phone-side stick pipeline for RC Vehicle Pro (no ESP32 change required):
 * raw → deadzone → expo → travel (dual rate) → reverse → trim (steer X only).
 */
object RcStickMapping {

    fun mapThrottleY(rawY: Float, settings: RcVehicleProControlSettings): Float {
        var y = RcStickDeadzone.applyAxis(rawY, settings.deadzone)
        y = applyExpo(y, settings.throttleExpo)
        y *= settings.throttleTravel.coerceIn(0.1f, 1f)
        if (settings.reverseThrottle) y = -y
        return y.coerceIn(-1f, 1f)
    }

    fun mapSteerX(rawX: Float, settings: RcVehicleProControlSettings): Float {
        var x = RcStickDeadzone.applyAxis(rawX, settings.deadzone)
        x = applyExpo(x, settings.steerExpo)
        x *= settings.steerTravel.coerceIn(0.1f, 1f)
        if (settings.reverseSteer) x = -x
        x = RcSteerTrim.apply(x, settings.steerTrim)
        return x.coerceIn(-1f, 1f)
    }

    fun mapThrottleStick(
        rawX: Float,
        rawY: Float,
        settings: RcVehicleProControlSettings,
    ): Pair<Float, Float> = Pair(0f, mapThrottleY(rawY, settings))

    fun mapSteerStick(
        rawX: Float,
        rawY: Float,
        settings: RcVehicleProControlSettings,
    ): Pair<Float, Float> = Pair(mapSteerX(rawX, settings), 0f)

    /**
     * Exponential curve: soft near center, full authority at ends.
     * [expo] 0 = linear, 1 ≈ cubic.
     */
    fun applyExpo(value: Float, expo: Float): Float {
        val amount = expo.coerceIn(0f, 1f)
        if (amount <= 0f || value == 0f) return value.coerceIn(-1f, 1f)
        val v = value.coerceIn(-1f, 1f)
        val curved = (1f - amount) * v + amount * v * v * v
        return curved.coerceIn(-1f, 1f)
    }

    fun nextTravelPreset(current: Float): Float {
        val presets = RcVehicleProControlSettings.TRAVEL_PRESETS
        val idx = presets.indexOfFirst { abs(it - current) < 0.01f }
        return presets[(idx + 1).mod(presets.size)]
    }

    fun travelPercentLabel(travel: Float): Int =
        (travel.coerceIn(0.1f, 1f) * 100f).toInt()
}
