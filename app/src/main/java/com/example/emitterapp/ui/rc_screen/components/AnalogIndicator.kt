package com.example.emitterapp.ui.rc_screen.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.emitterapp.R

@Composable
fun AnalogIndicator(
    value: Int,
    modifier: Modifier = Modifier,
) {

    Box(
        modifier = modifier
//            .width(width)
            .aspectRatio(969f / 479f),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.analog_indicator),
            contentDescription = "Analog Indicator Background",
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val rotationCenter = Offset(canvasWidth / 2.4f, canvasHeight * 0.9f)

            val needleLength = canvasHeight * 0.65f
            val needleBaseWidth = canvasWidth * 0.05f
            val hubRadius = needleBaseWidth / 1.5f


            val angleInDegrees = (value.coerceIn(0, 100) / 100f) * 180f - 90f

            withTransform({
                rotate(degrees = angleInDegrees, pivot = rotationCenter)
            }) {
                val needlePath = Path().apply {
                    moveTo(rotationCenter.x - needleBaseWidth / 2f, rotationCenter.y)
                    lineTo(rotationCenter.x + needleBaseWidth / 2f, rotationCenter.y)
                    lineTo(rotationCenter.x, rotationCenter.y - needleLength)
                    close()
                }

                drawPath(
                    path = needlePath,
                    color = Color.LightGray
                )
            }

            drawCircle(
                color = Color.LightGray,
                radius = hubRadius,
                center = rotationCenter
            )

            drawCircle(
                color = Color.Black,
                radius = hubRadius / 2f,
                center = rotationCenter
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalogIndicatorPreview() {
    AnalogIndicator(
        value = 100,
    )
}