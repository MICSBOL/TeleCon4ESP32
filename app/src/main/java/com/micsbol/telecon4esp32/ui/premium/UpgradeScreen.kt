package com.micsbol.telecon4esp32.ui.premium

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.CoinUnlockOption
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy
import com.micsbol.telecon4esp32.ui.ads.LocalRewardedAdManager
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.components.NeoSectionTitle
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.theme.Neo
import com.micsbol.telecon4esp32.ui.wallet.CoinBalanceChip
import com.micsbol.telecon4esp32.ui.wallet.CoinMessageDialog
import com.micsbol.telecon4esp32.ui.wallet.CoinWatchAdPillButton
import com.micsbol.telecon4esp32.ui.wallet.WalletViewModel

@Composable
fun UpgradeScreen(navController: NavController) {
    val entitlement = LocalEntitlement.current
    val walletViewModel = hiltViewModel<WalletViewModel>()
    val wallet by walletViewModel.wallet.collectAsState()
    val rewardedAdManager = LocalRewardedAdManager.current
    val activity = LocalContext.current as? Activity
    var coinMessage by remember { mutableStateOf<String?>(null) }
    val adRewardGrantedMessage = stringResource(R.string.coins_ad_reward_granted)
    val adUnavailableMessage = stringResource(R.string.coins_ad_unavailable)

    NeoScaffold(
        title = stringResource(R.string.upgrade_title),
        subtitle = stringResource(R.string.upgrade_subtitle),
        onNavigateBack = { navController.navigateUp() },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (entitlement.usesCoinEconomy()) {
                NeoCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_coin_stack),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            NeoSectionTitle(text = stringResource(R.string.coins_wallet_title))
                            CoinBalanceChip(balance = wallet.balance)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.coins_wallet_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Neo.TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CoinUnlockOption.entries.forEach { option ->
                        Text(
                            text = coinOptionLabel(option),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Neo.TextPrimary,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    CoinWatchAdPillButton(
                        onClick = {
                            if (activity != null && rewardedAdManager != null) {
                                rewardedAdManager.tryShow(activity) { granted ->
                                    coinMessage = if (granted) {
                                        adRewardGrantedMessage
                                    } else {
                                        adUnavailableMessage
                                    }
                                }
                            } else {
                                coinMessage = adUnavailableMessage
                            }
                        },
                        compact = false,
                        fillMaxWidth = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            NeoCard(modifier = Modifier.fillMaxWidth()) {
                NeoSectionTitle(text = stringResource(R.string.upgrade_benefits_title))
                Spacer(modifier = Modifier.height(12.dp))
                UpgradeBenefitLine(stringResource(R.string.upgrade_benefit_ad_free))
                UpgradeBenefitLine(stringResource(R.string.upgrade_benefit_no_coins))
                UpgradeBenefitLine(stringResource(R.string.upgrade_benefit_rc_vehicle))
                UpgradeBenefitLine(stringResource(R.string.upgrade_benefit_applications))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.upgrade_billing_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = Neo.TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            NeoPillButton(
                text = stringResource(R.string.upgrade_purchase_button),
                onClick = { /* Play Billing — next implementation step */ },
                modifier = Modifier.fillMaxWidth(0.92f),
                enabled = false,
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { /* restore purchases — next step */ }) {
                Text(
                    text = stringResource(R.string.upgrade_restore_button),
                    color = Neo.Accent,
                )
            }
        }
    }

    if (coinMessage != null) {
        CoinMessageDialog(
            message = coinMessage.orEmpty(),
            onDismiss = { coinMessage = null },
        )
    }
}

@Composable
private fun coinOptionLabel(option: CoinUnlockOption): String = when (option) {
    CoinUnlockOption.ONE_USE -> stringResource(R.string.coins_option_one_use, option.coinCost)
    CoinUnlockOption.HOURS_24 -> stringResource(R.string.coins_option_24h, option.coinCost)
    CoinUnlockOption.DAYS_3 -> stringResource(R.string.coins_option_3d, option.coinCost)
    CoinUnlockOption.WEEK -> stringResource(R.string.coins_option_week, option.coinCost)
}

@Composable
private fun UpgradeBenefitLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Neo.TextPrimary,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
