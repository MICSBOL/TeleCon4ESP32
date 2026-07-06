package com.micsbol.telecon4esp32.ui.solarsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.solarsystem.SolarBatteryInfo
import com.micsbol.telecon4esp32.ui.solarsystem.SolarGlass
import com.micsbol.telecon4esp32.ui.solarsystem.SolarLiveTelemetry
import java.util.Locale

@Composable
fun SolarBatterySummaryCard(
    batteryInfo: SolarBatteryInfo,
    live: SolarLiveTelemetry,
    modifier: Modifier = Modifier,
) {
    SolarSystemCard(modifier = modifier) {
        SolarSectionHeader(title = stringResource(R.string.solar_system_section_battery))
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BatteryStat(
                value = stringResource(
                    R.string.solar_system_energy_kwh_value,
                    String.format(Locale.US, "%.0f", batteryInfo.capacityKwh),
                ),
                label = stringResource(R.string.solar_system_battery_capacity),
                modifier = Modifier.weight(1f),
            )
            BatteryStat(
                value = formatChargeEta(batteryInfo.chargeEtaMinutes),
                label = stringResource(R.string.solar_system_charging_time),
                modifier = Modifier.weight(1f),
            )
            BatteryStat(
                value = stringResource(
                    R.string.solar_system_energy_kwh_value,
                    String.format(Locale.US, "%.1f", batteryInfo.totalChargedKwh),
                ),
                label = stringResource(R.string.solar_system_total_charging),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(
                R.string.solar_system_live_electrical,
                String.format(Locale.US, "%.1f", live.voltage),
                String.format(Locale.US, "%.1f", live.current),
                live.gridW,
            ),
            color = SolarGlass.TextOnGlassSecondary,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun BatteryStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            color = SolarGlass.TextOnGlassPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = SolarGlass.TextOnGlassSecondary,
            fontSize = 10.sp,
        )
    }
}

private fun formatChargeEta(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return "${hours}hr ${mins}min"
}
