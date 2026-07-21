package com.micsbol.telecon4esp32.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.micsbol.telecon4esp32.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.micsbol.telecon4esp32.ui.components.EmitterBrandLogo
import com.micsbol.telecon4esp32.ui.components.NeumorphicBackground
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.theme.Neo
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SPLASH_DISPLAY_MS = 2_000L
private const val LOGO_ANIM_DURATION_MS = 1_050
private const val LOGO_SCALE_START = 0.84f
private const val LOGO_SLIDE_START_DP = 16f
private const val TEXT_ANIM_DURATION_MS = 900
private const val TEXT_SLIDE_START_DP = 12f
private val SplashMotionEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
fun SplashScreen(
    navController: NavHostController,
    onComposeSplashReady: () -> Unit = {},
) {
    LaunchedEffect(Unit) {
        onComposeSplashReady()
    }

    val navigateToHome: () -> Unit = {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    LaunchedEffect(Unit) {
        delay(SPLASH_DISPLAY_MS)
        navigateToHome()
    }

    SplashScreenContent(
        onSkip = navigateToHome,
        displayDurationMs = SPLASH_DISPLAY_MS,
    )
}

@Composable
fun SplashScreenContent(
    onSkip: () -> Unit,
    displayDurationMs: Long = SPLASH_DISPLAY_MS,
) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(displayDurationMs) {
        progress = 0f
        val steps = 40
        val stepDelay = displayDurationMs / steps
        repeat(steps) { step ->
            progress = (step + 1) / steps.toFloat()
            delay(stepDelay)
        }
    }

    val hintPulse = rememberInfiniteTransition(label = "hintPulse")
    val hintAlpha by hintPulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hintAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSkip,
            ),
    ) {
        NeumorphicBackground()
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SplashAnimatedLogo {
                EmitterBrandLogo(
                    size = 156.dp,
                    fullLogo = true,
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            SplashFadeSlideText(animationDelayMs = 320) {
                Text(
                    text = stringResource(R.string.app_header_subtitle),
                    color = Neo.Accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            SplashFadeSlideText(animationDelayMs = 660) {
                Text(
                    text = stringResource(R.string.splash_tagline),
                    color = Neo.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        SplashFadeSlideText(
            animationDelayMs = 840,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 40.dp, vertical = 36.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.splash_tap_hint).uppercase(),
                    color = Neo.TextSecondary.copy(alpha = hintAlpha),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(0.55f),
                    color = Neo.Accent,
                    trackColor = Neo.SurfaceLow,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.splash_brand_credit),
                    color = Neo.TextSecondary.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SplashAnimatedLogo(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val slideStartPx = with(density) { LOGO_SLIDE_START_DP.dp.toPx() }
    val scale = remember { Animatable(LOGO_SCALE_START) }
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(slideStartPx) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = LOGO_ANIM_DURATION_MS - 120,
                        easing = SplashMotionEasing,
                    ),
                )
            }
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = LOGO_ANIM_DURATION_MS,
                        easing = SplashMotionEasing,
                    ),
                )
            }
            launch {
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = LOGO_ANIM_DURATION_MS,
                        easing = SplashMotionEasing,
                    ),
                )
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            this.alpha = alpha.value
            translationY = offsetY.value
        },
    ) {
        content()
    }
}

@Composable
private fun SplashFadeSlideText(
    animationDelayMs: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val slideStartPx = with(density) { TEXT_SLIDE_START_DP.dp.toPx() }
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(slideStartPx) }

    LaunchedEffect(animationDelayMs) {
        delay(animationDelayMs.toLong())
        coroutineScope {
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = TEXT_ANIM_DURATION_MS,
                        easing = SplashMotionEasing,
                    ),
                )
            }
            launch {
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = TEXT_ANIM_DURATION_MS,
                        easing = SplashMotionEasing,
                    ),
                )
            }
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetY.value
        },
    ) {
        content()
    }
}
