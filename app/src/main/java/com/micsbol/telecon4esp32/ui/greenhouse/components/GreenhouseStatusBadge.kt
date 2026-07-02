package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.components.ConnectionStatusDot
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseGlass

@Composable
fun GreenhouseStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = GreenhouseGlass.BadgeBackground.copy(alpha = GreenhouseGlass.BadgeSurfaceAlpha),
    contentColor: Color = GreenhouseGlass.TextOnGlassPrimary,
    showStatusDot: Boolean = false,
    connected: Boolean = true,
    leadingIcon: ImageVector? = null,
    iconTint: Color = GreenhouseGlass.AccentGreen,
) {
    Row(
        modifier = modifier
            .clip(GreenhouseGlass.PillShape)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showStatusDot) {
            ConnectionStatusDot(
                connected = connected,
                modifier = Modifier.size(7.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
        )
    }
}

@Composable
fun GreenhouseStableBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    GreenhouseStatusBadge(
        text = text,
        modifier = modifier,
        backgroundColor = GreenhouseGlass.ChipBackground.copy(alpha = GreenhouseGlass.ChipSurfaceAlpha),
        contentColor = GreenhouseGlass.AccentGreen,
        leadingIcon = Icons.Default.Check,
        iconTint = GreenhouseGlass.AccentGreen,
    )
}
