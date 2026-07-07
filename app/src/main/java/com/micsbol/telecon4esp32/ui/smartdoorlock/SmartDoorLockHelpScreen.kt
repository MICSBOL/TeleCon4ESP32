package com.micsbol.telecon4esp32.ui.smartdoorlock

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
import com.micsbol.telecon4esp32.ui.smartdoorlock.components.SmartDoorLockCard

private data class SmartDoorLockGuideEntry(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val readRes: Int,
    @StringRes val writeRes: Int? = null,
    val isPinMap: Boolean = false,
)

@Composable
fun SmartDoorLockHelpScreen(
    navController: NavController,
) {
    SmartDoorLockHelpContent(onBackClick = { navController.navigateUp() })
}

@Composable
private fun SmartDoorLockHelpContent(
    onBackClick: () -> Unit,
) {
    val guideEntries = remember { smartDoorLockGuideEntries() }
    var expandedTitleRes by remember { mutableIntStateOf(0) }
    val edgeInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
    )

    SmartDoorLockBackground {
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
                SmartDoorLockGlassIconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.smart_door_lock_help_back),
                        tint = SmartDoorLockGlass.AccentGreenBright,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.smart_door_lock_help_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SmartDoorLockGlass.TextPrimary,
                )
            }
            Text(
                text = stringResource(R.string.smart_door_lock_help_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = SmartDoorLockGlass.TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SmartDoorLockCard(elevated = false) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.smart_door_lock_help_intro_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SmartDoorLockGlass.TextPrimary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.smart_door_lock_help_intro_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = SmartDoorLockGlass.TextSecondary,
                        )
                    }
                }
                SmartDoorLockCard(elevated = false) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    guideEntries.forEachIndexed { index, entry ->
                        SmartDoorLockHelpTopicRow(
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
                                color = SmartDoorLockGlass.TextMuted.copy(alpha = 0.20f),
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartDoorLockHelpTopicRow(
    entry: SmartDoorLockGuideEntry,
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
                color = SmartDoorLockGlass.TextPrimary,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = SmartDoorLockGlass.AccentGreenBright,
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
                        color = SmartDoorLockGlass.TextSecondary,
                    )
                    SmartDoorLockPinMapContent()
                } else {
                    Text(
                        text = stringResource(entry.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartDoorLockGlass.TextSecondary,
                    )
                    Text(
                        text = stringResource(R.string.smart_door_lock_guide_read_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SmartDoorLockGlass.AccentGreenBright,
                    )
                    Text(
                        text = stringResource(entry.readRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartDoorLockGlass.TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                    entry.writeRes?.let { writeRes ->
                        Text(
                            text = stringResource(R.string.smart_door_lock_guide_write_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SmartDoorLockGlass.AccentGreenBright,
                        )
                        Text(
                            text = stringResource(writeRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = SmartDoorLockGlass.TextSecondary,
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
private fun SmartDoorLockPinMapContent() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SmartDoorLockPinMap.sections().forEach { section ->
            Text(
                text = stringResource(section.sectionTitleRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = SmartDoorLockGlass.AccentGreenBright,
            )
            section.assignments.forEach { assignment ->
                Text(
                    text = stringResource(
                        R.string.smart_door_lock_pin_line,
                        stringResource(assignment.elementLabelRes),
                        assignment.gpio,
                        stringResource(assignment.hardwareRes),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartDoorLockGlass.TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        }
        Text(
            text = stringResource(R.string.smart_door_lock_pin_footer),
            style = MaterialTheme.typography.bodySmall,
            color = SmartDoorLockGlass.TextMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
}

private fun smartDoorLockGuideEntries(): List<SmartDoorLockGuideEntry> = listOf(
    SmartDoorLockGuideEntry(
        titleRes = R.string.smart_door_lock_guide_topbar_title,
        descriptionRes = R.string.smart_door_lock_guide_topbar_description,
        readRes = R.string.smart_door_lock_guide_topbar_read,
    ),
    SmartDoorLockGuideEntry(
        titleRes = R.string.smart_door_lock_guide_status_card_title,
        descriptionRes = R.string.smart_door_lock_guide_status_card_description,
        readRes = R.string.smart_door_lock_guide_status_card_read,
    ),
    SmartDoorLockGuideEntry(
        titleRes = R.string.smart_door_lock_guide_swipe_title,
        descriptionRes = R.string.smart_door_lock_guide_swipe_description,
        readRes = R.string.smart_door_lock_guide_swipe_read,
        writeRes = R.string.smart_door_lock_guide_swipe_write,
    ),
    SmartDoorLockGuideEntry(
        titleRes = R.string.smart_door_lock_guide_quick_actions_title,
        descriptionRes = R.string.smart_door_lock_guide_quick_actions_description,
        readRes = R.string.smart_door_lock_guide_quick_actions_read,
        writeRes = R.string.smart_door_lock_guide_quick_actions_write,
    ),
    SmartDoorLockGuideEntry(
        titleRes = R.string.smart_door_lock_guide_media_title,
        descriptionRes = R.string.smart_door_lock_guide_media_description,
        readRes = R.string.smart_door_lock_guide_media_read,
        writeRes = R.string.smart_door_lock_guide_media_write,
    ),
    SmartDoorLockGuideEntry(
        titleRes = R.string.smart_door_lock_guide_relay_title,
        descriptionRes = R.string.smart_door_lock_guide_relay_description,
        readRes = R.string.smart_door_lock_guide_relay_read,
        writeRes = R.string.smart_door_lock_guide_relay_write,
    ),
    SmartDoorLockGuideEntry(
        titleRes = R.string.smart_door_lock_guide_end_call_title,
        descriptionRes = R.string.smart_door_lock_guide_end_call_description,
        readRes = R.string.smart_door_lock_guide_end_call_read,
    ),
    SmartDoorLockGuideEntry(
        titleRes = R.string.smart_door_lock_guide_pins_title,
        descriptionRes = R.string.smart_door_lock_guide_pins_description,
        readRes = R.string.smart_door_lock_guide_pins_title,
        isPinMap = true,
    ),
)
