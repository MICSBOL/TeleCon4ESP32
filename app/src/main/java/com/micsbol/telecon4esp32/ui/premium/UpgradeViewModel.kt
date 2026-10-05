package com.micsbol.telecon4esp32.ui.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.micsbol.telecon4esp32.domain.billing.PremiumBillingStatus
import com.micsbol.telecon4esp32.domain.repository.IEntitlementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class UpgradeViewModel @Inject constructor(
    private val entitlementRepository: IEntitlementRepository,
) : ViewModel() {

    val premiumProductAvailable: StateFlow<Boolean> = entitlementRepository.premiumProductAvailable
    val billingStatus: StateFlow<PremiumBillingStatus> = entitlementRepository.billingStatus

    fun purchase(activity: Activity) {
        entitlementRepository.purchasePremium(activity)
    }

    fun restore() {
        entitlementRepository.restorePurchases()
    }
}
