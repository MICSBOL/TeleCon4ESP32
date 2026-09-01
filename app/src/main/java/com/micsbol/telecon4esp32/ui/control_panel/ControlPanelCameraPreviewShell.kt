package com.micsbol.telecon4esp32.ui.control_panel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.theme.Neo
import kotlinx.coroutines.delay

@Composable
internal fun ControlPanelCameraPreviewShell(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.control_panel_camera_hint),
    showProgress: Boolean = false,
    grayedOut: Boolean = false,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    var scanline by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(grayedOut) {
        if (grayedOut) return@LaunchedEffect
        while (true) {
            delay(32L)
            scanline = (scanline + 0.012f) % 1.15f
        }
    }
    val accent = if (grayedOut) Neo.TextMuted else brandPrimary()
    val titleColor = if (grayedOut) Neo.TextMuted else Color.White
    val bodyColor = if (grayedOut) {
        Neo.TextMuted.copy(alpha = 0.85f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }
    val panelColors = if (grayedOut) {
        listOf(
            Color(0xFF3A3A3A),
            Color(0xFF2B2B2B),
            Color(0xFF404040),
        )
    } else {
        listOf(
            Color(0xFF102033),
            Color(0xFF0A1018),
            Color(0xFF14283A),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (grayedOut) Color(0xFF2B2B2B) else Color(0xFF0A1018)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(colors = panelColors),
            )
            if (!grayedOut) {
                val y = size.height * scanline.coerceIn(0f, 1f)
                drawLine(
                    color = accent.copy(alpha = 0.35f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2.5f,
                )
            }
            val m = size.minDimension * 0.08f
            val len = size.minDimension * 0.12f
            val stroke = 3f
            val c = accent.copy(alpha = if (grayedOut) 0.45f else 0.7f)
            drawLine(c, Offset(m, m), Offset(m + len, m), stroke)
            drawLine(c, Offset(m, m), Offset(m, m + len), stroke)
            drawLine(c, Offset(size.width - m, m), Offset(size.width - m - len, m), stroke)
            drawLine(c, Offset(size.width - m, m), Offset(size.width - m, m + len), stroke)
            drawLine(c, Offset(m, size.height - m), Offset(m + len, size.height - m), stroke)
            drawLine(c, Offset(m, size.height - m), Offset(m, size.height - m - len), stroke)
            drawLine(c, Offset(size.width - m, size.height - m), Offset(size.width - m - len, size.height - m), stroke)
            drawLine(c, Offset(size.width - m, size.height - m), Offset(size.width - m, size.height - m - len), stroke)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector = if (grayedOut) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(36.dp),
            )
            if (showProgress && !grayedOut) {
                CircularProgressIndicator(
                    color = accent,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(
                text = stringResource(R.string.control_panel_camera_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = bodyColor,
                textAlign = TextAlign.Center,
            )
            if (!grayedOut && primaryActionLabel != null && onPrimaryAction != null) {
                TextButton(onClick = onPrimaryAction) {
                    Text(
                        text = primaryActionLabel,
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            if (!grayedOut && secondaryActionLabel != null && onSecondaryAction != null) {
                TextButton(onClick = onSecondaryAction) {
                    Text(
                        text = secondaryActionLabel,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
