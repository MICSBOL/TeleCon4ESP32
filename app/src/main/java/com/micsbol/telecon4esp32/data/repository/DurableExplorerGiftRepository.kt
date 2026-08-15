package com.micsbol.telecon4esp32.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.micsbol.telecon4esp32.domain.model.ExplorerGiftPolicy
import com.micsbol.telecon4esp32.domain.repository.IExplorerGiftRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private object ExplorerGiftKeys {
    val LAST_CLAIM_EPOCH_MS = longPreferencesKey("explorer_gift_last_claim_epoch_ms")
    /** Legacy permanent-claim flag from the one-shot gift era. */
    val CLAIMED = booleanPreferencesKey("explorer_gift_claimed")
}

private val Context.explorerGiftDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "explorer_gift",
)

/**
 * Cooldown between explorer gifts, persisted in DataStore and mirrored to MediaStore
 * so a reinstall cannot skip the wait (API 29+).
 */
@Singleton
class DurableExplorerGiftRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : IExplorerGiftRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isAvailable = MutableStateFlow(true)
    override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        scope.launch {
            migrateLegacyClaimIfNeeded()
            reconcileAvailability()
            context.explorerGiftDataStore.data
                .map { preferences -> preferences[ExplorerGiftKeys.LAST_CLAIM_EPOCH_MS] }
                .collect { lastClaimInStore ->
                    val durableLastClaim = readDurableLastClaimEpochMs()
                    val effective = maxOfNullable(lastClaimInStore, durableLastClaim)
                    if (durableLastClaim != null &&
                        (lastClaimInStore == null || durableLastClaim > lastClaimInStore)
                    ) {
                        context.explorerGiftDataStore.edit {
                            it[ExplorerGiftKeys.LAST_CLAIM_EPOCH_MS] = durableLastClaim
                        }
                    }
                    publishAvailability(effective)
                }
        }
    }

    override suspend fun markClaimed(nowEpochMs: Long) {
        context.explorerGiftDataStore.edit { preferences ->
            preferences[ExplorerGiftKeys.LAST_CLAIM_EPOCH_MS] = nowEpochMs
            preferences.remove(ExplorerGiftKeys.CLAIMED)
        }
        writeDurableLastClaim(nowEpochMs)
        publishAvailability(nowEpochMs)
    }

    private suspend fun migrateLegacyClaimIfNeeded() {
        val prefs = context.explorerGiftDataStore.data.first()
        if (prefs[ExplorerGiftKeys.LAST_CLAIM_EPOCH_MS] != null) return
        val legacyClaimed = prefs[ExplorerGiftKeys.CLAIMED] == true
        val durableRaw = readDurableMarkerRaw()
        val legacyDurable = durableRaw != null && durableRaw.toLongOrNull() == null
        if (!legacyClaimed && !legacyDurable) return
        val now = System.currentTimeMillis()
        context.explorerGiftDataStore.edit {
            it[ExplorerGiftKeys.LAST_CLAIM_EPOCH_MS] = now
            it.remove(ExplorerGiftKeys.CLAIMED)
        }
        writeDurableLastClaim(now)
    }

    private suspend fun reconcileAvailability() {
        val lastClaimInStore = context.explorerGiftDataStore.data
            .map { it[ExplorerGiftKeys.LAST_CLAIM_EPOCH_MS] }
            .first()
        val durableLastClaim = readDurableLastClaimEpochMs()
        val effective = maxOfNullable(lastClaimInStore, durableLastClaim)
        if (durableLastClaim != null &&
            (lastClaimInStore == null || durableLastClaim > lastClaimInStore)
        ) {
            context.explorerGiftDataStore.edit {
                it[ExplorerGiftKeys.LAST_CLAIM_EPOCH_MS] = durableLastClaim
            }
        }
        publishAvailability(effective)
    }

    private fun publishAvailability(lastClaimEpochMs: Long?) {
        val now = System.currentTimeMillis()
        _isAvailable.value = ExplorerGiftPolicy.isAvailable(lastClaimEpochMs, now)
        scheduleCooldownRefresh(lastClaimEpochMs, now)
    }

    private fun scheduleCooldownRefresh(lastClaimEpochMs: Long?, nowEpochMs: Long) {
        cooldownJob?.cancel()
        val waitMs = ExplorerGiftPolicy.millisUntilAvailable(lastClaimEpochMs, nowEpochMs)
        if (waitMs <= 0L) return
        cooldownJob = scope.launch {
            delay(waitMs)
            publishAvailability(lastClaimEpochMs)
        }
    }

    private suspend fun readDurableLastClaimEpochMs(): Long? {
        val raw = readDurableMarkerRaw() ?: return null
        return raw.toLongOrNull()
    }

    private suspend fun readDurableMarkerRaw(): String? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
        val uri = findDurableMarkerUri() ?: return@withContext null
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText().trim()
        }
    }

    private suspend fun writeDurableLastClaim(epochMs: Long) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext
        val payload = epochMs.toString().toByteArray()
        val existing = findDurableMarkerUri()
        if (existing != null) {
            context.contentResolver.openOutputStream(existing, "wt")?.use { it.write(payload) }
            return@withContext
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, MARKER_DISPLAY_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(MediaStore.MediaColumns.RELATIVE_PATH, MARKER_RELATIVE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri: Uri? = context.contentResolver.insert(collection, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(payload) }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }
    }

    private fun findDurableMarkerUri(): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection =
            "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        val args = arrayOf(MARKER_DISPLAY_NAME, MARKER_RELATIVE_PATH)
        return context.contentResolver.query(collection, projection, selection, args, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val id = cursor.getLong(0)
                Uri.withAppendedPath(collection, id.toString())
            }
    }

    private fun maxOfNullable(a: Long?, b: Long?): Long? = when {
        a == null -> b
        b == null -> a
        else -> maxOf(a, b)
    }

    private companion object {
        const val MARKER_DISPLAY_NAME = "telecon4esp32_explorer_gift_claimed.marker"
        const val MARKER_RELATIVE_PATH = "Documents/TeleCon4ESP32/"
    }
}
