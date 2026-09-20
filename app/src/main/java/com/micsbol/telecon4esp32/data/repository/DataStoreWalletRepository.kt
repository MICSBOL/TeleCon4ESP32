package com.micsbol.telecon4esp32.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.micsbol.telecon4esp32.BuildConfig
import com.micsbol.telecon4esp32.domain.model.CoinUnlockOption
import com.micsbol.telecon4esp32.domain.model.CoinEconomy
import com.micsbol.telecon4esp32.domain.model.CoinWalletState
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.FeatureGrant
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.domain.model.WalletUnlockResult
import com.micsbol.telecon4esp32.domain.model.hasPremiumAccess
import com.micsbol.telecon4esp32.domain.repository.IWalletRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private object WalletPreferencesKeys {
    val COIN_BALANCE = intPreferencesKey("coin_balance")
    val TIMED_GRANTS = stringPreferencesKey("timed_grants")
}

private val Context.walletDataStore: DataStore<Preferences> by preferencesDataStore(name = "coin_wallet")

@Singleton
class DataStoreWalletRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : IWalletRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionGrants = MutableStateFlow<Map<PremiumFeature, FeatureGrant>>(emptyMap())

    private val persistedWallet = context.walletDataStore.data.map { preferences ->
        val balance = preferences[WalletPreferencesKeys.COIN_BALANCE] ?: 0
        val timedGrants = parseTimedGrants(preferences[WalletPreferencesKeys.TIMED_GRANTS])
        balance to timedGrants
    }

    private val _wallet = MutableStateFlow(CoinWalletState.Empty)
    override val wallet: StateFlow<CoinWalletState> = _wallet.asStateFlow()

    init {
        scope.launch {
            if (BuildConfig.DEBUG) {
                context.walletDataStore.edit { preferences ->
                    preferences[WalletPreferencesKeys.COIN_BALANCE] = CoinEconomy.DEBUG_STARTING_BALANCE
                }
            }
            combine(persistedWallet, sessionGrants) { (balance, timedGrants), sessions ->
                CoinWalletState(balance = balance, grants = timedGrants + sessions)
            }.collect { state ->
                _wallet.value = state
            }
        }
    }

    override suspend fun addCoins(amount: Int) {
        if (amount <= 0) return
        context.walletDataStore.edit { preferences ->
            val current = preferences[WalletPreferencesKeys.COIN_BALANCE] ?: 0
            preferences[WalletPreferencesKeys.COIN_BALANCE] = current + amount
        }
    }

    override suspend fun unlockFeature(
        feature: PremiumFeature,
        option: CoinUnlockOption,
        nowEpochMs: Long,
    ): WalletUnlockResult {
        pruneExpiredGrants(nowEpochMs)
        val current = _wallet.value
        if (hasPremiumAccess(Entitlement.Free, feature, current, nowEpochMs)) {
            return WalletUnlockResult.AlreadyUnlocked
        }

        require(option.isCoinPurchasable) { "Option $option is not coin-purchasable" }

        val cost = option.coinCost(feature)
        if (current.balance < cost) {
            return WalletUnlockResult.InsufficientBalance
        }

        val grant = FeatureGrant(
            feature = feature,
            option = option,
            expiresAtEpochMs = option.durationMillis()?.let { nowEpochMs + it },
        )

        context.walletDataStore.edit { preferences ->
            preferences[WalletPreferencesKeys.COIN_BALANCE] = current.balance - cost
            if (!grant.isSessionOnly) {
                val timed = parseTimedGrants(preferences[WalletPreferencesKeys.TIMED_GRANTS])
                    .toMutableMap()
                timed[feature] = mergeTimedGrant(timed[feature], grant, nowEpochMs)
                preferences[WalletPreferencesKeys.TIMED_GRANTS] = serializeTimedGrants(timed)
            }
        }

        if (grant.isSessionOnly) {
            sessionGrants.update { it + (feature to grant) }
        }

        return WalletUnlockResult.Success
    }

    override suspend fun grantTimedAccess(
        feature: PremiumFeature,
        option: CoinUnlockOption,
        nowEpochMs: Long,
    ): WalletUnlockResult {
        pruneExpiredGrants(nowEpochMs)
        val current = _wallet.value
        if (hasPremiumAccess(Entitlement.Free, feature, current, nowEpochMs)) {
            return WalletUnlockResult.AlreadyUnlocked
        }

        val duration = option.durationMillis()
            ?: return WalletUnlockResult.InsufficientBalance

        val grant = FeatureGrant(
            feature = feature,
            option = option,
            expiresAtEpochMs = nowEpochMs + duration,
        )

        context.walletDataStore.edit { preferences ->
            val timed = parseTimedGrants(preferences[WalletPreferencesKeys.TIMED_GRANTS])
                .toMutableMap()
            timed[feature] = mergeTimedGrant(timed[feature], grant, nowEpochMs)
            preferences[WalletPreferencesKeys.TIMED_GRANTS] = serializeTimedGrants(timed)
        }

        return WalletUnlockResult.Success
    }

    override fun clearSessionGrant(feature: PremiumFeature) {
        sessionGrants.update { it - feature }
    }

    override suspend fun pruneExpiredGrants(nowEpochMs: Long) {
        context.walletDataStore.edit { preferences ->
            val timed = parseTimedGrants(preferences[WalletPreferencesKeys.TIMED_GRANTS])
            val active = timed.filterValues { it.isActive(nowEpochMs) }
            if (active.size != timed.size) {
                preferences[WalletPreferencesKeys.TIMED_GRANTS] = serializeTimedGrants(active)
            }
        }
    }

    private fun mergeTimedGrant(
        existing: FeatureGrant?,
        incoming: FeatureGrant,
        nowEpochMs: Long,
    ): FeatureGrant {
        val duration = incoming.option.durationMillis() ?: return incoming
        val base = when {
            existing == null || !existing.isActive(nowEpochMs) -> nowEpochMs
            existing.expiresAtEpochMs == null -> nowEpochMs
            else -> existing.expiresAtEpochMs
        }
        return incoming.copy(expiresAtEpochMs = base + duration)
    }

    private fun parseTimedGrants(raw: String?): Map<PremiumFeature, FeatureGrant> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(';')
            .mapNotNull { entry ->
                val parts = entry.split(':')
                if (parts.size != 3) return@mapNotNull null
                val feature = runCatching { PremiumFeature.valueOf(parts[0]) }.getOrNull()
                    ?: return@mapNotNull null
                val option = CoinUnlockOption.fromStoredName(parts[1])
                    ?: return@mapNotNull null
                val expiresAt = parts[2].toLongOrNull() ?: return@mapNotNull null
                feature to FeatureGrant(feature, option, expiresAt)
            }
            .toMap()
    }

    private fun serializeTimedGrants(grants: Map<PremiumFeature, FeatureGrant>): String =
        grants.values
            .filter { !it.isSessionOnly && it.expiresAtEpochMs != null }
            .joinToString(";") { grant ->
                "${grant.feature.name}:${grant.option.name}:${grant.expiresAtEpochMs}"
            }
}
