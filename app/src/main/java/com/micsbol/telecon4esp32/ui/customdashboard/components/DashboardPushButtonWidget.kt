package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.brandPrimary

@Composable
fun DashboardPushButtonWidget(
    isPressed: Boolean,
    onPress: () -> Unit,
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
                    if (isPressed) {
                        R.string.custom_dashboard_push_button_pressed
                    } else {
                        R.string.custom_dashboard_push_button_idle
                    },
                ),
                isActive = isPressed,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isPressed) {
                            brandPrimary().copy(alpha = 0.88f)
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                        },
                    )
                    .clickable(enabled = enabled, onClick = onPress),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.custom_dashboard_push_button_action),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPressed) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
