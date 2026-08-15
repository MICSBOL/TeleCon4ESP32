package com.micsbol.telecon4esp32.ui.smarthome.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.safeHudPadding
import com.micsbol.telecon4esp32.ui.smarthome.SmartHomeGlass
import com.micsbol.telecon4esp32.ui.smarthome.SmartHomeGlassIconButton

@Composable
fun SmartHomeTopBar(
    allSystemsNormal: Boolean,
    isOnline: Boolean,
    deviceId: String,
    updatedAgo: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 0.50f),
                        0.75f to Color.Black.copy(alpha = 0.18f),
                        1f to Color.Transparent,
                    ),
                ),
            )
            .safeHudPadding(
                includeTop = true,
                includeBottom = false,
                includeHorizontal = true,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmartHomeGlassIconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.smart_home_menu_content_description),
                    tint = SmartHomeGlass.AccentWarm,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            actions()
        }

        Text(
            text = stringResource(R.string.smart_home_screen_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = SmartHomeGlass.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmartHomeStatusBadge(
                text = stringResource(
                    if (isOnline) R.string.smart_home_status_online else R.string.smart_home_status_offline,
                ),
                dotColor = if (isOnline) {
                    SmartHomeGlass.AccentGreenBright
                } else {
                    SmartHomeGlass.TextMuted
                },
            )
            if (allSystemsNormal) {
                SmartHomeStatusBadge(
                    text = stringResource(R.string.smart_home_all_systems_normal),
                    dotColor = SmartHomeGlass.AccentGreenBright,
                )
            } else {
                SmartHomeStatusBadge(
                    text = stringResource(R.string.smart_home_systems_attention),
                    dotColor = SmartHomeGlass.AccentOrange,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = deviceId,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = SmartHomeGlass.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.smart_home_updated_ago, updatedAgo),
                style = MaterialTheme.typography.labelSmall,
                color = SmartHomeGlass.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SmartHomeStatusBadge(
    text: String,
    dotColor: Color,
) {
    Row(
        modifier = Modifier
            .clip(SmartHomeGlass.PillShape)
            .background(SmartHomeGlass.CardSurface.copy(alpha = SmartHomeGlass.BadgeSurfaceAlpha))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = SmartHomeGlass.TextSecondary,
        )
    }
}
