package com.micsbol.telecon4esp32.ui.control_panel.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object ControlPanelPlasticColors {
    val Highlight = Color(0xFF6E6E6E)
    val Mid = Color(0xFF434343)
    val Shadow = Color(0xFF242424)
    val RimDark = Color(0xFF1A1A1A)
    val ScreenBackground = Color(0xFF0A0F1A)
    val AccentGlow = Color(0xFF2DD4BF)
}

fun DrawScope.drawPlasticRaisedRoundRect(
    topLeft: Offset,
    size: Size,
    cornerRadius: CornerRadius,
) {
    drawRoundRect(
        color = ControlPanelPlasticColors.RimDark,
        topLeft = topLeft,
        size = size,
        cornerRadius = cornerRadius,
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                ControlPanelPlasticColors.Highlight,
                ControlPanelPlasticColors.Mid,
                ControlPanelPlasticColors.Shadow,
            ),
            start = topLeft,
            end = topLeft + Offset(size.width, size.height),
        ),
        topLeft = topLeft + Offset(1.5f, 1.5f),
        size = Size(
            width = (size.width - 3f).coerceAtLeast(0f),
            height = (size.height - 3f).coerceAtLeast(0f),
        ),
        cornerRadius = CornerRadius(
            x = (cornerRadius.x - 1.5f).coerceAtLeast(0f),
            y = (cornerRadius.y - 1.5f).coerceAtLeast(0f),
        ),
    )
}

fun DrawScope.drawControlPanelDisplayBezel(
    bezelWidth: Float,
    outerCornerRadius: Float,
    innerCornerRadius: Float,
    chinHeight: Float = 0f,
    accentColor: Color = ControlPanelPlasticColors.AccentGlow,
) {
    val outerRoundRect = RoundRect(
        rect = Rect(0f, 0f, size.width, size.height),
        cornerRadius = CornerRadius(outerCornerRadius, outerCornerRadius),
    )
    val innerRoundRect = RoundRect(
        rect = Rect(
            left = bezelWidth,
            top = bezelWidth,
            right = size.width - bezelWidth,
            bottom = size.height - bezelWidth - chinHeight,
        ),
        cornerRadius = CornerRadius(innerCornerRadius, innerCornerRadius),
    )

    val bezelPath = Path().apply {
        addRoundRect(outerRoundRect)
        addRoundRect(innerRoundRect)
        fillType = PathFillType.EvenOdd
    }

    drawPath(
        path = bezelPath,
        brush = Brush.linearGradient(
            colors = listOf(
                ControlPanelPlasticColors.Highlight,
                ControlPanelPlasticColors.Mid,
                ControlPanelPlasticColors.Shadow,
                ControlPanelPlasticColors.RimDark,
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height),
        ),
    )

    drawRoundRect(
        color = ControlPanelPlasticColors.RimDark.copy(alpha = 0.85f),
        topLeft = Offset(0f, 0f),
        size = Size(size.width, size.height),
        cornerRadius = CornerRadius(outerCornerRadius, outerCornerRadius),
        style = Stroke(width = 2f),
    )

    drawRoundRect(
        color = ControlPanelPlasticColors.Highlight.copy(alpha = 0.55f),
        topLeft = Offset(2f, 2f),
        size = Size(
            width = (size.width - 4f).coerceAtLeast(0f),
            height = (size.height - 4f).coerceAtLeast(0f),
        ),
        cornerRadius = CornerRadius(
            x = (outerCornerRadius - 2f).coerceAtLeast(0f),
            y = (outerCornerRadius - 2f).coerceAtLeast(0f),
        ),
        style = Stroke(width = 1.5f),
    )

    val innerTopLeft = Offset(innerRoundRect.left, innerRoundRect.top)
    val innerSize = Size(innerRoundRect.width, innerRoundRect.height)
    val innerCorners = CornerRadius(innerCornerRadius, innerCornerRadius)

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.55f),
        topLeft = innerTopLeft + Offset(1f, 1f),
        size = innerSize,
        cornerRadius = innerCorners,
        style = Stroke(width = 2.5f),
    )

    drawRoundRect(
        color = accentColor.copy(alpha = 0.45f),
        topLeft = innerTopLeft,
        size = innerSize,
        cornerRadius = innerCorners,
        style = Stroke(width = 1.25f),
    )

    drawRoundRect(
        color = ControlPanelPlasticColors.Highlight.copy(alpha = 0.25f),
        topLeft = innerTopLeft + Offset(0f, innerSize.height - 3f),
        size = Size(innerSize.width, 2f),
        cornerRadius = CornerRadius(1f, 1f),
    )
}

val ControlPanelDisplayBezelWidth: Dp = 14.dp
val ControlPanelDisplayOuterCornerRadius: Dp = 16.dp
val ControlPanelDisplayInnerCornerRadius: Dp = 6.dp
val ControlPanelDisplayChinHeight: Dp = 18.dp
