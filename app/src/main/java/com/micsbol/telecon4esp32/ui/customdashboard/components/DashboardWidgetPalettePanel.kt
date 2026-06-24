package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.domain.model.DashboardWidgetType
import com.micsbol.telecon4esp32.ui.customdashboard.descriptionRes
import com.micsbol.telecon4esp32.ui.customdashboard.titleRes

private data class PaletteCategory(
    val titleRes: Int,
    val types: List<DashboardWidgetType>,
)

private val paletteCategories = listOf(
    PaletteCategory(
        titleRes = R.string.custom_dashboard_palette_category_input,
        types = listOf(DashboardWidgetType.JOYSTICK, DashboardWidgetType.SLIDER),
    ),
    PaletteCategory(
        titleRes = R.string.custom_dashboard_palette_category_output,
        types = listOf(
            DashboardWidgetType.RELAY,
            DashboardWidgetType.OUTPUT_KNOB,
            DashboardWidgetType.PUSH_BUTTON,
            DashboardWidgetType.LED_INDICATOR,
        ),
    ),
    PaletteCategory(
        titleRes = R.string.custom_dashboard_palette_category_monitor,
        types = listOf(DashboardWidgetType.WAVEFORM, DashboardWidgetType.VALUE_READOUT),
    ),
)

@Composable
fun DashboardWidgetPalettePanel(
    onPaletteDragStart: (DashboardWidgetType) -> Unit,
    onPaletteDragMove: (Offset) -> Unit,
    onPaletteDragEnd: (DashboardWidgetType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(220.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .verticalScroll(scrollState),
    ) {
        Text(
            text = stringResource(R.string.custom_dashboard_palette_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.custom_dashboard_palette_subtitle),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )

        paletteCategories.forEachIndexed { index, category ->
            if (index > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            Text(
                text = stringResource(category.titleRes),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            category.types.forEach { type ->
                DashboardPaletteItemRow(
                    type = type,
                    onPaletteDragStart = onPaletteDragStart,
                    onPaletteDragMove = onPaletteDragMove,
                    onPaletteDragEnd = onPaletteDragEnd,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DashboardPaletteItemRow(
    type: DashboardWidgetType,
    onPaletteDragStart: (DashboardWidgetType) -> Unit,
    onPaletteDragMove: (Offset) -> Unit,
    onPaletteDragEnd: (DashboardWidgetType) -> Unit,
) {
    var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentItemCoordinates by rememberUpdatedState(itemCoordinates)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { itemCoordinates = it }
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .pointerInput(type) {
                detectDragGestures(
                    onDragStart = {
                        onPaletteDragStart(type)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val coords = currentItemCoordinates
                        if (coords != null) {
                            onPaletteDragMove(coords.localToWindow(change.position))
                        }
                    },
                    onDragEnd = {
                        onPaletteDragEnd(type)
                    },
                    onDragCancel = {
                        onPaletteDragEnd(type)
                    },
                )
            }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = type.paletteIcon(),
            contentDescription = null,
            tint = brandPrimary(),
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(type.titleRes()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(type.descriptionRes()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun DashboardWidgetType.paletteIcon(): ImageVector = when (this) {
    DashboardWidgetType.JOYSTICK -> Icons.Default.SportsEsports
    DashboardWidgetType.SLIDER -> Icons.Default.LinearScale
    DashboardWidgetType.WAVEFORM -> Icons.Default.ShowChart
    DashboardWidgetType.VALUE_READOUT -> Icons.Default.Numbers
    DashboardWidgetType.RELAY -> Icons.Default.ToggleOn
    DashboardWidgetType.OUTPUT_KNOB -> Icons.Default.Battery5Bar
    DashboardWidgetType.PUSH_BUTTON -> Icons.Default.TouchApp
    DashboardWidgetType.LED_INDICATOR -> Icons.Default.Lightbulb
}
