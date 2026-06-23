package com.micsbol.telecon4esp32.ui.smartlighting.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.ui.components.mutedTextColor

@Composable
fun SmartLightingSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = mutedTextColor(),
    )
}
