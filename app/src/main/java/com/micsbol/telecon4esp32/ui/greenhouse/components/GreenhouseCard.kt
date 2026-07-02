package com.micsbol.telecon4esp32.ui.greenhouse.components

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
import com.micsbol.telecon4esp32.ui.greenhouse.GreenhouseGlass

@Composable
fun GreenhouseCard(
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
    strongGlass: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val surfaceAlpha = if (strongGlass) {
        GreenhouseGlass.SurfaceAlphaStrong
    } else {
        GreenhouseGlass.SurfaceAlpha
    }

    Column(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = GreenhouseGlass.CardShape,
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.10f),
            )
            .clip(GreenhouseGlass.CardShape)
            .background(GreenhouseGlass.BadgeBackground.copy(alpha = surfaceAlpha))
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = GreenhouseGlass.BorderAlpha),
                shape = GreenhouseGlass.CardShape,
            )
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
            .padding(18.dp),
        content = content,
    )
}
