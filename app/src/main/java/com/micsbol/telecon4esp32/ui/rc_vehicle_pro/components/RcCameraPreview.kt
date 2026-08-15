package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.ui.components.EmitterCardShape
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProGlass
import com.micsbol.telecon4esp32.ui.theme.DarkBackground

@Composable
fun RcCameraPreview(
    cameraState: CameraStreamState,
    modifier: Modifier = Modifier,
    cameraLinkProfile: CameraLinkProfile = CameraLinkProfile.WIFI_SOFTAP,
    showConnectStreamButton: Boolean = false,
    onConnectStreamClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val showWifiAction = cameraLinkProfile != CameraLinkProfile.CONTROL_ONLY
    val idleMessage = when (cameraLinkProfile) {
        CameraLinkProfile.CONTROL_ONLY ->
            stringResource(R.string.rc_vehicle_camera_devkit_idle)
        CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE ->
            stringResource(R.string.rc_vehicle_camera_ble_idle)
        CameraLinkProfile.WIFI_SOFTAP ->
            stringResource(R.string.rc_vehicle_camera_idle)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center,
    ) {
        when (cameraState) {
            is CameraStreamState.Frame -> {
                RcCameraFrameImage(bitmap = cameraState.bitmap)
            }
            CameraStreamState.Connecting -> {
                RcCameraFallbackBackground()
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
            is CameraStreamState.Error -> {
                RcCameraFallbackBackground()
                RcCameraFallbackBanner(
                    message = stringResource(R.string.rc_vehicle_camera_unavailable),
                    showWifiAction = showWifiAction,
                    showConnectStreamButton = showConnectStreamButton,
                    onOpenWifiSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK,
                            ),
                        )
                    },
                    onConnectStreamClick = onConnectStreamClick,
                )
            }
            CameraStreamState.Idle -> {
                RcCameraFallbackBackground()
                RcCameraFallbackBanner(
                    message = idleMessage,
                    showWifiAction = showWifiAction,
                    showConnectStreamButton = showConnectStreamButton,
                    onOpenWifiSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK,
                            ),
                        )
                    },
                    onConnectStreamClick = onConnectStreamClick,
                )
            }
        }
    }
}

/**
 * Converts [Bitmap] → [ImageBitmap] only when the Android bitmap identity changes.
 */
@Composable
private fun RcCameraFrameImage(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
) {
    val imageBitmap: ImageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    Image(
        bitmap = imageBitmap,
        contentDescription = stringResource(R.string.rc_vehicle_camera_content_description),
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
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

@Composable
private fun RcCameraFallbackBanner(
    message: String,
    modifier: Modifier = Modifier,
    showWifiAction: Boolean = false,
    showConnectStreamButton: Boolean = false,
    onOpenWifiSettings: (() -> Unit)? = null,
    onConnectStreamClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 16.dp),
        shape = EmitterCardShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = RcVehicleProGlass.SURFACE_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            if (showWifiAction && onOpenWifiSettings != null) {
                TextButton(onClick = onOpenWifiSettings) {
                    Text(
                        text = stringResource(R.string.rc_vehicle_camera_open_wifi),
                        color = brandPrimary(),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
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
