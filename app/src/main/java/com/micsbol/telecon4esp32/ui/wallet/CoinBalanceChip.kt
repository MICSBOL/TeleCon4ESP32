package com.micsbol.telecon4esp32.ui.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.glassSurface
import com.micsbol.telecon4esp32.ui.theme.Neo

@Composable
fun CoinBalanceChip(
    balance: Int,
    modifier: Modifier = Modifier,
    enlarged: Boolean = false,
) {
    val iconSize = if (enlarged) 30.dp else 22.dp
    val horizontalPadding = if (enlarged) 14.dp else 10.dp
    val verticalPadding = if (enlarged) 10.dp else 6.dp
    val textStyle = if (enlarged) {
        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp)
    } else {
        MaterialTheme.typography.labelLarge
    }

    Row(
        modifier = modifier
            .glassSurface(cornerRadius = if (enlarged) 16.dp else 12.dp, alpha = 0.14f)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (enlarged) 8.dp else 6.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_coin_gold),
            contentDescription = stringResource(R.string.coins_content_description),
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = stringResource(R.string.coins_balance_format, balance),
            style = textStyle,
            color = Neo.TextPrimary,
        )
    }
}
