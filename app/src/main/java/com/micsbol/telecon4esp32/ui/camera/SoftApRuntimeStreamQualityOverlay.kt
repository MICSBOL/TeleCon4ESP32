package com.micsbol.telecon4esp32.ui.camera

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.camera.SoftApHudProcessingRate
import com.micsbol.telecon4esp32.domain.camera.SoftApPerformancePreset
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.brandPrimary

enum class StreamQualityLauncherStyle {
    /** Compact chip for camera panes (Control Panel center). */
    CHIP,

    /** Icon-only for crowded HUD top bars (RC Vehicle Pro). */
    ICON_BUTTON,
}

/**
 * Runtime SoftAP stream-quality launcher + modal dialog.
 * Persists via the caller; applies `/camconfig` when the host ViewModel is armed.
 */
@Composable
fun SoftApRuntimeStreamQualityControl(
    selectedPreset: SoftApPerformancePreset,
    selectedHudRate: SoftApHudProcessingRate,
    onPresetSelected: (SoftApPerformancePreset) -> Unit,
    onHudRateSelected: (SoftApHudProcessingRate) -> Unit,
    modifier: Modifier = Modifier,
    launcherStyle: StreamQualityLauncherStyle = StreamQualityLauncherStyle.CHIP,
) {
    var showDialog by remember { mutableStateOf(false) }
    val openLabel = stringResource(R.string.softap_stream_quality_open)

    Box(modifier = modifier) {
        when (launcherStyle) {
            StreamQualityLauncherStyle.CHIP -> {
                SoftApStreamQualityChipLauncher(
                    selectedPreset = selectedPreset,
                    contentDescription = openLabel,
                    onClick = { showDialog = true },
                )
            }
            StreamQualityLauncherStyle.ICON_BUTTON -> {
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Hd,
                        contentDescription = openLabel,
                        tint = brandPrimary(),
                    )
                }
            }
        }
    }

    if (showDialog) {
        SoftApStreamQualityDialog(
            selectedPreset = selectedPreset,
            selectedHudRate = selectedHudRate,
            onPresetSelected = onPresetSelected,
            onHudRateSelected = onHudRateSelected,
            onDismiss = { showDialog = false },
        )
    }
}

/** @see SoftApRuntimeStreamQualityControl */
@Composable
fun SoftApRuntimeStreamQualityOverlay(
    selectedPreset: SoftApPerformancePreset,
    selectedHudRate: SoftApHudProcessingRate,
    onPresetSelected: (SoftApPerformancePreset) -> Unit,
    onHudRateSelected: (SoftApHudProcessingRate) -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftApRuntimeStreamQualityControl(
        selectedPreset = selectedPreset,
        selectedHudRate = selectedHudRate,
        onPresetSelected = onPresetSelected,
        onHudRateSelected = onHudRateSelected,
        modifier = modifier,
        launcherStyle = StreamQualityLauncherStyle.CHIP,
    )
}

@Composable
private fun SoftApStreamQualityChipLauncher(
    selectedPreset: SoftApPerformancePreset,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = brandPrimary()
    val panelShape = RoundedCornerShape(12.dp)
    val panelBackground = Color(0xE6121824)
    val panelBorder = accent.copy(alpha = 0.35f)

    Box(
        modifier = modifier
            .padding(8.dp)
            .clip(panelShape)
            .background(panelBackground)
            .border(1.dp, panelBorder, panelShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Hd,
                contentDescription = contentDescription,
                tint = accent,
            )
            Text(
                text = stringResource(selectedPreset.titleRes()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun SoftApStreamQualityDialog(
    selectedPreset: SoftApPerformancePreset,
    selectedHudRate: SoftApHudProcessingRate,
    onPresetSelected: (SoftApPerformancePreset) -> Unit,
    onHudRateSelected: (SoftApHudProcessingRate) -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val horizontalMargin = if (isLandscape) 48.dp else 16.dp
    val maxDialogWidth = if (isLandscape) 520.dp else 420.dp
    val accent = brandPrimary()
    val dialogSurface = Color(0xFF121A26)
    val bodyText = Color(0xFFD8E2EC)
    val sectionLabel = Color(0xFFF2F7FC)
    val chipUnselectedBg = Color(0xFF1E2A3A)
    val chipUnselectedLabel = Color(0xFFF0F5FA)
    val chipUnselectedBorder = accent.copy(alpha = 0.72f)
    val chipSelectedBg = accent.copy(alpha = 0.95f)
    val chipSelectedLabel = Color(0xFF061018)
    val chipSelectedBorder = accent

    NeoDialog(
        onDismissRequest = onDismiss,
        horizontalMargin = horizontalMargin,
        surfaceColor = dialogSurface,
        surfaceAlpha = 1f,
        scrimAlpha = 0.68f,
        wrapContentHeight = true,
        modifier = Modifier.widthIn(max = maxDialogWidth),
        title = {
            NeoDialogTitle(text = stringResource(R.string.softap_stream_quality_panel_title))
        },
        subtitle = {
            Text(
                text = stringResource(R.string.softap_stream_quality_dialog_body),
                modifier = Modifier.fillMaxWidth(),
                color = bodyText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        },
        content = {
            Text(
                text = stringResource(R.string.softap_stream_quality_preset_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = sectionLabel,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SoftApPerformancePreset.entries.forEach { preset ->
                    StreamQualityFilterChip(
                        label = stringResource(preset.titleRes()),
                        selected = preset == selectedPreset,
                        unselectedContainerColor = chipUnselectedBg,
                        unselectedLabelColor = chipUnselectedLabel,
                        unselectedBorderColor = chipUnselectedBorder,
                        selectedContainerColor = chipSelectedBg,
                        selectedLabelColor = chipSelectedLabel,
                        selectedBorderColor = chipSelectedBorder,
                        onClick = {
                            if (preset != selectedPreset) {
                                onPresetSelected(preset)
                            }
                        },
                    )
                }
            }
            Text(
                text = stringResource(R.string.softap_stream_quality_hud_rate_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = sectionLabel,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HudRateChipRow(
                    rates = listOf(
                        SoftApHudProcessingRate.AUTO,
                        SoftApHudProcessingRate.FPS_5,
                        SoftApHudProcessingRate.FPS_8,
                        SoftApHudProcessingRate.FPS_10,
                    ),
                    selectedHudRate = selectedHudRate,
                    chipUnselectedBg = chipUnselectedBg,
                    chipUnselectedLabel = chipUnselectedLabel,
                    chipUnselectedBorder = chipUnselectedBorder,
                    chipSelectedBg = chipSelectedBg,
                    chipSelectedLabel = chipSelectedLabel,
                    chipSelectedBorder = chipSelectedBorder,
                    onHudRateSelected = onHudRateSelected,
                )
                HudRateChipRow(
                    rates = listOf(
                        SoftApHudProcessingRate.FPS_12,
                        SoftApHudProcessingRate.FPS_15,
                        SoftApHudProcessingRate.UNCAPPED,
                    ),
                    selectedHudRate = selectedHudRate,
                    chipUnselectedBg = chipUnselectedBg,
                    chipUnselectedLabel = chipUnselectedLabel,
                    chipUnselectedBorder = chipUnselectedBorder,
                    chipSelectedBg = chipSelectedBg,
                    chipSelectedLabel = chipSelectedLabel,
                    chipSelectedBorder = chipSelectedBorder,
                    onHudRateSelected = onHudRateSelected,
                )
            }
        },
        actions = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = accent.copy(alpha = 0.28f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(R.string.softap_stream_quality_done),
                            color = accent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun HudRateChipRow(
    rates: List<SoftApHudProcessingRate>,
    selectedHudRate: SoftApHudProcessingRate,
    chipUnselectedBg: Color,
    chipUnselectedLabel: Color,
    chipUnselectedBorder: Color,
    chipSelectedBg: Color,
    chipSelectedLabel: Color,
    chipSelectedBorder: Color,
    onHudRateSelected: (SoftApHudProcessingRate) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rates.forEach { rate ->
            StreamQualityFilterChip(
                label = stringResource(rate.chipLabelRes()),
                selected = rate == selectedHudRate,
                unselectedContainerColor = chipUnselectedBg,
                unselectedLabelColor = chipUnselectedLabel,
                unselectedBorderColor = chipUnselectedBorder,
                selectedContainerColor = chipSelectedBg,
                selectedLabelColor = chipSelectedLabel,
                selectedBorderColor = chipSelectedBorder,
                onClick = {
                    if (rate != selectedHudRate) {
                        onHudRateSelected(rate)
                    }
                },
            )
        }
    }
}

@Composable
private fun StreamQualityFilterChip(
    label: String,
    selected: Boolean,
    unselectedContainerColor: Color,
    unselectedLabelColor: Color,
    unselectedBorderColor: Color,
    selectedContainerColor: Color,
    selectedLabelColor: Color,
    selectedBorderColor: Color,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) selectedLabelColor else unselectedLabelColor,
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedContainerColor,
            selectedLabelColor = selectedLabelColor,
            containerColor = unselectedContainerColor,
            labelColor = unselectedLabelColor,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = unselectedBorderColor,
            selectedBorderColor = selectedBorderColor,
            borderWidth = if (selected) 1.5.dp else 1.dp,
            selectedBorderWidth = 1.5.dp,
        ),
    )
}

private fun SoftApPerformancePreset.titleRes(): Int = when (this) {
    SoftApPerformancePreset.SMOOTH -> R.string.rc_vehicle_softap_perf_smooth
    SoftApPerformancePreset.BALANCED -> R.string.rc_vehicle_softap_perf_balanced
    SoftApPerformancePreset.HIGH_QUALITY -> R.string.rc_vehicle_softap_perf_high_quality
}

private fun SoftApHudProcessingRate.chipLabelRes(): Int = when (this) {
    SoftApHudProcessingRate.AUTO -> R.string.softap_stream_quality_hud_chip_auto
    SoftApHudProcessingRate.FPS_5 -> R.string.softap_stream_quality_hud_chip_5
    SoftApHudProcessingRate.FPS_8 -> R.string.softap_stream_quality_hud_chip_8
    SoftApHudProcessingRate.FPS_10 -> R.string.softap_stream_quality_hud_chip_10
    SoftApHudProcessingRate.FPS_12 -> R.string.softap_stream_quality_hud_chip_12
    SoftApHudProcessingRate.FPS_15 -> R.string.softap_stream_quality_hud_chip_15
    SoftApHudProcessingRate.UNCAPPED -> R.string.softap_stream_quality_hud_chip_unlimited
}
