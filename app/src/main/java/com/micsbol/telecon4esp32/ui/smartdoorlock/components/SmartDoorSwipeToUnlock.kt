package com.micsbol.telecon4esp32.ui.smartdoorlock.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.smartdoorlock.SmartDoorLockGlass
import kotlin.math.roundToInt

@Composable
fun SmartDoorSwipeToUnlock(
    onUnlock: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val thumbSize = 52.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    val maxOffsetPx = (trackWidthPx - thumbSizePx).coerceAtLeast(0f)
    val unlockThreshold = maxOffsetPx * 0.85f

    val animatedOffsetPx by animateFloatAsState(
        targetValue = dragOffsetPx,
        animationSpec = tween(durationMillis = 200),
        label = "swipeThumb",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(SmartDoorLockGlass.PillShape)
            .background(SmartDoorLockGlass.CardSurfaceTint.copy(alpha = 0.55f))
            .onSizeChanged { trackWidthPx = it.width.toFloat() },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 72.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(4) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = SmartDoorLockGlass.TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier
                        .padding(horizontal = 1.dp)
                        .size(18.dp),
                )
            }
        }

        Icon(
            imageVector = Icons.Default.LockOpen,
            contentDescription = null,
            tint = SmartDoorLockGlass.TextMuted,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp)
                .size(22.dp),
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffsetPx.roundToInt(), 0) }
                .padding(start = 4.dp)
                .size(thumbSize)
                .clip(CircleShape)
                .background(
                    if (enabled) SmartDoorLockGlass.TextPrimary else SmartDoorLockGlass.TextMuted,
                )
                .pointerInput(enabled, maxOffsetPx) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffsetPx >= unlockThreshold) {
                                onUnlock()
                            }
                            dragOffsetPx = 0f
                        },
                        onDragCancel = { dragOffsetPx = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffsetPx = (dragOffsetPx + dragAmount)
                                .coerceIn(0f, maxOffsetPx)
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = stringResource(R.string.smart_door_lock_swipe_to_unlock),
                tint = SmartDoorLockGlass.AccentGreen,
                modifier = Modifier.size(24.dp),
            )
        }

        if (animatedOffsetPx < unlockThreshold * 0.3f) {
            Text(
                text = stringResource(R.string.smart_door_lock_swipe_to_unlock),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = SmartDoorLockGlass.TextSecondary.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(start = 56.dp),
            )
        }
    }
}
