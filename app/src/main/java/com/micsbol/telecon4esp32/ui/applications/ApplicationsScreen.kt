package com.micsbol.telecon4esp32.ui.applications

import android.app.Activity
import com.micsbol.telecon4esp32.BuildConfig
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.domain.model.WalletUnlockResult
import com.micsbol.telecon4esp32.domain.model.CoinWalletState
import com.micsbol.telecon4esp32.domain.model.has
import com.micsbol.telecon4esp32.domain.model.isFree
import com.micsbol.telecon4esp32.domain.model.premiumFeature
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy
import com.micsbol.telecon4esp32.ui.ads.LocalRewardedAdManager
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.wallet.CoinBalanceChip
import com.micsbol.telecon4esp32.ui.wallet.CoinMessageDialog
import com.micsbol.telecon4esp32.ui.wallet.CoinPricingTableDialog
import com.micsbol.telecon4esp32.ui.wallet.CoinUnlockDialog
import com.micsbol.telecon4esp32.ui.wallet.WalletViewModel

@Composable
fun ApplicationsScreen(navController: NavController) {
    val entitlement = LocalEntitlement.current
    val walletViewModel = hiltViewModel<WalletViewModel>()
    val wallet by walletViewModel.wallet.collectAsState()
    val rewardedAdManager = LocalRewardedAdManager.current
    val activity = LocalContext.current as? Activity
    val catalog = remember { defaultApplicationCatalog() }
    var comingSoonAppName by remember { mutableStateOf<String?>(null) }
    var unlockTarget by remember { mutableStateOf<ApplicationCatalogItem?>(null) }
    var unlockMessage by remember { mutableStateOf<String?>(null) }
    var showPricingTable by remember { mutableStateOf(false) }
    val insufficientBalanceMessage = stringResource(R.string.coins_insufficient_balance)
    val adRewardGrantedMessage = stringResource(R.string.coins_ad_reward_granted)
    val adUnavailableMessage = stringResource(R.string.coins_ad_unavailable)
    val requiresCoinEntry = entitlement.usesCoinEconomy() || BuildConfig.DEBUG

    NeoScaffold(
        title = stringResource(R.string.applications_title),
        subtitle = stringResource(R.string.applications_subtitle),
        onNavigateBack = { navController.navigateUp() },
        actions = {
            if (requiresCoinEntry) {
                CoinBalanceChip(
                    balance = wallet.balance,
                    modifier = Modifier
                        .clickable { showPricingTable = true }
                        .padding(end = 4.dp),
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(catalog, key = { it.id.name }) { item ->
                val title = stringResource(item.titleRes)
                val feature = item.id.premiumFeature()
                val isUnlocked = feature != null &&
                    hasApplicationEntryAccess(entitlement, feature, wallet)
                val showCoinsButton = requiresCoinEntry &&
                    !item.id.isFree() &&
                    !item.comingSoon &&
                    feature != null &&
                    !isUnlocked

                val trailingAction = when {
                    item.comingSoon -> ApplicationTrailingAction.DEFAULT
                    item.id.isFree() -> ApplicationTrailingAction.ENTER
                    isUnlocked -> ApplicationTrailingAction.ENTER
                    showCoinsButton -> ApplicationTrailingAction.UNLOCK
                    else -> ApplicationTrailingAction.DEFAULT
                }

                ApplicationListItemCard(
                    icon = item.icon,
                    title = title,
                    subtitle = stringResource(item.subtitleRes),
                    badge = applicationBadge(item, entitlement, wallet, requiresCoinEntry),
                    trailingAction = trailingAction,
                    onUnlockClick = { unlockTarget = item },
                    onClick = {
                        handleApplicationClick(
                            item = item,
                            entitlement = entitlement,
                            wallet = wallet,
                            requiresCoinEntry = requiresCoinEntry,
                            navController = navController,
                            onComingSoon = { comingSoonAppName = title },
                            onRequestUnlock = { unlockTarget = item },
                            onRequestUpgrade = { navController.navigate(Screen.Upgrade.route) },
                        )
                    },
                )
            }
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
                onDismiss = { unlockTarget = null },
                onUnlock = { option ->
                    walletViewModel.unlockFeature(feature, option) { result ->
                        when (result) {
                            WalletUnlockResult.Success,
                            WalletUnlockResult.AlreadyUnlocked -> {
                                unlockTarget = null
                                navController.navigate(target.route)
                            }
                            WalletUnlockResult.InsufficientBalance -> {
                                unlockMessage = insufficientBalanceMessage
                            }
                        }
                    }
                },
                onWatchAd = {
                    watchRewardedAd(
                        activity = activity,
                        rewardedAdManager = rewardedAdManager,
                        onGranted = { unlockMessage = adRewardGrantedMessage },
                        onUnavailable = { unlockMessage = adUnavailableMessage },
                    )
                },
                onUpgrade = {
                    unlockTarget = null
                    navController.navigate(Screen.Upgrade.route)
                },
            )
        }
    }

    if (showPricingTable && requiresCoinEntry) {
        CoinPricingTableDialog(
            walletBalance = wallet.balance,
            onDismiss = { showPricingTable = false },
            onWatchAd = {
                watchRewardedAd(
                    activity = activity,
                    rewardedAdManager = rewardedAdManager,
                    onGranted = {
                        unlockMessage = adRewardGrantedMessage
                        showPricingTable = false
                    },
                    onUnavailable = { unlockMessage = adUnavailableMessage },
                )
            },
        )
    }

    if (comingSoonAppName != null) {
        AlertDialog(
            onDismissRequest = { comingSoonAppName = null },
            title = { Text(stringResource(R.string.applications_coming_soon_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.applications_coming_soon_message,
                        comingSoonAppName.orEmpty(),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { comingSoonAppName = null }) {
                    Text(stringResource(R.string.codes_dialog_ok))
                }
            },
        )
    }

    if (unlockMessage != null) {
        CoinMessageDialog(
            message = unlockMessage.orEmpty(),
            onDismiss = { unlockMessage = null },
        )
    }
}

private fun watchRewardedAd(
    activity: Activity?,
    rewardedAdManager: com.micsbol.telecon4esp32.ui.ads.RewardedAdManager?,
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

private fun handleApplicationClick(
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
            val feature = item.id.premiumFeature() ?: return
            if (hasApplicationEntryAccess(entitlement, feature, wallet, requiresCoinEntry)) {
                navController.navigate(item.route)
            } else if (requiresCoinEntry) {
                onRequestUnlock()
            } else {
                onRequestUpgrade()
            }
        }
    }
}

/**
 * Pro apps need an active coin grant to enter for free/debug users.
 * Premium subscribers skip the coin requirement unless [requiresCoinEntry] is true (debug).
 */
private fun hasApplicationEntryAccess(
    entitlement: Entitlement,
    feature: PremiumFeature,
    wallet: CoinWalletState,
    requiresCoinEntry: Boolean = entitlement.usesCoinEconomy() || BuildConfig.DEBUG,
    nowEpochMs: Long = System.currentTimeMillis(),
): Boolean {
    if (!requiresCoinEntry && entitlement.has(feature)) return true
    if (!requiresCoinEntry) return false
    val grant = wallet.grants[feature] ?: return false
    return grant.isActive(nowEpochMs)
}

@Composable
private fun applicationBadge(
    item: ApplicationCatalogItem,
    entitlement: Entitlement,
    wallet: CoinWalletState,
    requiresCoinEntry: Boolean,
): String {
    return when {
        item.id.isFree() -> stringResource(R.string.applications_badge_free)
        item.comingSoon -> stringResource(R.string.applications_badge_coming_soon)
        else -> {
            val feature = item.id.premiumFeature()
            if (feature != null && hasApplicationEntryAccess(entitlement, feature, wallet, requiresCoinEntry)) {
                if (!requiresCoinEntry && entitlement.has(feature)) {
                    stringResource(R.string.applications_badge_pro_unlocked)
                } else {
                    stringResource(R.string.applications_badge_coins_unlocked)
                }
            } else {
                stringResource(R.string.applications_badge_pro)
            }
        }
    }
}
