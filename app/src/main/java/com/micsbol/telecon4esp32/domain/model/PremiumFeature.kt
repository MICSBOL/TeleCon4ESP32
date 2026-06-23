package com.micsbol.telecon4esp32.domain.model

/**
 * Catalog of capabilities that require premium access.
 *
 * Add new entries here before gating UI or behavior.
 */
enum class PremiumFeature {
    /** No banner or interstitial ads. */
    AD_FREE,

    /** RC Vehicle with camera and advanced RC session. */
    RC_VEHICLE_PRO,

    /** Greenhouse monitoring and control. */
    GREENHOUSE,

    /** Solar power monitoring. */
    SOLAR_POWER,

    /** Smart home rooms and scenes. */
    SMART_HOME,

    /** Water tank level monitoring. */
    WATER_TANK,

    /** Smart door lock video intercom and GPIO control. */
    SMART_DOOR_LOCK,

    /** Smart lighting devices, scenes, and schedules. */
    SMART_LIGHTING,

    /** User-defined dashboard builder. */
    CUSTOM_DASHBOARD,
}
