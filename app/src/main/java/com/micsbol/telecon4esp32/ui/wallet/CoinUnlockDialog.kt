package com.micsbol.telecon4esp32.ui.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.CoinEconomy
import com.micsbol.telecon4esp32.domain.model.CoinUnlockOption
import com.micsbol.telecon4esp32.domain.model.CoinWalletState
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.ui.components.NeoDialogTextAction
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.theme.Neo

private val ProGold = Color(0xFFFFD54F)
private val ProGoldDeep = Color(0xFFFFB300)

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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_coin_stack),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    text = stringResource(R.string.coins_unlock_title, appName),
                    // Avoid Syncopate titleLarge — it wraps mid-word and looks oversized on some OEMs.
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Neo.TextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true,
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
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // Room for two-digit costs + coin icon; ES "Desbloquear" ellipsizes if needed.
                val unlockButtonWidth = if (maxWidth < 340.dp) 136.dp else 148.dp
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CoinUnlockOption.entries.filter { it.isCoinPurchasable }.forEach { option ->
                        CoinUnlockOptionRow(
                            option = option,
                            balance = wallet.balance,
                            unlockButtonWidth = unlockButtonWidth,
                            onUnlock = { onUnlock(option) },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CoinBalanceChip(
                    balance = wallet.balance,
                    enlarged = false,
                )
            }
        },
        actions = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CoinWatchAdPillButton(
                    onClick = onWatchAd,
                    fillMaxWidth = true,
                )
                CoinGoProButton(onClick = onUpgrade)
            }
        },
    )
}

@Composable
private fun CoinGoProButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.coins_upgrade_button)
    val benefit = stringResource(R.string.coins_upgrade_benefit)
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        ProGold.copy(alpha = 0.30f),
                        ProGoldDeep.copy(alpha = 0.18f),
                    ),
                ),
            )
            .border(1.5.dp, ProGold.copy(alpha = 0.85f), shape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = "$label. $benefit"
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = ProGold,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = benefit,
            color = ProGold.copy(alpha = 0.88f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_coin_gold),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    text = stringResource(R.string.coins_pricing_table_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Neo.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
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
                    CoinWatchAdPillButton(
                        onClick = onWatchAd,
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
        CoinUnlockOption.HOURS_4 -> stringResource(R.string.coins_table_4h)
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
        )
        CoinUnlockPillButton(
            coinCost = option.coinCost,
            onClick = onUnlock,
            enabled = balance >= option.coinCost,
            compact = true,
            fixedWidth = unlockButtonWidth,
        )
    }
}

@Composable
fun CoinHomeWalletPanel(
    balance: Int,
    onWatchAd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    com.micsbol.telecon4esp32.ui.components.NeoCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).widthIn(min = 0.dp)) {
                Text(
                    text = stringResource(R.string.coins_wallet_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Neo.TextPrimary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                CoinBalanceChip(balance = balance)
            }
            Spacer(modifier = Modifier.width(8.dp))
            CoinWatchAdPillButton(onClick = onWatchAd)
        }
    }
}

@Composable
fun CoinWatchAdPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    fillMaxWidth: Boolean = false,
) {
    NeoPillButton(
        text = stringResource(
            R.string.coins_watch_ad_amount,
            CoinEconomy.COINS_PER_REWARDED_AD,
        ),
        onClick = onClick,
        iconPainter = painterResource(R.drawable.ic_play_gold),
        trailingPainter = painterResource(R.drawable.ic_coin_gold),
        compact = compact,
        fillMaxWidth = fillMaxWidth,
        modifier = modifier,
    )
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Neo.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
