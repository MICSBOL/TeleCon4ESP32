package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.DashboardWidgetPlacement
import com.micsbol.telecon4esp32.domain.model.DashboardWidgetType
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.customdashboard.CustomDashboardEditorState
import com.micsbol.telecon4esp32.ui.customdashboard.defaultColumnSpan
import com.micsbol.telecon4esp32.ui.customdashboard.defaultRowSpan
import com.micsbol.telecon4esp32.ui.customdashboard.hasWidgetOverlap
import com.micsbol.telecon4esp32.ui.customdashboard.joystickStickPosition
import com.micsbol.telecon4esp32.ui.customdashboard.titleRes
import kotlin.math.roundToInt

object DashboardGridSpec {
    const val COLUMNS = 12
    const val ROWS = 6
}

@Composable
fun DraggableDashboardGrid(
    editorState: CustomDashboardEditorState,
    onWidgetSelected: (String?) -> Unit,
    onWidgetMoved: (String, Int, Int) -> Unit,
    onWidgetDeleted: (String) -> Unit,
    onJoystickMove: (String, Float, Float) -> Unit,
    onJoystickConfigRequest: (String) -> Unit,
    onRelayToggle: (String) -> Unit,
    onOutputLevelChange: (String, Float) -> Unit,
    onSliderChange: (String, Float) -> Unit,
    onPushButtonPress: (String) -> Unit,
    onLedToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    dropHighlight: DropHighlight? = null,
) {
    var draggingWidgetId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isOverDeleteZone by remember { mutableStateOf(false) }

    LaunchedEffect(editorState.widgets) {
        if (draggingWidgetId != null && editorState.widgets.none { it.id == draggingWidgetId }) {
            draggingWidgetId = null
            dragOffset = Offset.Zero
            isOverDeleteZone = false
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        val density = LocalDensity.current
        val cellWidth = maxWidth / DashboardGridSpec.COLUMNS
        val cellHeight = maxHeight / DashboardGridSpec.ROWS
        val cellWidthPx = with(density) { cellWidth.roundToPx() }
        val cellHeightPx = with(density) { cellHeight.roundToPx() }
        val gridWidthPx = cellWidthPx * DashboardGridSpec.COLUMNS.toFloat()
        val gridHeightPx = cellHeightPx * DashboardGridSpec.ROWS.toFloat()

        val sortedWidgets = remember(editorState.widgets) {
            editorState.widgets.sortedWith(compareBy({ it.row }, { it.column }))
        }

        val currentWidgets by rememberUpdatedState(editorState.widgets)
        val currentOnWidgetMoved by rememberUpdatedState(onWidgetMoved)
        val currentOnWidgetDeleted by rememberUpdatedState(onWidgetDeleted)
        val currentOnWidgetSelected by rememberUpdatedState(onWidgetSelected)
        val currentOnJoystickConfigRequest by rememberUpdatedState(onJoystickConfigRequest)

        if (editorState.isEditMode) {
            DashboardGridBackground(
                cellWidth = cellWidth,
                cellHeight = cellHeight,
            )
        }

        dropHighlight?.let { highlight ->
            DropHighlightOverlay(
                highlight = highlight,
                cellWidth = cellWidth,
                cellHeight = cellHeight,
            )
        }

        if (editorState.widgets.isEmpty() && editorState.isEditMode && draggingWidgetId == null) {
            Text(
                text = stringResource(R.string.custom_dashboard_canvas_empty_hint),
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        sortedWidgets.forEach { widget ->
            key(widget.id) {
                WidgetGridCell(
                    widget = widget,
                    editorState = editorState,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    cellWidthPx = cellWidthPx,
                    cellHeightPx = cellHeightPx,
                    draggingWidgetId = draggingWidgetId,
                    dragOffset = dragOffset,
                    onJoystickMove = onJoystickMove,
                    onRelayToggle = onRelayToggle,
                    onOutputLevelChange = onOutputLevelChange,
                    onSliderChange = onSliderChange,
                    onPushButtonPress = onPushButtonPress,
                    onLedToggle = onLedToggle,
                )
            }
        }

        if (editorState.isEditMode) {
            DashboardWidgetDeleteZone(
                isActive = isOverDeleteZone,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = DashboardDeleteZoneSpec.BOTTOM_PADDING)
                    .zIndex(20f),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(30f)
                    .pointerInput(currentWidgets, cellWidthPx, cellHeightPx) {
                        detectTapGestures { offset ->
                            findWidgetAt(offset, currentWidgets, cellWidthPx, cellHeightPx)?.let { widget ->
                                if (widget.type == DashboardWidgetType.JOYSTICK) {
                                    currentOnJoystickConfigRequest(widget.id)
                                }
                            }
                        }
                    }
                    .pointerInput(currentWidgets, cellWidthPx, cellHeightPx, gridWidthPx, gridHeightPx) {
                        var gestureWidgetId: String? = null
                        var gestureWidgetBaseX = 0
                        var gestureWidgetBaseY = 0
                        var gestureColumnSpan = 1
                        var gestureRowSpan = 1
                        var gestureDragOffset = Offset.Zero
                        var gesturePendingDelete = false

                        detectDragGesturesAfterLongPress(
                            onDragStart = { startOffset ->
                                val widget = findWidgetAt(
                                    pointerInGrid = startOffset,
                                    widgets = currentWidgets,
                                    cellWidthPx = cellWidthPx,
                                    cellHeightPx = cellHeightPx,
                                )
                                if (widget == null) {
                                    gestureWidgetId = null
                                    return@detectDragGesturesAfterLongPress
                                }

                                gestureWidgetId = widget.id
                                gestureWidgetBaseX = widget.column * cellWidthPx
                                gestureWidgetBaseY = widget.row * cellHeightPx
                                gestureColumnSpan = widget.columnSpan
                                gestureRowSpan = widget.rowSpan
                                gestureDragOffset = Offset.Zero
                                gesturePendingDelete = false
                                isOverDeleteZone = false
                                draggingWidgetId = widget.id
                                dragOffset = Offset.Zero
                                currentOnWidgetSelected(widget.id)
                            },
                            onDrag = { change, dragAmount ->
                                if (gestureWidgetId == null) return@detectDragGesturesAfterLongPress
                                change.consume()
                                gestureDragOffset += dragAmount
                                dragOffset = gestureDragOffset

                                val deleteBounds = computeDeleteZoneBoundsInGrid(
                                    gridWidthPx = gridWidthPx,
                                    gridHeightPx = gridHeightPx,
                                    density = density,
                                )
                                gesturePendingDelete = deleteBounds.contains(change.position)
                                isOverDeleteZone = gesturePendingDelete
                            },
                            onDragEnd = {
                                val widgetId = gestureWidgetId ?: return@detectDragGesturesAfterLongPress
                                val shouldDelete = gesturePendingDelete
                                val finalDragOffset = gestureDragOffset
                                val columnSpan = gestureColumnSpan
                                val rowSpan = gestureRowSpan
                                val baseX = gestureWidgetBaseX
                                val baseY = gestureWidgetBaseY

                                gestureWidgetId = null
                                gesturePendingDelete = false
                                draggingWidgetId = null
                                dragOffset = Offset.Zero
                                isOverDeleteZone = false

                                if (shouldDelete) {
                                    currentOnWidgetDeleted(widgetId)
                                } else {
                                    val newXPx = baseX + finalDragOffset.x
                                    val newYPx = baseY + finalDragOffset.y
                                    val targetColumn = (newXPx / cellWidthPx.toFloat())
                                        .roundToInt()
                                        .coerceIn(0, DashboardGridSpec.COLUMNS - columnSpan)
                                    val targetRow = (newYPx / cellHeightPx.toFloat())
                                        .roundToInt()
                                        .coerceIn(0, DashboardGridSpec.ROWS - rowSpan)

                                    if (!hasWidgetOverlap(
                                            widgetId = widgetId,
                                            column = targetColumn,
                                            row = targetRow,
                                            columnSpan = columnSpan,
                                            rowSpan = rowSpan,
                                            widgets = currentWidgets,
                                        )
                                    ) {
                                        currentOnWidgetMoved(widgetId, targetColumn, targetRow)
                                    }
                                }
                            },
                            onDragCancel = {
                                gestureWidgetId = null
                                gesturePendingDelete = false
                                draggingWidgetId = null
                                dragOffset = Offset.Zero
                                isOverDeleteZone = false
                            },
                        )
                    },
            )
        }
    }
}

@Composable
private fun WidgetGridCell(
    widget: DashboardWidgetPlacement,
    editorState: CustomDashboardEditorState,
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
    cellWidthPx: Int,
    cellHeightPx: Int,
    draggingWidgetId: String?,
    dragOffset: Offset,
    onJoystickMove: (String, Float, Float) -> Unit,
    onRelayToggle: (String) -> Unit,
    onOutputLevelChange: (String, Float) -> Unit,
    onSliderChange: (String, Float) -> Unit,
    onPushButtonPress: (String) -> Unit,
    onLedToggle: (String) -> Unit,
) {
    val baseXPx = cellWidthPx * widget.column
    val baseYPx = cellHeightPx * widget.row
    val isDragging = draggingWidgetId == widget.id
    val controlsEnabled = !editorState.isEditMode

    Box(
        modifier = Modifier
            .offset { IntOffset(x = baseXPx, y = baseYPx) }
            .size(
                width = cellWidth * widget.columnSpan,
                height = cellHeight * widget.rowSpan,
            )
            .zIndex(if (isDragging) 15f else 0f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = if (isDragging) dragOffset.x.roundToInt() else 0,
                        y = if (isDragging) dragOffset.y.roundToInt() else 0,
                    )
                },
        ) {
            DashboardWidgetContent(
                widget = widget,
                editorState = editorState,
                isSelected = editorState.selectedWidgetId == widget.id,
                controlsEnabled = controlsEnabled,
                onJoystickMove = onJoystickMove,
                onRelayToggle = onRelayToggle,
                onOutputLevelChange = onOutputLevelChange,
                onSliderChange = onSliderChange,
                onPushButtonPress = onPushButtonPress,
                onLedToggle = onLedToggle,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun findWidgetAt(
    pointerInGrid: Offset,
    widgets: List<DashboardWidgetPlacement>,
    cellWidthPx: Int,
    cellHeightPx: Int,
): DashboardWidgetPlacement? {
    return widgets
        .asReversed()
        .firstOrNull { widget ->
            val left = widget.column * cellWidthPx.toFloat()
            val top = widget.row * cellHeightPx.toFloat()
            val right = left + widget.columnSpan * cellWidthPx
            val bottom = top + widget.rowSpan * cellHeightPx
            pointerInGrid.x >= left && pointerInGrid.x < right &&
                pointerInGrid.y >= top && pointerInGrid.y < bottom
        }
}

private fun computeDeleteZoneBoundsInGrid(
    gridWidthPx: Float,
    gridHeightPx: Float,
    density: Density,
): Rect {
    val zoneSizePx = with(density) { DashboardDeleteZoneSpec.SIZE.roundToPx().toFloat() }
    val bottomPaddingPx = with(density) { DashboardDeleteZoneSpec.BOTTOM_PADDING.roundToPx().toFloat() }
    val hitSlopPx = with(density) { DashboardDeleteZoneSpec.HIT_SLOP.roundToPx().toFloat() }
    val left = (gridWidthPx - zoneSizePx) / 2f - hitSlopPx
    val top = gridHeightPx - bottomPaddingPx - zoneSizePx - hitSlopPx
    val size = zoneSizePx + hitSlopPx * 2f
    return Rect(left, top, left + size, top + size)
}

data class DropHighlight(
    val type: DashboardWidgetType,
    val column: Int,
    val row: Int,
)

@Composable
private fun DropHighlightOverlay(
    highlight: DropHighlight,
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
) {
    val columnSpan = highlight.type.defaultColumnSpan()
    val rowSpan = highlight.type.defaultRowSpan()
    Box(
        modifier = Modifier
            .zIndex(0.5f)
            .offset {
                IntOffset(
                    x = (cellWidth * highlight.column).roundToPx(),
                    y = (cellHeight * highlight.row).roundToPx(),
                )
            }
            .size(
                width = cellWidth * columnSpan,
                height = cellHeight * rowSpan,
            )
            .border(
                width = 2.dp,
                color = brandPrimary().copy(alpha = 0.85f),
                shape = RoundedCornerShape(14.dp),
            )
            .background(brandPrimary().copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(highlight.type.titleRes()),
            style = MaterialTheme.typography.labelMedium,
            color = brandPrimary(),
        )
    }
}

@Composable
private fun DashboardWidgetContent(
    widget: DashboardWidgetPlacement,
    editorState: CustomDashboardEditorState,
    isSelected: Boolean,
    controlsEnabled: Boolean,
    onJoystickMove: (String, Float, Float) -> Unit,
    onRelayToggle: (String) -> Unit,
    onOutputLevelChange: (String, Float) -> Unit,
    onSliderChange: (String, Float) -> Unit,
    onPushButtonPress: (String) -> Unit,
    onLedToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (widget.type) {
        DashboardWidgetType.JOYSTICK -> DashboardJoystickWidget(
            stickPosition = joystickStickPosition(widget.id, editorState.joystickPositions),
            mode = widget.joystickMode,
            onMove = { x, y -> onJoystickMove(widget.id, x, y) },
            isSelected = isSelected,
            enabled = controlsEnabled,
            modifier = modifier,
        )

        DashboardWidgetType.SLIDER -> DashboardSliderWidget(
            value = widget.level,
            onValueChange = { level -> onSliderChange(widget.id, level) },
            isSelected = isSelected,
            enabled = controlsEnabled,
            modifier = modifier,
        )

        DashboardWidgetType.WAVEFORM -> DashboardWaveformWidget(
            plotSeries = editorState.plotSeries,
            plotRevision = editorState.plotRevision,
            stats = editorState.waveformStats,
            isSelected = isSelected,
            modifier = modifier,
        )

        DashboardWidgetType.VALUE_READOUT -> DashboardValueReadoutWidget(
            value = widget.readoutValue,
            unitLabel = stringResource(R.string.custom_dashboard_value_readout_unit),
            isSelected = isSelected,
            modifier = modifier,
        )

        DashboardWidgetType.RELAY -> DashboardRelayWidget(
            isOn = widget.isOn,
            onToggle = { onRelayToggle(widget.id) },
            isSelected = isSelected,
            enabled = controlsEnabled,
            modifier = modifier,
        )

        DashboardWidgetType.OUTPUT_KNOB -> DashboardOutputKnobWidget(
            level = widget.level,
            onLevelChange = { level -> onOutputLevelChange(widget.id, level) },
            isSelected = isSelected,
            enabled = controlsEnabled,
            modifier = modifier,
        )

        DashboardWidgetType.PUSH_BUTTON -> DashboardPushButtonWidget(
            isPressed = widget.isPressed,
            onPress = { onPushButtonPress(widget.id) },
            isSelected = isSelected,
            enabled = controlsEnabled,
            modifier = modifier,
        )

        DashboardWidgetType.LED_INDICATOR -> DashboardLedIndicatorWidget(
            isOn = widget.isOn,
            onToggle = { onLedToggle(widget.id) },
            isSelected = isSelected,
            enabled = controlsEnabled,
            modifier = modifier,
        )
    }
}

@Composable
private fun DashboardGridBackground(
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
) {
    val dotColor = Color.White.copy(alpha = 0.08f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cellWidthPx = cellWidth.toPx()
        val cellHeightPx = cellHeight.toPx()
        for (column in 0..DashboardGridSpec.COLUMNS) {
            for (row in 0..DashboardGridSpec.ROWS) {
                drawCircle(
                    color = dotColor,
                    radius = 2.dp.toPx(),
                    center = Offset(column * cellWidthPx, row * cellHeightPx),
                )
            }
        }
    }
}

fun windowPositionToGridCell(
    gridOriginInWindow: Offset,
    windowPosition: Offset,
    cellWidthPx: Float,
    cellHeightPx: Float,
    columnSpan: Int,
    rowSpan: Int,
): Pair<Int, Int>? {
    if (windowPosition.x < 0f || windowPosition.y < 0f) return null
    val localX = windowPosition.x - gridOriginInWindow.x
    val localY = windowPosition.y - gridOriginInWindow.y
    if (localX < 0f || localY < 0f) return null

    val column = (localX / cellWidthPx).toInt()
        .coerceIn(0, DashboardGridSpec.COLUMNS - columnSpan)
    val row = (localY / cellHeightPx).toInt()
        .coerceIn(0, DashboardGridSpec.ROWS - rowSpan)
    return column to row
}
