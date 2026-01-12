package com.example.emitterapp.ui.rc_screen.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.emitterapp.R

@Composable
fun BatteryStatus(
    modifier: Modifier = Modifier,
    level: Int
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

    Image(
        painter = painterResource(id = frames[frameIndex]),
        contentDescription = "Battery Status",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun BatteryStatusPreview() {
    Column {
        BatteryStatus(level = 100)
        BatteryStatus(level = 80)
        BatteryStatus(level = 50)
        BatteryStatus(level = 10)
        BatteryStatus(level = 25)
    }
}