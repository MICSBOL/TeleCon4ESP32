package com.micsbol.telecon4esp32.ui.smartlighting

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

object SmartLightingGlass {
    val CardShape = RoundedCornerShape(16.dp)
    val SmallCardShape = RoundedCornerShape(14.dp)
    val PillShape = RoundedCornerShape(50)

    const val SurfaceAlpha = 0.14f
    const val SurfaceAlphaStrong = 0.22f
    const val ChipSurfaceAlpha = 0.18f
    const val BadgeSurfaceAlpha = 0.20f
    const val BorderAlpha = 0.28f
    const val TopBarButtonAlpha = 0.16f

    val BackgroundTop = Color(0xFF0A1020)
    val BackgroundMid = Color(0xFF121A2E)
    val BackgroundBottom = Color(0xFF1A2340)

    val CardSurface = Color(0xFFFFFFFF)
    val CardSurfaceTint = Color(0xFF1E293B)

    val AccentWarm = Color(0xFFF97316)
    val AccentCool = Color(0xFF3B82F6)
    val AccentPurple = Color(0xFFA855F7)
    val AccentCyan = Color(0xFF22D3EE)
    val AccentGreen = Color(0xFF34D399)

    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFFCBD5E1)
    val TextMuted = Color(0xFF94A3B8)

    val BorderColor = Color(0xFF94A3B8)

    val DialGradient = Brush.sweepGradient(
        0f to AccentWarm,
        0.45f to AccentPurple,
        1f to AccentCool,
    )

    val HeroGradient = Brush.linearGradient(
        colors = listOf(
            AccentPurple.copy(alpha = 0.55f),
            AccentCool.copy(alpha = 0.45f),
            AccentWarm.copy(alpha = 0.35f),
        ),
    )
}

@Composable
fun SmartLightingBackground(
    modifier: Modifier = Modifier,
    showPhoto: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val view = LocalView.current
    val baseColors = listOf(
        SmartLightingGlass.BackgroundTop,
        SmartLightingGlass.BackgroundMid,
        SmartLightingGlass.BackgroundBottom,
    )

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SmartLightingGlass.BackgroundTop.toArgb()
                window.navigationBarColor = SmartLightingGlass.BackgroundBottom.toArgb()
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
                painter = painterResource(R.drawable.smart_lighting_background),
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
                                0f to SmartLightingGlass.BackgroundTop.copy(alpha = 0.72f),
                                0.18f to SmartLightingGlass.BackgroundMid.copy(alpha = 0.58f),
                                0.45f to Color.Black.copy(alpha = 0.42f),
                                0.72f to SmartLightingGlass.BackgroundBottom.copy(alpha = 0.68f),
                                1f to SmartLightingGlass.BackgroundTop.copy(alpha = 0.88f),
                            )
                        } else {
                            arrayOf(
                                0f to Color.Black.copy(alpha = 0.35f),
                                0.5f to Color.Transparent,
                                1f to SmartLightingGlass.AccentCool.copy(alpha = 0.12f),
                            )
                        },
                    ),
                ),
        )
        content()
    }
}

@Composable
fun SmartLightingGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 44.dp,
    content: @Composable () -> Unit,
) {
    com.micsbol.telecon4esp32.ui.components.GlassChromeIconButton(
        onClick = onClick,
        background = SmartLightingGlass.CardSurface.copy(alpha = SmartLightingGlass.TopBarButtonAlpha),
        borderColor = SmartLightingGlass.BorderColor.copy(alpha = SmartLightingGlass.BorderAlpha),
        modifier = modifier,
        enabled = enabled,
        size = size,
        content = content,
    )
}
