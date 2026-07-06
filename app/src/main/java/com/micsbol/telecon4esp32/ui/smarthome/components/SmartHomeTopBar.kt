package com.micsbol.telecon4esp32.ui.smarthome.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsNone
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.smarthome.SmartHomeGlass
import com.micsbol.telecon4esp32.ui.smarthome.SmartHomeGlassIconButton

@Composable
fun SmartHomeTopBar(
    allSystemsNormal: Boolean,
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
            .windowInsetsPadding(WindowInsets.statusBars)
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
            SmartHomeGlassIconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = stringResource(R.string.smart_home_notifications_content_description),
                    tint = SmartHomeGlass.TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            actions()
        }

        Text(
            text = stringResource(R.string.smart_home_screen_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = SmartHomeGlass.TextPrimary,
        )

        if (allSystemsNormal) {
            SmartHomeStatusBadge(
                text = stringResource(R.string.smart_home_all_systems_normal),
            )
        }
    }
}

@Composable
private fun SmartHomeStatusBadge(
    text: String,
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
                .background(SmartHomeGlass.AccentGreenBright),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = SmartHomeGlass.TextSecondary,
        )
    }
}
