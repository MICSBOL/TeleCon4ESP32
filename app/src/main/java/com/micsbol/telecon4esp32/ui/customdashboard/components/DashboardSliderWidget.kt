package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.brandPrimary

@Composable
fun DashboardSliderWidget(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
) {
    val percent = (value.coerceIn(0f, 1f) * 100f).toInt()

    DashboardWidgetCard(
        isSelected = isSelected,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.custom_dashboard_slider_value, percent),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = brandPrimary(),
            )
            Slider(
                value = value.coerceIn(0f, 1f),
                onValueChange = { if (enabled) onValueChange(it) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = brandPrimary(),
                    activeTrackColor = brandPrimary(),
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                ),
            )
        }
    }
}
