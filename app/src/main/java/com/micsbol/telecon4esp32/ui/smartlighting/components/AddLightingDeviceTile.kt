package com.micsbol.telecon4esp32.ui.smartlighting.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.smartlighting.AddLightingDeviceOptionUiModel
import com.micsbol.telecon4esp32.ui.smartlighting.SmartLightingGlass

@Composable
fun AddLightingDeviceTile(
    option: AddLightingDeviceOptionUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SmartLightingGlass.SmallCardShape)
            .background(SmartLightingGlass.CardSurface.copy(alpha = SmartLightingGlass.SurfaceAlpha))
            .border(
                width = 1.dp,
                color = option.accentColor.copy(alpha = 0.45f),
                shape = SmartLightingGlass.SmallCardShape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 20.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = null,
            tint = option.accentColor,
            modifier = Modifier.size(32.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(option.titleRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = SmartLightingGlass.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(option.subtitleRes),
            style = MaterialTheme.typography.labelSmall,
            color = option.accentColor,
            textAlign = TextAlign.Center,
        )
    }
}
