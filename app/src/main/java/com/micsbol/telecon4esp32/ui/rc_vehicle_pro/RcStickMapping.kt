package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import com.micsbol.telecon4esp32.domain.model.JoystickAxis
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.domain.model.JoystickRangeShape
import com.micsbol.telecon4esp32.domain.model.RcVehicleProControlSettings
import kotlin.math.abs

/**
 * Phone-side stick pipeline for RC Vehicle Pro (no ESP32 change required):
 * raw → range shape (circle/square) → axis mask from [JoystickMode] → deadzone
 * → expo → travel (dual rate) → reverse → trim (per stick, per enabled axis).
 */
object RcStickMapping {

    fun mapThrottleY(rawY: Float, settings: RcVehicleProControlSettings): Float {
        val y = mapAssistAxis(
            raw = rawY,
            deadzone = settings.deadzone,
            expo = settings.throttleExpo,
            travel = settings.throttleTravel,
            reverse = settings.reverseThrottle,
        )
        return RcStickTrim.apply(y, settings.leftTrimY)
    }

    fun mapSteerX(rawX: Float, settings: RcVehicleProControlSettings): Float {
        val x = mapAssistAxis(
            raw = rawX,
            deadzone = settings.deadzone,
            expo = settings.steerExpo,
            travel = settings.steerTravel,
            reverse = settings.reverseSteer,
        )
        return RcStickTrim.apply(x, settings.rightTrimX)
    }

    fun mapThrottleStick(
        rawX: Float,
        rawY: Float,
        settings: RcVehicleProControlSettings,
    ): Pair<Float, Float> = mapStick(
        rawX = rawX,
        rawY = rawY,
        mode = settings.leftStickMode,
        rangeShape = settings.leftStickRangeShape,
        mapX = {
            RcStickTrim.apply(
                mapAssistAxis(it, settings.deadzone, settings.throttleExpo, settings.throttleTravel, settings.reverseThrottle),
                settings.leftTrimX,
            )
        },
        mapY = { mapThrottleY(it, settings) },
    )

    fun mapSteerStick(
        rawX: Float,
        rawY: Float,
        settings: RcVehicleProControlSettings,
    ): Pair<Float, Float> = mapStick(
        rawX = rawX,
        rawY = rawY,
        mode = settings.rightStickMode,
        rangeShape = settings.rightStickRangeShape,
        mapX = { mapSteerX(it, settings) },
        mapY = {
            RcStickTrim.apply(
                mapAssistAxis(it, settings.deadzone, settings.steerExpo, settings.steerTravel, settings.reverseSteer),
                settings.rightTrimY,
            )
        },
    )

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

    private fun mapStick(
        rawX: Float,
        rawY: Float,
        mode: JoystickMode,
        rangeShape: JoystickRangeShape,
        mapX: (Float) -> Float,
        mapY: (Float) -> Float,
    ): Pair<Float, Float> {
        val shaped = rangeShape.mapOutput(rawX, rawY)
        val x = if (mode.usesHorizontal) mapX(shaped.first) else 0f
        val y = if (mode.usesVertical) mapY(shaped.second) else 0f
        return Pair(x, y)
    }

    private fun mapAssistAxis(
        raw: Float,
        deadzone: Float,
        expo: Float,
        travel: Float,
        reverse: Boolean,
    ): Float {
        var value = RcStickDeadzone.applyAxis(raw, deadzone)
        value = applyExpo(value, expo)
        value *= travel.coerceIn(0.1f, 1f)
        if (reverse) value = -value
        return value.coerceIn(-1f, 1f)
    }
}

private val JoystickMode.usesHorizontal: Boolean
    get() = axis != JoystickAxis.VERTICAL

private val JoystickMode.usesVertical: Boolean
    get() = axis != JoystickAxis.HORIZONTAL
