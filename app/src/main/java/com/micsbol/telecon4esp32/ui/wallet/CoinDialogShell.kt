package com.micsbol.telecon4esp32.ui.wallet

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.micsbol.telecon4esp32.ui.theme.AppGlass

private val CoinDialogShape = RoundedCornerShape(24.dp)

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
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Keep the card compact — especially in landscape, never edge-to-edge.
    val maxDialogWidth = when {
        isLandscape -> minOf(400.dp, screenWidth * 0.55f)
        configuration.screenWidthDp >= 600 -> 440.dp
        else -> minOf(400.dp, screenWidth - 48.dp)
    }
    val heightFraction = if (isLandscape || configuration.screenHeightDp < 500) 0.92f else 0.86f
    val maxDialogHeight = (screenHeight * heightFraction).coerceAtLeast(220.dp)
    val sidePad = if (isLandscape) 24.dp else horizontalMargin
    val scrimInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    ProvideCappedFontScale(maxFontScale = 1.15f) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true,
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
                    .padding(horizontal = sidePad, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = modifier
                        .widthIn(max = maxDialogWidth)
                        .fillMaxWidth()
                        .heightIn(max = maxDialogHeight)
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
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    title()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        content = content,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    actions()
                }
            }
        }
    }
}
