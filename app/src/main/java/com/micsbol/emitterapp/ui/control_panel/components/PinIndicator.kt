package com.micsbol.emitterapp.ui.control_panel.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.emitterapp.R

/**
 * A composable that displays a pin image in an 'on' or 'off' state.
 *
 * @param isOn The state of the pin. If true, `pin_on.png` is shown; otherwise, `pin_off.png` is shown.
 * @param size The size to render the indicator image.
 * @param modifier An optional modifier to apply to the composable.
 */
@Composable
fun PinIndicator(
    isOn: Boolean,
    size: Dp,
    modifier: Modifier = Modifier
) {
    // Select the correct drawable resource based on the 'isOn' state
    val pinImageRes = if (isOn) {
        R.drawable.pin_on
    } else {
        R.drawable.pin_off
    }

    Image(
        painter = painterResource(id = pinImageRes),
        contentDescription = if (isOn) "Pin on" else "Pin off",
        modifier = modifier.size(size)
    )
}

@Preview(showBackground = true)
@Composable
private fun PinIndicatorPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        // Preview for the 'on' state
        PinIndicator(
            isOn = true,
            size = 48.dp
        )
        Spacer(Modifier.height(16.dp))
        // Preview for the 'off' state
        PinIndicator(
            isOn = false,
            size = 48.dp
        )
    }
}