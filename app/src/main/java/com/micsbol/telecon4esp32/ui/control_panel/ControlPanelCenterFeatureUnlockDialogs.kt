package com.micsbol.telecon4esp32.ui.control_panel

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.domain.model.WalletUnlockResult
import com.micsbol.telecon4esp32.ui.ads.LocalRewardedAdManager
import com.micsbol.telecon4esp32.ui.applications.watchRewardedAd
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.wallet.CoinMessageDialog
import com.micsbol.telecon4esp32.ui.wallet.CoinUnlockDialog
import com.micsbol.telecon4esp32.ui.wallet.LocalWallet
import com.micsbol.telecon4esp32.ui.wallet.WalletViewModel

@Composable
fun ControlPanelCenterFeatureUnlockDialogs(
    unlockFeature: PremiumFeature?,
    featureTitle: String,
    navController: NavController,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit = {},
) {
    if (unlockFeature == null) return

    val walletViewModel = hiltViewModel<WalletViewModel>()
    val wallet = LocalWallet.current
    val rewardedAdManager = LocalRewardedAdManager.current
    val activity = LocalContext.current as? Activity
    var unlockMessage by remember { mutableStateOf<String?>(null) }
    val insufficientBalanceMessage = stringResource(R.string.coins_insufficient_balance)
    val adRewardGrantedMessage = stringResource(R.string.coins_ad_reward_granted)
    val adUnavailableMessage = stringResource(R.string.coins_ad_unavailable)

    CoinUnlockDialog(
        appName = featureTitle,
        feature = unlockFeature,
        wallet = wallet,
        onDismiss = onDismiss,
        onUnlock = { option ->
            walletViewModel.unlockFeature(unlockFeature, option) { result ->
                when (result) {
                    WalletUnlockResult.Success,
                    WalletUnlockResult.AlreadyUnlocked -> {
                        onUnlocked()
                        onDismiss()
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
            onDismiss()
            navController.navigate(Screen.Upgrade.route)
        },
    )

    unlockMessage?.let { message ->
        CoinMessageDialog(
            message = message,
            onDismiss = { unlockMessage = null },
        )
    }
}
