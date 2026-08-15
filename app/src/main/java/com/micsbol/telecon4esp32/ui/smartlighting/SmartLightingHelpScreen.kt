package com.micsbol.telecon4esp32.ui.smartlighting

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
import com.micsbol.telecon4esp32.ui.smartlighting.components.SmartLightingCard

private data class SmartLightingGuideEntry(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val readRes: Int,
    @StringRes val writeRes: Int? = null,
    val isPinMap: Boolean = false,
)

@Composable
fun SmartLightingHelpScreen(
    navController: NavController,
) {
    SmartLightingHelpContent(onBackClick = { navController.navigateUp() })
}

@Composable
private fun SmartLightingHelpContent(
    onBackClick: () -> Unit,
) {
    val guideEntries = remember { smartLightingGuideEntries() }
    var expandedTitleRes by remember { mutableIntStateOf(0) }
    val edgeInsets = rememberClampedSafeHudInsets(
        includeTop = true,
        includeBottom = true,
        includeHorizontal = true,
    )

    SmartLightingBackground {
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
                SmartLightingGlassIconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.smart_lighting_help_back),
                        tint = SmartLightingGlass.AccentCyan,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.smart_lighting_help_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmartLightingGlass.TextPrimary,
                )
            }
            Text(
                text = stringResource(R.string.smart_lighting_help_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = SmartLightingGlass.TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SmartLightingCard(elevated = false) {
                    Text(
                        text = stringResource(R.string.smart_lighting_help_intro_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SmartLightingGlass.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.smart_lighting_help_intro_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartLightingGlass.TextSecondary,
                    )
                }
                SmartLightingCard(elevated = false) {
                    guideEntries.forEachIndexed { index, entry ->
                        SmartLightingHelpTopicRow(
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
                                color = SmartLightingGlass.TextMuted.copy(alpha = 0.20f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartLightingHelpTopicRow(
    entry: SmartLightingGuideEntry,
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
                color = SmartLightingGlass.TextPrimary,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = SmartLightingGlass.AccentCyan,
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
                        color = SmartLightingGlass.TextSecondary,
                    )
                    SmartLightingPinMapContent()
                } else {
                    Text(
                        text = stringResource(entry.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartLightingGlass.TextSecondary,
                    )
                    Text(
                        text = stringResource(R.string.smart_lighting_guide_read_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SmartLightingGlass.AccentCyan,
                    )
                    Text(
                        text = stringResource(entry.readRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartLightingGlass.TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                    entry.writeRes?.let { writeRes ->
                        Text(
                            text = stringResource(R.string.smart_lighting_guide_write_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SmartLightingGlass.AccentCyan,
                        )
                        Text(
                            text = stringResource(writeRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = SmartLightingGlass.TextSecondary,
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
private fun SmartLightingPinMapContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SmartLightingPinMap.sections().forEach { section ->
            Text(
                text = stringResource(section.sectionTitleRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = SmartLightingGlass.AccentCyan,
            )
            section.assignments.forEach { assignment ->
                Text(
                    text = stringResource(
                        R.string.smart_lighting_pin_line,
                        stringResource(assignment.elementLabelRes),
                        assignment.gpio,
                        stringResource(assignment.hardwareRes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartLightingGlass.TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        }
        Text(
            text = stringResource(R.string.smart_lighting_pin_footer),
            style = MaterialTheme.typography.bodySmall,
            color = SmartLightingGlass.TextMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
}

private fun smartLightingGuideEntries(): List<SmartLightingGuideEntry> = listOf(
    SmartLightingGuideEntry(
        titleRes = R.string.smart_lighting_guide_topbar_title,
        descriptionRes = R.string.smart_lighting_guide_topbar_description,
        readRes = R.string.smart_lighting_guide_topbar_read,
    ),
    SmartLightingGuideEntry(
        titleRes = R.string.smart_lighting_guide_hero_title,
        descriptionRes = R.string.smart_lighting_guide_hero_description,
        readRes = R.string.smart_lighting_guide_hero_read,
    ),
    SmartLightingGuideEntry(
        titleRes = R.string.smart_lighting_guide_all_lights_title,
        descriptionRes = R.string.smart_lighting_guide_all_lights_description,
        readRes = R.string.smart_lighting_guide_all_lights_read,
        writeRes = R.string.smart_lighting_guide_all_lights_write,
    ),
    SmartLightingGuideEntry(
        titleRes = R.string.smart_lighting_guide_devices_title,
        descriptionRes = R.string.smart_lighting_guide_devices_description,
        readRes = R.string.smart_lighting_guide_devices_read,
        writeRes = R.string.smart_lighting_guide_devices_write,
    ),
    SmartLightingGuideEntry(
        titleRes = R.string.smart_lighting_guide_add_device_title,
        descriptionRes = R.string.smart_lighting_guide_add_device_description,
        readRes = R.string.smart_lighting_guide_add_device_read,
        writeRes = R.string.smart_lighting_guide_add_device_write,
    ),
    SmartLightingGuideEntry(
        titleRes = R.string.smart_lighting_guide_settings_title,
        descriptionRes = R.string.smart_lighting_guide_settings_description,
        readRes = R.string.smart_lighting_guide_settings_read,
        writeRes = R.string.smart_lighting_guide_settings_write,
    ),
    SmartLightingGuideEntry(
        titleRes = R.string.smart_lighting_guide_pins_title,
        descriptionRes = R.string.smart_lighting_guide_pins_description,
        readRes = R.string.smart_lighting_guide_pins_title,
        isPinMap = true,
    ),
)
