package com.micsbol.telecon4esp32.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.micsbol.telecon4esp32.ui.theme.Neo

/** UI region a tutorial step should leave uncovered. */
enum class TutorialAnchor {
    HOME_CONTROL_PANEL,
    PANEL_CABLE,
    PANEL_BLUETOOTH,
    PANEL_LEFT_STICK,
    PANEL_RIGHT_STICK,
    PANEL_LEFT_CONTROLS,
    PANEL_TOOL_DECK,
    SETTINGS_CONNECTION,
    SETTINGS_MATCHING_CODE,
    BLUETOOTH_REFRESH,
    BLUETOOTH_LIST,
}

class TutorialAnchorRegistry {
    val bounds: SnapshotStateMap<TutorialAnchor, Rect> = SnapshotStateMap()
}

val LocalTutorialAnchors = compositionLocalOf { TutorialAnchorRegistry() }

/** Reports this layout's bounds so the tutorial scrim can leave it clear. */
fun Modifier.reportTutorialAnchor(anchor: TutorialAnchor): Modifier = composed {
    val registry = LocalTutorialAnchors.current
    DisposableEffect(registry, anchor) {
        onDispose { registry.bounds.remove(anchor) }
    }
    onGloballyPositioned { coordinates ->
        registry.bounds[anchor] = coordinates.boundsInRoot()
    }
}

/**
 * Dims the screen and punches a rounded hole for each [holes] rect so that
 * control stays fully visible. This layer does not take touches.
 */
@Composable
fun TutorialDimScrim(holes: List<Rect>) {
    val density = LocalDensity.current
    val cornerPx = with(density) { 14.dp.toPx() }
    val strokePx = with(density) { 2.5.dp.toPx() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val accent = Neo.Accent

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        drawRect(Color.Black.copy(alpha = 0.58f))
        holes.forEach { hole ->
            val local = hole.translate(-origin.x, -origin.y)
            if (local.width <= 1f || local.height <= 1f) return@forEach
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(local.left, local.top),
                size = Size(local.width, local.height),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                blendMode = BlendMode.Clear,
            )
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        holes.forEach { hole ->
            val local = hole.translate(-origin.x, -origin.y)
            if (local.width <= 1f || local.height <= 1f) return@forEach
            drawRoundRect(
                color = accent,
                topLeft = Offset(local.left, local.top),
                size = Size(local.width, local.height),
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                style = Stroke(width = strokePx),
            )
        }
    }
}

/**
 * Consumes taps everywhere except [holes], which are in the same coordinates
 * as this composable's parent (the tutorial overlay).
 */
@Composable
fun TutorialOutsideHoleBlocker(
    holes: List<Rect>,
    widthPx: Float,
    heightPx: Float,
) {
    val density = LocalDensity.current
    val blocks = remember(holes, widthPx, heightPx) {
        subtractHoles(Rect(0f, 0f, widthPx, heightPx), holes)
    }
    blocks.forEachIndexed { index, rect ->
        key(index, rect.left, rect.top, rect.right, rect.bottom) {
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                    .size(
                        width = with(density) { rect.width.toDp() },
                        height = with(density) { rect.height.toDp() },
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = {},
                    ),
            )
        }
    }
}

private fun subtractHoles(screen: Rect, holes: List<Rect>): List<Rect> {
    var free = listOf(screen)
    holes.forEach { hole ->
        free = free.flatMap { it.minusHole(hole) }
    }
    return free.filter { it.width > 1f && it.height > 1f }
}

private fun Rect.minusHole(hole: Rect): List<Rect> {
    val cutLeft = maxOf(left, hole.left)
    val cutTop = maxOf(top, hole.top)
    val cutRight = minOf(right, hole.right)
    val cutBottom = minOf(bottom, hole.bottom)
    if (cutLeft >= cutRight || cutTop >= cutBottom) return listOf(this)
    val parts = ArrayList<Rect>(4)
    if (cutTop > top) parts += Rect(left, top, right, cutTop)
    if (cutBottom < bottom) parts += Rect(left, cutBottom, right, bottom)
    if (cutLeft > left) parts += Rect(left, cutTop, cutLeft, cutBottom)
    if (cutRight < right) parts += Rect(cutRight, cutTop, right, cutBottom)
    return parts
}
