package com.micsbol.telecon4esp32.data.billing

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.micsbol.telecon4esp32.BuildConfig
import com.micsbol.telecon4esp32.domain.billing.BillingConstants
import com.micsbol.telecon4esp32.domain.billing.PremiumBillingStatus
import com.micsbol.telecon4esp32.domain.billing.grantsPremium
import com.micsbol.telecon4esp32.domain.billing.shouldRevokeMissingPremium
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.PremiumSource
import com.micsbol.telecon4esp32.domain.repository.IEntitlementRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.premiumDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "premium_entitlement",
)

private val PREMIUM_OWNED = booleanPreferencesKey("premium_owned")

/**
 * Maps the Play one-time product [BillingConstants.PREMIUM_PRODUCT_ID] to [Entitlement.Premium].
 * License-tester purchases use the same purchase state as a real buy and are not charged.
 */
@Singleton
class PlayBillingEntitlementRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : IEntitlementRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _entitlement = MutableStateFlow<Entitlement>(Entitlement.Free)
    override val entitlement: StateFlow<Entitlement> = _entitlement.asStateFlow()

    private val _premiumProductAvailable = MutableStateFlow(false)
    override val premiumProductAvailable: StateFlow<Boolean> = _premiumProductAvailable.asStateFlow()

    private val _billingStatus = MutableStateFlow(PremiumBillingStatus.Connecting)
    override val billingStatus: StateFlow<PremiumBillingStatus> = _billingStatus.asStateFlow()

    private var billingClient: BillingClient? = null
    private var productDetails: com.android.billingclient.api.ProductDetails? = null
    private var connectionStarted = false

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        scope.launch { onPurchasesUpdated(billingResult, purchases) }
    }

    init {
        scope.launch {
            if (BuildConfig.CLOSED_TEST_UNLOCK) {
                _entitlement.value = Entitlement.Premium(PremiumSource.LICENSE_TESTER)
            } else if (context.premiumDataStore.data.first()[PREMIUM_OWNED] == true) {
                _entitlement.value = Entitlement.Premium(PremiumSource.PURCHASE)
            }
            connect()
        }
    }

    override suspend fun refresh() {
        val client = billingClient
        if (client == null || !client.isReady) {
            connect()
            return
        }
        loadProduct()
        syncOwnedPurchases(reportRestore = false)
    }

    override fun purchasePremium(activity: Activity) {
        val client = billingClient
        val details = productDetails
        if (client == null || !client.isReady || details == null) {
            _billingStatus.value = if (client?.isReady == true && details == null) {
                PremiumBillingStatus.ProductUnavailable
            } else {
                PremiumBillingStatus.Unavailable
            }
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build(),
                ),
            )
            .build()
        val result = client.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _billingStatus.value = PremiumBillingStatus.Failed
        }
    }

    override fun restorePurchases() {
        scope.launch {
            val client = billingClient
            if (client == null || !client.isReady) {
                _billingStatus.value = PremiumBillingStatus.Unavailable
                connect()
                return@launch
            }
            syncOwnedPurchases(reportRestore = true)
        }
    }

    private fun connect() {
        val existing = billingClient
        if (existing?.isReady == true) {
            scope.launch {
                loadProduct()
                syncOwnedPurchases(reportRestore = false)
            }
            return
        }
        if (connectionStarted && existing != null) return
        connectionStarted = true
        val client = existing ?: BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
            )
            .build()
            .also { billingClient = it }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                scope.launch {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        loadProduct()
                        syncOwnedPurchases(reportRestore = false)
                    } else {
                        _premiumProductAvailable.value = false
                        _billingStatus.value = PremiumBillingStatus.Unavailable
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                connectionStarted = false
                _premiumProductAvailable.value = false
            }
        })
    }

    private suspend fun loadProduct() {
        val client = billingClient ?: return
        if (!client.isReady) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingConstants.PREMIUM_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _premiumProductAvailable.value = false
            if (_billingStatus.value == PremiumBillingStatus.Connecting) {
                _billingStatus.value = PremiumBillingStatus.Unavailable
            }
            return
        }
        val details = result.productDetailsList?.firstOrNull()
        productDetails = details
        _premiumProductAvailable.value = details != null
        if (details == null) {
            _billingStatus.value = PremiumBillingStatus.ProductUnavailable
        } else if (
            _billingStatus.value == PremiumBillingStatus.Connecting ||
            _billingStatus.value == PremiumBillingStatus.ProductUnavailable ||
            _billingStatus.value == PremiumBillingStatus.Unavailable
        ) {
            _billingStatus.value = PremiumBillingStatus.Ready
        }
    }

    private suspend fun syncOwnedPurchases(reportRestore: Boolean) {
        val client = billingClient ?: return
        if (!client.isReady) {
            _billingStatus.value = PremiumBillingStatus.Unavailable
            return
        }
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        )
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _billingStatus.value = PremiumBillingStatus.Unavailable
            return
        }
        val sync = applyPurchases(result.purchasesList, revokeIfMissing = true)
        _billingStatus.value = when {
            reportRestore && sync.owned -> PremiumBillingStatus.Restored
            reportRestore -> PremiumBillingStatus.NoPurchaseFound
            sync.pending && !sync.owned -> PremiumBillingStatus.Pending
            _premiumProductAvailable.value -> PremiumBillingStatus.Ready
            else -> PremiumBillingStatus.ProductUnavailable
        }
    }

    private suspend fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: List<Purchase>?,
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val sync = applyPurchases(purchases.orEmpty(), revokeIfMissing = false)
                _billingStatus.value = when {
                    sync.pending && !sync.owned -> PremiumBillingStatus.Pending
                    else -> PremiumBillingStatus.Ready
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _billingStatus.value = PremiumBillingStatus.Ready
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                syncOwnedPurchases(reportRestore = false)
            }
            else -> _billingStatus.value = PremiumBillingStatus.Failed
        }
    }

    private suspend fun applyPurchases(
        purchases: List<Purchase>,
        revokeIfMissing: Boolean,
    ): PurchaseSync {
        val client = billingClient
        var owned = false
        var pending = false
        for (purchase in purchases) {
            val productIds = purchase.products
            if (grantsPremium(productIds, purchase.purchaseState == Purchase.PurchaseState.PURCHASED)) {
                owned = true
                if (client != null) acknowledgeIfNeeded(client, purchase)
            } else if (
                BillingConstants.PREMIUM_PRODUCT_ID in productIds &&
                purchase.purchaseState == Purchase.PurchaseState.PENDING
            ) {
                pending = true
            }
        }
        if (owned) {
            grantPremium()
        } else if (
            shouldRevokeMissingPremium(
                revokeIfMissing = revokeIfMissing,
                pending = pending,
                closedTestUnlock = BuildConfig.CLOSED_TEST_UNLOCK,
            )
        ) {
            revokePremium()
        }
        return PurchaseSync(owned = owned, pending = pending)
    }

    private suspend fun acknowledgeIfNeeded(client: BillingClient, purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params)
    }

    private suspend fun grantPremium() {
        _entitlement.value = Entitlement.Premium(PremiumSource.PURCHASE)
        context.premiumDataStore.edit { preferences ->
            preferences[PREMIUM_OWNED] = true
        }
    }

    private suspend fun revokePremium() {
        _entitlement.value = Entitlement.Free
        context.premiumDataStore.edit { preferences ->
            preferences[PREMIUM_OWNED] = false
        }
    }

    private data class PurchaseSync(val owned: Boolean, val pending: Boolean)
}
