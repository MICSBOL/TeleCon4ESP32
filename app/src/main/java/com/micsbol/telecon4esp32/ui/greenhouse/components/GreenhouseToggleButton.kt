package com.micsbol.telecon4esp32.ui.greenhouse.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseGlass

@Composable
fun GreenhouseToggleButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = GreenhouseGlass.AccentGreen,
) {
    val backgroundColor = if (isActive) {
        GreenhouseGlass.ChipBackground.copy(alpha = GreenhouseGlass.ChipSurfaceAlpha + 0.06f)
    } else {
        GreenhouseGlass.BadgeBackground.copy(alpha = GreenhouseGlass.ChipSurfaceAlpha)
    }
    val borderColor = if (isActive) {
        GreenhouseGlass.AccentGreen.copy(alpha = 0.45f)
    } else {
        Color.White.copy(alpha = GreenhouseGlass.BorderAlpha)
    }
    val contentColor = if (isActive) {
        GreenhouseGlass.AccentGreen
    } else {
        GreenhouseGlass.TextOnGlassSecondary
    }

    Column(
        modifier = modifier
            .clip(GreenhouseGlass.SmallCardShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, GreenhouseGlass.SmallCardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}
