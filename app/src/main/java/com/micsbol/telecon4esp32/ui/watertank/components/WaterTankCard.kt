package com.micsbol.telecon4esp32.ui.watertank.components

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
import com.micsbol.telecon4esp32.ui.watertank.WaterTankGlass

@Composable
fun WaterTankCard(
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
    strongGlass: Boolean = false,
    elevated: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val surfaceAlpha = if (strongGlass) {
        WaterTankGlass.SurfaceAlphaStrong
    } else {
        WaterTankGlass.SurfaceAlpha
    }

    Column(
        modifier = modifier
            .then(
                if (elevated) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = WaterTankGlass.CardShape,
                        ambientColor = WaterTankGlass.AccentCyan.copy(alpha = 0.10f),
                        spotColor = Color.Black.copy(alpha = 0.06f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(WaterTankGlass.CardShape)
            .background(WaterTankGlass.CardSurface.copy(alpha = surfaceAlpha))
            .border(
                width = 1.dp,
                color = WaterTankGlass.BorderColor.copy(alpha = WaterTankGlass.BorderAlpha),
                shape = WaterTankGlass.CardShape,
            )
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
            .padding(16.dp),
        content = content,
    )
}
