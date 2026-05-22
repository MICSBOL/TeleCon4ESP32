package com.micsbol.emitterapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.emitterapp.ui.theme.TechBlue
import com.micsbol.emitterapp.ui.theme.TechBlueBright
import com.micsbol.emitterapp.ui.theme.TechBlueDark
import com.micsbol.emitterapp.ui.theme.TechCyan
import com.micsbol.emitterapp.ui.theme.TechCyanBright
import com.micsbol.emitterapp.ui.theme.TechCyanDark
import com.micsbol.emitterapp.ui.theme.TechOnPrimary
import com.micsbol.emitterapp.ui.theme.StatusConnected
import com.micsbol.emitterapp.ui.theme.StatusDisconnected

val EmitterCardShape = RoundedCornerShape(14.dp)
val EmitterInnerShape = RoundedCornerShape(10.dp)
val EmitterPillButtonShape = RoundedCornerShape(28.dp)

@Composable
fun brandPrimary(): Color =
    if (isSystemInDarkTheme()) TechBlueBright else TechBlue

@Composable
fun brandSecondary(): Color =
    if (isSystemInDarkTheme()) TechCyanBright else TechCyan

@Composable
fun mutedTextColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun actionButtonGradient(): Brush {
    val start = if (isSystemInDarkTheme()) TechBlueBright else TechBlue
    val end = if (isSystemInDarkTheme()) TechCyanBright else TechCyanDark
    return Brush.horizontalGradient(listOf(start, end))
}

@Composable
fun EmitterAppScaffold(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            EmitterAppHeader(
                title = title,
                subtitle = subtitle,
                navigationIcon = navigationIcon,
                actions = actions
            )
        },
        content = content
    )
}

@Composable
fun EmitterAppHeader(
    title: String,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(MaterialTheme.colorScheme.surface)
            .shadow(
                elevation = if (isSystemInDarkTheme()) 0.dp else 2.dp,
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                navigationIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedTextColor()
                    )
                }
            }
            Row(content = actions)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            brandPrimary().copy(alpha = 0.85f),
                            brandSecondary().copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun EmitterIconContainer(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = brandPrimary(),
    iconSize: Dp = 22.dp,
    boxSize: Dp = 44.dp
) {
    Box(
        modifier = modifier
            .size(boxSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun EmitterStyledCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val elevation = if (isSystemInDarkTheme()) 0.dp else 3.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, EmitterCardShape),
        shape = EmitterCardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSystemInDarkTheme()) 2.dp else 0.dp,
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                listOf(brandPrimary(), brandSecondary())
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    content = content
                )
            }
        }
    )
}

@Composable
fun EmitterSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier.padding(bottom = 8.dp, start = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun EmitterQuickStartButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    supportingText: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = if (enabled) 6.dp else 0.dp,
                    shape = EmitterPillButtonShape,
                    ambientColor = brandSecondary().copy(alpha = 0.35f),
                    spotColor = brandPrimary().copy(alpha = 0.4f)
                )
                .clip(EmitterPillButtonShape)
                .background(
                    if (enabled) {
                        actionButtonGradient()
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                brandPrimary().copy(alpha = 0.4f),
                                brandSecondary().copy(alpha = 0.4f)
                            )
                        )
                    }
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TechOnPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TechOnPrimary
                )
            }
        }
        if (supportingText != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelSmall,
                color = mutedTextColor()
            )
        }
    }
}

@Composable
fun EmitterFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = EmitterPillButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = brandPrimary(),
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ConnectionStatusDot(connected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(if (connected) StatusConnected else StatusDisconnected)
    )
}
