package com.micsbol.telecon4esp32.domain.camera

/**
 * SoftAP HUD decode/publish frequency override for weaker phones.
 * Does not change SoftAP CTRL period or firmware `/camconfig` stream FPS.
 */
enum class SoftApHudProcessingRate {
    /** Follow [SoftApPerformancePreset.maxHudFps]. */
    AUTO,

    /** Very weak / hot SoftAP devices. */
    FPS_5,

    /** Weak mid-range. */
    FPS_8,

    /** Typical Smooth / Poco. */
    FPS_10,

    /** Mild throttle. */
    FPS_12,

    /** Balanced-like. */
    FPS_15,

    /** Strong phones only — no hard cap. */
    UNCAPPED;

    /** Null = no hard cap beyond SoftAP publish floor. */
    fun resolveMaxHudFps(preset: SoftApPerformancePreset): Int? = when (this) {
        AUTO -> preset.maxHudFps
        FPS_5 -> 5
        FPS_8 -> 8
        FPS_10 -> 10
        FPS_12 -> 12
        FPS_15 -> 15
        UNCAPPED -> null
    }

    companion object {
        val DEFAULT = AUTO

        fun fromStored(value: String?): SoftApHudProcessingRate =
            value?.let { runCatching { valueOf(it) }.getOrNull() } ?: DEFAULT
    }
}
