package com.micsbol.telecon4esp32.domain.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumOwnershipTest {

    @Test
    fun purchasedPremiumProduct_grantsPremium() {
        assertTrue(
            grantsPremium(
                productIds = listOf(BillingConstants.PREMIUM_PRODUCT_ID),
                purchased = true,
            ),
        )
    }

    @Test
    fun pendingOrOtherProduct_doesNotGrantPremium() {
        assertFalse(
            grantsPremium(
                productIds = listOf(BillingConstants.PREMIUM_PRODUCT_ID),
                purchased = false,
            ),
        )
        assertFalse(
            grantsPremium(
                productIds = listOf("other_product"),
                purchased = true,
            ),
        )
    }

    @Test
    fun closedTestUnlock_keepsPremiumWhenPlayHasNoPurchase() {
        assertFalse(
            shouldRevokeMissingPremium(
                revokeIfMissing = true,
                pending = false,
                closedTestUnlock = true,
            ),
        )
    }

    @Test
    fun releaseBuild_revokesPremiumWhenPlayHasNoPurchase() {
        assertTrue(
            shouldRevokeMissingPremium(
                revokeIfMissing = true,
                pending = false,
                closedTestUnlock = false,
            ),
        )
    }
}
