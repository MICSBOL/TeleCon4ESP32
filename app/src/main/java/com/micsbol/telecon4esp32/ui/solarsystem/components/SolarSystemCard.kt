package com.micsbol.telecon4esp32.ui.solarsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.solarsystem.SolarGlass

@Composable
fun SolarSystemCard(
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
    strongGlass: Boolean = false,
    elevated: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val surfaceAlpha = if (strongGlass) {
        SolarGlass.SurfaceAlphaStrong
    } else {
        SolarGlass.SurfaceAlpha
    }

    Column(
        modifier = modifier
            .then(
                if (elevated) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = SolarGlass.CardShape,
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.10f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(SolarGlass.CardShape)
            .background(SolarGlass.BadgeBackground.copy(alpha = surfaceAlpha))
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = SolarGlass.BorderAlpha),
                shape = SolarGlass.CardShape,
            )
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
            .padding(18.dp),
        content = content,
    )
}
