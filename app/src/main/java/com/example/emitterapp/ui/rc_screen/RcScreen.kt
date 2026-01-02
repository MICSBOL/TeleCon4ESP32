package com.example.emitterapp.ui.rc_screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.service.controls.Control
import androidx.compose.animation.core.copy
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emitterapp.R
import com.example.emitterapp.ui.rc_screen.components.Joystick_RC3D

@Composable
fun RcScreen() {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    ErgonomicRow(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray),
        centerContent = {
            CenterDisplay(
                modifier = Modifier.fillMaxSize()
            )
        },
        sideContent = {
            ControllerSide(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 7.dp)
            )
        }
    )
}

// DELETE LeftControls and RightControls. They are not used.

@Composable
fun CenterDisplay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(vertical = 24.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.car_bouncing01),
            contentDescription = "Center Screen",
            contentScale = ContentScale.Crop
        )
    }
}

// ControllerSide remains the same, its logic is correct.
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ControllerSide(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ) {
        val aspectRatio = maxWidth / maxHeight
        val sizePercentage = when {
            // Ultra-wide phones (e.g., 21:9)
            aspectRatio > 2.0f -> 0.5f
            // Modern wide phones (e.g., 19.5:9)
            aspectRatio > 1.7f -> 0.6f
            // Standard 16:9 and 16:10 tablets
            aspectRatio > 1.4f -> 0.4f
            // "Square-ish" 4:3 tablets
            else -> 0.3f
        }
        val joystickSize = maxHeight * sizePercentage

        Box(
            modifier = Modifier.size(joystickSize),
            contentAlignment = Alignment.Center
        ) {
            Joystick_RC3D()
        }
    }
}

// Our powerful new custom layout
@Composable
fun ErgonomicRow(
    modifier: Modifier = Modifier,
    centerContent: @Composable () -> Unit,
    sideContent: @Composable () -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->

        val sideConstraints = constraints.copy(minWidth = 0)
        // 1. Measure side controls first
        val leftPlaceable = subcompose("left") { sideContent() }.first().measure(sideConstraints)
        val rightPlaceable = subcompose("right") { sideContent() }.first().measure(sideConstraints)

        // 2. Calculate remaining width for the center
        val centerWidth = constraints.maxWidth - leftPlaceable.width - rightPlaceable.width
        val coercedCenterWidth = centerWidth.coerceAtLeast(0)
        // 3. Measure center content with exact remaining space
        val centerPlaceable = subcompose("center") { centerContent() }
            .first()
            .measure(
                constraints.copy(
                    minWidth = coercedCenterWidth,
                    maxWidth = coercedCenterWidth
                )
            )

        // 4. Place all components
        layout(constraints.maxWidth, constraints.maxHeight) {
            leftPlaceable.placeRelative(0, 0)
            centerPlaceable.placeRelative(leftPlaceable.width, 0)
            rightPlaceable.placeRelative(leftPlaceable.width + centerPlaceable.width, 0)
        }
    }
}
@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    SideEffect {
        val activity = context as? Activity ?: return@SideEffect
        if (activity.requestedOrientation != orientation) {
            activity.requestedOrientation = orientation
        }
    }
}



@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Preview(device = "spec:width=800dp,height=600dp,dpi=240")

@Preview(device = "spec:width=2340px,height=1080px,dpi=440")
@Preview(device = "spec:width=2520px,height=1080px,dpi=440")
@Preview(device = "spec:width=1920px,height=1080px,dpi=420")
@Preview(device = Devices.AUTOMOTIVE_1024p)
@Preview(device = "spec:width=2560px,height=1600px,dpi=320")
@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Preview(device = "spec:width=2048px,height=1536px,dpi=320")
@Composable
fun RcScreenPreview() {
    RcScreen()
}
