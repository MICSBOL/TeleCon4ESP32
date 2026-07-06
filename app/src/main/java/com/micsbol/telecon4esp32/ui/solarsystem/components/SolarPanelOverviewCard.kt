package com.micsbol.telecon4esp32.ui.solarsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.solarsystem.SolarEnergyTotals
import com.micsbol.telecon4esp32.ui.solarsystem.SolarGlass
import com.micsbol.telecon4esp32.ui.solarsystem.SolarLiveTelemetry
import java.util.Locale

@Composable
fun SolarPanelOverviewCard(
    live: SolarLiveTelemetry,
    totals: SolarEnergyTotals,
    modifier: Modifier = Modifier,
) {
    SolarSystemCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.solar_system_card_title),
                    color = SolarGlass.TextOnGlassPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.solar_system_card_subtitle),
                    color = SolarGlass.TextOnGlassSecondary,
                    fontSize = 13.sp,
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SolarGlass.Positive.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = SolarGlass.Positive,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(
                        R.string.solar_system_battery_percent,
                        live.batteryPercent,
                    ),
                    color = SolarGlass.Positive,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Image(
            painter = painterResource(R.drawable.solar_system_hero_illustration),
            contentDescription = stringResource(R.string.solar_system_hero_illustration_content_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GenerationStat(
                value = totals.todayKwh,
                label = stringResource(R.string.solar_system_period_today),
                modifier = Modifier.weight(1f),
            )
            GenerationStat(
                value = totals.monthKwh,
                label = stringResource(R.string.solar_system_period_month),
                modifier = Modifier.weight(1f),
            )
            GenerationStat(
                value = totals.totalKwh,
                label = stringResource(R.string.solar_system_period_all_time),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GenerationStat(
    value: Float,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                R.string.solar_system_energy_kwh_value,
                String.format(Locale.US, "%.0f", value),
            ),
            color = SolarGlass.TextOnGlassPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = SolarGlass.TextOnGlassSecondary,
            fontSize = 11.sp,
        )
    }
}

@Composable
fun SolarLivePowerRow(
    live: SolarLiveTelemetry,
    modifier: Modifier = Modifier,
) {
    SolarSystemCard(modifier = modifier, elevated = false) {
        SolarSectionHeader(title = stringResource(R.string.solar_system_section_live_power))
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LivePowerChip(
                label = stringResource(R.string.solar_system_metric_solar),
                value = stringResource(R.string.solar_system_power_watts, live.solarW),
                modifier = Modifier.weight(1f),
            )
            LivePowerChip(
                label = stringResource(R.string.solar_system_metric_load),
                value = stringResource(R.string.solar_system_power_watts, live.loadW),
                modifier = Modifier.weight(1f),
            )
            LivePowerChip(
                label = stringResource(R.string.solar_system_metric_battery),
                value = stringResource(R.string.solar_system_power_watts, live.batteryW),
                modifier = Modifier.weight(1f),
            )
            LivePowerChip(
                label = stringResource(R.string.solar_system_metric_grid),
                value = stringResource(R.string.solar_system_power_watts, live.gridW),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LivePowerChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SolarGlass.ChipBackground.copy(alpha = SolarGlass.ChipSurfaceAlpha))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            color = SolarGlass.TextOnGlassPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = SolarGlass.TextOnGlassSecondary,
            fontSize = 10.sp,
        )
    }
}
