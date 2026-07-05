package com.micsbol.telecon4esp32.ui.greenhouse

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
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseCard
import com.micsbol.telecon4esp32.ui.navigation.Screen

private data class GreenhouseGuideEntry(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val readRes: Int,
    @StringRes val writeRes: Int? = null,
    val monospaceBodyOnly: Boolean = false,
)

@Composable
fun GreenhouseHelpScreen(
    navController: NavController,
) {
    val onBackClick = {
        GreenhouseEmulatorNavigation.backToGreenhouse(navController, Screen.GreenhouseHelp.route)
    }

    if (GreenhouseEmulatorSupport.isEmulator()) {
        GreenhouseHelpEmulatorContent(onBackClick = onBackClick)
        return
    }

    GreenhouseHelpFullContent(onBackClick = onBackClick)
}

@Composable
private fun GreenhouseHelpEmulatorContent(
    onBackClick: () -> Unit,
) {
    val guideEntries = remember { greenhouseGuideEntries() }
    var expandedTitleRes by remember { mutableIntStateOf(0) }
    val edgeInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
    )
    val scrollState = rememberScrollState()

    GreenhouseBackground(showPhoto = false) {
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
                GreenhouseGlassIconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.greenhouse_settings_back),
                        tint = GreenhouseGlass.AccentGreen,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.greenhouse_help_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenhouseGlass.TextPrimary,
                )
            }
            Text(
                text = stringResource(R.string.greenhouse_help_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = GreenhouseGlass.TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.greenhouse_settings_guide_section_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = GreenhouseGlass.TextSecondary,
                )
                guideEntries.forEachIndexed { index, entry ->
                    GreenhouseHelpTopicRow(
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
                        HorizontalDivider(color = GreenhouseGlass.ForestMid.copy(alpha = 0.12f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GreenhouseHelpFullContent(
    onBackClick: () -> Unit,
) {
    val guideEntries = remember { greenhouseGuideEntries() }
    var expandedTitleRes by remember { mutableIntStateOf(0) }
    val edgeInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
    )
    val bottomInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)

    GreenhouseBackground(showPhoto = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(edgeInsets)
                .windowInsetsPadding(bottomInsets),
        ) {
            GreenhouseHelpFullTopBar(onBackClick = onBackClick)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GreenhouseCard(elevated = false) {
                    Text(
                        text = stringResource(R.string.greenhouse_settings_guide_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenhouseGlass.TextOnGlassPrimary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.greenhouse_settings_guide_section_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenhouseGlass.TextOnGlassSecondary,
                    )
                }
                GreenhouseCard(elevated = false) {
                    guideEntries.forEachIndexed { index, entry ->
                        GreenhouseHelpTopicRow(
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
                                color = GreenhouseGlass.ForestMid.copy(alpha = 0.12f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GreenhouseHelpFullTopBar(
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GreenhouseGlassIconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.greenhouse_settings_back),
                    tint = GreenhouseGlass.AccentGreen,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = stringResource(R.string.greenhouse_help_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GreenhouseGlass.TextOnGlassPrimary,
            )
        }
        Text(
            text = stringResource(R.string.greenhouse_help_subtitle),
            modifier = Modifier.padding(start = 54.dp),
            style = MaterialTheme.typography.bodySmall,
            color = GreenhouseGlass.TextOnGlassSecondary,
        )
    }
}

@Composable
private fun GreenhouseHelpTopicRow(
    entry: GreenhouseGuideEntry,
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
                color = GreenhouseGlass.TextOnGlassPrimary,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = GreenhouseGlass.AccentGreen,
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
                        color = GreenhouseGlass.TextOnGlassSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                } else {
                    Text(
                        text = stringResource(entry.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenhouseGlass.TextOnGlassSecondary,
                    )
                    Text(
                        text = stringResource(R.string.greenhouse_guide_read_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenhouseGlass.AccentGreen,
                    )
                    Text(
                        text = stringResource(entry.readRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenhouseGlass.TextOnGlassSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                    entry.writeRes?.let { writeRes ->
                        Text(
                            text = stringResource(R.string.greenhouse_guide_write_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenhouseGlass.AccentGreen,
                        )
                        Text(
                            text = stringResource(writeRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = GreenhouseGlass.TextOnGlassSecondary,
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

private fun greenhouseGuideEntries(): List<GreenhouseGuideEntry> = listOf(
    GreenhouseGuideEntry(
        titleRes = R.string.greenhouse_guide_topbar_title,
        descriptionRes = R.string.greenhouse_guide_topbar_description,
        readRes = R.string.greenhouse_guide_topbar_read,
    ),
    GreenhouseGuideEntry(
        titleRes = R.string.greenhouse_guide_weather_title,
        descriptionRes = R.string.greenhouse_guide_weather_description,
        readRes = R.string.greenhouse_guide_weather_read,
    ),
    GreenhouseGuideEntry(
        titleRes = R.string.greenhouse_guide_soil_title,
        descriptionRes = R.string.greenhouse_guide_soil_description,
        readRes = R.string.greenhouse_guide_soil_read,
        writeRes = R.string.greenhouse_guide_soil_write,
    ),
    GreenhouseGuideEntry(
        titleRes = R.string.greenhouse_guide_light_title,
        descriptionRes = R.string.greenhouse_guide_light_description,
        readRes = R.string.greenhouse_guide_light_read,
        writeRes = R.string.greenhouse_guide_light_write,
    ),
    GreenhouseGuideEntry(
        titleRes = R.string.greenhouse_guide_delta_title,
        descriptionRes = R.string.greenhouse_guide_delta_description,
        readRes = R.string.greenhouse_guide_delta_read,
        writeRes = R.string.greenhouse_guide_delta_write,
    ),
    GreenhouseGuideEntry(
        titleRes = R.string.greenhouse_guide_tank_title,
        descriptionRes = R.string.greenhouse_guide_tank_description,
        readRes = R.string.greenhouse_guide_tank_read,
        writeRes = R.string.greenhouse_guide_tank_write,
    ),
    GreenhouseGuideEntry(
        titleRes = R.string.greenhouse_guide_chart_title,
        descriptionRes = R.string.greenhouse_guide_chart_description,
        readRes = R.string.greenhouse_guide_chart_read,
    ),
    GreenhouseGuideEntry(
        titleRes = R.string.greenhouse_guide_controls_title,
        descriptionRes = R.string.greenhouse_guide_controls_description,
        readRes = R.string.greenhouse_guide_controls_read,
        writeRes = R.string.greenhouse_guide_controls_write,
    ),
    GreenhouseGuideEntry(
        titleRes = R.string.greenhouse_guide_auto_title,
        descriptionRes = R.string.greenhouse_guide_auto_description,
        readRes = R.string.greenhouse_guide_auto_read,
        writeRes = R.string.greenhouse_guide_auto_write,
    ),
    GreenhouseGuideEntry(
        titleRes = R.string.greenhouse_settings_pins_section_title,
        descriptionRes = R.string.greenhouse_settings_pins_section_title,
        readRes = R.string.greenhouse_settings_pins_body,
        monospaceBodyOnly = true,
    ),
)
