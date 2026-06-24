package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.StatusDisconnected

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
        ConnectionStatusChip(isConnected = isEsp32Connected)

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

@Composable
private fun ConnectionStatusChip(isConnected: Boolean) {
    val backgroundColor = if (isConnected) {
        StatusConnected.copy(alpha = 0.18f)
    } else {
        StatusDisconnected.copy(alpha = 0.18f)
    }
    val contentColor = if (isConnected) StatusConnected else StatusDisconnected

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(
                if (isConnected) {
                    R.string.custom_dashboard_esp32_connected
                } else {
                    R.string.custom_dashboard_esp32_disconnected
                },
            ),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
        )
    }
}
