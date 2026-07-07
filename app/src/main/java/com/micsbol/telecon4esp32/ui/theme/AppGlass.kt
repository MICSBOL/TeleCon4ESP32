package com.micsbol.telecon4esp32.ui.theme

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.micsbol.telecon4esp32.R

/**
 * Dark glassmorphism design tokens for shared app screens.
 *
 * Pro application dashboards keep their own per-app Glass tokens; this palette
 * applies only to the general shell (home, applications list, settings, etc.).
 */
object AppGlass {
    val CardShape = RoundedCornerShape(24.dp)
    val SmallCardShape = RoundedCornerShape(16.dp)
    val PillShape = RoundedCornerShape(16.dp)

    const val SurfaceAlpha = 0.12f
    const val SurfaceAlphaStrong = 0.20f
    const val ChipSurfaceAlpha = 0.16f
    const val BorderAlpha = 0.22f
    const val TopBarButtonAlpha = 0.14f
    const val DialogSurfaceAlpha = 0.90f

    val BackgroundTop = Color(0xFF1A1A1A)
    val BackgroundMid = Color(0xFF232323)
    val BackgroundBottom = Color(0xFF2D2D2D)

    val CardSurface = Color(0xFFFFFFFF)
    val DialogSurface = Color(0xFF252525)

    val Accent = Color(0xFFFF5F1F)
    val AccentDark = Color(0xFFE04A0A)
    val AccentPressed = Color(0xFFCC3D00)
    val AccentHighlight = Color(0xFFFF8A4C)
    val OnAccent = Color(0xFFFFFFFF)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA0A0A0)
    val TextMuted = Color(0xFF707070)

    val BorderColor = Color(0xFFCCCCCC)

    val Positive = Color(0xFF53D86A)
    val Negative = Color(0xFFFF5A5A)
    val Warning = Color(0xFFF7B955)

    val accentGradient: Brush
        get() = Brush.horizontalGradient(listOf(AccentHighlight, Accent, AccentDark))

    val accentVerticalGradient: Brush
        get() = Brush.verticalGradient(
            0f to AccentHighlight,
            0.45f to Accent,
            1f to AccentDark,
        )
}

@Composable
fun AppGlassBackground(
    modifier: Modifier = Modifier,
    showPhoto: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val view = LocalView.current
    val baseColors = listOf(
        AppGlass.BackgroundTop,
        AppGlass.BackgroundMid,
        AppGlass.BackgroundBottom,
    )

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = AppGlass.BackgroundTop.toArgb()
                window.navigationBarColor = AppGlass.BackgroundBottom.toArgb()
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
                painter = painterResource(R.drawable.bg_telecon_wireless_link),
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
                                0f to AppGlass.BackgroundTop.copy(alpha = 0.78f),
                                0.15f to AppGlass.BackgroundMid.copy(alpha = 0.62f),
                                0.45f to Color.Black.copy(alpha = 0.48f),
                                0.75f to AppGlass.BackgroundBottom.copy(alpha = 0.70f),
                                1f to AppGlass.BackgroundTop.copy(alpha = 0.85f),
                            )
                        } else {
                            arrayOf(
                                0f to Color.Black.copy(alpha = 0.35f),
                                0.5f to Color.Transparent,
                                1f to AppGlass.Accent.copy(alpha = 0.08f),
                            )
                        },
                    ),
                ),
        )
        content()
    }
}
