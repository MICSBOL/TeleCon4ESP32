package com.micsbol.telecon4esp32.ui.rc_vehicle_pro.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal enum class RcHudChevronDirection {
    Up,
    Down,
    Start,
    End,
}

internal fun Modifier.hudVerticalSwipe(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    hideTowardBottom: Boolean = false,
): Modifier = hudEdgeSwipe(
    expanded = expanded,
    onExpandedChange = onExpandedChange,
    orientation = Orientation.Vertical,
    hideTowardPositive = hideTowardBottom,
)

internal fun Modifier.hudHorizontalSwipe(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    hideTowardEnd: Boolean,
): Modifier = hudEdgeSwipe(
    expanded = expanded,
    onExpandedChange = onExpandedChange,
    orientation = Orientation.Horizontal,
    hideTowardPositive = hideTowardEnd,
)

private fun Modifier.hudEdgeSwipe(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    orientation: Orientation,
    hideTowardPositive: Boolean,
): Modifier = composed {
    val thresholdPx = with(LocalDensity.current) {
        RcVehicleProLayout.HudChromeSwipeThreshold.toPx()
    }
    val accumulated = remember { mutableFloatStateOf(0f) }
    val dragState = rememberDraggableState { delta ->
        accumulated.floatValue += delta
    }
    draggable(
        state = dragState,
        orientation = orientation,
        onDragStarted = { accumulated.floatValue = 0f },
        onDragStopped = { velocity ->
            val hideByDrag = if (hideTowardPositive) {
                accumulated.floatValue > thresholdPx
            } else {
                accumulated.floatValue < -thresholdPx
            }
            val showByDrag = if (hideTowardPositive) {
                accumulated.floatValue < -thresholdPx
            } else {
                accumulated.floatValue > thresholdPx
            }
            val hideByFling = if (hideTowardPositive) {
                velocity > RcVehicleProLayout.HUD_CHROME_FLING_VELOCITY
            } else {
                velocity < -RcVehicleProLayout.HUD_CHROME_FLING_VELOCITY
            }
            val showByFling = if (hideTowardPositive) {
                velocity < -RcVehicleProLayout.HUD_CHROME_FLING_VELOCITY
            } else {
                velocity > RcVehicleProLayout.HUD_CHROME_FLING_VELOCITY
            }
            when {
                expanded && (hideByFling || hideByDrag) -> onExpandedChange(false)
                !expanded && (showByFling || showByDrag) -> onExpandedChange(true)
            }
            accumulated.floatValue = 0f
        },
    )
}

internal fun RcHudChevronDirection.chevronIcon(): ImageVector = when (this) {
    RcHudChevronDirection.Down -> Icons.Filled.KeyboardDoubleArrowDown
    RcHudChevronDirection.Up -> Icons.Filled.KeyboardDoubleArrowUp
    RcHudChevronDirection.Start -> Icons.Filled.KeyboardDoubleArrowLeft
    RcHudChevronDirection.End -> Icons.Filled.KeyboardDoubleArrowRight
}

private fun RcHudChevronDirection.handlePadding(): Modifier {
    val isHorizontal = this == RcHudChevronDirection.Start || this == RcHudChevronDirection.End
    return if (isHorizontal) {
        Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    } else {
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    }
}

/**
 * Representative icon for a hidden HUD feature. It fades in and out on its own
 * draw node so the rest of the HUD does not recompose.
 */
@Composable
internal fun RcHudRevealIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    appearDelayMs: Long = 0L,
) {
    val painter = rememberVectorPainter(icon)
    val themeColor = brandPrimary()
    Spacer(
        modifier = modifier
            .size(RcHudRevealIconSize)
            .revealIcon(painter, themeColor, appearDelayMs),
    )
}

private fun Modifier.revealIcon(painter: Painter, color: Color, appearDelayMs: Long): Modifier =
    this.then(RevealIconElement(painter, color, appearDelayMs))

private data class RevealIconElement(
    val painter: Painter,
    val color: Color,
    val appearDelayMs: Long,
) : ModifierNodeElement<RevealIconNode>() {
    override fun create(): RevealIconNode = RevealIconNode(painter, color, appearDelayMs)

    override fun update(node: RevealIconNode) {
        node.painter = painter
        node.color = color
        node.appearDelayMs = appearDelayMs
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "rcHudRevealIcon"
    }
}

private class RevealIconNode(
    var painter: Painter,
    var color: Color,
    var appearDelayMs: Long,
) : Modifier.Node(), DrawModifierNode {
    private var reveal = 0f

    override fun onAttach() {
        coroutineScope.launch {
            while (isActive) {
                val elapsedMs = withFrameNanos { it } / 1_000_000L
                val phase = elapsedMs % GRID_APPEAR_PERIOD_MS
                val nextReveal = gridReveal(phase, appearDelayMs)
                if (nextReveal > 0f) {
                    reveal = nextReveal
                    invalidateDraw()
                } else {
                    if (reveal != 0f) {
                        reveal = 0f
                        invalidateDraw()
                    }
                    delay(millisUntilGridVisible(phase, appearDelayMs))
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        if (reveal <= 0f) return
        with(painter) {
            draw(
                size = size,
                alpha = reveal,
                colorFilter = ColorFilter.tint(color),
            )
        }
    }
}

/**
 * Linear fade-in, short hold, then a linear fade-out.
 * The whole window shifts by [appearDelayMs], so the center leaves before the edges.
 */
private fun gridReveal(phaseMs: Long, appearDelayMs: Long): Float {
    val windowEnd = appearDelayMs + GRID_VISIBLE_MS
    if (phaseMs >= windowEnd || phaseMs < appearDelayMs) return 0f
    val fadeOutStart = windowEnd - GRID_FADE_MS
    val fadeIn = ((phaseMs - appearDelayMs) / GRID_FADE_MS.toFloat()).coerceIn(0f, 1f)
    val fadeOut = if (phaseMs < fadeOutStart) {
        1f
    } else {
        1f - ((phaseMs - fadeOutStart) / GRID_FADE_MS.toFloat()).coerceIn(0f, 1f)
    }
    return minOf(fadeIn, fadeOut)
}

private fun millisUntilGridVisible(phaseMs: Long, appearDelayMs: Long): Long {
    val wait = if (phaseMs < appearDelayMs) {
        appearDelayMs - phaseMs
    } else {
        GRID_APPEAR_PERIOD_MS - phaseMs + appearDelayMs
    }
    return wait.coerceAtLeast(1L)
}

private const val GRID_APPEAR_PERIOD_MS = 8_000L
private const val GRID_VISIBLE_MS = 2_000L
private const val GRID_FADE_MS = 250L
private const val GRID_EDGE_APPEAR_DELAY_MS = 250L

private val RcHudRevealIconSize = 24.dp

/** Collapsed top/center target. The icon stands for the hidden feature. */
@Composable
internal fun RcHudSwipeRevealStrip(
    icon: ImageVector,
    contentDescription: String,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    hideTowardBottom: Boolean = false,
) {
    Box(
        modifier = modifier
            .hudVerticalSwipe(
                expanded = false,
                onExpandedChange = { shown -> if (shown) onExpand() },
                hideTowardBottom = hideTowardBottom,
            )
            .clickable(role = Role.Button, onClick = onExpand)
            .semantics {
                this.role = Role.Button
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        RcHudRevealIcon(icon = icon)
    }
}

/** Icon-only chevron; no glass container so the camera stays clear. */
@Composable
internal fun RcHudBareChevronHandle(
    direction: RcHudChevronDirection,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Icon(
        imageVector = direction.chevronIcon(),
        contentDescription = contentDescription,
        tint = brandPrimary(),
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .then(direction.handlePadding())
            .size(22.dp),
    )
}

/**
 * Slides [content] off the start or end edge. When hidden, [icon] remains so
 * the feature can be shown again.
 */
@Composable
internal fun RcHudCollapsibleToEdge(
    towardEnd: Boolean,
    icon: ImageVector,
    showContentDescription: String,
    hideContentDescription: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = true,
    fillHeight: Boolean = false,
    swipeEntireContent: Boolean = false,
    content: @Composable () -> Unit,
) {
    val edgeAlignment = if (towardEnd) Alignment.CenterEnd else Alignment.CenterStart
    val expandFrom = if (towardEnd) Alignment.End else Alignment.Start
    val hideDirection = if (towardEnd) RcHudChevronDirection.End else RcHudChevronDirection.Start
    val chromeEnter = expandHorizontally(
        expandFrom = expandFrom,
        animationSpec = tween(RcVehicleProLayout.HUD_CHROME_ANIM_MS),
    ) + fadeIn(animationSpec = tween(RcVehicleProLayout.HUD_CHROME_FADE_MS))
    val chromeExit = shrinkHorizontally(
        shrinkTowards = expandFrom,
        animationSpec = tween(RcVehicleProLayout.HUD_CHROME_ANIM_MS),
    ) + fadeOut(animationSpec = tween(RcVehicleProLayout.HUD_CHROME_FADE_MS))

    val widthModifier = if (fillWidth) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.wrapContentWidth(unbounded = true)
    }
    val heightModifier = if (fillHeight) Modifier.fillMaxHeight() else Modifier
    Box(modifier = modifier, contentAlignment = edgeAlignment) {
        AnimatedVisibility(
            visible = expanded,
            modifier = widthModifier.then(heightModifier),
            enter = chromeEnter,
            exit = chromeExit,
        ) {
            val contentSwipe = if (swipeEntireContent) {
                Modifier.hudHorizontalSwipe(
                    expanded = true,
                    onExpandedChange = onExpandedChange,
                    hideTowardEnd = towardEnd,
                )
            } else {
                Modifier
            }
            val handleSwipe = if (swipeEntireContent) {
                Modifier
            } else {
                Modifier.hudHorizontalSwipe(
                    expanded = true,
                    onExpandedChange = onExpandedChange,
                    hideTowardEnd = towardEnd,
                )
            }
            Row(
                modifier = widthModifier.then(heightModifier).then(contentSwipe),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val contentBoxModifier = (if (fillWidth) Modifier.weight(1f) else Modifier)
                    .then(heightModifier)
                if (towardEnd) {
                    RcHudBareChevronHandle(
                        direction = hideDirection,
                        contentDescription = hideContentDescription,
                        modifier = handleSwipe,
                    )
                    Box(modifier = contentBoxModifier) { content() }
                } else {
                    Box(modifier = contentBoxModifier) { content() }
                    RcHudBareChevronHandle(
                        direction = hideDirection,
                        contentDescription = hideContentDescription,
                        modifier = handleSwipe,
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = !expanded,
            enter = chromeEnter,
            exit = chromeExit,
        ) {
            RcHudRevealIcon(
                icon = icon,
                appearDelayMs = GRID_EDGE_APPEAR_DELAY_MS,
                modifier = Modifier
                    .hudHorizontalSwipe(
                        expanded = false,
                        onExpandedChange = onExpandedChange,
                        hideTowardEnd = towardEnd,
                    )
                    .clickable(role = Role.Button, onClick = { onExpandedChange(true) })
                    .semantics {
                        this.role = Role.Button
                        this.contentDescription = showContentDescription
                    },
            )
        }
    }
}
