package com.example.emitterapp.ui.splash

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.emitterapp.R
import com.example.emitterapp.ui.navigation.Screen
import com.example.emitterapp.ui.theme.EmitterAppTheme
import com.example.emitterapp.ui.theme.syncopate
import com.example.emitterapp.ui.theme.titanOneRegular

@Composable
fun SplashScreen(navController: NavHostController) {
    var startAnimation by remember { mutableStateOf(false) }

    val imageScale by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 500), label = ""
    )
    val topDomeOffset by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else (-300).dp,
        animationSpec = tween(durationMillis = 1000, delayMillis = 500), label = ""
    )
    val bottomDomeOffset by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else 300.dp,
        animationSpec = tween(durationMillis = 1000, delayMillis = 500), label = ""
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, delayMillis = 1500), label = ""
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
    }

    val domeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0.0f to MaterialTheme.colorScheme.surface,
                    0.6f to MaterialTheme.colorScheme.background,
                    1.0f to MaterialTheme.colorScheme.background
                )
            ),
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .scale(imageScale)
                .alpha(imageScale)
                .align(Alignment.Center),
            painter = painterResource(R.drawable.joystick_01),
            contentDescription = stringResource(R.string.splash_logo_content_description),
        )
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = topDomeOffset)
        ) {
            val domeHeight = size.height * 0.7f
            val canvasWidth = size.width * 2f
            drawArc(
                color = domeColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(x = -size.width * 0.5f, y = -domeHeight / 1.5f),
                size = Size(width = canvasWidth, height = domeHeight)
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = bottomDomeOffset)
        ) {
            val domeHeight = size.height * 0.7f
            val canvasWidth = size.width * 2f
            val canvasHeight = size.height
            drawArc(
                color = domeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(x = -size.width * 0.5f, y = canvasHeight - domeHeight / 3.0f),
                size = Size(width = canvasWidth, height = domeHeight)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.splash_title),
                fontFamily = titanOneRegular,
                fontSize = 35.sp,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 100.dp)
                    .alpha(contentAlpha)
            )

            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .padding(bottom = 50.dp)
                    .alpha(contentAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { navController.navigate(Screen.Home.route) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(
                        text = stringResource(R.string.splash_get_started),
                        fontSize = 18.sp,
                        fontFamily = syncopate,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.splash_subtitle),
                    fontFamily = titanOneRegular,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    EmitterAppTheme {
        SplashScreen(NavHostController(LocalContext.current))
    }
}

@Preview(showSystemUi = true, name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SplashScreenDarkPreview() {
    EmitterAppTheme {
        SplashScreen(NavHostController(LocalContext.current))
    }
}