package com.micsbol.telecon4esp32.ui.watertank

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.ui.components.rememberClampedSafeHudInsets
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.watertank.components.WaterTankCard

private data class WaterTankGuideEntry(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val readRes: Int,
    @StringRes val writeRes: Int? = null,
    val isPinMap: Boolean = false,
)

@Composable
fun WaterTankHelpScreen(
    navController: NavController,
) {
    WaterTankHelpContent(onBackClick = { navController.navigateUp() })
}

@Composable
private fun WaterTankHelpContent(
    onBackClick: () -> Unit,
) {
    val guideEntries = remember { waterTankGuideEntries() }
    var expandedTitleRes by remember { mutableIntStateOf(0) }
    val edgeInsets = rememberClampedSafeHudInsets(
        includeTop = true,
        includeBottom = true,
        includeHorizontal = true,
    )

    WaterTankBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(edgeInsets)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WaterTankGlassIconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.water_tank_help_back),
                        tint = WaterTankGlass.AccentCyan,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.water_tank_help_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = WaterTankGlass.TextPrimary,
                )
            }
            Text(
                text = stringResource(R.string.water_tank_help_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = WaterTankGlass.TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WaterTankCard(elevated = false) {
                    Text(
                        text = stringResource(R.string.water_tank_help_intro_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WaterTankGlass.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.water_tank_help_intro_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = WaterTankGlass.TextSecondary,
                    )
                }
                WaterTankCard(elevated = false) {
                    guideEntries.forEachIndexed { index, entry ->
                        WaterTankHelpTopicRow(
                            entry = entry,
                            expanded = expandedTitleRes == entry.titleRes,
                            onToggle = {
                                expandedTitleRes = if (expandedTitleRes == entry.titleRes) {
                                    0
                                } else {
                                    entry.titleRes
                                }
                            },
                        )
                        if (index < guideEntries.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = WaterTankGlass.TextMuted.copy(alpha = 0.20f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterTankHelpTopicRow(
    entry: WaterTankGuideEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(entry.titleRes),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = WaterTankGlass.TextPrimary,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = WaterTankGlass.AccentCyan,
                modifier = Modifier.size(20.dp),
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (entry.isPinMap) {
                    Text(
                        text = stringResource(entry.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = WaterTankGlass.TextSecondary,
                    )
                    WaterTankPinMapContent()
                } else {
                    Text(
                        text = stringResource(entry.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = WaterTankGlass.TextSecondary,
                    )
                    Text(
                        text = stringResource(R.string.water_tank_guide_read_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WaterTankGlass.AccentCyan,
                    )
                    Text(
                        text = stringResource(entry.readRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = WaterTankGlass.TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                    entry.writeRes?.let { writeRes ->
                        Text(
                            text = stringResource(R.string.water_tank_guide_write_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = WaterTankGlass.AccentCyan,
                        )
                        Text(
                            text = stringResource(writeRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = WaterTankGlass.TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterTankPinMapContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        WaterTankPinMap.sections().forEach { section ->
            Text(
                text = stringResource(section.sectionTitleRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = WaterTankGlass.AccentCyan,
            )
            section.assignments.forEach { assignment ->
                Text(
                    text = stringResource(
                        R.string.water_tank_pin_line,
                        stringResource(assignment.elementLabelRes),
                        assignment.gpio,
                        stringResource(assignment.hardwareRes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = WaterTankGlass.TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        }
        Text(
            text = stringResource(R.string.water_tank_pin_footer),
            style = MaterialTheme.typography.bodySmall,
            color = WaterTankGlass.TextMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
}

private fun waterTankGuideEntries(): List<WaterTankGuideEntry> = listOf(
    WaterTankGuideEntry(
        titleRes = R.string.water_tank_guide_topbar_title,
        descriptionRes = R.string.water_tank_guide_topbar_description,
        readRes = R.string.water_tank_guide_topbar_read,
    ),
    WaterTankGuideEntry(
        titleRes = R.string.water_tank_guide_level_title,
        descriptionRes = R.string.water_tank_guide_level_description,
        readRes = R.string.water_tank_guide_level_read,
    ),
    WaterTankGuideEntry(
        titleRes = R.string.water_tank_guide_metrics_title,
        descriptionRes = R.string.water_tank_guide_metrics_description,
        readRes = R.string.water_tank_guide_metrics_read,
    ),
    WaterTankGuideEntry(
        titleRes = R.string.water_tank_guide_status_title,
        descriptionRes = R.string.water_tank_guide_status_description,
        readRes = R.string.water_tank_guide_status_read,
    ),
    WaterTankGuideEntry(
        titleRes = R.string.water_tank_guide_period_title,
        descriptionRes = R.string.water_tank_guide_period_description,
        readRes = R.string.water_tank_guide_period_read,
    ),
    WaterTankGuideEntry(
        titleRes = R.string.water_tank_guide_chart_title,
        descriptionRes = R.string.water_tank_guide_chart_description,
        readRes = R.string.water_tank_guide_chart_read,
    ),
    WaterTankGuideEntry(
        titleRes = R.string.water_tank_guide_footer_title,
        descriptionRes = R.string.water_tank_guide_footer_description,
        readRes = R.string.water_tank_guide_footer_read,
    ),
    WaterTankGuideEntry(
        titleRes = R.string.water_tank_guide_pump_title,
        descriptionRes = R.string.water_tank_guide_pump_description,
        readRes = R.string.water_tank_guide_pump_read,
        writeRes = R.string.water_tank_guide_pump_write,
    ),
    WaterTankGuideEntry(
        titleRes = R.string.water_tank_guide_pins_title,
        descriptionRes = R.string.water_tank_guide_pins_description,
        readRes = R.string.water_tank_guide_pins_title,
        isPinMap = true,
    ),
)
