package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.LiveControlBluetoothStatusChip
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.theme.StatusConnected

@Composable
fun CustomDashboardTopBar(
    layoutName: String,
    isEsp32Connected: Boolean,
    isEditMode: Boolean,
    onCloseClick: () -> Unit,
    onToggleEditMode: () -> Unit,
    onSaveClick: () -> Unit,
    onRenameClick: () -> Unit,
    canSave: Boolean,
    onBluetoothDisconnectedClick: () -> Unit,
    saveConfirmationVisible: Boolean = false,
    modifier: Modifier = Modifier,
    topBarActions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveControlBluetoothStatusChip(
            isConnected = isEsp32Connected,
            onDisconnectedClick = onBluetoothDisconnectedClick,
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = layoutName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickable(onClick = onRenameClick),
        )

        Spacer(modifier = Modifier.weight(1f))

        if (saveConfirmationVisible) {
            Text(
                text = stringResource(R.string.custom_dashboard_layout_saved),
                style = MaterialTheme.typography.labelMedium,
                color = StatusConnected,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        if (isEditMode) {
            TextButton(
                onClick = onSaveClick,
                enabled = canSave,
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = stringResource(R.string.custom_dashboard_save_layout_content_description),
                    tint = if (canSave) brandPrimary() else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.custom_dashboard_save_layout),
                    color = if (canSave) brandPrimary() else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            }
        }

        TextButton(onClick = onToggleEditMode) {
            Icon(
                imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                contentDescription = null,
                tint = brandPrimary(),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(
                    if (isEditMode) {
                        R.string.custom_dashboard_preview_mode
                    } else {
                        R.string.custom_dashboard_edit_mode
                    },
                ),
                color = brandPrimary(),
            )
        }

        topBarActions()

        IconButton(onClick = onCloseClick) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.custom_dashboard_close_content_description),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
