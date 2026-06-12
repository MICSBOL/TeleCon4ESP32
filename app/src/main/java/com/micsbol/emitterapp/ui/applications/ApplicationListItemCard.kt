package com.micsbol.emitterapp.ui.applications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.micsbol.emitterapp.ui.components.EmitterIconContainer
import com.micsbol.emitterapp.ui.components.EmitterStyledCard
import com.micsbol.emitterapp.ui.components.brandPrimary
import com.micsbol.emitterapp.ui.components.mutedTextColor

@Composable
fun ApplicationListItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit,
    settingsContentDescription: String,
    onSettingsClick: () -> Unit,
) {
    Row {
        EmitterStyledCard(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmitterIconContainer(
                    icon = icon,
                    contentDescription = title,
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        ApplicationBadgeChip(text = badge)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedTextColor(),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(72.dp)
                .clickable(onClick = onSettingsClick),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            BoxCenteredSettingsIcon(contentDescription = settingsContentDescription)
        }
    }
}

@Composable
private fun BoxCenteredSettingsIcon(contentDescription: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        EmitterIconContainer(
            icon = Icons.Default.Settings,
            contentDescription = contentDescription,
            boxSize = 44.dp,
        )
    }
}

@Composable
fun ApplicationBadgeChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = brandPrimary().copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = brandPrimary(),
        )
    }
}
