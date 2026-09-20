package com.micsbol.telecon4esp32.ui.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.CoinUnlockOption
import com.micsbol.telecon4esp32.domain.model.PremiumFeature
import com.micsbol.telecon4esp32.ui.theme.Neo

@Composable
fun CoinPricingTable(
    modifier: Modifier = Modifier,
    feature: PremiumFeature = PremiumFeature.RC_VEHICLE_PRO,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        CoinPricingTableHeaderRow()
        HorizontalDivider(color = Neo.TextMuted.copy(alpha = 0.3f))
        CoinUnlockOption.entries.filter { it.isCoinPurchasable }.forEach { option ->
            CoinPricingTableDataRow(option = option, feature = feature)
            HorizontalDivider(color = Neo.TextMuted.copy(alpha = 0.15f))
        }
    }
}

@Composable
private fun CoinPricingTableHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        CoinPricingTableHeaderCell(
            text = stringResource(R.string.coins_table_header_option),
            modifier = Modifier.weight(1.2f),
        )
        CoinPricingTableHeaderCell(
            text = stringResource(R.string.coins_table_header_cost),
            modifier = Modifier.weight(0.8f),
        )
        CoinPricingTableHeaderCell(
            text = stringResource(R.string.coins_table_header_uses),
            modifier = Modifier.weight(0.7f),
        )
        CoinPricingTableHeaderCell(
            text = stringResource(R.string.coins_table_header_per_day),
            modifier = Modifier.weight(0.9f),
        )
    }
}

@Composable
private fun CoinPricingTableHeaderCell(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = Neo.TextSecondary,
        modifier = modifier,
    )
}

@Composable
private fun CoinPricingTableDataRow(
    option: CoinUnlockOption,
    feature: PremiumFeature,
) {
    val optionLabel = optionLabel(option)
    val perDay = perDayLabel(option, feature)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = optionLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = Neo.TextPrimary,
            modifier = Modifier.weight(1.2f),
        )
        Text(
            text = option.coinCost(feature).toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Neo.Accent,
            modifier = Modifier.weight(0.8f),
        )
        Text(
            text = stringResource(R.string.coins_table_uses_format, option.usesEquivalent(feature)),
            style = MaterialTheme.typography.bodyMedium,
            color = Neo.TextPrimary,
            modifier = Modifier.weight(0.7f),
        )
        Text(
            text = perDay,
            style = MaterialTheme.typography.bodyMedium,
            color = Neo.TextSecondary,
            modifier = Modifier.weight(0.9f),
        )
    }
}

@Composable
private fun optionLabel(option: CoinUnlockOption): String = when (option) {
    CoinUnlockOption.ONE_USE -> stringResource(R.string.coins_table_one_use)
    CoinUnlockOption.HOURS_4 -> stringResource(R.string.coins_table_4h)
    CoinUnlockOption.HOURS_24 -> stringResource(R.string.coins_table_24h)
    CoinUnlockOption.DAYS_3 -> stringResource(R.string.coins_table_3d)
    CoinUnlockOption.WEEK -> stringResource(R.string.coins_table_week)
}

@Composable
private fun perDayLabel(option: CoinUnlockOption, feature: PremiumFeature): String {
    val days = when (option) {
        CoinUnlockOption.HOURS_24 -> 1.0
        CoinUnlockOption.DAYS_3 -> 3.0
        CoinUnlockOption.WEEK -> 7.0
        CoinUnlockOption.ONE_USE,
        CoinUnlockOption.HOURS_4 -> return stringResource(R.string.coins_table_dash)
    }
    val perDay = option.coinCost(feature) / days
    val rounded = kotlin.math.round(perDay).toInt()
    return if (kotlin.math.abs(perDay - rounded) < 0.01) {
        rounded.toString()
    } else {
        "~$rounded"
    }
}
