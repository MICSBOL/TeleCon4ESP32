package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.LiveControlBluetoothStatusChip
import com.micsbol.telecon4esp32.ui.components.LiveControlLinkKind
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.components.brandSecondary
import com.micsbol.telecon4esp32.ui.components.mutedTextColor
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProGlass
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProUiState
import com.micsbol.telecon4esp32.ui.theme.StatusConnected
import com.micsbol.telecon4esp32.ui.theme.StatusDisconnected

@Composable
fun RcVehicleHudTopBar(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    metricsContent: @Composable () -> Unit = {},
    statusContent: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = RcVehicleProGlass.TOP_BAR_ALPHA))
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.about_back),
                    tint = brandPrimary(),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(10.dp))
            metricsContent()
            Spacer(modifier = Modifier.weight(1f))
            statusContent()
            Spacer(modifier = Modifier.width(6.dp))
            actions()
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
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

@Composable
fun RcHudMetricsRow(
    uiState: RcVehicleProUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RcHudMetricBadge(
            icon = Icons.Default.Speed,
            value = stringResource(R.string.rc_vehicle_speed_value, uiState.speedKmh),
            contentDescription = stringResource(R.string.rc_vehicle_metric_speed),
        )
        RcHudMetricBadge(
            icon = Icons.Default.Wifi,
            value = if (uiState.isCameraOnline) {
                stringResource(R.string.rc_vehicle_rssi_online)
            } else {
                stringResource(R.string.rc_vehicle_rssi_offline)
            },
            contentDescription = stringResource(R.string.rc_vehicle_metric_rssi),
        )
        RcHudMetricBadge(
            icon = Icons.Default.Thermostat,
            value = stringResource(R.string.rc_vehicle_motor_temp_value, uiState.motorTempCelsius),
            contentDescription = stringResource(R.string.rc_vehicle_metric_motor_temp),
        )
    }
}

@Composable
private fun RcHudMetricBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = RcVehicleProGlass.TOP_BAR_ALPHA + 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = brandPrimary(),
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
fun RcMetricTilesColumn(
    uiState: RcVehicleProUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RcMetricTile(
            icon = Icons.Default.Speed,
            label = stringResource(R.string.rc_vehicle_metric_speed),
            value = stringResource(R.string.rc_vehicle_speed_value, uiState.speedKmh),
            progress = (uiState.speedKmh / 40f).coerceIn(0f, 1f),
        )
        RcMetricTile(
            icon = Icons.Default.Wifi,
            label = stringResource(R.string.rc_vehicle_metric_rssi),
            value = if (uiState.isCameraOnline) {
                stringResource(R.string.rc_vehicle_rssi_online)
            } else {
                stringResource(R.string.rc_vehicle_rssi_offline)
            },
            progress = if (uiState.isCameraOnline) 0.75f else 0.15f,
        )
        RcMetricTile(
            icon = Icons.Default.Thermostat,
            label = stringResource(R.string.rc_vehicle_metric_motor_temp),
            value = stringResource(R.string.rc_vehicle_motor_temp_value, uiState.motorTempCelsius),
            progress = (uiState.motorTempCelsius / 80f).coerceIn(0f, 1f),
        )
    }
}

@Composable
private fun RcMetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    RcGlassCard(modifier = modifier.width(132.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = brandPrimary(),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedTextColor(),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(2.dp),
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(brandPrimary(), brandSecondary()),
                                ),
                                RoundedCornerShape(2.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
fun RcHudTopBarStatusRow(
    uiState: RcVehicleProUiState,
    isBluetoothConnecting: Boolean = false,
    onBluetoothDisconnectedClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isWifiSoftApMode: Boolean = false,
) {
    val camOnline = uiState.isCameraOnline
    val camColor = if (camOnline) brandSecondary() else StatusDisconnected
    val camDescription = if (camOnline) {
        stringResource(R.string.rc_vehicle_cam_online)
    } else {
        stringResource(R.string.rc_vehicle_cam_offline)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveControlBluetoothStatusChip(
            isConnected = uiState.isBluetoothConnected,
            isConnecting = isBluetoothConnecting,
            onDisconnectedClick = onBluetoothDisconnectedClick,
            linkKind = if (isWifiSoftApMode) {
                LiveControlLinkKind.WifiSoftAp
            } else {
                LiveControlLinkKind.Bluetooth
            },
            isWifiCameraOnly = isWifiSoftApMode && camOnline,
        )
        RcHudTopBarStatusBadge(
            backgroundColor = brandPrimary().copy(alpha = 0.15f),
            contentDescription = stringResource(R.string.rc_vehicle_battery_value, uiState.batteryPercent),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = null,
                    tint = brandPrimary(),
                    modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = stringResource(R.string.rc_vehicle_battery_value, uiState.batteryPercent),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = brandPrimary(),
                )
            }
        }
        RcHudTopBarStatusBadge(
            backgroundColor = camColor.copy(alpha = 0.15f),
            contentDescription = camDescription,
        ) {
            Icon(
                imageVector = if (camOnline) Icons.Default.Videocam else Icons.Default.Engineering,
                contentDescription = null,
                tint = camColor,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun RcHudTopBarStatusBadge(
    backgroundColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun RcViewfinderBrackets(
    modifier: Modifier = Modifier,
    color: Color = brandPrimary().copy(alpha = 0.55f),
) {
    Canvas(modifier = modifier) {
        val arm = size.minDimension * 0.12f
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val inset = size.minDimension * 0.18f

        fun corner(topLeft: Offset) {
            drawLine(color, topLeft, topLeft + Offset(arm, 0f), stroke.width)
            drawLine(color, topLeft, topLeft + Offset(0f, arm), stroke.width)
        }

        corner(Offset(inset, inset))
        corner(Offset(size.width - inset, inset))
        corner(Offset(inset, size.height - inset))
        corner(Offset(size.width - inset, size.height - inset))
    }
}
