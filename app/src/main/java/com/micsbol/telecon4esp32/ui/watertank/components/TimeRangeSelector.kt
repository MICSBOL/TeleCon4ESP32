package com.micsbol.telecon4esp32.ui.watertank.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.theme.TechBlueBright
import com.micsbol.telecon4esp32.ui.watertank.TankChartPeriod

@Composable
fun TimeRangeSelector(
    selectedPeriod: TankChartPeriod,
    onPeriodSelected: (TankChartPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TankChartPeriod.entries.forEach { period ->
            val isSelected = period == selectedPeriod
            val label = when (period) {
                TankChartPeriod.HOURS_24 -> stringResource(R.string.water_tank_period_24h)
                TankChartPeriod.DAYS_7 -> stringResource(R.string.water_tank_period_7d)
                TankChartPeriod.DAYS_30 -> stringResource(R.string.water_tank_period_30d)
            }
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(TechBlueBright.copy(alpha = 0.2f))
                        } else {
                            Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(20.dp),
                                )
                        },
                    )
                    .clickable { onPeriodSelected(period) }
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TechBlueBright else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
