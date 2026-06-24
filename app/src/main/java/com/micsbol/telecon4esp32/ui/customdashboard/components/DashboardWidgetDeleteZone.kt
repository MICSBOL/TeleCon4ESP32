package com.micsbol.telecon4esp32.ui.customdashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.theme.StatusDisconnected

@Composable
fun DashboardWidgetDeleteZone(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) {
            StatusDisconnected.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
        },
        label = "deleteZoneBackground",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) StatusDisconnected else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
        label = "deleteZoneBorder",
    )
    val iconColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "deleteZoneIcon",
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.12f else 1f,
        label = "deleteZoneScale",
    )

    Box(
        modifier = modifier
            .size(DashboardDeleteZoneSpec.SIZE)
            .scale(scale)
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.custom_dashboard_delete_zone_content_description),
            tint = iconColor,
            modifier = Modifier.size(26.dp),
        )
    }
}
