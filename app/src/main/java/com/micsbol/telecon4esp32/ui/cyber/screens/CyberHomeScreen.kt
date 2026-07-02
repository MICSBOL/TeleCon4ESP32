package com.micsbol.telecon4esp32.ui.cyber.screens

import android.app.Activity
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.ads.AdPolicy
import com.micsbol.telecon4esp32.ui.components.AdBanner
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoIconBadge
import com.micsbol.telecon4esp32.ui.components.NeoIconButton
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeoStatTile
import com.micsbol.telecon4esp32.ui.components.NeumorphicBackground
import com.micsbol.telecon4esp32.ui.components.neuRaised
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.home.HomeHelpDialog
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.theme.Neo

private data class HomeMenuItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val onClick: () -> Unit,
)

private data class HomeMetric(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val valueColor: Color = Neo.TextPrimary,
)

/**
 * Soft-UI neumorphic home dashboard.
 *
 * Vertical structure:
 *  Header -> Quick Status -> Bluetooth -> 2x2 Grid -> Recent Project -> CTA -> Footer
 */
@Composable
fun CyberHomeScreen(
    navController: NavHostController? = null,
    isConnecting: Boolean = false,
    isBluetoothConnected: Boolean = false,
    errorMessage: String? = null,
    lastDeviceName: String? = null,
    onStartClick: () -> Unit = {},
    onDismissError: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val entitlement = LocalEntitlement.current
    var showHelpDialog by remember { mutableStateOf(false) }

    BackHandler { activity?.finish() }

    val gridItems = listOf(
        HomeMenuItem(
            icon = Icons.Filled.GridView,
            title = stringResource(R.string.cyber_grid_modules_title),
            description = stringResource(R.string.cyber_grid_modules_desc),
            onClick = { navController?.navigate(Screen.Applications.route) },
        ),
        HomeMenuItem(
            icon = Icons.Filled.Code,
            title = stringResource(R.string.cyber_grid_codes_title),
            description = stringResource(R.string.cyber_grid_codes_desc),
            onClick = { navController?.navigate(Screen.Codes.route) },
        ),
        HomeMenuItem(
            icon = Icons.Filled.PlayCircleOutline,
            title = stringResource(R.string.cyber_grid_tutorials_title),
            description = stringResource(R.string.cyber_grid_tutorials_desc),
            onClick = { navController?.navigate(Screen.Tutorial.route) },
        ),
        HomeMenuItem(
            icon = Icons.Filled.Info,
            title = stringResource(R.string.about_title),
            description = stringResource(R.string.cyber_grid_about_desc),
            onClick = { navController?.navigate(Screen.About.route) },
        ),
    )

    val quickMetrics = connectionMetrics(isBluetoothConnected) + listOf(
        HomeMetric(
            icon = Icons.Filled.Bolt,
            label = stringResource(R.string.cyber_power),
            value = stringResource(R.string.cyber_power_value),
        ),
        HomeMetric(
            icon = Icons.Filled.Schedule,
            label = stringResource(R.string.cyber_latency),
            value = stringResource(R.string.cyber_latency_value),
        ),
    )

    val footerMetrics = connectionMetrics(isBluetoothConnected) + listOf(
        HomeMetric(
            icon = Icons.Filled.Bolt,
            label = stringResource(R.string.cyber_power),
            value = stringResource(R.string.cyber_power_value),
        ),
        HomeMetric(
            icon = Icons.Filled.Layers,
            label = stringResource(R.string.cyber_packets),
            value = "0",
        ),
        HomeMetric(
            icon = Icons.Filled.MonitorHeart,
            label = stringResource(R.string.cyber_uptime),
            value = "00:00:00",
        ),
    )

    val startSubtitle = if (lastDeviceName != null) {
        stringResource(R.string.home_last_devices) + " " + lastDeviceName
    } else {
        stringResource(R.string.cyber_start_subtitle)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NeumorphicBackground()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                HeaderPanel(
                    isConnected = isBluetoothConnected,
                    onHelpClick = { showHelpDialog = true },
                    onHeaderClick = { navController?.navigate(Screen.About.route) },
                )
            }

            item {
                QuickStatusPanel(metrics = quickMetrics)
            }

            item {
                BluetoothPanel(
                    isConnected = isBluetoothConnected,
                    onConnectClick = { navController?.navigate(Screen.Bluetooth.route) },
                )
            }

            items(gridItems.chunked(2)) { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowItems.forEach { gridItem ->
                        MenuCard(item = gridItem, modifier = Modifier.weight(1f))
                    }
                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }

            item {
                RecentProjectPanel(
                    onOpenClick = { navController?.navigate(Screen.Applications.route) },
                )
            }

            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NeoPillButton(
                        text = stringResource(R.string.cyber_start),
                        onClick = onStartClick,
                        icon = Icons.Filled.Rocket,
                        enabled = !isConnecting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = startSubtitle,
                        color = Neo.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            item { FooterPanel(metrics = footerMetrics) }

            if (AdPolicy.hasBanner(Screen.Home.route, entitlement)) {
                item { AdBanner(modifier = Modifier.fillMaxWidth()) }
            }
        }

        if (showHelpDialog) {
            HomeHelpDialog(onDismissRequest = { showHelpDialog = false })
        }

        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = onDismissError,
                title = { Text(stringResource(R.string.bluetooth_connection_error)) },
                text = { Text(errorMessage) },
                confirmButton = {
                    TextButton(onClick = onDismissError) {
                        Text(stringResource(R.string.codes_dialog_ok))
                    }
                },
            )
        }

        if (isConnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Neo.Background.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Neo.Accent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (lastDeviceName != null) {
                            stringResource(R.string.home_bluetooth_status_connecting) + " $lastDeviceName"
                        } else {
                            stringResource(R.string.home_bluetooth_status_connecting)
                        },
                        color = Neo.Accent,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun connectionMetrics(isConnected: Boolean): List<HomeMetric> = listOf(
    HomeMetric(
        icon = Icons.Filled.SignalCellularAlt,
        label = stringResource(R.string.cyber_signal),
        value = if (isConnected) {
            stringResource(R.string.cyber_signal_online)
        } else {
            stringResource(R.string.cyber_signal_offline)
        },
        valueColor = if (isConnected) Neo.Positive else Neo.Negative,
    ),
    HomeMetric(
        icon = Icons.Filled.Gamepad,
        label = stringResource(R.string.cyber_mode),
        value = stringResource(R.string.cyber_mode_remote),
    ),
)

@Composable
private fun HeaderPanel(
    isConnected: Boolean,
    onHelpClick: () -> Unit,
    onHeaderClick: () -> Unit,
) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onHeaderClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Neo.SurfaceLow),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_telecon4esp32_icon),
                    contentDescription = stringResource(R.string.about_logo_content_description),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_hud_title),
                    color = Neo.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.cyber_header_subtitle),
                    color = Neo.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusLine(isConnected = isConnected)
            }
            Spacer(modifier = Modifier.width(12.dp))
            NeoIconButton(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                onClick = onHelpClick,
                contentDescription = stringResource(R.string.home_help),
                size = 44.dp,
            )
        }
    }
}

@Composable
private fun StatusLine(isConnected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isConnected) Neo.Positive else Neo.Negative),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isConnected) {
                stringResource(R.string.cyber_device_connected)
            } else {
                stringResource(R.string.cyber_device_disconnected)
            },
            color = if (isConnected) Neo.Positive else Neo.Negative,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun QuickStatusPanel(metrics: List<HomeMetric>) {
    NeoCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.cyber_quick_status),
            color = Neo.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            metrics.forEach { metric ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    NeoIconBadge(icon = metric.icon)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = metric.value,
                        color = metric.valueColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = metric.label,
                        color = Neo.TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BluetoothPanel(isConnected: Boolean, onConnectClick: () -> Unit) {
    NeoCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeoIconBadge(icon = Icons.Filled.Bluetooth, size = 52.dp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cyber_bluetooth),
                    color = Neo.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.cyber_bluetooth_subtitle),
                    color = Neo.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusLine(isConnected = isConnected)
            }
            Spacer(modifier = Modifier.width(12.dp))
            NeoPillButton(
                text = stringResource(R.string.cyber_connect),
                onClick = onConnectClick,
                compact = true,
            )
        }
    }
}

@Composable
private fun MenuCard(item: HomeMenuItem, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier = modifier
            .height(150.dp)
            .neuRaised(cornerRadius = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = item.onClick,
            )
            .padding(18.dp),
    ) {
        NeoIconBadge(icon = item.icon, size = 46.dp, selected = pressed)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = item.title,
            color = Neo.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.description,
            color = Neo.TextSecondary,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RecentProjectPanel(onOpenClick: () -> Unit) {
    NeoCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.cyber_recent_project),
            color = Neo.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 84.dp, height = 64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Neo.SurfaceLow),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.car_bouncing01),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.cyber_recent_name),
                    color = Neo.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.cyber_recent_last),
                    color = Neo.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(stringResource(R.string.cyber_tag_robotics))
                    Tag(stringResource(R.string.cyber_tag_esp32))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            NeoPillButton(
                text = stringResource(R.string.cyber_open),
                onClick = onOpenClick,
                compact = true,
            )
        }
    }
}

@Composable
private fun Tag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Neo.Accent.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            color = Neo.Accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FooterPanel(metrics: List<HomeMetric>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        metrics.take(3).forEach { metric ->
            NeoStatTile(
                icon = metric.icon,
                label = metric.label,
                value = metric.value,
                valueColor = metric.valueColor,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview(showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CyberHomeScreenPreview() {
    CyberHomeScreen()
}

@Preview(showSystemUi = true, uiMode = UI_MODE_NIGHT_YES, name = "Connected")
@Composable
private fun CyberHomeScreenConnectedPreview() {
    CyberHomeScreen(isBluetoothConnected = true)
}





