package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.ui.components.EmitterCardShape
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.components.mutedTextColor
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProGlass
import com.micsbol.telecon4esp32.ui.theme.DarkBackground

@Composable
fun RcCameraPreview(
    cameraState: CameraStreamState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center,
    ) {
        when (cameraState) {
            is CameraStreamState.Frame -> {
                Image(
                    bitmap = cameraState.bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.rc_vehicle_camera_content_description),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            CameraStreamState.Connecting -> {
                RcCameraFallbackBackground()
                CircularProgressIndicator(color = brandPrimary())
            }
            is CameraStreamState.Error -> {
                RcCameraFallbackBackground()
                RcCameraFallbackBanner(message = cameraState.message)
            }
            CameraStreamState.Idle -> {
                RcCameraFallbackBackground()
                RcCameraFallbackBanner(
                    message = stringResource(R.string.rc_vehicle_camera_idle),
                )
            }
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

@Composable
private fun RcCameraFallbackBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 16.dp),
        shape = EmitterCardShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = RcVehicleProGlass.SURFACE_ALPHA),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}
