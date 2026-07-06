package com.micsbol.telecon4esp32.ui.solarsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.solarsystem.SolarDiyControls
import com.micsbol.telecon4esp32.ui.solarsystem.SolarEnergyDistribution
import com.micsbol.telecon4esp32.ui.solarsystem.SolarGlass
import com.micsbol.telecon4esp32.ui.solarsystem.SolarGridMode
import java.util.Locale

@Composable
fun SolarDiyStatusCard(
    diy: SolarDiyControls,
    distribution: SolarEnergyDistribution,
    lowBatteryWarning: Boolean,
    hasFault: Boolean,
    onInverterToggle: () -> Unit,
    onResetDaily: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SolarSystemCard(modifier = modifier) {
        SolarSectionHeader(title = stringResource(R.string.solar_system_section_diy_status))
        Spacer(modifier = Modifier.height(12.dp))

        if (hasFault) {
            FaultBanner(faultCode = diy.faultCode)
            Spacer(modifier = Modifier.height(10.dp))
        }
        if (lowBatteryWarning) {
            WarningBanner(text = stringResource(R.string.solar_system_low_battery_warning))
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = SolarGlass.AccentAmber,
                )
                Column {
                    Text(
                        text = stringResource(R.string.solar_system_inverter_label),
                        color = SolarGlass.TextOnGlassPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            if (diy.inverterOn) {
                                R.string.solar_system_inverter_on
                            } else {
                                R.string.solar_system_inverter_off
                            },
                        ),
                        color = SolarGlass.TextOnGlassSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
            Switch(
                checked = diy.inverterOn,
                onCheckedChange = { onInverterToggle() },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusChip(
                label = stringResource(R.string.solar_system_grid_mode_label),
                value = gridModeLabel(diy.gridMode),
                modifier = Modifier.weight(1f),
            )
            StatusChip(
                label = stringResource(R.string.solar_system_panel_efficiency_label),
                value = "${diy.panelEfficiencyPercent}%",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.solar_system_energy_distribution_title),
            color = SolarGlass.TextOnGlassSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DistributionStat(
                value = distribution.toHomeKwh,
                label = stringResource(R.string.solar_system_distribution_home),
            )
            DistributionStat(
                value = distribution.toBatteryKwh,
                label = stringResource(R.string.solar_system_distribution_battery),
            )
            DistributionStat(
                value = distribution.toGridKwh,
                label = stringResource(R.string.solar_system_distribution_grid),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.solar_system_reset_daily_hint),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(SolarGlass.ChipBackground.copy(alpha = SolarGlass.ChipSurfaceAlpha))
                .clickable(onClick = onResetDaily)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            color = SolarGlass.AccentAmber,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FaultBanner(faultCode: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SolarGlass.Warning.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = SolarGlass.Warning,
        )
        Text(
            text = stringResource(R.string.solar_system_fault_code, faultCode),
            color = SolarGlass.Warning,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun WarningBanner(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SolarGlass.AccentAmber.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        color = SolarGlass.TextOnGlassPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun StatusChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SolarGlass.ChipBackground.copy(alpha = SolarGlass.ChipSurfaceAlpha))
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Text(
            text = value,
            color = SolarGlass.TextOnGlassPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = SolarGlass.TextOnGlassSecondary,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun DistributionStat(
    value: Float,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(
                R.string.solar_system_energy_kwh_value,
                String.format(Locale.US, "%.0f", value),
            ),
            color = SolarGlass.TextOnGlassPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = SolarGlass.TextOnGlassSecondary,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun gridModeLabel(mode: SolarGridMode): String = when (mode) {
    SolarGridMode.EXPORT -> stringResource(R.string.solar_system_grid_export)
    SolarGridMode.IMPORT -> stringResource(R.string.solar_system_grid_import)
    SolarGridMode.IDLE -> stringResource(R.string.solar_system_grid_idle)
}
