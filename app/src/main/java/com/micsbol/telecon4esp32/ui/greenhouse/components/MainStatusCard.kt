package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
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
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseGlass
import java.util.Locale

@Composable
fun MainStatusCard(
    temperatureC: Float,
    humidityPercent: Int,
    vpdKpa: Float,
    isStable: Boolean,
    modifier: Modifier = Modifier,
) {
    GreenhouseCard(modifier = modifier, strongGlass = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.WbSunny,
                    contentDescription = null,
                    tint = GreenhouseGlass.AccentLime,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.greenhouse_weather_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenhouseGlass.TextPrimary,
                )
            }
            GreenhouseStatusBadge(
                text = stringResource(
                    if (isStable) {
                        R.string.greenhouse_stable
                    } else {
                        R.string.greenhouse_monitoring
                    },
                ),
                backgroundColor = if (isStable) {
                    GreenhouseGlass.ChipBackground.copy(alpha = GreenhouseGlass.ChipSurfaceAlpha)
                } else {
                    Color(0xFFFFEDD5).copy(alpha = GreenhouseGlass.ChipSurfaceAlpha)
                },
                contentColor = if (isStable) {
                    GreenhouseGlass.AccentGreen
                } else {
                    GreenhouseGlass.WarningOrange
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.greenhouse_temperature_reading, temperatureC),
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            ),
            color = GreenhouseGlass.TextPrimary,
        )

        Spacer(modifier = Modifier.height(14.dp))

        TemperatureGradientSlider(temperatureC = temperatureC)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ClimateStatChip(
                label = stringResource(R.string.greenhouse_humidity_label),
                value = stringResource(R.string.greenhouse_percent_value, humidityPercent),
                valueColor = GreenhouseGlass.ValueGreen,
                modifier = Modifier.weight(1f),
            )
            ClimateStatChip(
                label = stringResource(R.string.greenhouse_vpd_label),
                value = stringResource(
                    R.string.greenhouse_vpd_value,
                    String.format(Locale.US, "%.1f", vpdKpa),
                ),
                valueColor = GreenhouseGlass.ValueOrange,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ClimateStatChip(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(
                color = GreenhouseGlass.ChipBackground.copy(alpha = GreenhouseGlass.ChipSurfaceAlpha),
                shape = GreenhouseGlass.SmallCardShape,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = GreenhouseGlass.TextOnGlassMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = valueColor,
        )
    }
}
