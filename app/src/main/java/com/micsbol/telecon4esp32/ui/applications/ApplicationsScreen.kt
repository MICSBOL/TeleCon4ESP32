package com.micsbol.telecon4esp32.ui.applications

import android.app.Activity
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import com.micsbol.telecon4esp32.BuildConfig
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.CoinUnlockOption
import com.micsbol.telecon4esp32.domain.model.CoinWalletState
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.FeatureGrant
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.domain.model.WalletUnlockResult
import com.micsbol.telecon4esp32.domain.model.has
import com.micsbol.telecon4esp32.domain.model.hasEntryAccess
import com.micsbol.telecon4esp32.domain.model.isFree
import com.micsbol.telecon4esp32.domain.model.premiumFeature
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy
import com.micsbol.telecon4esp32.ui.ads.LocalRewardedAdManager
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme
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
    val explorerGiftAvailable by walletViewModel.explorerGiftAvailable.collectAsState()
    val rewardedAdManager = LocalRewardedAdManager.current
    val activity = LocalContext.current as? Activity
    val catalog = remember { defaultApplicationCatalog() }
    var comingSoonAppName by remember { mutableStateOf<String?>(null) }
    var unlockTarget by remember { mutableStateOf<ApplicationCatalogItem?>(null) }
    var explorerGiftTarget by remember { mutableStateOf<ApplicationCatalogItem?>(null) }
    var unlockMessage by remember { mutableStateOf<String?>(null) }
    var showPricingTable by remember { mutableStateOf(false) }
    val insufficientBalanceMessage = stringResource(R.string.coins_insufficient_balance)
    val adRewardGrantedMessage = stringResource(R.string.coins_ad_reward_granted)
    val adUnavailableMessage = stringResource(R.string.coins_ad_unavailable)
    val explorerGiftClaimedMessage = stringResource(R.string.applications_explorer_gift_claimed)
    val explorerGiftAlreadyUnlockedMessage = stringResource(R.string.applications_explorer_gift_already_unlocked)
    val requiresCoinEntry = entitlement.usesCoinEconomy() || BuildConfig.DEBUG

    ApplicationsScreenContent(
        catalog = catalog,
        entitlement = entitlement,
        wallet = wallet,
        requiresCoinEntry = requiresCoinEntry,
        explorerGiftAvailable = explorerGiftAvailable,
        onNavigateBack = { navController.navigateUp() },
        onShowPricingTable = { showPricingTable = true },
        onCodesClick = { item ->
            navController.navigate(Screen.ApplicationCodes.createRoute(item.id))
        },
        onUnlockClick = { unlockTarget = it },
        onSubscribeClick = { navController.navigate(Screen.Upgrade.route) },
        onExplorerSparkleClick = { explorerGiftTarget = it },
        onItemClick = { item, title ->
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

    val giftTarget = explorerGiftTarget
    if (giftTarget != null) {
        val giftFeature = giftTarget.id.premiumFeature()
        if (giftFeature != null) {
            ExplorerGiftDialog(
                appName = stringResource(giftTarget.titleRes),
                onDismiss = { explorerGiftTarget = null },
                onWatchAd = {
                    watchRewardedAd(
                        activity = activity,
                        rewardedAdManager = rewardedAdManager,
                        onGranted = {
                            walletViewModel.claimExplorerGift(giftFeature) { result ->
                                when (result) {
                                    WalletUnlockResult.Success -> {
                                        explorerGiftTarget = null
                                        unlockMessage = explorerGiftClaimedMessage
                                        navController.navigate(giftTarget.route)
                                    }
                                    WalletUnlockResult.AlreadyUnlocked -> {
                                        explorerGiftTarget = null
                                        unlockMessage = explorerGiftAlreadyUnlockedMessage
                                    }
                                    WalletUnlockResult.InsufficientBalance -> {
                                        explorerGiftTarget = null
                                        unlockMessage = insufficientBalanceMessage
                                    }
                                }
                            }
                        },
                        onUnavailable = {
                            unlockMessage = adUnavailableMessage
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

@Composable
fun ApplicationsScreenContent(
    catalog: List<ApplicationCatalogItem>,
    entitlement: Entitlement,
    wallet: CoinWalletState,
    requiresCoinEntry: Boolean,
    explorerGiftAvailable: Boolean = false,
    onNavigateBack: () -> Unit,
    onShowPricingTable: () -> Unit,
    onCodesClick: (ApplicationCatalogItem) -> Unit,
    onUnlockClick: (ApplicationCatalogItem) -> Unit,
    onSubscribeClick: () -> Unit,
    onExplorerSparkleClick: (ApplicationCatalogItem) -> Unit = {},
    onItemClick: (ApplicationCatalogItem, String) -> Unit,
) {
    NeoScaffold(
        title = stringResource(R.string.applications_title),
        subtitle = stringResource(R.string.applications_subtitle),
        onNavigateBack = onNavigateBack,
        actions = {
            if (requiresCoinEntry) {
                CoinBalanceChip(
                    balance = wallet.balance,
                    modifier = Modifier
                        .clickable(onClick = onShowPricingTable)
                        .padding(end = 4.dp),
                )
            }
        },
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Scaffold already applies horizontal pad; avoid stacking another 16.dp on narrow phones.
            val extraH = if (maxWidth < 360.dp) 0.dp else 4.dp
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = extraH, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(catalog, key = { it.id.name }) { item ->
                    val title = stringResource(item.titleRes)
                    val feature = item.id.premiumFeature()
                    val isUnlocked = item.id.hasEntryAccess(
                        entitlement = entitlement,
                        wallet = wallet,
                        requiresCoinEntry = requiresCoinEntry,
                    )
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

                    val badge = applicationBadge(item)

                    val enabledProHighlight = !item.id.isFree() &&
                        !item.comingSoon &&
                        isUnlocked
                    val showExplorerSparkle = explorerGiftAvailable &&
                        !item.id.isFree() &&
                        !item.comingSoon &&
                        !isUnlocked

                    ApplicationListItemCard(
                        thumbnailRes = item.id.thumbnailRes(),
                        title = title,
                        subtitle = stringResource(item.subtitleRes),
                        badge = badge,
                        trailingAction = trailingAction,
                        onUnlockClick = { onUnlockClick(item) },
                        onSubscribeClick = onSubscribeClick,
                        onCodesClick = { onCodesClick(item) },
                        onClick = { onItemClick(item, title) },
                        showExplorerSparkle = showExplorerSparkle,
                        onExplorerSparkleClick = { onExplorerSparkleClick(item) },
                        enabledProHighlight = enabledProHighlight,
                    )
                }
            }
        }
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
private fun applicationBadge(item: ApplicationCatalogItem): String {
    return when {
        item.id.isFree() -> stringResource(R.string.applications_badge_free)
        item.comingSoon -> stringResource(R.string.applications_badge_coming_soon)
        else -> stringResource(R.string.applications_badge_pro)
    }
}

@Preview(showSystemUi = true, name = "Applications", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ApplicationsScreenPreview() {
    TeleCon4Esp32Theme {
        ApplicationsScreenContent(
            catalog = defaultApplicationCatalog(),
            entitlement = Entitlement.Free,
            wallet = CoinWalletState(
                balance = 84,
                grants = mapOf(
                    PremiumFeature.GREENHOUSE to FeatureGrant(
                        feature = PremiumFeature.GREENHOUSE,
                        option = CoinUnlockOption.WEEK,
                        expiresAtEpochMs = Long.MAX_VALUE,
                    ),
                    PremiumFeature.SMART_HOME to FeatureGrant(
                        feature = PremiumFeature.SMART_HOME,
                        option = CoinUnlockOption.DAYS_3,
                        expiresAtEpochMs = Long.MAX_VALUE,
                    ),
                ),
            ),
            requiresCoinEntry = true,
            onNavigateBack = {},
            onShowPricingTable = {},
            onCodesClick = {},
            onUnlockClick = {},
            onSubscribeClick = {},
            onExplorerSparkleClick = {},
            onItemClick = { _, _ -> },
        )
    }
}
