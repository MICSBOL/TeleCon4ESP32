package com.micsbol.telecon4esp32.ui.cyber.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.cyber.theme.CyberColors
import com.micsbol.telecon4esp32.ui.cyber.theme.CyberType

/**
 * Convenience [CyberPanel] wrapper that exposes a [ColumnScope] body, so it can
 * be dropped in where a vertical content card is needed across the shell screens.
 */
@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    chamfer: Dp = 12.dp,
    contentPadding: Dp = 16.dp,
    glowIntensity: Float = 0.7f,
    content: @Composable ColumnScope.() -> Unit,
) {
    CyberPanel(
        modifier = modifier.fillMaxWidth(),
        chamfer = chamfer,
        glowIntensity = glowIntensity,
        contentPadding = contentPadding,
        cornerTicks = false,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

/** Section heading in the cyber type style, matching the dashboard look. */
@Composable
fun CyberSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(bottom = 8.dp, start = 4.dp),
        style = CyberType.SectionTitle.copy(color = CyberColors.NeonPrimary),
    )
}
