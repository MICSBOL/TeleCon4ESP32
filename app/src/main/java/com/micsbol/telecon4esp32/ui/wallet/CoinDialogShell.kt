package com.micsbol.telecon4esp32.ui.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.theme.AppGlass

private val CoinDialogShape = RoundedCornerShape(24.dp)

@Composable
fun CoinDialogShell(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalMargin: Dp = 20.dp,
    scrollableContent: Boolean = false,
    title: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = horizontalMargin)
                .clip(CoinDialogShape)
                .border(
                    width = 1.dp,
                    color = AppGlass.BorderColor.copy(alpha = AppGlass.BorderAlpha),
                    shape = CoinDialogShape,
                ),
        ) {
            Image(
                painter = painterResource(R.drawable.bg_telecon_workbench),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = 1.14f
                        scaleY = 1.14f
                        translationY = 28f
                    },
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Black.copy(alpha = 0.42f),
                                0.4f to AppGlass.BackgroundMid.copy(alpha = 0.38f),
                                0.75f to Color.Black.copy(alpha = 0.48f),
                                1f to AppGlass.BackgroundTop.copy(alpha = 0.52f),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (scrollableContent) {
                            Modifier
                                .heightIn(max = 460.dp)
                                .verticalScroll(rememberScrollState())
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                title()
                content()
                Spacer(modifier = Modifier.height(4.dp))
                actions()
            }
        }
    }
}
