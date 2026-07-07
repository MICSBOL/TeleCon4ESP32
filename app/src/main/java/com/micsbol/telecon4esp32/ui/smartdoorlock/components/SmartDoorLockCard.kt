package com.micsbol.telecon4esp32.ui.smartdoorlock.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.smartdoorlock.SmartDoorLockGlass

enum class SmartDoorLockCardStyle {
    Glass,
    Green,
    Gray,
}

@Composable
fun SmartDoorLockCard(
    modifier: Modifier = Modifier,
    style: SmartDoorLockCardStyle = SmartDoorLockCardStyle.Glass,
    fillHeight: Boolean = false,
    elevated: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = when (style) {
        SmartDoorLockCardStyle.Glass -> SmartDoorLockGlass.CardShape
        SmartDoorLockCardStyle.Green,
        SmartDoorLockCardStyle.Gray,
        -> SmartDoorLockGlass.SmallCardShape
    }

    val backgroundModifier = when (style) {
        SmartDoorLockCardStyle.Glass -> Modifier.background(
            SmartDoorLockGlass.CardSurface.copy(alpha = SmartDoorLockGlass.SurfaceAlpha),
        )
        SmartDoorLockCardStyle.Green -> Modifier.background(SmartDoorLockGlass.StatusCardGradient)
        SmartDoorLockCardStyle.Gray -> Modifier.background(SmartDoorLockGlass.ActionGrayGradient)
    }

    Column(
        modifier = modifier
            .then(
                if (elevated) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = shape,
                        ambientColor = SmartDoorLockGlass.AccentGreen.copy(alpha = 0.15f),
                        spotColor = Color.Black.copy(alpha = 0.4f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .then(backgroundModifier)
            .border(
                width = 1.dp,
                color = SmartDoorLockGlass.BorderColor.copy(
                    alpha = if (style == SmartDoorLockCardStyle.Glass) {
                        SmartDoorLockGlass.BorderAlpha
                    } else {
                        0.18f
                    },
                ),
                shape = shape,
            )
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier),
        content = content,
    )
}

@Composable
fun SmartDoorLockActionCard(
    modifier: Modifier = Modifier,
    style: SmartDoorLockCardStyle,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(SmartDoorLockGlass.SmallCardShape)
            .then(
                when (style) {
                    SmartDoorLockCardStyle.Green -> Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(
                                SmartDoorLockGlass.AccentGreenLight,
                                SmartDoorLockGlass.AccentGreen,
                            ),
                        ),
                    )
                    SmartDoorLockCardStyle.Gray -> Modifier.background(SmartDoorLockGlass.ActionGrayGradient)
                    SmartDoorLockCardStyle.Glass -> Modifier.background(
                        SmartDoorLockGlass.CardSurface.copy(alpha = SmartDoorLockGlass.SurfaceAlphaStrong),
                    )
                },
            )
            .border(
                1.dp,
                SmartDoorLockGlass.BorderColor.copy(alpha = 0.2f),
                SmartDoorLockGlass.SmallCardShape,
            ),
        content = content,
    )
}
