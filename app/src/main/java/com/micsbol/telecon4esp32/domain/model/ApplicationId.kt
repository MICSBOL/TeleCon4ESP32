package com.micsbol.telecon4esp32.domain.model

/**
 * Built-in application profiles shown on the Control modules screen.
 */
enum class ApplicationId {
    CONTROL_PANEL,
    RC_VEHICLE_PRO,
    GREENHOUSE,
    SOLAR_POWER,
    SMART_HOME,
    WATER_TANK,
    SMART_DOOR_LOCK,
    SMART_LIGHTING,
}

fun ApplicationId.isFree(): Boolean = this == ApplicationId.CONTROL_PANEL

fun ApplicationId.premiumFeature(): PremiumFeature? = when (this) {
    ApplicationId.CONTROL_PANEL -> null
    ApplicationId.RC_VEHICLE_PRO -> PremiumFeature.RC_VEHICLE_PRO
    ApplicationId.GREENHOUSE -> PremiumFeature.GREENHOUSE
    ApplicationId.SOLAR_POWER -> PremiumFeature.SOLAR_POWER
    ApplicationId.SMART_HOME -> PremiumFeature.SMART_HOME
    ApplicationId.WATER_TANK -> PremiumFeature.WATER_TANK
    ApplicationId.SMART_DOOR_LOCK -> PremiumFeature.SMART_DOOR_LOCK
    ApplicationId.SMART_LIGHTING -> PremiumFeature.SMART_LIGHTING
}
