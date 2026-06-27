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
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.protocolPrefix
import com.micsbol.telecon4esp32.ui.components.EmitterSectionTitle
import com.micsbol.telecon4esp32.ui.components.EmitterStyledCard
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.components.mutedTextColor
import com.micsbol.telecon4esp32.ui.rc_settings.SettingsSection

@Composable
fun ApplicationProtocolSettingsSection(
    applicationId: ApplicationId,
    selectedMode: BluetoothProtocolMode,
    onModeSelected: (BluetoothProtocolMode) -> Unit,
) {
    val supportsAdvanced = applicationId == ApplicationId.CONTROL_PANEL

    SettingsSection(
        title = stringResource(R.string.app_settings_protocol_section_title),
    ) {
        Text(
            text = stringResource(
                R.string.app_settings_protocol_prefix_label,
                applicationId.protocolPrefix(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = mutedTextColor(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.app_settings_protocol_section_description),
            style = MaterialTheme.typography.bodySmall,
            color = mutedTextColor(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(Modifier.selectableGroup()) {
            ProtocolModeOption(
                label = stringResource(R.string.app_settings_protocol_simple),
                description = stringResource(R.string.app_settings_protocol_simple_description),
                selected = selectedMode == BluetoothProtocolMode.SIMPLE,
                onClick = { onModeSelected(BluetoothProtocolMode.SIMPLE) },
            )
            if (supportsAdvanced) {
                Spacer(modifier = Modifier.height(8.dp))
                ProtocolModeOption(
                    label = stringResource(R.string.app_settings_protocol_advanced),
                    description = stringResource(R.string.app_settings_protocol_advanced_description),
                    selected = selectedMode == BluetoothProtocolMode.ADVANCED,
                    onClick = { onModeSelected(BluetoothProtocolMode.ADVANCED) },
                )
            }
        }
    }
}

@Composable
private fun ProtocolModeOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    EmitterStyledCard(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
    ) {
        Column(Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = brandPrimary()),
                )
                EmitterSectionTitle(text = label)
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = mutedTextColor(),
                modifier = Modifier.padding(start = 48.dp),
            )
        }
    }
}
