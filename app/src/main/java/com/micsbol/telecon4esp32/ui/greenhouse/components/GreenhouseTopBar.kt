package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseGlass
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseGlassIconButton

@Composable
fun GreenhouseTopBar(
    isOnline: Boolean,
    isAutoMode: Boolean,
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
                        0f to Color.Black.copy(alpha = 0.42f),
                        0.75f to Color.Black.copy(alpha = 0.16f),
                        1f to Color.Transparent,
                    ),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GreenhouseGlassIconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.greenhouse_menu_content_description),
                    tint = GreenhouseGlass.AccentGreen,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = null,
                tint = GreenhouseGlass.LeafBright,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.greenhouse_screen_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GreenhouseGlass.TextOnBackgroundPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(8.dp))
            actions()
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GreenhouseStatusBadge(
                text = stringResource(
                    if (isOnline) R.string.greenhouse_status_online else R.string.greenhouse_status_offline,
                ),
                showStatusDot = true,
                connected = isOnline,
                backgroundColor = GreenhouseGlass.BadgeBackground.copy(alpha = GreenhouseGlass.BadgeSurfaceAlpha),
                contentColor = GreenhouseGlass.TextOnGlassPrimary,
            )
            if (isAutoMode) {
                GreenhouseStatusBadge(
                    text = stringResource(R.string.greenhouse_mode_auto),
                    backgroundColor = GreenhouseGlass.ChipBackground.copy(
                        alpha = GreenhouseGlass.ChipSurfaceAlpha,
                    ),
                    contentColor = GreenhouseGlass.AccentGreen,
                    leadingIcon = Icons.Default.Eco,
                    iconTint = GreenhouseGlass.AccentGreen,
                )
            } else {
                GreenhouseStatusBadge(
                    text = stringResource(R.string.greenhouse_mode_manual),
                    backgroundColor = GreenhouseGlass.BadgeBackground.copy(
                        alpha = GreenhouseGlass.BadgeSurfaceAlpha,
                    ),
                    contentColor = GreenhouseGlass.TextOnGlassSecondary,
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
                color = GreenhouseGlass.TextOnBackgroundSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.greenhouse_updated_ago, updatedAgo),
                style = MaterialTheme.typography.labelSmall,
                color = GreenhouseGlass.TextOnBackgroundSecondary,
                maxLines = 1,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
fun GreenhouseSubScreenTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier
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
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = null,
                tint = GreenhouseGlass.LeafBright,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GreenhouseGlass.TextOnGlassPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                modifier = Modifier.padding(start = 54.dp),
                style = MaterialTheme.typography.bodySmall,
                color = GreenhouseGlass.TextOnGlassSecondary,
            )
        }
    }
}
