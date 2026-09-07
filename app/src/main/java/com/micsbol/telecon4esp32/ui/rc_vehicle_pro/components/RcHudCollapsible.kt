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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.rc_vehicle_pro.RcVehicleProLayout

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

/** Invisible swipe/tap target so collapsed top/center chrome can reopen without chevrons. */
@Composable
internal fun RcHudSwipeRevealStrip(
    contentDescription: String,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    hideTowardBottom: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(
                width = RcVehicleProLayout.HudSwipeRevealWidth,
                height = RcVehicleProLayout.HudSwipeRevealHeight,
            )
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
    )
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

/** Collapsed peek: panel identity plus a small expand chevron. */
@Composable
internal fun RcHudCollapsedIdentityHandle(
    icon: ImageVector,
    expandDirection: RcHudChevronDirection,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .then(expandDirection.handlePadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (expandDirection == RcHudChevronDirection.Up) {
            Icon(
                imageVector = expandDirection.chevronIcon(),
                contentDescription = null,
                tint = brandPrimary(),
                modifier = Modifier.size(14.dp),
            )
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = brandPrimary(),
                modifier = Modifier.size(18.dp),
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = brandPrimary(),
                modifier = Modifier.size(18.dp),
            )
            Icon(
                imageVector = expandDirection.chevronIcon(),
                contentDescription = null,
                tint = brandPrimary(),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * Slides [content] off the start or end edge. When hidden, a related identity
 * icon (or a bare chevron) remains — no glass container.
 */
@Composable
internal fun RcHudCollapsibleToEdge(
    towardEnd: Boolean,
    showContentDescription: String,
    hideContentDescription: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = true,
    fillHeight: Boolean = false,
    swipeEntireContent: Boolean = false,
    collapsedIcon: ImageVector? = null,
    content: @Composable () -> Unit,
) {
    val edgeAlignment = if (towardEnd) Alignment.CenterEnd else Alignment.CenterStart
    val expandFrom = if (towardEnd) Alignment.End else Alignment.Start
    val hideDirection = if (towardEnd) RcHudChevronDirection.End else RcHudChevronDirection.Start
    val showDirection = if (towardEnd) RcHudChevronDirection.Start else RcHudChevronDirection.End
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
            val collapsedModifier = Modifier.hudHorizontalSwipe(
                expanded = false,
                onExpandedChange = onExpandedChange,
                hideTowardEnd = towardEnd,
            )
            if (collapsedIcon != null) {
                RcHudCollapsedIdentityHandle(
                    icon = collapsedIcon,
                    expandDirection = showDirection,
                    contentDescription = showContentDescription,
                    onClick = { onExpandedChange(true) },
                    modifier = collapsedModifier,
                )
            } else {
                RcHudBareChevronHandle(
                    direction = showDirection,
                    contentDescription = showContentDescription,
                    onClick = { onExpandedChange(true) },
                    modifier = collapsedModifier,
                )
            }
        }
    }
}
