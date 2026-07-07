package com.micsbol.telecon4esp32.ui.smartdoorlock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Sensors
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
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.smartdoorlock.RelayPinState
import com.micsbol.telecon4esp32.ui.smartdoorlock.SmartDoorLockGlass

@Composable
fun SmartDoorRelayPanel(
    relayPinState: RelayPinState,
    isPulseActive: Boolean,
    onTriggerPulse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmartDoorLockCard(
        modifier = modifier.fillMaxWidth(),
        style = SmartDoorLockCardStyle.Glass,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = SmartDoorLockGlass.AccentGreenBright,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.smart_door_lock_relay_pin_state,
                            relayPinState.name,
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SmartDoorLockGlass.TextPrimary,
                    )
                }
                Text(
                    text = stringResource(R.string.smart_door_lock_relay_active_low),
                    style = MaterialTheme.typography.labelSmall,
                    color = SmartDoorLockGlass.TextMuted,
                )
            }

            Row(
                modifier = Modifier
                    .clip(SmartDoorLockGlass.PillShape)
                    .background(SmartDoorLockGlass.AccentGreen.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isPulseActive,
                        onClick = onTriggerPulse,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = SmartDoorLockGlass.TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = if (isPulseActive) {
                        stringResource(R.string.smart_door_lock_trigger_pulse_active)
                    } else {
                        stringResource(R.string.smart_door_lock_trigger_pulse)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SmartDoorLockGlass.TextPrimary,
                )
            }
        }
    }
}
