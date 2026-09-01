package com.micsbol.telecon4esp32.ui.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogBody
import com.micsbol.telecon4esp32.ui.components.NeoDialogTextAction
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.NeoIconButton
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeoSecondaryButton
import com.micsbol.telecon4esp32.ui.theme.Neo

private val ProGold = Color(0xFFFFD54F)

/**
 * Top-bar lock next to the settings title while Advanced protocol options are locked.
 */
@Composable
fun RowScope.AdvancedSettingsTitleLockAction(
    onClick: () -> Unit,
) {
    NeoIconButton(
        icon = Icons.Filled.Lock,
        onClick = onClick,
        contentDescription = stringResource(
            R.string.app_settings_advanced_lock_content_description,
        ),
        size = 40.dp,
        iconTint = ProGold,
    )
}

/**
 * Top-bar restore control in the same slot as [AdvancedSettingsTitleLockAction]
 * once Advanced settings are unlocked.
 */
@Composable
fun RowScope.ResetDefaultConfigurationTitleAction(
    onReset: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }
    if (showConfirm) {
        ResetDefaultConfigurationConfirmDialog(
            onConfirm = {
                showConfirm = false
                onReset()
            },
            onDismiss = { showConfirm = false },
        )
    }
    NeoIconButton(
        icon = Icons.Filled.SettingsBackupRestore,
        onClick = { showConfirm = true },
        contentDescription = stringResource(R.string.app_settings_reset_defaults_button),
        size = 40.dp,
        iconTint = Neo.Accent,
    )
}

@Composable
fun ResetDefaultConfigurationConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    NeoDialog(
        onDismissRequest = onDismiss,
        title = {
            NeoDialogTitle(text = stringResource(R.string.app_settings_reset_defaults_title))
        },
        subtitle = {
            NeoDialogBody(text = stringResource(R.string.app_settings_reset_defaults_body))
        },
        actions = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NeoPillButton(
                    text = stringResource(R.string.app_settings_reset_defaults_confirm),
                    onClick = onConfirm,
                    fillMaxWidth = true,
                    compact = true,
                )
                NeoSecondaryButton(
                    text = stringResource(R.string.app_settings_reset_defaults_cancel),
                    onClick = onDismiss,
                    fillMaxWidth = true,
                    compact = true,
                )
            }
        },
    )
}

/**
 * Explains Advanced settings and offers the coin / Pro unlock entry point.
 */
@Composable
fun AdvancedSettingsInfoDialog(
    onDismiss: () -> Unit,
    onUnlockClick: () -> Unit,
) {
    NeoDialog(
        onDismissRequest = onDismiss,
        horizontalMargin = 28.dp,
        title = {
            NeoDialogTitle(text = stringResource(R.string.app_settings_advanced_unlock_title))
        },
        content = {
            Text(
                text = stringResource(R.string.app_settings_advanced_info_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Neo.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.app_settings_advanced_unlock_row_subtitle),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Neo.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.app_settings_advanced_info_audience_note),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Neo.Positive,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        actions = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NeoPillButton(
                    text = stringResource(R.string.app_settings_advanced_info_unlock_button),
                    onClick = onUnlockClick,
                    fillMaxWidth = true,
                )
                NeoDialogTextAction(
                    text = stringResource(R.string.codes_pdf_cancel),
                    onClick = onDismiss,
                )
            }
        },
    )
}
