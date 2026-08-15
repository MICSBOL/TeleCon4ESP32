package com.micsbol.telecon4esp32.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExplorerGiftPolicyTest {

    @Test
    fun neverClaimed_isAvailable() {
        assertTrue(ExplorerGiftPolicy.isAvailable(lastClaimEpochMs = null, nowEpochMs = 1_000L))
    }

    @Test
    fun withinCooldown_isUnavailable() {
        val claimedAt = 1_000L
        val almostThreeDays = claimedAt + ExplorerGiftPolicy.COOLDOWN_MS - 1L
        assertFalse(ExplorerGiftPolicy.isAvailable(claimedAt, almostThreeDays))
    }

    @Test
    fun afterCooldown_isAvailable() {
        val claimedAt = 1_000L
        val after = claimedAt + ExplorerGiftPolicy.COOLDOWN_MS
        assertTrue(ExplorerGiftPolicy.isAvailable(claimedAt, after))
    }

    @Test
    fun millisUntilAvailable_matchesRemainingCooldown() {
        val claimedAt = 10_000L
        val now = claimedAt + 1_000L
        assertEquals(
            ExplorerGiftPolicy.COOLDOWN_MS - 1_000L,
            ExplorerGiftPolicy.millisUntilAvailable(claimedAt, now),
        )
        assertEquals(0L, ExplorerGiftPolicy.millisUntilAvailable(null, now))
    }
}
