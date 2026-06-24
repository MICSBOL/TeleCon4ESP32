package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.SavedDashboardLayout
import com.micsbol.telecon4esp32.ui.components.EmitterBackButton
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.customdashboard.displayDashboardName
import com.micsbol.telecon4esp32.ui.theme.StatusConnected

@Composable
fun CustomDashboardHomeContent(
    savedDashboards: List<SavedDashboardLayout>,
    pendingDeleteLayoutId: String?,
    onBackClick: () -> Unit,
    onCreateDashboardClick: () -> Unit,
    onOpenDashboardClick: (String) -> Unit,
    onDeleteDashboardClick: (String) -> Unit,
    onDeleteDashboardConfirm: () -> Unit,
    onDeleteDashboardDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    topBarActions: @Composable () -> Unit = {},
) {
    val untitledLabel = stringResource(R.string.custom_dashboard_untitled_layout)
    val pendingDeleteLayout = pendingDeleteLayoutId?.let { layoutId ->
        savedDashboards.firstOrNull { it.id == layoutId }
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        CustomDashboardHomeTopBar(
            onBackClick = onBackClick,
            topBarActions = topBarActions,
        )

        if (savedDashboards.isEmpty()) {
            CustomDashboardEmptyState(
                onCreateDashboardClick = onCreateDashboardClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Button(
                        onClick = onCreateDashboardClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(R.string.custom_dashboard_create_new),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                items(savedDashboards, key = { it.id }) { layout ->
                    SavedDashboardLayoutCard(
                        layout = layout,
                        displayName = displayDashboardName(layout.name, untitledLabel),
                        onClick = { onOpenDashboardClick(layout.id) },
                        onDeleteClick = { onDeleteDashboardClick(layout.id) },
                    )
                }
            }
        }
    }

    pendingDeleteLayout?.let { layout ->
        AlertDialog(
            onDismissRequest = onDeleteDashboardDismiss,
            title = { Text(text = stringResource(R.string.custom_dashboard_delete_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.custom_dashboard_delete_message,
                        displayDashboardName(layout.name, untitledLabel),
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = onDeleteDashboardConfirm) {
                    Text(text = stringResource(R.string.custom_dashboard_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDashboardDismiss) {
                    Text(text = stringResource(R.string.custom_dashboard_save_name_cancel))
                }
            },
        )
    }
}

@Composable
private fun CustomDashboardHomeTopBar(
    onBackClick: () -> Unit,
    topBarActions: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmitterBackButton(onClick = onBackClick)
        Text(
            text = stringResource(R.string.app_custom_dashboard_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        topBarActions()
    }
}

@Composable
private fun CustomDashboardEmptyState(
    onCreateDashboardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.DashboardCustomize,
            contentDescription = null,
            tint = brandPrimary(),
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp),
        )
        Text(
            text = stringResource(R.string.custom_dashboard_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.custom_dashboard_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Button(onClick = onCreateDashboardClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
            )
            Text(
                text = stringResource(R.string.custom_dashboard_create_new),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun SavedDashboardLayoutCard(
    layout: SavedDashboardLayout,
    displayName: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.custom_dashboard_saved_widget_count,
                        layout.widgets.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusConnected,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.custom_dashboard_delete_content_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
