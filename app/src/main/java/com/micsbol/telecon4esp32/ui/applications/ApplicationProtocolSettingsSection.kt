package com.micsbol.telecon4esp32.ui.applications

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
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Esp32Board
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoSectionTitle
import com.micsbol.telecon4esp32.ui.theme.Neo

@Composable
fun ApplicationDeviceSettingsSection(
    selectedBoard: Esp32Board,
    onBoardSelected: (Esp32Board) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        NeoSectionTitle(text = stringResource(R.string.app_settings_device_section_title))
        Text(
            text = stringResource(R.string.app_settings_device_section_description),
            style = MaterialTheme.typography.bodySmall,
            color = Neo.TextSecondary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(Modifier.selectableGroup()) {
            ConnectionModeOption(
                label = stringResource(R.string.app_settings_device_dev_kit),
                description = stringResource(R.string.app_settings_device_dev_kit_description),
                selected = selectedBoard == Esp32Board.DEV_KIT,
                onClick = { onBoardSelected(Esp32Board.DEV_KIT) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            ConnectionModeOption(
                label = stringResource(R.string.app_settings_device_cam),
                description = stringResource(R.string.app_settings_device_cam_description),
                selected = selectedBoard == Esp32Board.CAM,
                onClick = { onBoardSelected(Esp32Board.CAM) },
            )
        }
    }
}

@Composable
fun ApplicationProtocolSettingsSection(
    applicationId: ApplicationId,
    selectedMode: BluetoothConnectionMode,
    onModeSelected: (BluetoothConnectionMode) -> Unit,
    canUseAdvanced: Boolean = true,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        NeoSectionTitle(text = stringResource(R.string.app_settings_connection_section_title))
        Text(
            text = stringResource(
                R.string.app_settings_protocol_prefix_label,
                applicationId.protocolPrefix(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = Neo.TextSecondary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.app_settings_connection_section_description),
            style = MaterialTheme.typography.bodySmall,
            color = Neo.TextSecondary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(Modifier.selectableGroup()) {
            ConnectionModeOption(
                label = stringResource(R.string.app_settings_connection_classic_simple),
                description = stringResource(R.string.app_settings_connection_classic_simple_description),
                selected = selectedMode == BluetoothConnectionMode.CLASSIC_SIMPLE,
                onClick = { onModeSelected(BluetoothConnectionMode.CLASSIC_SIMPLE) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            ConnectionModeOption(
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
            ConnectionModeOption(
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
private fun ConnectionModeOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    NeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
                role = Role.RadioButton,
            ),
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Neo.Accent,
                    unselectedColor = Neo.TextSecondary,
                ),
            )
            Text(
                text = label,
                color = Neo.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Neo.TextSecondary,
            modifier = Modifier.padding(start = 48.dp),
        )
    }
}
