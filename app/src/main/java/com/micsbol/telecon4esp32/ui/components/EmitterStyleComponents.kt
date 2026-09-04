package com.micsbol.telecon4esp32.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.theme.HudCyan
import com.micsbol.telecon4esp32.ui.theme.HudCyanBright
import com.micsbol.telecon4esp32.ui.theme.Neo

val EmitterCardShape = RoundedCornerShape(14.dp)

@Composable
fun EmitterBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    fullLogo: Boolean = false,
) {
    Image(
        painter = painterResource(
            if (fullLogo) R.drawable.ic_telecon4esp32_logo else R.drawable.ic_telecon4esp32_icon
        ),
        contentDescription = stringResource(R.string.about_logo_content_description),
        modifier = modifier.size(size),
    )
}

@Composable
fun brandPrimary(): Color = HudCyan

@Composable
fun brandSecondary(): Color = HudCyanBright

/** Cyan on RC Vehicle HUD settings; orange elsewhere. */
@Composable
fun settingsChromeAccent(): Color =
    if (LocalHudGlassDialog.current) brandPrimary() else Neo.Accent

@Composable
fun mutedTextColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant
