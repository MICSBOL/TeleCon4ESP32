package com.micsbol.telecon4esp32.ui.smartdoorlock.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.smartdoorlock.SmartDoorLockGlass

@Composable
fun SmartDoorSignalButtons(
    onUnlockClick: () -> Unit,
    onLockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SmartDoorQuickActionCard(
            label = stringResource(R.string.smart_door_lock_send_unlock_short),
            icon = Icons.Default.Key,
            style = SmartDoorLockCardStyle.Green,
            onClick = onUnlockClick,
            modifier = Modifier
                .weight(1f)
                .height(100.dp),
        )
        SmartDoorQuickActionCard(
            label = stringResource(R.string.smart_door_lock_send_lock_short),
            icon = Icons.Default.Lock,
            style = SmartDoorLockCardStyle.Gray,
            onClick = onLockClick,
            modifier = Modifier
                .weight(1f)
                .height(100.dp),
        )
    }
}

@Composable
private fun SmartDoorQuickActionCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    style: SmartDoorLockCardStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmartDoorLockActionCard(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        style = style,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SmartDoorLockGlass.TextPrimary,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SmartDoorLockGlass.TextPrimary,
            )
        }
    }
}
