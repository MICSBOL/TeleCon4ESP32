package com.micsbol.telecon4esp32.ui.wallet

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.micsbol.telecon4esp32.ui.components.ProvideCappedFontScale
import com.micsbol.telecon4esp32.ui.components.safeHudPadding
import com.micsbol.telecon4esp32.ui.theme.AppGlass

private val CoinDialogShape = RoundedCornerShape(24.dp)

internal data class CoinDialogLimits(
    val maxWidth: Dp,
    val maxHeight: Dp,
    val compact: Boolean,
)

/**
 * Landscape Control Panel / short OEM windows (Huawei/EMUI) need a compact card
 * so title + options + actions stay on screen instead of collapsing into a
 * one-row scroll region.
 */
internal fun resolveCoinDialogLimits(
    screenWidthDp: Int,
    screenHeightDp: Int,
    isLandscape: Boolean,
): CoinDialogLimits {
    val compact = isLandscape || screenHeightDp < 500
    val maxWidth = when {
        isLandscape -> minOf(420.dp, (screenWidthDp * 0.62f).dp)
        screenWidthDp >= 600 -> 440.dp
        else -> minOf(400.dp, (screenWidthDp - 48).dp)
    }
    val heightFraction = if (compact) 0.94f else 0.86f
    val maxHeight = (screenHeightDp * heightFraction).dp.coerceAtLeast(220.dp)
    return CoinDialogLimits(maxWidth = maxWidth, maxHeight = maxHeight, compact = compact)
}

@Composable
fun CoinDialogShell(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalMargin: Dp = 16.dp,
    /** Kept for call-site compatibility; body content always scrolls when needed. */
    @Suppress("UNUSED_PARAMETER")
    scrollableContent: Boolean = true,
    title: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val limits = resolveCoinDialogLimits(
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
        isLandscape = isLandscape,
    )
    // Safe HUD already clears cutout / nav; keep a small extra gutter only.
    val sidePad = if (isLandscape) 8.dp else horizontalMargin
    val scrimInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    val cardHPad = if (limits.compact) 12.dp else 18.dp
    val cardVPad = if (limits.compact) 10.dp else 14.dp
    val stackGap = if (limits.compact) 6.dp else 10.dp

    ProvideCappedFontScale(maxFontScale = if (limits.compact) 1.08f else 1.15f) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                // EMUI sizes the dialog window to inflated insets when this is true,
                // which clips the card on Huawei landscape HUDs.
                decorFitsSystemWindows = false,
                dismissOnClickOutside = true,
                dismissOnBackPress = true,
            ),
        ) {
            // Full-size scrim so outside taps dismiss (platform outside-click is blocked
            // when we use usePlatformDefaultWidth = false + a fillMaxSize host).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null,
                        onClick = onDismissRequest,
                    )
                    .safeHudPadding(
                        includeTop = true,
                        includeBottom = true,
                        includeHorizontal = true,
                    )
                    .padding(horizontal = sidePad, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    val cardMaxWidth = minOf(limits.maxWidth, maxWidth)
                    val cardMaxHeight = minOf(limits.maxHeight, maxHeight)
                    Column(
                        modifier = modifier
                            .widthIn(max = cardMaxWidth)
                            .fillMaxWidth()
                            .heightIn(max = cardMaxHeight)
                            .clip(CoinDialogShape)
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to AppGlass.DialogSurface.copy(alpha = 0.94f),
                                        0.55f to AppGlass.BackgroundMid.copy(alpha = 0.92f),
                                        1f to AppGlass.BackgroundTop.copy(alpha = 0.94f),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.dp,
                                color = AppGlass.BorderColor.copy(alpha = AppGlass.BorderAlpha),
                                shape = CoinDialogShape,
                            )
                            // Consume clicks so they don't dismiss via the scrim.
                            .clickable(
                                interactionSource = cardInteraction,
                                indication = null,
                                onClick = {},
                            )
                            .padding(horizontal = cardHPad, vertical = cardVPad),
                        verticalArrangement = Arrangement.spacedBy(stackGap),
                    ) {
                        title()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(stackGap),
                            content = content,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        actions()
                    }
                }
            }
        }
    }
}
