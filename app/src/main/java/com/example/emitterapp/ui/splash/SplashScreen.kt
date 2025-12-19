package com.example.emitterapp.ui.splash

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.emitterapp.R
import com.example.emitterapp.ui.navigation.Screen
import com.example.emitterapp.ui.theme.backgroundPurple
import com.example.emitterapp.ui.theme.backgroundSplash
import com.example.emitterapp.ui.theme.syncopate
import com.example.emitterapp.ui.theme.titanOneRegular
import com.example.emitterapp.ui.theme.titleColor

@Composable
fun SplashScreen(navController: NavHostController) {
    var startAnimation by remember { mutableStateOf(false) }

    val imageScale by animateFloatAsState(
        targetValue = if (startAnimation) 1.5f else 0f,
        animationSpec = tween(durationMillis = 1000), label = ""
    )

    val topDomeOffset by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else (-300).dp,
        animationSpec = tween(durationMillis = 1000, delayMillis = 500), label = ""
    )

    val bottomDomeOffset by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else 300.dp,
        animationSpec = tween(durationMillis = 1000, delayMillis = 500), label = ""
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 1200), label = ""
    )

    val buttonAndTextAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 1800), label = ""
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundSplash),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.scale(imageScale),
            painter = painterResource(R.drawable.splash_image),
            contentDescription = null,
        )
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = topDomeOffset)
        ) {
            val domeHeight =
                size.height * 0.7f // Adjust the height of the domes (20% of screen height)
            val canvasWidth = size.width * 2f

            // Draw the top dome
            drawArc(
                color = backgroundPurple,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(x = -size.width * 0.5f, y = -domeHeight / 1.3f),
                size = Size(width = canvasWidth, height = domeHeight)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = bottomDomeOffset)
        ) {
            val domeHeight =
                size.height * 0.7f
            val canvasWidth = size.width * 2f
            val canvasHeight = size.height

            // Draw the bottom dome
            drawArc(
                color = backgroundPurple,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(x = -size.width * 0.5f, y = canvasHeight - domeHeight / 3.0f),
                size = Size(width = canvasWidth, height = domeHeight)
            )
        }

        Text(
            text = "RC - EMITTER",
            fontFamily = titanOneRegular,
            fontSize = 35.sp,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 160.dp)
                .align(Alignment.TopCenter)
                .alpha(titleAlpha)
        )

        Button(
            onClick = {
                navController.navigate(
                    Screen.Home.route
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(bottom = 90.dp)
                .align(Alignment.BottomCenter)
                .alpha(buttonAndTextAlpha)
        ) {
            Text(
                text = "Get Started",
                fontSize = 18.sp,
                fontFamily = syncopate,
                color = backgroundSplash,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "Control your projects in one place.",
            fontFamily = titanOneRegular,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier
                .padding(bottom = 50.dp)
                .align(Alignment.BottomCenter)
                .alpha(buttonAndTextAlpha)
        )
    }
}

@Preview(showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(NavHostController(LocalContext.current))
}
