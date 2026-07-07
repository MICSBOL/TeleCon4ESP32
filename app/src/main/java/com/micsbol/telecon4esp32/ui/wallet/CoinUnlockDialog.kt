package com.micsbol.telecon4esp32.ui.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.CoinUnlockOption
import com.micsbol.telecon4esp32.domain.model.CoinWalletState
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.ui.components.NeoDialogTextAction
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.theme.Neo

@Composable
fun CoinUnlockDialog(
    appName: String,
    feature: PremiumFeature,
    wallet: CoinWalletState,
    onDismiss: () -> Unit,
    onUnlock: (CoinUnlockOption) -> Unit,
    onWatchAd: () -> Unit,
    onUpgrade: () -> Unit,
) {
    CoinDialogShell(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_coin_stack),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
                Text(
                    text = stringResource(R.string.coins_unlock_title, appName),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Neo.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        },
        content = {
            Text(
                text = stringResource(R.string.coins_unlock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Neo.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.coins_unlock_pick_option),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Neo.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            CoinUnlockOption.entries.forEach { option ->
                CoinUnlockOptionRow(
                    option = option,
                    balance = wallet.balance,
                    unlockButtonWidth = 132.dp,
                    onUnlock = { onUnlock(option) },
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CoinBalanceChip(
                    balance = wallet.balance,
                    enlarged = true,
                )
            }
        },
        actions = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NeoPillButton(
                    text = stringResource(R.string.coins_watch_ad_button),
                    onClick = onWatchAd,
                    compact = true,
                    fillMaxWidth = true,
                )
                NeoDialogTextAction(
                    text = stringResource(R.string.coins_upgrade_button),
                    onClick = onUpgrade,
                )
            }
        },
    )
}

@Composable
fun CoinPricingTableDialog(
    onDismiss: () -> Unit,
    walletBalance: Int? = null,
    onWatchAd: (() -> Unit)? = null,
) {
    CoinDialogShell(
        onDismissRequest = onDismiss,
        scrollableContent = true,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_coin_gold),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
                Text(
                    text = stringResource(R.string.coins_pricing_table_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Neo.TextPrimary,
                )
            }
        },
        content = {
            if (walletBalance != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CoinBalanceChip(balance = walletBalance, enlarged = true)
                }
            }
            Text(
                text = stringResource(R.string.coins_wallet_description),
                style = MaterialTheme.typography.bodyMedium,
                color = Neo.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
            CoinPricingTable()
        },
        actions = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (onWatchAd != null) {
                    NeoPillButton(
                        text = stringResource(R.string.coins_watch_ad_button),
                        onClick = onWatchAd,
                        compact = true,
                        fillMaxWidth = true,
                    )
                }
                NeoDialogTextAction(
                    text = stringResource(R.string.codes_dialog_ok),
                    onClick = onDismiss,
                )
            }
        },
    )
}

@Composable
private fun CoinUnlockOptionRow(
    option: CoinUnlockOption,
    balance: Int,
    unlockButtonWidth: Dp,
    onUnlock: () -> Unit,
) {
    val label = when (option) {
        CoinUnlockOption.ONE_USE -> stringResource(R.string.coins_table_one_use)
        CoinUnlockOption.HOURS_24 -> stringResource(R.string.coins_table_24h)
        CoinUnlockOption.DAYS_3 -> stringResource(R.string.coins_table_3d)
        CoinUnlockOption.WEEK -> stringResource(R.string.coins_table_week)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Neo.TextPrimary,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
        )
        CoinUnlockPillButton(
            coinCost = option.coinCost,
            onClick = onUnlock,
            enabled = balance >= option.coinCost,
            compact = true,
            coinIconAtEnd = true,
            fixedWidth = unlockButtonWidth,
        )
    }
}

@Composable
fun CoinHomeWalletPanel(
    balance: Int,
    onWatchAd: () -> Unit,
    onViewPricing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.micsbol.telecon4esp32.ui.components.NeoCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.coins_wallet_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Neo.TextPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                CoinBalanceChip(balance = balance)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.coins_home_earn_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Neo.TextSecondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                NeoPillButton(
                    text = stringResource(R.string.coins_watch_ad_short),
                    onClick = onWatchAd,
                    compact = true,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.coins_view_pricing),
                    style = MaterialTheme.typography.labelMedium,
                    color = Neo.Accent,
                    modifier = Modifier.clickable(onClick = onViewPricing),
                )
            }
        }
    }
}

@Composable
fun CoinMessageDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    CoinDialogShell(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.coins_message_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Neo.TextPrimary,
            )
        },
        content = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Neo.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        actions = {
            NeoDialogTextAction(
                text = stringResource(R.string.codes_dialog_ok),
                onClick = onDismiss,
            )
        },
    )
}
