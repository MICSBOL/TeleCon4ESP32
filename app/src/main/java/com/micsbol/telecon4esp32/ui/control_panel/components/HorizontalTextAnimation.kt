package com.micsbol.telecon4esp32.ui.control_panel.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A text component that animates horizontally (marquee) if the character count
 * exceeds [characterThreshold]. The visible width is constrained to [characterThreshold].
 *
 * @param text The string to display.
 * @param modifier Modifier for the container.
 * @param characterThreshold The maximum number of characters visible at once.
 * @param style Typography style.
 * @param color Text color.
 */
@Composable
fun HorizontalTextAnimation(
    text: String,
    modifier: Modifier = Modifier,
    characterThreshold: Int = 5,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val containerWidthDp = remember(characterThreshold, style, density) {
        val sample = "0".repeat(characterThreshold)
        val widthPx = textMeasurer.measure(sample, style).size.width
        with(density) { widthPx.toDp() }
    }

    val shouldAnimate = text.length > characterThreshold

    Text(
        text = text,
        modifier = modifier
            .width(containerWidthDp)
            // 1. PERFORMANCE OPTIMIZATION:
            // graphicsLayer isolates the animation to the GPU.
            // This prevents the marquee from triggering full screen redraws.
            .graphicsLayer()
            .then(
                if (shouldAnimate) {
                    Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 3000,
                        // 2. Adjust velocity if needed (default is 30.dp/s)
                        // Higher velocity uses slightly more CPU.
                        velocity = 30.dp
                    )
                } else {
                    Modifier
                }
            ),
        style = style,
        color = color,
        maxLines = 1,
        overflow = if (shouldAnimate) TextOverflow.Visible else TextOverflow.Ellipsis
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1C)
@Composable
private fun HorizontalTextAnimationPreview() {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Static version: Fits within 15 characters
        HorizontalTextAnimation(
            text = "Short",
            color = Color.Green,
            style = TextStyle(fontSize = 18.sp),
            characterThreshold = 7
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Animated version: Exceeds 15 characters, will scroll within a 15-char window
        HorizontalTextAnimation(
            text = "This is a very long title that should scroll infinitely",
            color = Color.Cyan,
            style = TextStyle(fontSize = 18.sp),
            characterThreshold = 7
        )
    }
}