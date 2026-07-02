package com.micsbol.telecon4esp32.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.control_panel.components.ControlPanelDisplayFrame
import com.micsbol.telecon4esp32.ui.control_panel.components.ControlPanelPlasticColors
import com.micsbol.telecon4esp32.ui.control_panel.components.drawPlasticRaisedRoundRect
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.StatusDisconnected

private val ShellPlasticCardShape = RoundedCornerShape(12.dp)
private val ShellPlasticCardCorner = 12.dp
private val ShellPlasticButtonShape = RoundedCornerShape(14.dp)
private val ShellPlasticButtonCorner = 14.dp

@Composable
fun ShellPlasticBackground(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.plastic_background),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
fun ShellPlasticDisplayPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    ControlPanelDisplayFrame(
        modifier = modifier,
        bezelWidth = 10.dp,
        outerCornerRadius = 14.dp,
        innerCornerRadius = 6.dp,
        chinHeight = 12.dp,
        accentColor = brandPrimary(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            content = content,
        )
    }
}

@Composable
fun ShellPlasticMenuCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    statusText: String? = null,
    statusConnected: Boolean = false,
) {
    val cornerPx = ShellPlasticCardCorner
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(ShellPlasticCardShape)
            .drawBehind {
                drawPlasticRaisedRoundRect(
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(cornerPx.toPx()),
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(ControlPanelPlasticColors.ScreenBackground.copy(alpha = 0.92f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ControlPanelPlasticColors.Mid.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ControlPanelPlasticColors.AccentGlow,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ControlPanelPlasticColors.AccentGlow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0BEC5),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (statusText != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (statusConnected) StatusConnected else StatusDisconnected),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (statusConnected) StatusConnected else StatusDisconnected,
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = ControlPanelPlasticColors.Highlight.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun ShellPlasticStartButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val cornerPx = ShellPlasticButtonCorner
    val accent = if (enabled) ControlPanelPlasticColors.AccentGlow else ControlPanelPlasticColors.Highlight
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(ShellPlasticButtonShape)
            .drawBehind {
                drawPlasticRaisedRoundRect(
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(cornerPx.toPx()),
                )
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.RocketLaunch,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB0BEC5),
                    )
                }
            }
        }
    }
}

@Composable
fun ShellPlasticStatusBar(
    signalLabel: String,
    modeLabel: String,
    powerLabel: String,
    modifier: Modifier = Modifier,
) {
    val cornerPx = 8.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(cornerPx))
            .drawBehind {
                drawPlasticRaisedRoundRect(
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(cornerPx.toPx()),
                )
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp))
                .background(ControlPanelPlasticColors.ScreenBackground.copy(alpha = 0.9f))
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShellPlasticStatusCell(label = "SIGNAL", value = signalLabel)
            ShellPlasticStatusCell(label = "MODE", value = modeLabel)
            ShellPlasticStatusCell(label = "POWER", value = powerLabel)
        }
    }
}

@Composable
private fun ShellPlasticStatusCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF78909C),
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = ControlPanelPlasticColors.AccentGlow,
            fontWeight = FontWeight.Bold,
        )
    }
}
