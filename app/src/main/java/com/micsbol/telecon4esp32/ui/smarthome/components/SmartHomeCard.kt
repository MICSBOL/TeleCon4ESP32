package com.micsbol.telecon4esp32.ui.smarthome.components

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
import com.micsbol.telecon4esp32.ui.smarthome.SmartHomeGlass

@Composable
fun SmartHomeCard(
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
    strongGlass: Boolean = false,
    elevated: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val surfaceAlpha = if (strongGlass) {
        SmartHomeGlass.SurfaceAlphaStrong
    } else {
        SmartHomeGlass.SurfaceAlpha
    }

    Column(
        modifier = modifier
            .then(
                if (elevated) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = SmartHomeGlass.CardShape,
                        ambientColor = Color.Black.copy(alpha = 0.35f),
                        spotColor = Color.Black.copy(alpha = 0.25f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(SmartHomeGlass.CardShape)
            .background(SmartHomeGlass.CardSurface.copy(alpha = surfaceAlpha))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = SmartHomeGlass.BorderAlpha),
                shape = SmartHomeGlass.CardShape,
            )
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
            .padding(16.dp),
        content = content,
    )
}
