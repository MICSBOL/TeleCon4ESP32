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
import com.micsbol.telecon4esp32.domain.model.hasEntryAccess
import com.micsbol.telecon4esp32.domain.model.isFree
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy
import com.micsbol.telecon4esp32.ui.ads.LocalRewardedAdManager
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme
import com.micsbol.telecon4esp32.ui.wallet.CoinBalanceChip
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

    ApplicationEntryDialogs(
        navController = navController,
        wallet = wallet,
        requiresCoinEntry = requiresCoinEntry,
        activity = activity,
        rewardedAdManager = rewardedAdManager,
        unlockTarget = unlockTarget,
        onUnlockTargetChange = { unlockTarget = it },
        explorerGiftTarget = explorerGiftTarget,
        onExplorerGiftTargetChange = { explorerGiftTarget = it },
        comingSoonAppName = comingSoonAppName,
        onComingSoonAppNameChange = { comingSoonAppName = it },
        unlockMessage = unlockMessage,
        onUnlockMessageChange = { unlockMessage = it },
        showPricingTable = showPricingTable,
        onShowPricingTableChange = { showPricingTable = it },
        onUnlockFeature = { feature, option, onResult ->
            walletViewModel.unlockFeature(feature, option, onResult)
        },
        onClaimExplorerGift = { feature, onResult ->
            walletViewModel.claimExplorerGift(feature, onResult)
        },
    )
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
                    val isUnlocked = item.id.hasEntryAccess(
                        entitlement = entitlement,
                        wallet = wallet,
                        requiresCoinEntry = requiresCoinEntry,
                    )
                    ApplicationListItemCard(
                        thumbnailRes = item.id.thumbnailRes(),
                        title = title,
                        subtitle = stringResource(item.subtitleRes),
                        badge = applicationBadge(item),
                        trailingAction = applicationTrailingAction(
                            item = item,
                            isUnlocked = isUnlocked,
                            requiresCoinEntry = requiresCoinEntry,
                        ),
                        onUnlockClick = { onUnlockClick(item) },
                        onSubscribeClick = onSubscribeClick,
                        onCodesClick = { onCodesClick(item) },
                        onClick = { onItemClick(item, title) },
                        showExplorerSparkle = explorerGiftAvailable &&
                            !item.id.isFree() &&
                            !item.comingSoon &&
                            !isUnlocked,
                        onExplorerSparkleClick = { onExplorerSparkleClick(item) },
                        enabledProHighlight = !item.id.isFree() &&
                            !item.comingSoon &&
                            isUnlocked,
                    )
                }
            }
        }
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
                    PremiumFeature.RC_VEHICLE_PRO to FeatureGrant(
                        feature = PremiumFeature.RC_VEHICLE_PRO,
                        option = CoinUnlockOption.WEEK,
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
