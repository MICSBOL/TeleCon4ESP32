package com.micsbol.telecon4esp32.ui.greenhouse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState
import com.micsbol.telecon4esp32.domain.camera.Esp32CameraDefaults
import com.micsbol.telecon4esp32.ui.greenhouse.components.GreenhouseStatusBadge

@Composable
fun GreenhouseCameraScreen(
    navController: NavController,
    viewModel: GreenhouseCameraViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onScreenVisible()
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.onScreenHidden() }
    }

    GreenhouseCameraScreenContent(
        uiState = uiState,
        onBackClick = { navController.navigateUp() },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GreenhouseCameraScreenContent(
    uiState: GreenhouseCameraUiState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val edgeInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
    )
    val bottomInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        GreenhouseCameraFeed(
            cameraState = uiState.cameraState,
            isEmulatorPreview = uiState.isEmulatorPreview,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.45f),
                            0.22f to Color.Transparent,
                            0.72f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.55f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(edgeInsets)
                .windowInsetsPadding(bottomInsets)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GreenhouseGlassIconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.greenhouse_settings_back),
                        tint = GreenhouseGlass.AccentGreen,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = GreenhouseGlass.LeafBright,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.greenhouse_camera_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                GreenhouseStatusBadge(
                    text = stringResource(
                        if (uiState.isBluetoothOnline) {
                            R.string.greenhouse_status_online
                        } else {
                            R.string.greenhouse_status_offline
                        },
                    ),
                    showStatusDot = true,
                    connected = uiState.isBluetoothOnline,
                    backgroundColor = Color.Black.copy(alpha = 0.42f),
                    contentColor = Color.White,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GreenhouseHudMetric(
                        label = stringResource(R.string.greenhouse_weather_label),
                        value = stringResource(
                            R.string.greenhouse_temperature_reading,
                            uiState.temperatureC,
                        ),
                    )
                    GreenhouseHudMetric(
                        label = stringResource(R.string.greenhouse_humidity_label),
                        value = stringResource(
                            R.string.greenhouse_percent_value,
                            uiState.humidityPercent,
                        ),
                    )
                    GreenhouseHudMetric(
                        label = stringResource(R.string.greenhouse_vpd_label),
                        value = stringResource(
                            R.string.greenhouse_vpd_value,
                            String.format("%.1f", uiState.vpdKpa),
                        ),
                    )
                    GreenhouseHudMetric(
                        label = stringResource(R.string.greenhouse_soil),
                        value = stringResource(
                            R.string.greenhouse_percent_value,
                            uiState.soilPercent,
                        ),
                    )
                    GreenhouseHudMetric(
                        label = stringResource(R.string.greenhouse_tank),
                        value = stringResource(
                            R.string.greenhouse_percent_value,
                            uiState.tankPercent,
                        ),
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GreenhouseStatusBadge(
                        text = stringResource(
                            if (uiState.isAutoMode) {
                                R.string.greenhouse_mode_auto
                            } else {
                                R.string.greenhouse_mode_manual
                            },
                        ),
                        backgroundColor = Color.Black.copy(alpha = 0.42f),
                        contentColor = GreenhouseGlass.LeafBright,
                        leadingIcon = Icons.Default.Eco,
                        iconTint = GreenhouseGlass.LeafBright,
                    )
                    if (uiState.isStable) {
                        GreenhouseStatusBadge(
                            text = stringResource(R.string.greenhouse_stable),
                            backgroundColor = Color.Black.copy(alpha = 0.42f),
                            contentColor = GreenhouseGlass.LeafLime,
                        )
                    }
                    GreenhouseStatusBadge(
                        text = stringResource(
                            if (uiState.fanOn) {
                                R.string.greenhouse_fan_on
                            } else {
                                R.string.greenhouse_fan_off
                            },
                        ),
                        backgroundColor = Color.Black.copy(alpha = 0.42f),
                        contentColor = Color.White,
                    )
                    GreenhouseStatusBadge(
                        text = stringResource(
                            if (uiState.lightsOn) {
                                R.string.greenhouse_lights_on
                            } else {
                                R.string.greenhouse_lights_off
                            },
                        ),
                        backgroundColor = Color.Black.copy(alpha = 0.42f),
                        contentColor = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun GreenhouseCameraFeed(
    cameraState: CameraStreamState,
    isEmulatorPreview: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        when {
            cameraState is CameraStreamState.Frame && !isEmulatorPreview -> {
                Image(
                    bitmap = cameraState.bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.greenhouse_camera_content_description),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            cameraState is CameraStreamState.Connecting && !isEmulatorPreview -> {
                GreenhouseCameraFallbackBackground()
                CircularProgressIndicator(color = GreenhouseGlass.AccentGreen)
                Text(
                    text = stringResource(R.string.greenhouse_camera_connecting),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
            else -> {
                GreenhouseCameraFallbackBackground()
                GreenhouseCameraIdleMessage(
                    title = stringResource(R.string.greenhouse_camera_unavailable_title),
                    body = when {
                        isEmulatorPreview -> stringResource(R.string.greenhouse_camera_emulator_body)
                        cameraState is CameraStreamState.Error -> cameraState.message
                        else -> stringResource(
                            R.string.greenhouse_camera_unavailable_body,
                            Esp32CameraDefaults.DEFAULT_BASE_URL,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun GreenhouseCameraFallbackBackground(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.greenhouse_camera_fallback),
        contentDescription = stringResource(R.string.greenhouse_camera_fallback_content_description),
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun GreenhouseCameraIdleMessage(
    title: String,
    body: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .padding(horizontal = 28.dp)
            .clip(GreenhouseGlass.SmallCardShape)
            .background(Color.Black.copy(alpha = 0.48f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.22f),
                shape = GreenhouseGlass.SmallCardShape,
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = Icons.Default.VideocamOff,
            contentDescription = null,
            tint = GreenhouseGlass.LeafLime,
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            color = Color.White.copy(alpha = 0.82f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GreenhouseHudMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(GreenhouseGlass.SmallCardShape)
            .background(Color.Black.copy(alpha = 0.42f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.28f),
                shape = GreenhouseGlass.SmallCardShape,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.78f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = GreenhouseGlass.LeafLime,
        )
    }
}
