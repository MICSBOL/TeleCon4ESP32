package com.micsbol.telecon4esp32.ui.smarthome

import android.app.Activity
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

object SmartHomeGlass {
    val CardShape = RoundedCornerShape(28.dp)
    val SmallCardShape = RoundedCornerShape(24.dp)
    val PillShape = RoundedCornerShape(50)

    const val SurfaceAlpha = 0.62f
    const val SurfaceAlphaStrong = 0.78f
    const val ChipSurfaceAlpha = 0.50f
    const val BadgeSurfaceAlpha = 0.58f
    const val BorderAlpha = 0.10f
    const val TopBarButtonAlpha = 0.55f

    val BackgroundDeep = Color(0xFF0A0A0B)
    val BackgroundMid = Color(0xFF121214)
    val BackgroundWarm = Color(0xFF1A1612)

    val CardSurface = Color(0xFF1C1C1E)
    val CardSurfaceLight = Color(0xFF2C2C2E)

    val AccentGreen = Color(0xFF4C6644)
    val AccentGreenBright = Color(0xFF5A7A50)
    val AccentGreenMuted = Color(0xFF3D5238)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFABABAB)
    val TextMuted = Color(0xFF6E6E73)

    val AccentWarm = Color(0xFFD4A574)
    val AccentOrange = Color(0xFFE8924A)
    val Warning = Color(0xFFE8924A)
    val Positive = Color(0xFF4C6644)

    val ChartLine = Color(0xFFE8924A)
    val ChartGridAlpha = 0.18f
}

@Composable
fun SmartHomeBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val view = LocalView.current
    val baseColors = listOf(
        SmartHomeGlass.BackgroundDeep,
        SmartHomeGlass.BackgroundMid,
        SmartHomeGlass.BackgroundWarm,
    )

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SmartHomeGlass.BackgroundDeep.toArgb()
                window.navigationBarColor = SmartHomeGlass.BackgroundWarm.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(baseColors)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SmartHomeGlass.AccentGreen.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        radius = 900f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.7f to Color.Black.copy(alpha = 0.12f),
                            1f to Color.Black.copy(alpha = 0.28f),
                        ),
                    ),
                ),
        )
        content()
    }
}

@Composable
fun SmartHomeGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 44.dp,
    content: @Composable () -> Unit,
) {
    com.micsbol.telecon4esp32.ui.components.GlassChromeIconButton(
        onClick = onClick,
        background = SmartHomeGlass.CardSurface.copy(alpha = SmartHomeGlass.TopBarButtonAlpha),
        borderColor = Color.White.copy(alpha = SmartHomeGlass.BorderAlpha),
        modifier = modifier,
        enabled = enabled,
        size = size,
        content = content,
    )
}
