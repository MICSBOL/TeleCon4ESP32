package com.micsbol.telecon4esp32.ui.smartdoorlock

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.micsbol.telecon4esp32.R

object SmartDoorLockGlass {
    val CardShape = RoundedCornerShape(32.dp)
    val SmallCardShape = RoundedCornerShape(24.dp)
    val PillShape = RoundedCornerShape(50)

    const val SurfaceAlpha = 0.14f
    const val SurfaceAlphaStrong = 0.24f
    const val ChipSurfaceAlpha = 0.18f
    const val BorderAlpha = 0.28f
    const val TopBarButtonAlpha = 0.16f

    val BackgroundTop = Color(0xFF000000)
    val BackgroundMid = Color(0xFF0A120E)
    val BackgroundBottom = Color(0xFF121A16)

    val AccentGreen = Color(0xFF1A3026)
    val AccentGreenLight = Color(0xFF2D4A3E)
    val AccentGreenBright = Color(0xFF34D399)

    val CardSurface = Color(0xFFFFFFFF)
    val CardSurfaceTint = Color(0xFF2C2C2C)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFE2E8F0)
    val TextMuted = Color(0xFF94A3B8)

    val BorderColor = Color(0xFF64748B)

    val StatusCardGradient = Brush.linearGradient(
        colors = listOf(
            AccentGreen.copy(alpha = 0.85f),
            AccentGreenLight.copy(alpha = 0.75f),
            Color(0xFF1A1A1A).copy(alpha = 0.6f),
        ),
    )

    val ActionGrayGradient = Brush.linearGradient(
        colors = listOf(
            CardSurfaceTint.copy(alpha = 0.9f),
            Color(0xFF252525).copy(alpha = 0.85f),
        ),
    )
}

@Composable
fun SmartDoorLockBackground(
    modifier: Modifier = Modifier,
    showPhoto: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val view = LocalView.current
    val baseColors = listOf(
        SmartDoorLockGlass.BackgroundTop,
        SmartDoorLockGlass.BackgroundMid,
        SmartDoorLockGlass.BackgroundBottom,
    )

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SmartDoorLockGlass.BackgroundTop.toArgb()
                window.navigationBarColor = SmartDoorLockGlass.BackgroundBottom.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (showPhoto) {
            Image(
                painter = painterResource(R.drawable.smart_door_lock_connection_lost),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(baseColors)),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = if (showPhoto) {
                            arrayOf(
                                0f to Color.Black.copy(alpha = 0.78f),
                                0.25f to SmartDoorLockGlass.BackgroundMid.copy(alpha = 0.72f),
                                0.55f to Color.Black.copy(alpha = 0.65f),
                                0.85f to SmartDoorLockGlass.AccentGreen.copy(alpha = 0.35f),
                                1f to Color.Black.copy(alpha = 0.88f),
                            )
                        } else {
                            arrayOf(
                                0f to Color.Black.copy(alpha = 0.4f),
                                0.5f to Color.Transparent,
                                1f to SmartDoorLockGlass.AccentGreen.copy(alpha = 0.15f),
                            )
                        },
                    ),
                ),
        )
        content()
    }
}

@Composable
fun SmartDoorLockGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 44.dp,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(SmartDoorLockGlass.CardSurface.copy(alpha = SmartDoorLockGlass.TopBarButtonAlpha))
            .border(
                1.dp,
                SmartDoorLockGlass.BorderColor.copy(alpha = SmartDoorLockGlass.BorderAlpha),
                CircleShape,
            ),
    ) {
        content()
    }
}
