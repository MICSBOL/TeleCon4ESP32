package com.example.emitterapp.ui.splash

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emitterapp.R
import com.example.emitterapp.ui.navigation.Screen
import com.example.emitterapp.ui.theme.EmitterAppTheme
import com.example.emitterapp.ui.theme.syncopate
import com.example.emitterapp.ui.theme.titanOneRegular

@Composable
fun SplashScreen(navController: androidx.navigation.NavHostController) {
    SplashScreenContent(
        onGetStarted = {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    )
}

@Composable
fun SplashScreenContent(
    onGetStarted: () -> Unit,
) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        startAnimation = true
    }

    val domeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0.0f to MaterialTheme.colorScheme.surface,
                    0.6f to MaterialTheme.colorScheme.background,
                    1.0f to MaterialTheme.colorScheme.background
                )
            )
    ) {
        val screenMaxWidth = maxWidth
        val isLandscape = maxWidth > maxHeight
        val isTablet = minOf(maxWidth, maxHeight) >= 600.dp

        val entranceTravel = maxHeight * 0.18f

        // Portrait domes: keep a strong visible slice on all vertical layouts,
        // including Tablet Portrait.
        val portraitDomeDiameter = when {
            isTablet -> maxWidth * 3.05f
            else -> maxWidth * 3.35f
        }
        val portraitTopVisibleSlice = when {
            isTablet -> maxHeight * 0.50f
            else -> maxHeight * 0.50f
        }
        val portraitBottomVisibleSlice = when {
            isTablet -> maxHeight * 0.50f
            else -> maxHeight * 0.50f
        }
        val portraitTopRestOffset = -(portraitDomeDiameter - portraitTopVisibleSlice)
        val portraitBottomRestOffset = portraitDomeDiameter - portraitBottomVisibleSlice

        val topDomeOffset by animateDpAsState(
            targetValue = if (startAnimation) portraitTopRestOffset else portraitTopRestOffset - entranceTravel,
            animationSpec = tween(durationMillis = 1000, delayMillis = 500),
            label = "topDomeOffset"
        )
        val bottomDomeOffset by animateDpAsState(
            targetValue = if (startAnimation) portraitBottomRestOffset else portraitBottomRestOffset + entranceTravel,
            animationSpec = tween(durationMillis = 1000, delayMillis = 500),
            label = "bottomDomeOffset"
        )

        // Tablets get larger corner domes so they remain visible and decorative on wide layouts.
        val landscapeCornerDiameter = when {
            isTablet -> maxHeight * 2.30f
            else -> maxHeight * 1.65f
        }
        val landscapeCornerInsetX = when {
            isTablet -> landscapeCornerDiameter * 0.52f
            else -> landscapeCornerDiameter * 0.42f
        }
        val landscapeCornerInsetY = when {
            isTablet -> landscapeCornerDiameter * 0.52f
            else -> landscapeCornerDiameter * 0.42f
        }
        val topLeftCornerX by animateDpAsState(
            targetValue = if (startAnimation) -landscapeCornerInsetX else -landscapeCornerInsetX - entranceTravel,
            animationSpec = tween(durationMillis = 1000, delayMillis = 500),
            label = "topLeftCornerX"
        )
        val topLeftCornerY by animateDpAsState(
            targetValue = if (startAnimation) -landscapeCornerInsetY else -landscapeCornerInsetY - entranceTravel,
            animationSpec = tween(durationMillis = 1000, delayMillis = 500),
            label = "topLeftCornerY"
        )
        val bottomRightCornerX by animateDpAsState(
            targetValue = if (startAnimation) landscapeCornerInsetX else landscapeCornerInsetX + entranceTravel,
            animationSpec = tween(durationMillis = 1000, delayMillis = 500),
            label = "bottomRightCornerX"
        )
        val bottomRightCornerY by animateDpAsState(
            targetValue = if (startAnimation) landscapeCornerInsetY else landscapeCornerInsetY + entranceTravel,
            animationSpec = tween(durationMillis = 1000, delayMillis = 500),
            label = "bottomRightCornerY"
        )
        val imageScale by animateFloatAsState(
            targetValue = if (startAnimation) 1.0f else 0f,
            animationSpec = tween(durationMillis = 1000, delayMillis = 500),
            label = "imageScale"
        )
        val contentAlpha by animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = tween(durationMillis = 1000, delayMillis = 1500),
            label = "contentAlpha"
        )

        val titleTopPadding = if (isLandscape) maxHeight * 0.08f else maxHeight * 0.10f
        val bottomPadding = if (isLandscape) maxHeight * 0.06f else maxHeight * 0.07f
        val logoWidthFraction = if (isLandscape) 0.45f else 0.82f
        val buttonWidthFraction = if (isLandscape) 0.38f else 0.72f
        val titleFontSize = when {
            maxWidth < 360.dp -> 28.sp
            isTablet && isLandscape -> 58.sp
            isTablet -> 52.sp
            isLandscape && maxWidth >= 840.dp -> 50.sp
            isLandscape -> 46.sp
            maxWidth >= 500.dp -> 48.sp
            else -> 35.sp
        }
        val subtitleFontSize = if (maxWidth < 360.dp) 14.sp else 16.sp
        val buttonTextSize = if (maxWidth < 360.dp) 16.sp else 18.sp

        if (isLandscape) {
            Box(
                modifier = Modifier
                    .requiredSize(landscapeCornerDiameter)
                    .offset(x = topLeftCornerX, y = topLeftCornerY)
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(domeColor)
            )

            Box(
                modifier = Modifier
                    .requiredSize(landscapeCornerDiameter)
                    .offset(x = bottomRightCornerX, y = bottomRightCornerY)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(domeColor)
            )
        } else {
            Box(
                modifier = Modifier
                    .requiredSize(portraitDomeDiameter)
                    .offset(y = topDomeOffset)
                    .align(Alignment.TopCenter)
                    .clip(CircleShape)
                    .background(domeColor)
            )

            Box(
                modifier = Modifier
                    .requiredSize(portraitDomeDiameter)
                    .offset(y = bottomDomeOffset)
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(domeColor)
            )
        }

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .scale(imageScale)
                            .alpha(imageScale),
                        painter = painterResource(R.drawable.joystick_01),
                        contentDescription = stringResource(R.string.splash_logo_content_description),
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 30.dp)
                        .fillMaxSize()
                        .padding(horizontal = screenMaxWidth * 0.03f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = stringResource(R.string.splash_title),
                        fontFamily = titanOneRegular,
                        fontSize = titleFontSize,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = titleTopPadding)
                            .alpha(contentAlpha)
                    )
                    Spacer(modifier = Modifier.size(30.dp))
                    Column(
                        modifier = Modifier
                            .padding(bottom = bottomPadding)
                            .alpha(contentAlpha),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = onGetStarted,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(0.76f)
                        ) {
                            Text(
                                text = stringResource(R.string.splash_get_started),
                                fontSize = buttonTextSize,
                                fontFamily = syncopate,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.splash_subtitle),
                            fontFamily = titanOneRegular,
                            fontSize = subtitleFontSize,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            Image(
                modifier = Modifier
                    .fillMaxWidth(logoWidthFraction)
                    .scale(imageScale)
                    .alpha(imageScale)
                    .align(Alignment.Center),
                painter = painterResource(R.drawable.joystick_01),
                contentDescription = stringResource(R.string.splash_logo_content_description),
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.splash_title),
                    fontFamily = titanOneRegular,
                    fontSize = titleFontSize,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = titleTopPadding)
                        .alpha(contentAlpha)
                )

                Spacer(modifier = Modifier.weight(1f))

                Column(
                    modifier = Modifier
                        .padding(bottom = bottomPadding)
                        .alpha(contentAlpha),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onGetStarted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(buttonWidthFraction)
                    ) {
                        Text(
                            text = stringResource(R.string.splash_get_started),
                            fontSize = buttonTextSize,
                            fontFamily = syncopate,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.splash_subtitle),
                        fontFamily = titanOneRegular,
                        fontSize = subtitleFontSize,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    EmitterAppTheme {
        SplashScreenContent(onGetStarted = {})
    }
}

@Preview(showSystemUi = true, name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun SplashScreenDarkPreview() {
    EmitterAppTheme {
        SplashScreenContent(onGetStarted = {})
    }
}

@Preview(
    showBackground = true,
    name = "Phone Portrait",
    device = "spec:width=411dp,height=891dp,dpi=420"
)
@Composable
private fun SplashScreenPhonePortraitPreview() {
    EmitterAppTheme {
        SplashScreenContent(onGetStarted = {})
    }
}

@Preview(
    showBackground = true,
    name = "Phone Landscape",
    device = "spec:width=891dp,height=411dp,dpi=420"
)
@Composable
private fun SplashScreenPhoneLandscapePreview() {
    EmitterAppTheme {
        SplashScreenContent(onGetStarted = {})
    }
}

@Preview(
    showSystemUi = true,
    name = "Tablet Portrait",
    device = "spec:width=800dp,height=1280dp,dpi=240"
)
@Composable
private fun SplashScreenTabletPortraitPreview() {
    EmitterAppTheme {
        SplashScreenContent(onGetStarted = {})
    }
}

@Preview(
    showSystemUi = true,
    name = "Tablet Landscape",
    device = "spec:width=1280dp,height=800dp,dpi=240"
)
@Composable
private fun SplashScreenTabletLandscapePreview() {
    EmitterAppTheme {
        SplashScreenContent(onGetStarted = {})
    }
}