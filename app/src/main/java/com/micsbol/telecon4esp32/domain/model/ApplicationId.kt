package com.micsbol.telecon4esp32.domain.model

/**
 * Built-in application profiles shown on the Modules screen.
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

/**
 * Applications included in the current Play Store build.
 *
 * Non-shipped modules remain in git history / tag `archive/full-apps-with-media`
 * for later re-incorporation. Flip this gate (and restore UI + media) to ship more apps.
 */
fun ApplicationId.isShipped(): Boolean = when (this) {
    ApplicationId.CONTROL_PANEL,
    ApplicationId.RC_VEHICLE_PRO,
    -> true
    else -> false
}

fun shippedApplicationCount(): Int = ApplicationId.entries.count { it.isShipped() }

/**
 * Dedicated Catalog screen is only useful once Home would be crowded.
 * While [shippedApplicationCount] is ≤ this value, modules live on Home only.
 */
const val APPLICATION_CATALOG_VISIBLE_AFTER: Int = 3

fun isApplicationCatalogVisible(): Boolean =
    shippedApplicationCount() > APPLICATION_CATALOG_VISIBLE_AFTER

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
