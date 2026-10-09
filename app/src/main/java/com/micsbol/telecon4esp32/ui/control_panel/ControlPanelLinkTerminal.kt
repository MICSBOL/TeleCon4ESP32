package com.micsbol.telecon4esp32.ui.control_panel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.LinkTrafficDirection
import com.micsbol.telecon4esp32.domain.bluetooth.LinkTrafficEntry

private val TerminalBackground = Color(0xFF07110C)
private val TerminalMuted = Color(0xFF6B8F78)

enum class TerminalTrafficFilter {
    BOTH,
    SENT,
    RECEIVED,
}

enum class TerminalTextSize(val bodySp: TextUnit, val againSp: TextUnit) {
    SMALL(9.sp, 8.sp),
    MEDIUM(11.sp, 10.sp),
    LARGE(14.sp, 12.sp),
}

enum class TerminalPalette(val sent: Color, val received: Color) {
    CLASSIC(Color(0xFF5CFFD0), Color(0xFFFFD56A)),
    WARM(Color(0xFFFFC14D), Color(0xFFFF7A59)),
    ICE(Color(0xFF7EC8FF), Color(0xFFF2FBFF)),
    MONO(Color(0xFFD5FFE4), Color(0xFF8CFFB0)),
}

@Composable
fun ControlPanelLinkTerminal(
    lines: List<LinkTrafficEntry>,
    traffic: TerminalTrafficFilter,
    onTrafficChange: (TerminalTrafficFilter) -> Unit,
    textSize: TerminalTextSize,
    onTextSizeChange: (TerminalTextSize) -> Unit,
    palette: TerminalPalette,
    onPaletteChange: (TerminalPalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = remember(lines, traffic) {
        when (traffic) {
            TerminalTrafficFilter.BOTH -> lines
            TerminalTrafficFilter.SENT -> lines.filter { it.direction == LinkTrafficDirection.SENT }
            TerminalTrafficFilter.RECEIVED ->
                lines.filter { it.direction == LinkTrafficDirection.RECEIVED }
        }
    }
    val listState = rememberLazyListState()
    // Oldest line is index 0, at the top. New lines queue toward the bottom.
    var followLive by remember { mutableStateOf(true) }
    var pinnedNewestId by remember { mutableLongStateOf(0L) }
    val userDragging by listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(userDragging, listState) {
        if (userDragging) {
            followLive = false
            return@LaunchedEffect
        }
        snapshotFlow {
            !listState.isScrollInProgress && !listState.canScrollForward
        }.collect { settledAtBottom ->
            if (settledAtBottom) followLive = true
        }
    }
    LaunchedEffect(traffic) {
        followLive = true
    }
    SideEffect {
        val newestId = visible.lastOrNull()?.id ?: return@SideEffect
        if (!followLive || listState.isScrollInProgress) {
            pinnedNewestId = 0L
            return@SideEffect
        }
        // Stick to the bottom only while the user is already there. Scrolling
        // up leaves the viewport on the lines they are reading.
        if (newestId != pinnedNewestId) {
            listState.requestScrollToItem(visible.lastIndex)
            pinnedNewestId = newestId
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.control_panel_terminal_header),
            color = TerminalMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        TerminalSettingsBar(
            traffic = traffic,
            onTrafficChange = onTrafficChange,
            textSize = textSize,
            onTextSizeChange = onTextSizeChange,
            palette = palette,
            onPaletteChange = onPaletteChange,
        )
        if (lines.isEmpty()) {
            Text(
                text = stringResource(R.string.control_panel_terminal_idle),
                color = TerminalMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else if (visible.isEmpty()) {
            Text(
                text = stringResource(
                    if (traffic == TerminalTrafficFilter.SENT) {
                        R.string.control_panel_terminal_empty_tx
                    } else {
                        R.string.control_panel_terminal_empty_rx
                    },
                ),
                color = TerminalMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = textSize.bodySp,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    items(visible, key = { it.id }) { entry ->
                        TerminalLine(entry, palette, textSize)
                    }
                }
                TerminalScrollbar(
                    listState = listState,
                    thumbColor = palette.sent,
                    onScrub = { followLive = false },
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .width(14.dp)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun TerminalSettingsBar(
    traffic: TerminalTrafficFilter,
    onTrafficChange: (TerminalTrafficFilter) -> Unit,
    textSize: TerminalTextSize,
    onTextSizeChange: (TerminalTextSize) -> Unit,
    palette: TerminalPalette,
    onPaletteChange: (TerminalPalette) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TerminalOptionButton(
            label = stringResource(R.string.control_panel_terminal_filter_both),
            description = stringResource(R.string.control_panel_terminal_filter_both_cd),
            selected = traffic == TerminalTrafficFilter.BOTH,
            accent = palette.sent,
            onClick = { onTrafficChange(TerminalTrafficFilter.BOTH) },
        )
        TerminalOptionButton(
            label = stringResource(R.string.control_panel_terminal_filter_tx),
            description = stringResource(R.string.control_panel_terminal_filter_tx_cd),
            selected = traffic == TerminalTrafficFilter.SENT,
            accent = palette.sent,
            onClick = { onTrafficChange(TerminalTrafficFilter.SENT) },
        )
        TerminalOptionButton(
            label = stringResource(R.string.control_panel_terminal_filter_rx),
            description = stringResource(R.string.control_panel_terminal_filter_rx_cd),
            selected = traffic == TerminalTrafficFilter.RECEIVED,
            accent = palette.received,
            onClick = { onTrafficChange(TerminalTrafficFilter.RECEIVED) },
        )
        TerminalTextSize.entries.forEach { size ->
            val description = stringResource(
                when (size) {
                    TerminalTextSize.SMALL -> R.string.control_panel_terminal_text_small_cd
                    TerminalTextSize.MEDIUM -> R.string.control_panel_terminal_text_medium_cd
                    TerminalTextSize.LARGE -> R.string.control_panel_terminal_text_large_cd
                },
            )
            TerminalOptionButton(
                label = "A",
                description = description,
                selected = textSize == size,
                accent = palette.sent,
                labelSize = size.bodySp,
                onClick = { onTextSizeChange(size) },
            )
        }
        TerminalPalette.entries.forEach { option ->
            val name = stringResource(
                when (option) {
                    TerminalPalette.CLASSIC -> R.string.control_panel_terminal_palette_classic
                    TerminalPalette.WARM -> R.string.control_panel_terminal_palette_warm
                    TerminalPalette.ICE -> R.string.control_panel_terminal_palette_ice
                    TerminalPalette.MONO -> R.string.control_panel_terminal_palette_mono
                },
            )
            TerminalOptionButton(
                label = name,
                description = stringResource(R.string.control_panel_terminal_palette_cd, name),
                selected = palette == option,
                accent = option.sent,
                onClick = { onPaletteChange(option) },
            )
        }
    }
}

@Composable
private fun TerminalOptionButton(
    label: String,
    description: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    labelSize: TextUnit = 10.sp,
) {
    val shape = RoundedCornerShape(6.dp)
    Text(
        text = label,
        color = if (selected) accent else TerminalMuted,
        fontFamily = FontFamily.Monospace,
        fontSize = labelSize,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .clip(shape)
            .border(1.dp, if (selected) accent else TerminalMuted.copy(alpha = 0.45f), shape)
            .background(if (selected) accent.copy(alpha = 0.18f) else Color(0xFF102018))
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun TerminalLine(
    entry: LinkTrafficEntry,
    palette: TerminalPalette,
    textSize: TerminalTextSize,
) {
    val sent = entry.direction == LinkTrafficDirection.SENT
    val color = if (sent) palette.sent else palette.received
    val alpha = remember(entry.id) { Animatable(1f) }
    LaunchedEffect(entry.repeatEpoch) {
        if (entry.repeatCount <= 0) return@LaunchedEffect
        alpha.snapTo(0.25f)
        alpha.animateTo(1f, tween(durationMillis = 280))
    }
    Column(modifier = Modifier.graphicsLayer { this.alpha = alpha.value }) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (sent) "TX" else "RX",
                color = color,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = textSize.bodySp,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = entry.text,
                color = color.copy(alpha = 0.92f),
                fontFamily = FontFamily.Monospace,
                fontSize = textSize.bodySp,
            )
        }
        if (entry.repeatCount > 0) {
            val again = if (entry.repeatCount == 1) {
                stringResource(
                    if (sent) {
                        R.string.control_panel_terminal_sent_again
                    } else {
                        R.string.control_panel_terminal_received_again
                    },
                )
            } else {
                stringResource(
                    if (sent) {
                        R.string.control_panel_terminal_sent_again_count
                    } else {
                        R.string.control_panel_terminal_received_again_count
                    },
                    entry.repeatCount,
                )
            }
            Text(
                text = again,
                color = color,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = textSize.againSp,
                modifier = Modifier.padding(start = 26.dp, bottom = 2.dp),
            )
        }
    }
}

@Composable
private fun TerminalScrollbar(
    listState: LazyListState,
    thumbColor: Color,
    onScrub: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val layoutInfo = listState.layoutInfo
    val total = layoutInfo.totalItemsCount
    val visible = layoutInfo.visibleItemsInfo.size
    val first = listState.firstVisibleItemIndex
    Canvas(
        modifier.pointerInput(listState) {
            fun scrubTo(y: Float) {
                val itemCount = listState.layoutInfo.totalItemsCount
                if (itemCount <= 1) return
                val visibleCount = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
                val maxStart = (itemCount - visibleCount).coerceAtLeast(0)
                val fraction = (y / size.height).coerceIn(0f, 1f)
                val index = (fraction * maxStart).toInt().coerceIn(0, maxStart)
                onScrub()
                scope.launch { listState.scrollToItem(index) }
            }
            detectDragGestures(
                onDragStart = { scrubTo(it.y) },
                onDrag = { change, _ -> scrubTo(change.position.y) },
            )
        },
    ) {
        val trackColor = Color(0xFF163028)
        drawRoundRect(
            color = trackColor,
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
        if (total <= 0) return@Canvas
        val scrollable = total > visible && visible > 0
        val thumbFraction = if (!scrollable) 1f else visible.toFloat() / total.toFloat()
        val thumbHeight = (size.height * thumbFraction).coerceIn(28.dp.toPx(), size.height)
        val travel = (size.height - thumbHeight).coerceAtLeast(0f)
        val maxStart = (total - visible).coerceAtLeast(1)
        val top = if (!scrollable) 0f else travel * first.toFloat() / maxStart.toFloat()
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(0f, top),
            size = Size(size.width, thumbHeight),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
    }
}
