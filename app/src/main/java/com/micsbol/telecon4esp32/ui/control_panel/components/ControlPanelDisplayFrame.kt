package com.micsbol.telecon4esp32.ui.control_panel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.micsbol.telecon4esp32.ui.components.brandPrimary

@Composable
fun ControlPanelDisplayFrame(
    modifier: Modifier = Modifier,
    bezelWidth: Dp = ControlPanelDisplayBezelWidth,
    outerCornerRadius: Dp = ControlPanelDisplayOuterCornerRadius,
    innerCornerRadius: Dp = ControlPanelDisplayInnerCornerRadius,
    chinHeight: Dp = ControlPanelDisplayChinHeight,
    screenBackground: Color = ControlPanelPlasticColors.ScreenBackground,
    accentColor: Color = brandPrimary(),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.drawBehind {
            drawControlPanelDisplayBezel(
                bezelWidth = bezelWidth.toPx(),
                outerCornerRadius = outerCornerRadius.toPx(),
                innerCornerRadius = innerCornerRadius.toPx(),
                chinHeight = chinHeight.toPx(),
                accentColor = accentColor,
            )
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = bezelWidth,
                    end = bezelWidth,
                    top = bezelWidth,
                    bottom = bezelWidth + chinHeight,
                )
                .clip(RoundedCornerShape(innerCornerRadius))
                .background(screenBackground),
            content = content,
        )
    }
}
