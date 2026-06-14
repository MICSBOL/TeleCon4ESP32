package com.micsbol.telecon4esp32.domain.model

/**
 * How premium access was granted. Used for analytics and debugging; not shown in UI.
 */
enum class PremiumSource {
    /** Active Google Play purchase. */
    PURCHASE,

    /** Play Console license tester account. */
    LICENSE_TESTER,

    /** Debug-only override while billing is not wired. */
    DEBUG_OVERRIDE,
}
