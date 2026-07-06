package com.micsbol.telecon4esp32.ui.smarthome.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.ui.smarthome.SmartHomeGlass
import com.micsbol.telecon4esp32.ui.smarthome.SystemTileUiModel

@Composable
fun SystemTile(
    system: SystemTileUiModel,
    modifier: Modifier = Modifier,
) {
    SmartHomeCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(system.iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = system.icon,
                    contentDescription = null,
                    tint = system.iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = stringResource(system.titleRes),
                style = MaterialTheme.typography.labelMedium,
                color = SmartHomeGlass.TextSecondary,
                textAlign = TextAlign.Center,
            )
            if (system.value.isNotEmpty()) {
                Text(
                    text = system.value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = SmartHomeGlass.TextPrimary,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = stringResource(system.statusRes),
                style = MaterialTheme.typography.labelSmall,
                color = system.statusColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
