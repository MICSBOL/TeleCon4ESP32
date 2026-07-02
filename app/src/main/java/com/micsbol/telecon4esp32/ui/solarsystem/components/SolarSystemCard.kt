package com.micsbol.telecon4esp32.ui.solarsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.cyber.components.CyberPanel

@Composable
fun SolarSystemCard(
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    CyberPanel(
        modifier = modifier,
        chamfer = 12.dp,
        glowIntensity = 0.7f,
        cornerTicks = false,
        contentPadding = 16.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier),
            content = content,
        )
    }
}
