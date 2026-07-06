package com.micsbol.telecon4esp32.ui.watertank

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

object WaterTankGlass {
    val CardShape = RoundedCornerShape(16.dp)
    val SmallCardShape = RoundedCornerShape(14.dp)
    val PillShape = RoundedCornerShape(50)

    const val SurfaceAlpha = 0.55f
    const val SurfaceAlphaStrong = 0.64f
    const val ChipSurfaceAlpha = 0.50f
    const val BadgeSurfaceAlpha = 0.52f
    const val BorderAlpha = 0.22f
    const val TopBarButtonAlpha = 0.58f

    val BackgroundTop = Color(0xFFEAF6FD)
    val BackgroundMid = Color(0xFFD6EDFA)
    val BackgroundBottom = Color(0xFFC4E4F6)

    val CardSurface = Color(0xFFFFFFFF)
    val CardSurfaceLight = Color(0xFFF0F9FF)

    val AccentCyan = Color(0xFF0EA5E9)
    val AccentCyanBright = Color(0xFF0284C7)
    val AccentCyanMuted = Color(0xFFBAE6FD)

    val TextPrimary = Color(0xFF0F2942)
    val TextSecondary = Color(0xFF4A6B85)
    val TextMuted = Color(0xFF7A96AD)

    val Warning = Color(0xFFD97706)
    val Danger = Color(0xFFDC2626)
    val Positive = Color(0xFF059669)

    val ChartLine = Color(0xFF0EA5E9)
    val ChartGridAlpha = 0.22f

    val BorderColor = Color(0xFF7DD3FC)
}

@Composable
fun WaterTankBackground(
    modifier: Modifier = Modifier,
    showPhoto: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val view = LocalView.current
    val baseColors = listOf(
        WaterTankGlass.BackgroundTop,
        WaterTankGlass.BackgroundMid,
        WaterTankGlass.BackgroundBottom,
    )

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = WaterTankGlass.BackgroundTop.toArgb()
                window.navigationBarColor = WaterTankGlass.BackgroundBottom.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (showPhoto) {
            Image(
                painter = painterResource(R.drawable.water_tank_background),
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
                                0f to Color.White.copy(alpha = 0.38f),
                                0.22f to Color.White.copy(alpha = 0.14f),
                                0.50f to Color.Transparent,
                                0.78f to WaterTankGlass.AccentCyanMuted.copy(alpha = 0.12f),
                                1f to WaterTankGlass.BackgroundBottom.copy(alpha = 0.28f),
                            )
                        } else {
                            arrayOf(
                                0f to Color.White.copy(alpha = 0.55f),
                                0.5f to Color.Transparent,
                                1f to WaterTankGlass.AccentCyan.copy(alpha = 0.10f),
                            )
                        },
                    ),
                ),
        )
        content()
    }
}

@Composable
fun WaterTankGlassIconButton(
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
            .background(WaterTankGlass.CardSurface.copy(alpha = WaterTankGlass.TopBarButtonAlpha))
            .border(
                1.dp,
                WaterTankGlass.BorderColor.copy(alpha = WaterTankGlass.BorderAlpha),
                CircleShape,
            ),
    ) {
        content()
    }
}
