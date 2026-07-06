package com.micsbol.telecon4esp32.ui.solarsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.ui.solarsystem.SolarGlass

@Composable
fun SolarSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = SolarGlass.TextOnGlassPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                color = SolarGlass.TextOnGlassSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(enabled = onActionClick != null) {
                    onActionClick?.invoke()
                },
            )
        }
    }
}
