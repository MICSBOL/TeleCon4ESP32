package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import android.graphics.Bitmap
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.cameraJoinWarningShowsBluetoothConnect
import com.micsbol.telecon4esp32.domain.camera.cameraJoinWarningStartsExpanded
import com.micsbol.telecon4esp32.domain.camera.shouldShowCameraJoinWarning
import com.micsbol.telecon4esp32.domain.camera.shouldStartCameraStream
import com.micsbol.telecon4esp32.ui.bluetooth.openSystemWifiSettings
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProGlass
import com.micsbol.telecon4esp32.ui.theme.DarkBackground
import kotlin.math.roundToInt

private val CameraWarningAccent = Color(0xFFF4A261)

@Composable
fun RcCameraPreview(
    cameraState: CameraStreamState,
    modifier: Modifier = Modifier,
    cameraLinkProfile: CameraLinkProfile = CameraLinkProfile.CONTROL_ONLY,
    isControlConnected: Boolean = false,
    onConnectControl: (() -> Unit)? = null,
    showConnectStreamButton: Boolean = false,
    onConnectStreamClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val cameraRequired = cameraLinkProfile.shouldStartCameraStream
    val liveFrame = cameraState as? CameraStreamState.Frame
    val hasLiveFrame = liveFrame != null && !liveFrame.bitmap.isRecycled
    val showWarning = shouldShowCameraJoinWarning(
        profile = cameraLinkProfile,
        hasLiveFrame = hasLiveFrame,
        isControlConnected = isControlConnected,
    )
    val showConnecting = cameraRequired &&
        cameraState is CameraStreamState.Connecting &&
        !showWarning
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(DarkBackground),
        contentAlignment = Alignment.Center,
    ) {
        when {
            liveFrame != null && !liveFrame.bitmap.isRecycled -> {
                RcCameraFrameImage(bitmap = liveFrame.bitmap)
            }
            else -> {
                RcCameraFallbackBackground()
            }
        }
        if (showConnecting) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(color = brandPrimary())
                Text(
                    text = stringResource(R.string.rc_vehicle_camera_connecting),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
        if (showWarning) {
            RcCameraJoinWarning(
                profile = cameraLinkProfile,
                isControlConnected = isControlConnected,
                onConnectControl = onConnectControl,
                onOpenWifi = { openSystemWifiSettings(context) },
                showConnectStreamButton = showConnectStreamButton,
                onConnectStreamClick = onConnectStreamClick,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun RcCameraJoinWarning(
    profile: CameraLinkProfile,
    isControlConnected: Boolean,
    onConnectControl: (() -> Unit)?,
    onOpenWifi: () -> Unit,
    showConnectStreamButton: Boolean,
    onConnectStreamClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember {
        mutableStateOf(cameraJoinWarningStartsExpanded(profile, isControlConnected))
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (expanded) {
            RcCameraJoinWarningCard(
                profile = profile,
                isControlConnected = isControlConnected,
                onConnectControl = onConnectControl,
                onOpenWifi = onOpenWifi,
                showConnectStreamButton = showConnectStreamButton,
                onConnectStreamClick = onConnectStreamClick,
                onMinimize = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
            )
        } else {
            RcCameraJoinWarningIcon(onClick = { expanded = true })
        }
    }
}

@Composable
private fun RcCameraJoinWarningIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.rc_vehicle_camera_warning_icon)
    val tapHint = stringResource(R.string.rc_vehicle_camera_warning_tap)
    val pulse = rememberInfiniteTransition(label = "cameraWarningTap")
    val tapAlpha by pulse.animateFloat(
        initialValue = 0.28f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cameraWarningTapAlpha",
    )
    Column(
        modifier = modifier
            .semantics {
                contentDescription = "$label. $tapHint"
                role = Role.Button
            }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = RcVehicleProGlass.MENU_SURFACE_ALPHA),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = CameraWarningAccent,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = tapHint,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = CameraWarningAccent,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(tapAlpha),
        )
    }
}

@Composable
private fun RcCameraJoinWarningCard(
    profile: CameraLinkProfile,
    isControlConnected: Boolean,
    onConnectControl: (() -> Unit)?,
    onOpenWifi: () -> Unit,
    showConnectStreamButton: Boolean,
    onConnectStreamClick: (() -> Unit)?,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showBluetoothConnect = cameraJoinWarningShowsBluetoothConnect(
        profile = profile,
        isControlConnected = isControlConnected,
    )
    RcGlassCard(
        modifier = modifier,
        surfaceAlpha = RcVehicleProGlass.MENU_SURFACE_ALPHA,
        fillWidth = true,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = CameraWarningAccent,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp),
            )
            Text(
                text = stringResource(R.string.rc_vehicle_camera_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.rc_vehicle_camera_warning_minimize),
                    tint = brandPrimary(),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        ) {
            if (showBluetoothConnect && onConnectControl != null) {
                TextButton(onClick = onConnectControl) {
                    Text(
                        text = stringResource(R.string.rc_vehicle_camera_connect_bluetooth),
                        color = brandPrimary(),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            TextButton(onClick = onOpenWifi) {
                Text(
                    text = stringResource(R.string.rc_vehicle_camera_connect_wifi),
                    color = brandPrimary(),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (showConnectStreamButton && onConnectStreamClick != null) {
                TextButton(onClick = onConnectStreamClick) {
                    Text(
                        text = stringResource(R.string.rc_vehicle_camera_connect_stream),
                        color = brandPrimary(),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/**
 * Draws the live frame with a recycle check at paint time.
 * [Image] + [BitmapPainter] crashes on Huawei if the repository recycled the
 * native bitmap while a display list is still replaying.
 */
@Composable
private fun RcCameraFrameImage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
) {
    val imageBitmap: ImageBitmap? = remember(bitmap) {
        if (bitmap.isRecycled) null else bitmap.asImageBitmap()
    }
    if (imageBitmap == null) {
        RcCameraFallbackBackground(modifier)
        return
    }
    val contentDescription = stringResource(R.string.rc_vehicle_camera_content_description)
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds(),
        contentDescription = contentDescription,
    ) {
        if (bitmap.isRecycled) return@Canvas
        val dstW = size.width
        val dstH = size.height
        val srcW = imageBitmap.width.toFloat()
        val srcH = imageBitmap.height.toFloat()
        if (dstW <= 0f || dstH <= 0f || srcW <= 0f || srcH <= 0f) return@Canvas
        val scale = maxOf(dstW / srcW, dstH / srcH)
        val scaledW = (srcW * scale).roundToInt().coerceAtLeast(1)
        val scaledH = (srcH * scale).roundToInt().coerceAtLeast(1)
        val left = ((dstW - scaledW) / 2f).roundToInt()
        val top = ((dstH - scaledH) / 2f).roundToInt()
        try {
            drawImage(
                image = imageBitmap,
                dstOffset = IntOffset(left, top),
                dstSize = IntSize(scaledW, scaledH),
                filterQuality = FilterQuality.Low,
            )
        } catch (_: RuntimeException) {
            // Recycled after the isRecycled check (OEM display-list replay).
        }
    }
}

@Composable
private fun RcCameraFallbackBackground(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.rc_vehicle_camera_fallback),
        contentDescription = stringResource(R.string.rc_vehicle_camera_fallback_content_description),
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}
