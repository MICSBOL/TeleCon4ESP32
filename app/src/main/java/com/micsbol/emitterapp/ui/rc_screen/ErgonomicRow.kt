package com.micsbol.emitterapp.ui.rc_screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout

@Composable
fun ErgonomicRow(
    modifier: Modifier = Modifier,
    centerContent: @Composable () -> Unit,
    leftSideContent: @Composable () -> Unit,
    rightSideContent: @Composable () -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->

        val sideMaxWidth = (constraints.maxWidth * 0.7f).toInt()
        val sideConstraints = constraints.copy(minWidth = 0, maxWidth = sideMaxWidth)
        val leftPlaceable =
            subcompose("left") { leftSideContent() }.first().measure(sideConstraints)
        val rightPlaceable =
            subcompose("right") { rightSideContent() }.first().measure(sideConstraints)
        val centerWidth = constraints.maxWidth - leftPlaceable.width - rightPlaceable.width
        val coercedCenterWidth = centerWidth.coerceAtLeast(0)
        val centerPlaceable = subcompose("center") { centerContent() }
            .first()
            .measure(
                constraints.copy(
                    minWidth = coercedCenterWidth,
                    maxWidth = coercedCenterWidth
                )
            )
        layout(constraints.maxWidth, constraints.maxHeight) {
            leftPlaceable.placeRelative(0, 0)
            centerPlaceable.placeRelative(leftPlaceable.width, 0)
            rightPlaceable.placeRelative(leftPlaceable.width + centerPlaceable.width, 0)
        }
    }
}