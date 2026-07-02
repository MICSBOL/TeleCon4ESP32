package com.micsbol.telecon4esp32.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.cyber.components.CyberBackground
import com.micsbol.telecon4esp32.ui.cyber.theme.CyberColors
import com.micsbol.telecon4esp32.ui.control_panel.components.ControlPanelPlasticColors
import com.micsbol.telecon4esp32.ui.control_panel.components.drawPlasticRaisedRoundRect
import com.micsbol.telecon4esp32.ui.theme.HudCyan
import com.micsbol.telecon4esp32.ui.theme.HudCyanBright
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.StatusDisconnected
import com.micsbol.telecon4esp32.ui.theme.syncopate

val EmitterCardShape = RoundedCornerShape(14.dp)
val EmitterInnerShape = RoundedCornerShape(10.dp)
val EmitterPillButtonShape = RoundedCornerShape(28.dp)
val EmitterHeaderBrandLogoSizeHome = 72.dp
val EmitterHeaderBrandLogoSize = 56.dp

@Composable
fun EmitterBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.about_back),
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = contentDescription,
            tint = HudCyanBright,
        )
    }
}

@Composable
fun EmitterBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    fullLogo: Boolean = false,
) {
    Image(
        painter = painterResource(
            if (fullLogo) R.drawable.ic_telecon4esp32_logo else R.drawable.ic_telecon4esp32_icon
        ),
        contentDescription = stringResource(R.string.about_logo_content_description),
        modifier = modifier.size(size),
    )
}

@Composable
fun brandPrimary(): Color = HudCyan

@Composable
fun brandSecondary(): Color = HudCyanBright

@Composable
fun mutedTextColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun actionButtonGradient(): Brush {
    return Brush.horizontalGradient(listOf(HudCyan, HudCyanBright))
}

@Composable
fun TeleCon4Esp32Scaffold(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    showBrandLogo: Boolean = true,
    brandLogoSize: Dp? = null,
    showAdBanner: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val resolvedNavigationIcon = navigationIcon
        ?: onNavigateBack?.let { onBack ->
            { EmitterBackButton(onClick = onBack) }
        }
    val resolvedLogoSize = brandLogoSize
        ?: if (resolvedNavigationIcon == null && subtitle != null) {
            EmitterHeaderBrandLogoSizeHome
        } else {
            EmitterHeaderBrandLogoSize
        }
    Box(modifier = modifier.fillMaxSize()) {
        CyberBackground()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TeleCon4Esp32TopBar(
                    title = title,
                    subtitle = subtitle,
                    showBrandLogo = showBrandLogo,
                    brandLogoSize = resolvedLogoSize,
                    navigationIcon = resolvedNavigationIcon,
                    actions = actions
                )
            },
            bottomBar = {
                if (showAdBanner) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars),
                    ) {
                        AdBanner(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            content = content
        )
    }
}

@Composable
fun TeleCon4Esp32TopBar(
    title: String,
    subtitle: String? = null,
    showBrandLogo: Boolean = true,
    brandLogoSize: Dp = EmitterHeaderBrandLogoSize,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val titleGlow = Shadow(
        color = CyberColors.NeonPrimary.copy(alpha = 0.85f),
        offset = Offset.Zero,
        blurRadius = 12f,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                if (navigationIcon != null) {
                    navigationIcon()
                }
                if (showBrandLogo) {
                    EmitterBrandLogo(
                        modifier = Modifier.padding(
                            start = if (navigationIcon != null) 0.dp else 8.dp,
                            end = 12.dp,
                        ),
                        size = brandLogoSize,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title.uppercase(),
                        style = TextStyle(
                            fontFamily = syncopate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.5.sp,
                            shadow = titleGlow,
                        ),
                        color = CyberColors.NeonPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start,
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = TextStyle(
                                fontFamily = syncopate,
                                fontWeight = FontWeight.Normal,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                            ),
                            color = CyberColors.TextSecondary,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                content = actions
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            CyberColors.NeonPrimary.copy(alpha = 0.9f),
                            CyberColors.NeonSecondary.copy(alpha = 0.6f),
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
            .background(HudCyan.copy(alpha = 0.12f))
            .border(1.dp, HudCyan.copy(alpha = 0.4f), CircleShape),
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
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .drawBehind {
                drawPlasticRaisedRoundRect(
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                )
            }
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(ControlPanelPlasticColors.ScreenBackground.copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            content = content,
        )
    }
}

@Composable
fun EmitterSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(bottom = 8.dp, start = 4.dp),
        style = TextStyle(
            fontFamily = syncopate,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
        ),
        color = HudCyanBright,
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
        ShellPlasticStartButton(
            title = text,
            subtitle = supportingText ?: "",
            onClick = onClick,
            enabled = enabled,
        )
    }
}

@Composable
fun EmitterFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ShellPlasticStartButton(
        title = text,
        subtitle = "",
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
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
