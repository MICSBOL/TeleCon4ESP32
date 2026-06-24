package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.ui.rc_settings.JoystickModeSelector

@Composable
fun DashboardJoystickConfigDialog(
    selectedMode: JoystickMode,
    onModeSelected: (JoystickMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val allModes = listOf(
        JoystickMode.Spring(),
        JoystickMode.Hold(),
        JoystickMode.VerticalSpring(),
        JoystickMode.VerticalHold(),
        JoystickMode.HorizontalSpring(),
        JoystickMode.HorizontalHold(),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.custom_dashboard_joystick_config_title))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.custom_dashboard_joystick_config_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                JoystickModeSelector(
                    label = stringResource(R.string.custom_dashboard_joystick_config_behavior),
                    allModes = allModes,
                    selectedMode = selectedMode,
                    onModeSelected = onModeSelected,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.custom_dashboard_joystick_config_done))
            }
        },
    )
}
