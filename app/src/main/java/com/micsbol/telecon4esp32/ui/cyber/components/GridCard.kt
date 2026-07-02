package com.micsbol.telecon4esp32.ui.cyber.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.cyber.theme.CyberColors
import com.micsbol.telecon4esp32.ui.cyber.theme.CyberType

data class CyberGridItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val onClick: () -> Unit = {},
)

/**
 * Square-ish menu tile for the 2x2 grid. Icon top-left inside a hex frame,
 * forward chevron top-right, title + description at the bottom.
 */
@Composable
fun GridCard(
    item: CyberGridItem,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "gridScale")

    CyberPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .scale(scale)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = item.onClick,
            ),
        chamfer = 12.dp,
        glowIntensity = if (pressed) 1.3f else 0.85f,
        contentPadding = 16.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                HexIcon(icon = item.icon, size = 48.dp)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = CyberColors.NeonSecondary.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = item.title.uppercase(),
                style = CyberType.SectionTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.description,
                style = CyberType.Label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
