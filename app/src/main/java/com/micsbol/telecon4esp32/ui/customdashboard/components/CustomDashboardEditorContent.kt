package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.LiveControlBluetoothDisconnectedBannerOverlay
import com.micsbol.telecon4esp32.ui.customdashboard.CustomDashboardEditorState
import com.micsbol.telecon4esp32.domain.model.DashboardWidgetType
import com.micsbol.telecon4esp32.ui.customdashboard.defaultColumnSpan
import com.micsbol.telecon4esp32.ui.customdashboard.defaultRowSpan
import com.micsbol.telecon4esp32.ui.customdashboard.titleRes
import com.micsbol.telecon4esp32.domain.model.JoystickMode

@Composable
fun CustomDashboardEditorContent(
    editorState: CustomDashboardEditorState,
    layoutName: String,
    onBackClick: () -> Unit,
    onToggleEditMode: () -> Unit,
    onSaveLayoutRequested: () -> Unit,
    onRenameLayoutRequested: () -> Unit,
    onSaveNameChanged: (String) -> Unit,
    onSaveLayoutConfirmed: () -> Unit,
    onSaveNameDialogDismiss: () -> Unit,
    onWidgetSelected: (String?) -> Unit,
    onWidgetMoved: (String, Int, Int) -> Unit,
    onWidgetDeleted: (String) -> Unit,
    onWidgetAdded: (DashboardWidgetType, Int, Int) -> Unit,
    onJoystickMove: (String, Float, Float) -> Unit,
    onJoystickModeChanged: (String, JoystickMode) -> Unit,
    onJoystickConfigRequest: (String) -> Unit,
    onJoystickConfigDismiss: () -> Unit,
    onRelayToggle: (String) -> Unit,
    onOutputLevelChange: (String, Float) -> Unit,
    onSliderChange: (String, Float) -> Unit,
    onPushButtonPress: (String) -> Unit,
    onLedToggle: (String) -> Unit,
    onBluetoothDisconnectedClick: () -> Unit,
    modifier: Modifier = Modifier,
    topBarActions: @Composable () -> Unit = {},
) {
    var gridCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var draggingPaletteType by remember { mutableStateOf<DashboardWidgetType?>(null) }
    var dropHighlight by remember { mutableStateOf<DropHighlight?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        CustomDashboardTopBar(
            layoutName = layoutName,
            isEsp32Connected = editorState.isEsp32Connected,
            isEditMode = editorState.isEditMode,
            onCloseClick = onBackClick,
            onToggleEditMode = onToggleEditMode,
            onSaveClick = onSaveLayoutRequested,
            onRenameClick = onRenameLayoutRequested,
            canSave = editorState.widgets.isNotEmpty(),
            onBluetoothDisconnectedClick = onBluetoothDisconnectedClick,
            saveConfirmationVisible = editorState.saveConfirmationVisible,
            topBarActions = topBarActions,
        )

        if (editorState.isEditMode) {
            Text(
                text = stringResource(R.string.custom_dashboard_editor_drag_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (editorState.isEditMode) {
                DashboardWidgetPalettePanel(
                    onPaletteDragStart = { type ->
                        draggingPaletteType = type
                    },
                    onPaletteDragMove = { windowPosition ->
                        val gridCoords = gridCoordinates
                        if (gridCoords != null && draggingPaletteType != null) {
                            val type = draggingPaletteType!!
                            val gridOrigin = gridCoords.positionInWindow()
                            val cellWidthPx = gridCoords.size.width.toFloat() / DashboardGridSpec.COLUMNS
                            val cellHeightPx = gridCoords.size.height.toFloat() / DashboardGridSpec.ROWS
                            val cell = windowPositionToGridCell(
                                gridOriginInWindow = gridOrigin,
                                windowPosition = windowPosition,
                                cellWidthPx = cellWidthPx,
                                cellHeightPx = cellHeightPx,
                                columnSpan = type.defaultColumnSpan(),
                                rowSpan = type.defaultRowSpan(),
                            )
                            dropHighlight = cell?.let { (column, row) ->
                                DropHighlight(type = type, column = column, row = row)
                            }
                        }
                    },
                    onPaletteDragEnd = { type ->
                        dropHighlight?.takeIf { it.type == type }?.let { highlight ->
                            onWidgetAdded(type, highlight.column, highlight.row)
                        }
                        draggingPaletteType = null
                        dropHighlight = null
                    },
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(start = if (editorState.isEditMode) 8.dp else 0.dp)
                    .onGloballyPositioned { gridCoordinates = it },
            ) {
                DraggableDashboardGrid(
                    editorState = editorState,
                    onWidgetSelected = onWidgetSelected,
                    onWidgetMoved = onWidgetMoved,
                    onWidgetDeleted = onWidgetDeleted,
                    onJoystickMove = onJoystickMove,
                    onJoystickConfigRequest = onJoystickConfigRequest,
                    onRelayToggle = onRelayToggle,
                    onOutputLevelChange = onOutputLevelChange,
                    onSliderChange = onSliderChange,
                    onPushButtonPress = onPushButtonPress,
                    onLedToggle = onLedToggle,
                    dropHighlight = if (editorState.isEditMode) dropHighlight else null,
                    modifier = Modifier.fillMaxSize(),
                )

                draggingPaletteType?.let { type ->
                    PaletteDragGhost(
                        type = type,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

            LiveControlBluetoothDisconnectedBannerOverlay(
                visible = !editorState.isEsp32Connected,
                onClick = onBluetoothDisconnectedClick,
            )
        }
    }

    editorState.joystickConfigWidgetId?.let { widgetId ->
        val widget = editorState.widgets.firstOrNull { it.id == widgetId && it.type == DashboardWidgetType.JOYSTICK }
        if (widget != null) {
            DashboardJoystickConfigDialog(
                selectedMode = widget.joystickMode,
                onModeSelected = { mode -> onJoystickModeChanged(widgetId, mode) },
                onDismiss = onJoystickConfigDismiss,
            )
        }
    }

    if (editorState.saveNameDialogVisible) {
        DashboardSaveNameDialog(
            name = editorState.pendingSaveName,
            isRename = editorState.saveNameDialogIsRename,
            onNameChange = onSaveNameChanged,
            onConfirm = onSaveLayoutConfirmed,
            onDismiss = onSaveNameDialogDismiss,
        )
    }
}

@Composable
private fun PaletteDragGhost(
    type: DashboardWidgetType,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Text(
            text = stringResource(type.titleRes()),
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopCenter)
                .padding(top = 8.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
