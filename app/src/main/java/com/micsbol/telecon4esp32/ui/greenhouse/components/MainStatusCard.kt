package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseIllustration
import com.micsbol.telecon4esp32.ui.theme.TechBlueBright
import com.micsbol.telecon4esp32.ui.theme.PlotOrange
import java.util.Locale

@Composable
fun MainStatusCard(
    temperatureC: Float,
    humidityPercent: Int,
    vpdKpa: Float,
    isStable: Boolean,
    modifier: Modifier = Modifier,
) {
    GreenhouseCard(modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val illustrationWidth = maxWidth * 0.44f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.greenhouse_temperature_reading,
                            temperatureC,
                        ),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusMetricRow(
                            label = stringResource(R.string.greenhouse_humidity_label),
                            value = stringResource(R.string.greenhouse_percent_value, humidityPercent),
                            valueColor = TechBlueBright,
                        )
                        StatusMetricRow(
                            label = stringResource(R.string.greenhouse_vpd_label),
                            value = stringResource(
                                R.string.greenhouse_vpd_value,
                                String.format(Locale.US, "%.1f", vpdKpa),
                            ),
                            valueColor = PlotOrange,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isStable) {
                        GreenhouseStableBadge(
                            text = stringResource(R.string.greenhouse_stable),
                        )
                    }
                }
                GreenhouseIllustration(width = illustrationWidth)
            }
        }
    }
}

@Composable
private fun StatusMetricRow(
    label: String,
    value: String,
    valueColor: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
        )
    }
}
