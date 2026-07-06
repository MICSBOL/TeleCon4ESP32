package com.micsbol.telecon4esp32.ui.solarsystem

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.app.Activity
import androidx.core.view.WindowCompat

object SolarGlass {
    val CardShape = RoundedCornerShape(28.dp)
    val SmallCardShape = RoundedCornerShape(24.dp)
    val PillShape = RoundedCornerShape(50)

    const val SurfaceAlpha = 0.72f
    const val SurfaceAlphaStrong = 0.80f
    const val ChipSurfaceAlpha = 0.66f
    const val BadgeSurfaceAlpha = 0.74f
    const val BorderAlpha = 0.90f
    const val TopBarButtonAlpha = 0.72f

    val SkyDeep = Color(0xFF1E3A5F)
    val SkyMid = Color(0xFF3B82C4)
    val SunGold = Color(0xFFFFB703)
    val SunBright = Color(0xFFFFD60A)
    val DawnOrange = Color(0xFFFB8500)
    val CloudLight = Color(0xFFE8F4FD)

    val TextOnGlassPrimary = Color(0xFF1A2E44)
    val TextOnGlassSecondary = Color(0xFF3D5A73)
    val TextOnGlassMuted = Color(0xFF5C7A94)

    val TextOnBackgroundPrimary = Color(0xFFFFFFFF)
    val TextOnBackgroundSecondary = Color(0xFFEAF4FF)

    val AccentAmber = Color(0xFFFFB703)
    val AccentGold = Color(0xFFFFD60A)
    val Positive = Color(0xFF2A9D8F)
    val Warning = Color(0xFFE76F51)
    val GridExport = Color(0xFF8B5CF6)
    val GridImport = Color(0xFF3B82F6)

    val BadgeBackground = Color(0xFFF7FBFF)
    val ChipBackground = Color(0xFFE8F2FA)

    val ChartProduced = Color(0xFF007AFF)
    val ChartProducedFaded = Color(0xFF007AFF)
    val ChartConsumed = Color(0xFF785EF0)
    val ChartConsumedFaded = Color(0xFF785EF0)
    val ChartBaselineDot = Color(0xFF93C5FD)

    const val ChartFadedAlpha = 0.18f
    const val ChartHighlightAlpha = 1f

    const val ChartGridAlpha = 0.20f
    const val ChartGridMajorAlpha = 0.34f
    const val ChartSegmentAlpha = 0.07f
}

@Composable
fun SolarBackground(
    solarPowerW: Int,
    maxSolarW: Int = 1200,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val view = LocalView.current
    val intensity = remember(solarPowerW, maxSolarW) {
        (solarPowerW.toFloat() / maxSolarW.coerceAtLeast(1)).coerceIn(0f, 1f)
    }
    val baseColors = remember(intensity) {
        when {
            intensity < 0.15f -> listOf(
                Color(0xFF2B1B3D),
                Color(0xFF4A3060),
                Color(0xFF6B4E71),
            )
            intensity < 0.45f -> listOf(
                Color(0xFF4A6FA5),
                Color(0xFF7EB8DA),
                Color(0xFFB8D8E8),
            )
            intensity < 0.75f -> listOf(
                Color(0xFF5B9BD5),
                Color(0xFF87CEEB),
                Color(0xFFFFF4C2),
            )
            else -> listOf(
                Color(0xFF4A90D9),
                Color(0xFFFFD60A),
                Color(0xFFFFB703),
            )
        }
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = baseColors.first().toArgb()
                window.navigationBarColor = baseColors.last().toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = intensity > 0.5f
                    isAppearanceLightNavigationBars = intensity > 0.5f
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
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to SolarGlass.SunBright.copy(alpha = 0.08f + intensity * 0.18f),
                            0.45f to Color.White.copy(alpha = 0.05f + intensity * 0.08f),
                            1f to Color.Black.copy(alpha = 0.20f + (1f - intensity) * 0.18f),
                        ),
                    ),
                ),
        )
        content()
    }
}

@Composable
fun SolarGlassIconButton(
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
            .background(SolarGlass.BadgeBackground.copy(alpha = SolarGlass.TopBarButtonAlpha))
            .border(1.dp, Color.White.copy(alpha = SolarGlass.BorderAlpha), CircleShape),
    ) {
        content()
    }
}
