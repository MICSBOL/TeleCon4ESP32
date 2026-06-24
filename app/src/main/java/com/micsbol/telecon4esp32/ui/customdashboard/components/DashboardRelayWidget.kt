package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.micsbol.telecon4esp32.R

@Composable
fun DashboardRelayWidget(
    isOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
) {
    DashboardWidgetCard(
        isSelected = isSelected,
        modifier = modifier,
    ) {
        DashboardCompactControlRow {
            DashboardWidgetStatusPill(
                label = stringResource(
                    if (isOn) R.string.custom_dashboard_relay_on else R.string.custom_dashboard_relay_off,
                ),
                isActive = isOn,
            )
            DashboardWidgetSwitch(
                checked = isOn,
                onCheckedChange = { onToggle() },
                enabled = enabled,
            )
        }
    }
}
