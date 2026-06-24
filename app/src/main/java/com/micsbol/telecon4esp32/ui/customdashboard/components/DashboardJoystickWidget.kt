package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.JoystickMode
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components.RcTransparentJoystick

@Composable
fun DashboardJoystickWidget(
    stickPosition: Pair<Float, Float>,
    mode: JoystickMode,
    onMove: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
) {
    DashboardWidgetCard(
        isSelected = isSelected,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            RcTransparentJoystick(
                stickPosition = stickPosition,
                mode = mode,
                onMove = { x, y -> if (enabled) onMove(x, y) },
                size = 132.dp,
            )
        }

        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DashboardMetricChip(
                label = "X",
                value = stringResource(R.string.custom_dashboard_coordinate_x, stickPosition.first)
                    .substringAfter(": "),
                valueColor = brandPrimary(),
            )
            DashboardMetricChip(
                label = "Y",
                value = stringResource(R.string.custom_dashboard_coordinate_y, stickPosition.second)
                    .substringAfter(": "),
                valueColor = brandPrimary(),
            )
        }
    }
}
