package com.micsbol.telecon4esp32.ui.control_panel.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

internal fun Modifier.onDoubleTapAction(onDoubleTap: (() -> Unit)?): Modifier {
    if (onDoubleTap == null) return this
    return pointerInput(onDoubleTap) {
        detectTapGestures(onDoubleTap = { onDoubleTap() })
    }
}
