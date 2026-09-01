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

    /**
     * Control Panel camera center view.
     * Independent of [ADVANCED_PROTOCOL] and [CONTROL_PANEL_RADAR].
     */
    CONTROL_PANEL_CENTER_EXTRAS,

    /**
     * Control Panel radar center view.
     * Independent of [ADVANCED_PROTOCOL] and [CONTROL_PANEL_CENTER_EXTRAS].
     */
    CONTROL_PANEL_RADAR,

    /**
     * Control Panel session CSV recording and export.
     * Independent of camera, radar, and [ADVANCED_PROTOCOL].
     */
    CONTROL_PANEL_SESSION_CSV,

    /**
     * Advanced connection / protocol settings (binary, BLE, SoftAP Binary).
     * Default (Simple) settings stay free. Does not unlock camera or radar.
     */
    ADVANCED_PROTOCOL,

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
}
