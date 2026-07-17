package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseGlass

@Composable
fun GreenhouseProtocolSettingsSection(
    selectedMode: BluetoothConnectionMode,
    canUseAdvanced: Boolean,
    onModeSelected: (BluetoothConnectionMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.app_settings_connection_section_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = GreenhouseGlass.TextOnGlassPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.app_settings_protocol_prefix_label,
                ApplicationId.GREENHOUSE.protocolPrefix(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = GreenhouseGlass.TextOnGlassSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.app_settings_connection_section_description),
            style = MaterialTheme.typography.bodySmall,
            color = GreenhouseGlass.TextOnGlassSecondary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(modifier.selectableGroup()) {
            GreenhouseConnectionModeOption(
                label = stringResource(R.string.app_settings_connection_classic_simple),
                description = stringResource(R.string.app_settings_connection_classic_simple_description),
                selected = selectedMode == BluetoothConnectionMode.CLASSIC_SIMPLE,
                onClick = { onModeSelected(BluetoothConnectionMode.CLASSIC_SIMPLE) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            GreenhouseConnectionModeOption(
                label = stringResource(R.string.app_settings_connection_classic_binary),
                description = if (canUseAdvanced) {
                    stringResource(R.string.app_settings_connection_classic_binary_description)
                } else {
                    stringResource(R.string.app_settings_protocol_advanced_premium_required)
                },
                selected = selectedMode == BluetoothConnectionMode.CLASSIC_BINARY,
                enabled = canUseAdvanced,
                onClick = {
                    if (canUseAdvanced) {
                        onModeSelected(BluetoothConnectionMode.CLASSIC_BINARY)
                    }
                },
            )
            Spacer(modifier = Modifier.height(8.dp))
            GreenhouseConnectionModeOption(
                label = stringResource(R.string.app_settings_connection_ble_binary),
                description = if (canUseAdvanced) {
                    stringResource(R.string.app_settings_connection_ble_binary_description)
                } else {
                    stringResource(R.string.app_settings_protocol_advanced_premium_required)
                },
                selected = selectedMode == BluetoothConnectionMode.BLE_BINARY,
                enabled = canUseAdvanced,
                onClick = {
                    if (canUseAdvanced) {
                        onModeSelected(BluetoothConnectionMode.BLE_BINARY)
                    }
                },
            )
        }
    }
}

@Composable
private fun GreenhouseConnectionModeOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    GreenhouseCard(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        elevated = false,
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = GreenhouseGlass.AccentGreen,
                    unselectedColor = GreenhouseGlass.TextOnGlassSecondary,
                ),
            )
            Text(
                text = label,
                color = if (enabled) {
                    GreenhouseGlass.TextOnGlassPrimary
                } else {
                    GreenhouseGlass.TextOnGlassSecondary
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = GreenhouseGlass.TextOnGlassSecondary,
            modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
        )
    }
}
