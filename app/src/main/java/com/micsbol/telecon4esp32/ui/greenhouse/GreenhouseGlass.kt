package com.micsbol.telecon4esp32.ui.greenhouse

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R

object GreenhouseGlass {
    val CardShape = RoundedCornerShape(28.dp)
    val SmallCardShape = RoundedCornerShape(24.dp)
    val PillShape = RoundedCornerShape(50)

    const val SurfaceAlpha = 0.72f
    const val SurfaceAlphaStrong = 0.80f
    const val ChipSurfaceAlpha = 0.66f
    const val BadgeSurfaceAlpha = 0.74f
    const val BorderAlpha = 0.90f
    const val TopBarButtonAlpha = 0.72f

    val ForestDeep = Color(0xFF1B4332)
    val ForestMid = Color(0xFF2D6A4F)
    val LeafBright = Color(0xFF52B788)
    val LeafLime = Color(0xFF95D5B2)
    val MeadowLight = Color(0xFFD8F3DC)

    /** Lighter green copy on bright frosted surfaces. */
    val TextOnGlassPrimary = Color(0xFF1B4332)
    val TextOnGlassSecondary = Color(0xFF2D6A4F)
    val TextOnGlassMuted = Color(0xFF40916C)

    /** Copy over the photo background (top bar). */
    val TextOnBackgroundPrimary = Color(0xFFFFFFFF)
    val TextOnBackgroundSecondary = Color(0xFFEAF5ED)

    val TextPrimary = TextOnGlassPrimary
    val TextSecondary = TextOnGlassSecondary

    val AccentGreen = Color(0xFF52B788)
    val AccentLime = Color(0xFF74C69D)
    val WarningOrange = Color(0xFFF4A261)

    val ValueGreen = Color(0xFF40916C)
    val ValueTeal = Color(0xFF52B788)
    val ValueOrange = Color(0xFFE07A2F)

    val TempGradient = listOf(
        Color(0xFF52B788),
        Color(0xFF95D5B2),
        Color(0xFFF4D35E),
        Color(0xFFF4A261),
    )

    val ChartGreen = Color(0xFF40916C)
    val ChartGreenLight = Color(0xFFB7E4C7)
    val ChartHumidity = Color(0xFF52B788)
    val ChartTemp = Color(0xFFF4A261)
    val ChartLabel = TextOnGlassSecondary

    /** Chart time labels on frosted surfaces. */
    val ChartAxisTime = TextOnGlassMuted

    val BadgeBackground = Color(0xFFF7FFF9)
    val ChipBackground = Color(0xFFE8F5EC)
}

@Composable
fun GreenhouseBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.greenhouse_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.42f),
                            0.18f to Color.Black.copy(alpha = 0.18f),
                            0.42f to Color.White.copy(alpha = 0.16f),
                            0.62f to Color.White.copy(alpha = 0.12f),
                            0.82f to Color.Black.copy(alpha = 0.26f),
                            1f to Color.Black.copy(alpha = 0.52f),
                        ),
                    ),
                ),
        )
        content()
    }
}

@Composable
fun GreenhouseGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(GreenhouseGlass.BadgeBackground.copy(alpha = GreenhouseGlass.TopBarButtonAlpha))
            .border(1.dp, Color.White.copy(alpha = GreenhouseGlass.BorderAlpha), CircleShape),
    ) {
        content()
    }
}
