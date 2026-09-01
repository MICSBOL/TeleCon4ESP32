package com.micsbol.telecon4esp32.ui.applications

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.CoinUnlockOption
import com.micsbol.telecon4esp32.domain.model.CoinWalletState
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.domain.model.WalletUnlockResult
import com.micsbol.telecon4esp32.domain.model.hasEntryAccess
import com.micsbol.telecon4esp32.domain.model.isFree
import com.micsbol.telecon4esp32.domain.model.premiumFeature
import com.micsbol.telecon4esp32.ui.ads.RewardedAdManager
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogBody
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.wallet.CoinMessageDialog
import com.micsbol.telecon4esp32.ui.wallet.CoinPricingTableDialog
import com.micsbol.telecon4esp32.ui.wallet.CoinUnlockDialog

fun handleApplicationClick(
    item: ApplicationCatalogItem,
    entitlement: Entitlement,
    wallet: CoinWalletState,
    requiresCoinEntry: Boolean,
    navController: NavController,
    onComingSoon: () -> Unit,
    onRequestUnlock: () -> Unit,
    onRequestUpgrade: () -> Unit,
) {
    when {
        item.id.isFree() -> navController.navigate(item.route)
        item.comingSoon -> onComingSoon()
        else -> {
            if (item.id.premiumFeature() == null) return
            if (item.id.hasEntryAccess(entitlement, wallet, requiresCoinEntry)) {
                navController.navigate(item.route)
            } else if (requiresCoinEntry) {
                onRequestUnlock()
            } else {
                onRequestUpgrade()
            }
        }
    }
}

@Composable
fun applicationBadge(item: ApplicationCatalogItem): String {
    return when {
        item.id.isFree() -> stringResource(R.string.applications_badge_free)
        item.comingSoon -> stringResource(R.string.applications_badge_coming_soon)
        else -> stringResource(R.string.applications_badge_pro)
    }
}

fun applicationTrailingAction(
    item: ApplicationCatalogItem,
    isUnlocked: Boolean,
    requiresCoinEntry: Boolean,
): ApplicationTrailingAction {
    val feature = item.id.premiumFeature()
    val showCoinsButton = requiresCoinEntry &&
        !item.id.isFree() &&
        !item.comingSoon &&
        feature != null &&
        !isUnlocked
    return when {
        item.comingSoon -> ApplicationTrailingAction.DEFAULT
        item.id.isFree() -> ApplicationTrailingAction.ENTER
        isUnlocked -> ApplicationTrailingAction.ENTER
        showCoinsButton -> ApplicationTrailingAction.UNLOCK
        else -> ApplicationTrailingAction.DEFAULT
    }
}

fun watchRewardedAd(
    activity: Activity?,
    rewardedAdManager: RewardedAdManager?,
    onGranted: () -> Unit,
    onUnavailable: () -> Unit,
) {
    if (activity != null && rewardedAdManager != null) {
        rewardedAdManager.tryShow(activity) { granted ->
            if (granted) onGranted() else onUnavailable()
        }
    } else {
        onUnavailable()
    }
}

@Composable
fun ApplicationEntryDialogs(
    navController: NavController,
    wallet: CoinWalletState,
    requiresCoinEntry: Boolean,
    activity: Activity?,
    rewardedAdManager: RewardedAdManager?,
    unlockTarget: ApplicationCatalogItem?,
    onUnlockTargetChange: (ApplicationCatalogItem?) -> Unit,
    explorerGiftTarget: ApplicationCatalogItem?,
    onExplorerGiftTargetChange: (ApplicationCatalogItem?) -> Unit,
    comingSoonAppName: String?,
    onComingSoonAppNameChange: (String?) -> Unit,
    unlockMessage: String?,
    onUnlockMessageChange: (String?) -> Unit,
    showPricingTable: Boolean,
    onShowPricingTableChange: (Boolean) -> Unit,
    onUnlockFeature: (
        feature: PremiumFeature,
        option: CoinUnlockOption,
        onResult: (WalletUnlockResult) -> Unit,
    ) -> Unit,
    onClaimExplorerGift: (
        feature: PremiumFeature,
        onResult: (WalletUnlockResult) -> Unit,
    ) -> Unit,
) {
    val insufficientBalanceMessage = stringResource(R.string.coins_insufficient_balance)
    val adRewardGrantedMessage = stringResource(R.string.coins_ad_reward_granted)
    val adUnavailableMessage = stringResource(R.string.coins_ad_unavailable)
    val explorerGiftClaimedMessage = stringResource(R.string.applications_explorer_gift_claimed)
    val explorerGiftAlreadyUnlockedMessage =
        stringResource(R.string.applications_explorer_gift_already_unlocked)

    val giftTarget = explorerGiftTarget
    if (giftTarget != null) {
        val giftFeature = giftTarget.id.premiumFeature()
        if (giftFeature != null) {
            ExplorerGiftDialog(
                appName = stringResource(giftTarget.titleRes),
                onDismiss = { onExplorerGiftTargetChange(null) },
                onWatchAd = {
                    watchRewardedAd(
                        activity = activity,
                        rewardedAdManager = rewardedAdManager,
                        onGranted = {
                            onClaimExplorerGift(giftFeature) { result ->
                                when (result) {
                                    WalletUnlockResult.Success -> {
                                        onExplorerGiftTargetChange(null)
                                        onUnlockMessageChange(explorerGiftClaimedMessage)
                                        navController.navigate(giftTarget.route)
                                    }
                                    WalletUnlockResult.AlreadyUnlocked -> {
                                        onExplorerGiftTargetChange(null)
                                        onUnlockMessageChange(explorerGiftAlreadyUnlockedMessage)
                                    }
                                    WalletUnlockResult.InsufficientBalance -> {
                                        onExplorerGiftTargetChange(null)
                                        onUnlockMessageChange(insufficientBalanceMessage)
                                    }
                                }
                            }
                        },
                        onUnavailable = {
                            onUnlockMessageChange(adUnavailableMessage)
                        },
                    )
                },
            )
        }
    }

    val target = unlockTarget
    if (target != null) {
        val feature = target.id.premiumFeature()
        if (feature != null) {
            CoinUnlockDialog(
                appName = stringResource(target.titleRes),
                feature = feature,
                wallet = wallet,
                onDismiss = { onUnlockTargetChange(null) },
                onUnlock = { option ->
                    onUnlockFeature(feature, option) { result ->
                        when (result) {
                            WalletUnlockResult.Success,
                            WalletUnlockResult.AlreadyUnlocked -> {
                                onUnlockTargetChange(null)
                                navController.navigate(target.route)
                            }
                            WalletUnlockResult.InsufficientBalance -> {
                                onUnlockMessageChange(insufficientBalanceMessage)
                            }
                        }
                    }
                },
                onWatchAd = {
                    watchRewardedAd(
                        activity = activity,
                        rewardedAdManager = rewardedAdManager,
                        onGranted = { onUnlockMessageChange(adRewardGrantedMessage) },
                        onUnavailable = { onUnlockMessageChange(adUnavailableMessage) },
                    )
                },
                onUpgrade = {
                    onUnlockTargetChange(null)
                    navController.navigate(Screen.Upgrade.route)
                },
            )
        }
    }

    if (showPricingTable && requiresCoinEntry) {
        CoinPricingTableDialog(
            walletBalance = wallet.balance,
            onDismiss = { onShowPricingTableChange(false) },
            onWatchAd = {
                watchRewardedAd(
                    activity = activity,
                    rewardedAdManager = rewardedAdManager,
                    onGranted = {
                        onUnlockMessageChange(adRewardGrantedMessage)
                        onShowPricingTableChange(false)
                    },
                    onUnavailable = { onUnlockMessageChange(adUnavailableMessage) },
                )
            },
        )
    }

    if (comingSoonAppName != null) {
        NeoDialog(
            onDismissRequest = { onComingSoonAppNameChange(null) },
            wrapContentHeight = true,
            title = {
                NeoDialogTitle(text = stringResource(R.string.applications_coming_soon_title))
            },
            subtitle = {
                NeoDialogBody(
                    text = stringResource(
                        R.string.applications_coming_soon_message,
                        comingSoonAppName.orEmpty(),
                    ),
                )
            },
            actions = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NeoPillButton(
                        text = stringResource(R.string.codes_dialog_ok),
                        onClick = { onComingSoonAppNameChange(null) },
                        compact = true,
                    )
                }
            },
        )
    }

    if (unlockMessage != null) {
        CoinMessageDialog(
            message = unlockMessage.orEmpty(),
            onDismiss = { onUnlockMessageChange(null) },
        )
    }
}
