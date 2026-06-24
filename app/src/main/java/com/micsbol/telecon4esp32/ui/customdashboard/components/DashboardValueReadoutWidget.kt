package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.theme.TechCyanBright

@Composable
fun DashboardValueReadoutWidget(
    value: Float,
    unitLabel: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    DashboardWidgetCard(
        isSelected = isSelected,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.custom_dashboard_value_readout_value, value),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TechCyanBright,
            )
            Text(
                text = unitLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
