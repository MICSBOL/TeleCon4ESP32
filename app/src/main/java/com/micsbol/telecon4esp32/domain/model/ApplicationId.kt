package com.micsbol.telecon4esp32.domain.model

/**
 * Built-in application profiles shown on the Modules screen.
 */
enum class ApplicationId {
    CONTROL_PANEL,
    RC_VEHICLE_PRO,
}

fun ApplicationId.isFree(): Boolean = this == ApplicationId.CONTROL_PANEL

fun ApplicationId.isShipped(): Boolean = true

fun shippedApplicationCount(): Int = ApplicationId.entries.size

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
}
