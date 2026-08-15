package com.micsbol.telecon4esp32.domain.model

/**
 * Explorer sparkle gift cadence: 4h unlock, then cool down before the sparkle returns.
 */
object ExplorerGiftPolicy {
    const val COOLDOWN_MS: Long = 3L * 24 * 60 * 60 * 1000

    fun isAvailable(lastClaimEpochMs: Long?, nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        if (lastClaimEpochMs == null) return true
        return nowEpochMs - lastClaimEpochMs >= COOLDOWN_MS
    }

    fun millisUntilAvailable(
        lastClaimEpochMs: Long?,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Long {
        if (lastClaimEpochMs == null) return 0L
        return (lastClaimEpochMs + COOLDOWN_MS - nowEpochMs).coerceAtLeast(0L)
    }
}
