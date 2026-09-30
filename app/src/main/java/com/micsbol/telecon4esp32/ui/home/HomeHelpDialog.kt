package com.micsbol.telecon4esp32.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogBody
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.glassSurface
import com.micsbol.telecon4esp32.ui.theme.Neo

private data class HelpTopic(
    @StringRes val problemRes: Int,
    @StringRes val solutionRes: Int,
)

private val helpTopics = listOf(
    HelpTopic(R.string.home_help_problem_first_setup, R.string.home_help_solution_first_setup),
    HelpTopic(R.string.home_help_problem_open_catalog, R.string.home_help_solution_open_catalog),
    HelpTopic(R.string.home_help_problem_bt_not_found, R.string.home_help_solution_bt_not_found),
    HelpTopic(R.string.home_help_problem_connect_fails, R.string.home_help_solution_connect_fails),
    HelpTopic(R.string.home_help_problem_not_connected, R.string.home_help_solution_not_connected),
    HelpTopic(R.string.home_help_problem_module_no_response, R.string.home_help_solution_module_no_response),
    HelpTopic(R.string.home_help_problem_pro_unlock, R.string.home_help_solution_pro_unlock),
)

@Composable
fun HomeHelpDialog(
    onDismissRequest: () -> Unit,
    onPlayTutorial: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val listMaxHeight = when {
        isLandscape -> (configuration.screenHeightDp * 0.48f).dp.coerceIn(160.dp, 280.dp)
        else -> (configuration.screenHeightDp * 0.52f).dp.coerceIn(280.dp, 440.dp)
    }

    NeoDialog(
        onDismissRequest = onDismissRequest,
        horizontalMargin = if (isLandscape) 48.dp else 16.dp,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = Neo.Accent,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .glassSurface(cornerRadius = 20.dp)
                        .padding(8.dp),
                )
                NeoDialogTitle(
                    text = stringResource(R.string.home_help_dialog_title),
                    modifier = Modifier.weight(1f),
                )
            }
        },
        subtitle = {
            NeoDialogBody(text = stringResource(R.string.home_help_dialog_subtitle))
        },
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = listMaxHeight),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(helpTopics, key = { it.problemRes }) { topic ->
                    HomeHelpTopicItem(topic = topic)
                }
            }
        },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeoPillButton(
                    text = stringResource(R.string.home_help_play_tutorial),
                    onClick = onPlayTutorial,
                    compact = true,
                    icon = Icons.Filled.PlayArrow,
                    modifier = Modifier.weight(1f),
                )
                NeoPillButton(
                    text = stringResource(R.string.codes_dialog_ok),
                    onClick = onDismissRequest,
                    compact = true,
                )
            }
        },
    )
}

@Composable
private fun HomeHelpTopicItem(topic: HelpTopic) {
    var expanded by remember(topic.problemRes) { mutableStateOf(false) }
    val interaction = remember(topic.problemRes) { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(
                cornerRadius = 16.dp,
                alpha = if (pressed || expanded) 0.55f else 0.42f,
                borderAlpha = if (expanded) 0.55f else 0.28f,
            )
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = { expanded = !expanded },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(topic.problemRes),
                modifier = Modifier.weight(1f),
                color = Neo.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (expanded) Neo.Accent else Neo.TextSecondary,
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(topic.solutionRes),
                    modifier = Modifier.fillMaxWidth(),
                    color = Neo.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}
