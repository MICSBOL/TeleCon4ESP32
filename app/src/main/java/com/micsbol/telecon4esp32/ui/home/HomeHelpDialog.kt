package com.micsbol.telecon4esp32.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.EmitterCardShape
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.components.mutedTextColor

private data class HelpTopic(
    @StringRes val problemRes: Int,
    @StringRes val solutionRes: Int
)

private val helpTopics = listOf(
    HelpTopic(R.string.home_help_problem_bt_not_found, R.string.home_help_solution_bt_not_found),
    HelpTopic(R.string.home_help_problem_connect_fails, R.string.home_help_solution_connect_fails),
    HelpTopic(R.string.home_help_problem_not_connected, R.string.home_help_solution_not_connected),
    HelpTopic(R.string.home_help_problem_rc_no_response, R.string.home_help_solution_rc_no_response),
    HelpTopic(R.string.home_help_problem_telemetry_stale, R.string.home_help_solution_telemetry_stale),
    HelpTopic(R.string.home_help_problem_first_setup, R.string.home_help_solution_first_setup),
)

@Composable
fun HomeHelpDialog(onDismissRequest: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = EmitterCardShape,
            color = colorScheme.surfaceContainerHigh,
            tonalElevation = if (isDark) 4.dp else 2.dp,
            shadowElevation = if (isDark) 18.dp else 8.dp,
            border = BorderStroke(
                width = 1.dp,
                color = colorScheme.primary.copy(alpha = if (isDark) 0.52f else 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_help_dialog_title),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.home_help_dialog_subtitle),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedTextColor()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(helpTopics, key = { it.problemRes }) { topic ->
                        HomeHelpTopicItem(topic = topic)
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.12f)
                )
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = stringResource(R.string.codes_dialog_ok),
                        color = brandPrimary()
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHelpTopicItem(topic: HelpTopic) {
    var expanded by remember(topic.problemRes) { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = EmitterCardShape,
        color = colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(topic.problemRes),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = brandPrimary()
                )
            }
            if (expanded) {
                Text(
                    text = stringResource(topic.solutionRes),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
