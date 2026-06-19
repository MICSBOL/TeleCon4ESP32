package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.components.EmitterCardShape
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.components.brandSecondary
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProGlass

enum class RcGlassAccentEdge {
    START,
    END,
}

@Composable
fun RcGlassCard(
    modifier: Modifier = Modifier,
    surfaceAlpha: Float = RcVehicleProGlass.SURFACE_ALPHA,
    accentEdge: RcGlassAccentEdge = RcGlassAccentEdge.START,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = surfaceAlpha)

    Surface(
        modifier = modifier.clip(EmitterCardShape),
        shape = EmitterCardShape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            if (accentEdge == RcGlassAccentEdge.START) {
                RcGlassAccentBar()
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(surfaceColor)
                    .padding(contentPadding),
                content = content,
            )
            if (accentEdge == RcGlassAccentEdge.END) {
                RcGlassAccentBar()
            }
        }
    }
}

@Composable
private fun RcGlassAccentBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(4.dp)
            .fillMaxHeight()
            .background(
                Brush.verticalGradient(
                    listOf(
                        brandPrimary().copy(alpha = 0.9f),
                        brandSecondary().copy(alpha = 0.9f),
                    ),
                ),
            ),
    )
}
