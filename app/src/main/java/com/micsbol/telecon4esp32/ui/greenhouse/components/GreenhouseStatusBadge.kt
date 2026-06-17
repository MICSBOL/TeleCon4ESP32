package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.micsbol.telecon4esp32.ui.theme.StatusConnected

@Composable
fun GreenhouseStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    showStatusDot: Boolean = false,
    connected: Boolean = true,
    leadingIcon: ImageVector? = null,
    iconTint: Color = StatusConnected,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
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
        backgroundColor = StatusConnected.copy(alpha = 0.15f),
        contentColor = StatusConnected,
        leadingIcon = Icons.Default.Check,
        iconTint = StatusConnected,
    )
}
