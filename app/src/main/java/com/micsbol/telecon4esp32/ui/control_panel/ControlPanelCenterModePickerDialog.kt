package com.micsbol.telecon4esp32.ui.control_panel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ControlPanelCenterMode
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogTextAction
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.theme.Neo

@Composable
fun ControlPanelCenterModePickerDialog(
    selected: ControlPanelCenterMode,
    isModeUnlocked: (ControlPanelCenterMode) -> Boolean,
    onSelect: (ControlPanelCenterMode) -> Unit,
    onRequestUnlock: (ControlPanelCenterMode) -> Unit,
    onDismiss: () -> Unit,
) {
    NeoDialog(
        onDismissRequest = onDismiss,
        horizontalMargin = 48.dp,
        title = {
            NeoDialogTitle(text = stringResource(R.string.control_panel_center_mode_picker_title))
        },
        content = {
            ControlPanelCenterMode.entries.forEach { mode ->
                val unlocked = isModeUnlocked(mode)
                val rowAlpha = if (unlocked) 1f else 0.45f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(rowAlpha)
                        .clickable(role = Role.Button) {
                            if (unlocked) {
                                onSelect(mode)
                                onDismiss()
                            } else {
                                onRequestUnlock(mode)
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RadioButton(
                        selected = selected == mode,
                        enabled = unlocked,
                        onClick = {
                            if (unlocked) {
                                onSelect(mode)
                                onDismiss()
                            } else {
                                onRequestUnlock(mode)
                            }
                        },
                    )
                    Icon(
                        imageVector = mode.icon,
                        contentDescription = null,
                        tint = if (unlocked) brandPrimary() else Neo.TextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(mode.titleRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (unlocked) Neo.TextPrimary else Neo.TextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    if (!unlocked) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = stringResource(
                                R.string.control_panel_center_mode_locked_content_description,
                            ),
                            tint = Neo.TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
        actions = {
            NeoDialogTextAction(
                text = stringResource(R.string.codes_pdf_cancel),
                onClick = onDismiss,
            )
        },
    )
}
