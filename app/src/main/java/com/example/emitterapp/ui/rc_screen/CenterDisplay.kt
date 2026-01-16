package com.example.emitterapp.ui.rc_screen

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emitterapp.R
import com.example.emitterapp.domain.bluetooth.PlotData
import com.example.emitterapp.ui.rc_screen.components.ButtonSide
import com.example.emitterapp.ui.rc_screen.components.PushButtonSide
import com.example.emitterapp.ui.rc_screen.components.RealTimePlot
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CenterDisplay(
    modifier: Modifier = Modifier,
    onTopLeftPress: () -> Unit,
    onTopRightPress: () -> Unit,
    onBottomLeftPress: () -> Unit,
    onBottomRightPress: () -> Unit,
    screenAspectRatio: Float = 0f,
    series: List<PlotData> = emptyList()
) {
    val isWideScreen = screenAspectRatio > 1.7f
    val is4Over3 = screenAspectRatio == 4 / 3f
    Box(
        modifier = modifier
            .background(Color.Transparent)
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.center_frame_blue),
                    contentDescription = "Center Display Frame",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxHeight(0.90f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 4.dp), // Padding inside the frame
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.15f).padding(horizontal = 2.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box{
                                Text(
                                    text = "Volts",
                                    fontSize = 10.sp,
                                    fontStyle = FontStyle.Italic,
                                    fontFamily = FontFamily.Default,
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(2.dp)
                                    .background(Color.Cyan)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box{
                                Text(
                                    text = "Amps",
                                    fontSize = 10.sp,
                                    fontStyle = FontStyle.Italic,
                                    fontFamily = FontFamily.Default,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(2.dp)
                                    .background(Color.Red)
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RealTimePlot(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            series = series
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Time (s)",
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic,
                            fontFamily = FontFamily.Default,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (!isWideScreen) { //&& !is4Over3){
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PushButtonSide(
                            modifier = Modifier.size(70.dp),
                            side = ButtonSide.RIGHT,
                            onPress = onTopRightPress
                        )
                        PushButtonSide(
                            modifier = Modifier.size(70.dp),
                            side = ButtonSide.LEFT,
                            onPress = onTopLeftPress
                        )
                    }
                }
            }

            if (isWideScreen) {  //|| is4Over3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-20).dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PushButtonSide(
                        modifier = Modifier.size(50.dp),
                        side = ButtonSide.RIGHT,
                        onPress = onTopRightPress
                    )
                    PushButtonSide(
                        modifier = Modifier.size(50.dp),
                        side = ButtonSide.LEFT,
                        onPress = onTopLeftPress
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = if (isWideScreen) (-20).dp else 0.dp)
                    .padding(horizontal = 26.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PushButtonSide(
                    modifier = Modifier.size(60.dp),
                    side = ButtonSide.RIGHT,
                    onPress = onBottomRightPress
                )
                PushButtonSide(
                    modifier = Modifier.size(60.dp),
                    side = ButtonSide.LEFT,
                    onPress = onBottomLeftPress
                )
            }
        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF444444)
@Composable
fun InteractiveCenterDisplayPreview() {

    val voltsData = remember { mutableStateListOf<Float>() }
    val ampsData = remember { mutableStateListOf<Float>() }

    val maxDataPoints = 100
    var time by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            val voltsPoint = (sin(time * 1.5f * PI.toFloat()) * 0.4f) + 0.6f
            voltsData.add(voltsPoint)
            if (voltsData.size > maxDataPoints) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    voltsData.removeFirst()
                }
            }

            val ampsPoint = (cos(time * 3f * PI.toFloat()) * 0.2f) + 0.25f
            ampsData.add(ampsPoint)
            if (ampsData.size > maxDataPoints) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    ampsData.removeFirst()
                }
            }

            time += 0.02f
            delay(16L)
        }
    }

    CenterDisplay(
        modifier = Modifier.size(width = 800.dp, height = 400.dp),
        onTopLeftPress = {},
        onTopRightPress = {},
        onBottomLeftPress = {},
        onBottomRightPress = {},
        screenAspectRatio = 800f / 400f,
        series = listOf(
            PlotData(dataPoints = voltsData, color = Color.Cyan),
            PlotData(dataPoints = ampsData, color = Color.Red)
        )
    )
}