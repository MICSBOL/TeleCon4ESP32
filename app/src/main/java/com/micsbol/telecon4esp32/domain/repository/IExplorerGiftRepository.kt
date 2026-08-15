package com.micsbol.telecon4esp32.domain.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Explorer sparkle gift with a multi-day cooldown between claims.
 * Last-claim time is mirrored to a MediaStore marker (API 29+) so reinstall
 * cannot immediately reset the cooldown.
 */
interface IExplorerGiftRepository {

    /** `true` when the sparkle may be shown / claimed again. */
    val isAvailable: StateFlow<Boolean>

    suspend fun markClaimed(nowEpochMs: Long = System.currentTimeMillis())
}
