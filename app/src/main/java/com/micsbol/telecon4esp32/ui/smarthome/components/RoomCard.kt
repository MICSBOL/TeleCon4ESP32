package com.micsbol.telecon4esp32.ui.smarthome.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.smarthome.RoomDeviceUiModel
import com.micsbol.telecon4esp32.ui.smarthome.RoomStatusBadge
import com.micsbol.telecon4esp32.ui.smarthome.RoomUiModel
import com.micsbol.telecon4esp32.ui.smarthome.SmartHomeGlass

private val RoomCardWidth = 280.dp
private val RoomCardHeight = 453.dp

@Composable
fun RoomCard(
    room: RoomUiModel,
    onDeviceToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(RoomCardWidth)
            .height(RoomCardHeight)
            .shadow(
                elevation = 10.dp,
                shape = SmartHomeGlass.CardShape,
                ambientColor = Color.Black.copy(alpha = 0.40f),
                spotColor = Color.Black.copy(alpha = 0.30f),
            )
            .clip(SmartHomeGlass.CardShape)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = SmartHomeGlass.BorderAlpha),
                shape = SmartHomeGlass.CardShape,
            ),
    ) {
        Image(
            painter = painterResource(room.displayImageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.08f),
                            1f to Color.Black.copy(alpha = 0.55f),
                        ),
                    ),
                ),
        )
        RoomGlassOverlay(
            room = room,
            onDeviceToggle = onDeviceToggle,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun RoomGlassOverlay(
    room: RoomUiModel,
    onDeviceToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SmartHomeGlass.CardSurface.copy(alpha = SmartHomeGlass.SurfaceAlphaStrong))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(room.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SmartHomeGlass.TextPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = roomStatusSubtitle(room),
                    style = MaterialTheme.typography.labelSmall,
                    color = SmartHomeGlass.TextSecondary,
                )
            }
            RoomStatusRatioBadge(room = room)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            room.devices.forEach { device ->
                RoomDeviceIcon(
                    device = device,
                    roomName = stringResource(room.nameRes),
                    onDeviceToggle = { onDeviceToggle(device.id) },
                )
            }
        }
    }
}

@Composable
private fun RoomDeviceIcon(
    device: RoomDeviceUiModel,
    roomName: String,
    onDeviceToggle: () -> Unit,
) {
    val isActiveLight = device.isLight && device.isOn
    val backgroundColor = when {
        isActiveLight -> SmartHomeGlass.AccentWarm.copy(alpha = 0.28f)
        device.isLight -> Color.White.copy(alpha = 0.08f)
        device.isControllable && device.isOn -> SmartHomeGlass.AccentGreen.copy(alpha = 0.22f)
        device.isControllable -> Color.White.copy(alpha = 0.08f)
        device.isOn -> SmartHomeGlass.AccentGreen.copy(alpha = 0.22f)
        else -> Color.White.copy(alpha = 0.12f)
    }
    val borderColor = when {
        isActiveLight -> SmartHomeGlass.AccentWarm.copy(alpha = 0.55f)
        device.isControllable && device.isOn -> SmartHomeGlass.AccentGreen.copy(alpha = 0.40f)
        else -> Color.White.copy(alpha = 0.10f)
    }
    val iconTint = when {
        isActiveLight -> SmartHomeGlass.AccentWarm
        device.isLight -> SmartHomeGlass.TextMuted
        device.isControllable && device.isOn -> SmartHomeGlass.AccentGreenBright
        device.isControllable -> SmartHomeGlass.TextMuted
        device.isOn -> SmartHomeGlass.AccentGreenBright
        else -> SmartHomeGlass.TextPrimary
    }
    val statusLabel = stringResource(
        if (device.isOn) R.string.smart_lighting_status_on else R.string.smart_lighting_status_off,
    )

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .then(
                if (device.isControllable) {
                    Modifier.clickable(onClick = onDeviceToggle)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = device.icon,
            contentDescription = if (device.isControllable) {
                stringResource(
                    R.string.smart_home_room_device_toggle_content_description,
                    roomName,
                    device.id,
                    statusLabel,
                )
            } else {
                null
            },
            tint = iconTint,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun roomStatusSubtitle(room: RoomUiModel): String = when (room.statusBadge) {
    RoomStatusBadge.ON_COUNT -> {
        if (room.onCount == 1) {
            stringResource(R.string.smart_home_room_devices_active_one)
        } else {
            stringResource(R.string.smart_home_room_devices_active, room.onCount)
        }
    }
    RoomStatusBadge.OFF -> stringResource(R.string.smart_home_room_devices_off)
    RoomStatusBadge.OPEN -> room.alertTextRes?.let { stringResource(it) }
        ?: stringResource(R.string.smart_home_room_status_open)
}

@Composable
private fun RoomStatusRatioBadge(room: RoomUiModel) {
    val badgeBackground = SmartHomeGlass.CardSurfaceLight.copy(alpha = SmartHomeGlass.ChipSurfaceAlpha)
    val baseColor = SmartHomeGlass.TextSecondary
    val highlightColor = when (room.statusBadge) {
        RoomStatusBadge.ON_COUNT -> SmartHomeGlass.AccentWarm
        RoomStatusBadge.OFF -> SmartHomeGlass.TextMuted
        RoomStatusBadge.OPEN -> SmartHomeGlass.AccentOrange
    }

    val annotatedText = when (room.statusBadge) {
        RoomStatusBadge.ON_COUNT -> {
            val prefix = stringResource(
                R.string.smart_home_room_status_on_ratio_prefix,
                room.onCount,
                room.totalDeviceCount,
            )
            val suffix = stringResource(R.string.smart_home_room_status_on_ratio_suffix)
            buildAnnotatedString {
                withStyle(SpanStyle(color = baseColor)) {
                    append(prefix)
                }
                withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.SemiBold)) {
                    append(suffix)
                }
            }
        }
        RoomStatusBadge.OFF -> buildAnnotatedString {
            withStyle(SpanStyle(color = baseColor)) {
                append(
                    stringResource(
                        R.string.smart_home_room_status_off_ratio_prefix,
                        room.totalDeviceCount,
                    ),
                )
            }
            withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.SemiBold)) {
                append(stringResource(R.string.smart_home_room_status_off_ratio_suffix))
            }
        }
        RoomStatusBadge.OPEN -> buildAnnotatedString {
            withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.SemiBold)) {
                append(stringResource(R.string.smart_home_room_status_open))
            }
        }
    }

    Text(
        text = annotatedText,
        modifier = Modifier
            .clip(SmartHomeGlass.PillShape)
            .background(badgeBackground)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelSmall,
    )
}
