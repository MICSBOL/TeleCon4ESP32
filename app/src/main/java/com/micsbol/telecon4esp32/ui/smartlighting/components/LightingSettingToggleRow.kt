package com.micsbol.telecon4esp32.ui.smartlighting.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.smartlighting.LightingSettingToggleColor
import com.micsbol.telecon4esp32.ui.smartlighting.LightingSettingUiModel
import com.micsbol.telecon4esp32.ui.smartlighting.SmartLightingGlass

@Composable
fun LightingSettingToggleRow(
    setting: LightingSettingUiModel,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val checkedTrackColor = when (setting.toggleColor) {
        LightingSettingToggleColor.GREEN -> SmartLightingGlass.AccentGreen
        LightingSettingToggleColor.BLUE -> SmartLightingGlass.AccentCool
    }

    if (showDivider) {
        HorizontalDivider(color = SmartLightingGlass.BorderColor.copy(alpha = 0.22f))
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = setting.icon,
            contentDescription = null,
            tint = SmartLightingGlass.AccentCyan,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = stringResource(setting.titleRes),
            style = MaterialTheme.typography.bodyLarge,
            color = SmartLightingGlass.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = setting.isEnabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = checkedTrackColor,
                checkedThumbColor = SmartLightingGlass.TextPrimary,
                uncheckedTrackColor = SmartLightingGlass.CardSurfaceTint.copy(alpha = 0.55f),
                uncheckedThumbColor = SmartLightingGlass.TextMuted,
            ),
        )
    }
}
