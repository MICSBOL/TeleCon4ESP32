package com.micsbol.telecon4esp32.ui.solarsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.micsbol.telecon4esp32.ui.solarsystem.components.SolarSystemCard

private data class SolarGuideEntry(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val readRes: Int,
    @StringRes val writeRes: Int? = null,
    val monospaceBodyOnly: Boolean = false,
)

@Composable
fun SolarHelpScreen(
    navController: NavController,
) {
    SolarHelpContent(onBackClick = { navController.navigateUp() })
}

@Composable
private fun SolarHelpContent(
    onBackClick: () -> Unit,
) {
    val guideEntries = remember { solarGuideEntries() }
    var expandedTitleRes by remember { mutableIntStateOf(0) }
    val edgeInsets = rememberClampedSafeHudInsets(
        includeTop = true,
        includeBottom = true,
        includeHorizontal = true,
    )

    SolarBackground(solarPowerW = 480, maxSolarW = 1200) {
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
                SolarGlassIconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.solar_system_help_back),
                        tint = SolarGlass.AccentAmber,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.solar_system_help_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SolarGlass.TextOnBackgroundPrimary,
                )
            }
            Text(
                text = stringResource(R.string.solar_system_help_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = SolarGlass.TextOnBackgroundSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SolarSystemCard(elevated = false) {
                    Text(
                        text = stringResource(R.string.solar_system_help_intro_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SolarGlass.TextOnGlassPrimary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.solar_system_help_intro_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarGlass.TextOnGlassSecondary,
                    )
                }
                SolarSystemCard(elevated = false) {
                    guideEntries.forEachIndexed { index, entry ->
                        SolarHelpTopicRow(
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
                                color = SolarGlass.SkyMid.copy(alpha = 0.12f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SolarHelpTopicRow(
    entry: SolarGuideEntry,
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
                color = SolarGlass.TextOnGlassPrimary,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = SolarGlass.AccentAmber,
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
                if (entry.monospaceBodyOnly) {
                    Text(
                        text = stringResource(entry.readRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarGlass.TextOnGlassSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                } else {
                    Text(
                        text = stringResource(entry.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarGlass.TextOnGlassSecondary,
                    )
                    Text(
                        text = stringResource(R.string.solar_system_guide_read_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SolarGlass.AccentAmber,
                    )
                    Text(
                        text = stringResource(entry.readRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarGlass.TextOnGlassSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                    entry.writeRes?.let { writeRes ->
                        Text(
                            text = stringResource(R.string.solar_system_guide_write_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SolarGlass.AccentAmber,
                        )
                        Text(
                            text = stringResource(writeRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = SolarGlass.TextOnGlassSecondary,
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

private fun solarGuideEntries(): List<SolarGuideEntry> = listOf(
    SolarGuideEntry(
        titleRes = R.string.solar_system_guide_topbar_title,
        descriptionRes = R.string.solar_system_guide_topbar_description,
        readRes = R.string.solar_system_guide_topbar_read,
    ),
    SolarGuideEntry(
        titleRes = R.string.solar_system_guide_background_title,
        descriptionRes = R.string.solar_system_guide_background_description,
        readRes = R.string.solar_system_guide_background_read,
    ),
    SolarGuideEntry(
        titleRes = R.string.solar_system_guide_panel_card_title,
        descriptionRes = R.string.solar_system_guide_panel_card_description,
        readRes = R.string.solar_system_guide_panel_card_read,
    ),
    SolarGuideEntry(
        titleRes = R.string.solar_system_guide_consumption_title,
        descriptionRes = R.string.solar_system_guide_consumption_description,
        readRes = R.string.solar_system_guide_consumption_read,
    ),
    SolarGuideEntry(
        titleRes = R.string.solar_system_guide_battery_title,
        descriptionRes = R.string.solar_system_guide_battery_description,
        readRes = R.string.solar_system_guide_battery_read,
    ),
    SolarGuideEntry(
        titleRes = R.string.solar_system_guide_live_power_title,
        descriptionRes = R.string.solar_system_guide_live_power_description,
        readRes = R.string.solar_system_guide_live_power_read,
    ),
    SolarGuideEntry(
        titleRes = R.string.solar_system_guide_diy_title,
        descriptionRes = R.string.solar_system_guide_diy_description,
        readRes = R.string.solar_system_guide_diy_read,
        writeRes = R.string.solar_system_guide_diy_write,
    ),
    SolarGuideEntry(
        titleRes = R.string.solar_system_guide_pins_title,
        descriptionRes = R.string.solar_system_guide_pins_title,
        readRes = R.string.solar_system_guide_pins_body,
        monospaceBodyOnly = true,
    ),
)
