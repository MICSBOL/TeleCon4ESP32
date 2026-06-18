package com.micsbol.telecon4esp32.ui.smarthome.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.components.EmitterCardShape

@Composable
fun SmartHomeCard(
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val elevation = if (isSystemInDarkTheme()) 0.dp else 3.dp

    Surface(
        modifier = modifier.shadow(elevation, EmitterCardShape),
        shape = EmitterCardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSystemInDarkTheme()) 2.dp else 0.dp,
        content = {
            Column(
                modifier = Modifier
                    .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
                    .padding(16.dp),
                content = content,
            )
        },
    )
}
