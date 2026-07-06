package com.micsbol.telecon4esp32.ui.smartlighting.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.smartlighting.SmartLightingGlass

@Composable
fun SmartLightingCard(
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
    strongGlass: Boolean = false,
    elevated: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val surfaceAlpha = if (strongGlass) {
        SmartLightingGlass.SurfaceAlphaStrong
    } else {
        SmartLightingGlass.SurfaceAlpha
    }

    Column(
        modifier = modifier
            .then(
                if (elevated) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = SmartLightingGlass.CardShape,
                        ambientColor = SmartLightingGlass.AccentCool.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.35f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(SmartLightingGlass.CardShape)
            .background(SmartLightingGlass.CardSurface.copy(alpha = surfaceAlpha))
            .border(
                width = 1.dp,
                color = SmartLightingGlass.BorderColor.copy(alpha = SmartLightingGlass.BorderAlpha),
                shape = SmartLightingGlass.CardShape,
            )
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier),
        content = content,
    )
}
