package com.micsbol.telecon4esp32.ui.control_panel.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.R

@Composable
fun BatteryStatus(
    modifier: Modifier = Modifier,
    level: Int,
    title: String = "BATTERY",
){
    val frames = remember {
        (1..6).map {
            val resourceName = "battery_%02d".format(it)
            R.drawable::class.java.getField(resourceName).getInt(null)
        }
    }

    val frameIndex = when {
        level.coerceIn(0, 100) == 0 -> 0
        level.coerceIn(0, 100) == 100 -> 5
        else -> {
            val normalizedLevel = (level - 1) / 99f
            (1 + (normalizedLevel * 3.99f)).toInt()
        }
    }

    // --- START OF DEFINITIVE FIX ---
    // Wrap the Image in a Box and apply the same aspect ratio as AnalogIndicator.
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = modifier
                .aspectRatio(969f / 479f), // Enforce consistent aspect ratio
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = frames[frameIndex]),
                contentDescription = "Battery Status",
                // The Image can now fill the box. Use a suitable ContentScale.
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        IndicatorTitle(
            text = title,
            textColor = Color(0xFFFFA500), // Orange
            glowColor = Color(0xFFFFA500).copy(alpha = 0.5f),
            textSize = 10.sp,
            showFrame = true // Preview without the frame
        )
    }
    // --- END OF DEFINITIVE FIX ---
}

@Preview(showBackground = true)
@Composable
private fun BatteryStatusPreview() {
    Column {
        BatteryStatus(level = 100, title = "BATTERY")
        BatteryStatus(level = 80, title = "BATTERY")
        BatteryStatus(level = 50, title = "BATTERY")
        BatteryStatus(level = 10, title = "BATTERY")
        BatteryStatus(level = 25, title = "BATTERY")
        BatteryStatus(level = 0, title = "BATTERY")
    }
}