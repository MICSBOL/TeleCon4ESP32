package com.micsbol.telecon4esp32.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothViewModel
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.safeHudPadding
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.theme.AppGlass
import com.micsbol.telecon4esp32.ui.theme.Neo
import kotlin.math.abs

@Composable
fun FirstConnectionTutorialOverlay(
    step: FirstConnectionTutorialStep,
    navController: NavHostController,
    bluetoothViewModel: BluetoothViewModel,
    tutorialViewModel: FirstConnectionTutorialViewModel,
) {
    BackHandler(onBack = tutorialViewModel::dismiss)

    val configuration = LocalConfiguration.current
    val cardMaxHeight = if (step == FirstConnectionTutorialStep.SERIAL_MONITOR) {
        (configuration.screenHeightDp * 0.78f).dp.coerceIn(320.dp, 640.dp)
    } else {
        (configuration.screenHeightDp * 0.42f).dp.coerceIn(150.dp, 280.dp)
    }
    val closeDescription = stringResource(R.string.first_connection_tutorial_close)
    var handling by remember(step) { mutableStateOf(false) }
    val scrimInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    val registry = LocalTutorialAnchors.current
    val density = LocalDensity.current
    val holePadPx = with(density) { 8.dp.toPx() }
    val holes = step.anchors.mapNotNull { anchor ->
        registry.bounds[anchor]?.inflate(holePadPx)
    }
    val codeExportOpen by tutorialViewModel.codeExportOpen.collectAsState()
    val liftScrim = codeExportOpen &&
        step == FirstConnectionTutorialStep.CONFIRM_CLASSIC_SIMPLE
    val touchPolicy = if (liftScrim) TutorialTouchPolicy.All else step.touchPolicy
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(step) {
        watchTutorialAction(
            step = step,
            navController = navController,
            bluetoothViewModel = bluetoothViewModel,
            tutorialViewModel = tutorialViewModel,
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(20f)
            .onGloballyPositioned { overlayOrigin = it.positionInRoot() },
    ) {
        if (!liftScrim) {
            TutorialDimScrim(holes = holes)
        }
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        when (touchPolicy) {
            TutorialTouchPolicy.Block -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = scrimInteraction,
                            indication = null,
                            onClick = {},
                        ),
                )
            }
            TutorialTouchPolicy.HolesOnly -> {
                val localHoles = holes.map { hole ->
                    hole.translate(-overlayOrigin.x, -overlayOrigin.y)
                }
                if (localHoles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = scrimInteraction,
                                indication = null,
                                onClick = {},
                            ),
                    )
                } else {
                    TutorialOutsideHoleBlocker(
                        holes = localHoles,
                        widthPx = widthPx,
                        heightPx = heightPx,
                    )
                }
            }
            TutorialTouchPolicy.All -> Unit
        }

        val placement = if (step == FirstConnectionTutorialStep.SCAN_AND_CONNECT) {
            TutorialCardPlacement(Alignment.BottomCenter, 1f)
        } else {
            tutorialCardPlacement(
                holes = if (liftScrim) emptyList() else holes,
                widthPx = with(density) { maxWidth.toPx() },
                heightPx = with(density) { maxHeight.toPx() },
            )
        }
        val cardOnTop = placement.alignment == Alignment.TopCenter ||
            placement.alignment == Alignment.TopEnd
        val cardWidth = (maxWidth * placement.widthFraction).coerceAtMost(480.dp)

        Column(
            modifier = Modifier
                .align(placement.alignment)
                .safeHudPadding(
                    includeTop = cardOnTop,
                    includeBottom = !cardOnTop,
                    includeHorizontal = true,
                )
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .width(cardWidth)
                .heightIn(max = cardMaxHeight)
                .clip(Neo.CardShape)
                .background(AppGlass.DialogSurface.copy(alpha = 0.96f))
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = Neo.Accent,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Neo.Accent.copy(alpha = 0.16f))
                        .padding(8.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.first_connection_tutorial_step_progress,
                            step.ordinal + 1,
                            FirstConnectionTutorialStep.entries.size,
                        ),
                        color = Neo.Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(step.stageRes),
                        color = Neo.TextSecondary,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = stringResource(step.titleRes),
                        color = Neo.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = closeDescription,
                    tint = Neo.TextSecondary,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(
                            onClick = tutorialViewModel::dismiss,
                            role = Role.Button,
                        )
                        .padding(6.dp),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (step == FirstConnectionTutorialStep.SERIAL_MONITOR) {
                Image(
                    painter = painterResource(R.drawable.tutorial_serial_monitor),
                    contentDescription = stringResource(
                        R.string.first_connection_tutorial_serial_image,
                    ),
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = stringResource(
                    if (liftScrim) {
                        R.string.first_connection_tutorial_confirm_save_body
                    } else {
                        step.bodyRes
                    },
                ),
                color = Neo.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )

            if (step.showsNextButton) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    NeoPillButton(
                        text = stringResource(
                            if (step.isLast) {
                                R.string.first_connection_tutorial_done
                            } else {
                                step.actionRes
                            },
                        ),
                        onClick = {
                            if (handling) return@NeoPillButton
                            handling = true
                            advanceFirstConnectionTutorial(
                                step = step,
                                navController = navController,
                                tutorialViewModel = tutorialViewModel,
                            )
                        },
                        compact = true,
                    )
                }
            }
        }
    }
}

private suspend fun watchTutorialAction(
    step: FirstConnectionTutorialStep,
    navController: NavHostController,
    bluetoothViewModel: BluetoothViewModel,
    tutorialViewModel: FirstConnectionTutorialViewModel,
) {
    when (step) {
        FirstConnectionTutorialStep.OPEN_CONTROL_PANEL -> {
            navController.currentBackStackEntryFlow.collect { entry ->
                if (entry.destination.route == Screen.ControlPanel.route) {
                    tutorialViewModel.applyStarterConfiguration {
                        tutorialViewModel.show(FirstConnectionTutorialStep.OPEN_SETTINGS)
                    }
                }
            }
        }
        FirstConnectionTutorialStep.OPEN_SETTINGS -> {
            navController.currentBackStackEntryFlow.collect { entry ->
                val route = entry.destination.route.orEmpty()
                if (route.startsWith("app_settings")) {
                    tutorialViewModel.show(FirstConnectionTutorialStep.CONFIRM_CLASSIC_SIMPLE)
                }
            }
        }
        FirstConnectionTutorialStep.OPEN_BLUETOOTH -> {
            if (bluetoothViewModel.isSessionActiveFor(ApplicationId.CONTROL_PANEL)) {
                tutorialViewModel.show(FirstConnectionTutorialStep.MOVE_STICK)
                return
            }
            navController.currentBackStackEntryFlow.collect { entry ->
                if (entry.destination.route == Screen.Bluetooth.route) {
                    tutorialViewModel.show(FirstConnectionTutorialStep.SCAN_AND_CONNECT)
                }
            }
        }
        FirstConnectionTutorialStep.SCAN_AND_CONNECT -> {
            bluetoothViewModel.state.collect { state ->
                val session = state.activeSession
                if (state.isConnected && session?.applicationId == ApplicationId.CONTROL_PANEL) {
                    tutorialViewModel.show(FirstConnectionTutorialStep.MOVE_STICK)
                }
            }
        }
        FirstConnectionTutorialStep.MOVE_STICK -> {
            bluetoothViewModel.rcLeftStickPosition.collect { (x, y) ->
                if (abs(x) > 0.12f || abs(y) > 0.12f) {
                    tutorialViewModel.show(FirstConnectionTutorialStep.STICK_SETUP)
                }
            }
        }
        FirstConnectionTutorialStep.CONFIRM_CLASSIC_SIMPLE -> {
            tutorialViewModel.matchingCodeSaved.collect {
                tutorialViewModel.show(FirstConnectionTutorialStep.SERIAL_MONITOR)
            }
        }
        FirstConnectionTutorialStep.SERIAL_MONITOR,
        FirstConnectionTutorialStep.STICK_SETUP,
        FirstConnectionTutorialStep.OTHER_CONTROLS,
        FirstConnectionTutorialStep.CENTER_EXTRAS,
        -> Unit
    }
}

private fun advanceFirstConnectionTutorial(
    step: FirstConnectionTutorialStep,
    navController: NavHostController,
    tutorialViewModel: FirstConnectionTutorialViewModel,
) {
    when (step) {
        FirstConnectionTutorialStep.SERIAL_MONITOR -> {
            if (navController.currentDestination?.route?.startsWith("app_settings") == true) {
                navController.navigateUp()
            }
            tutorialViewModel.show(FirstConnectionTutorialStep.OPEN_BLUETOOTH)
        }
        FirstConnectionTutorialStep.OTHER_CONTROLS ->
            tutorialViewModel.show(FirstConnectionTutorialStep.CENTER_EXTRAS)
        FirstConnectionTutorialStep.CENTER_EXTRAS -> tutorialViewModel.dismiss()
        else -> Unit
    }
}

private data class TutorialCardPlacement(
    val alignment: Alignment,
    val widthFraction: Float,
)

/** Keeps the explanation card on the side with more room outside the clear areas. */
private fun tutorialCardPlacement(
    holes: List<Rect>,
    widthPx: Float,
    heightPx: Float,
): TutorialCardPlacement {
    if (holes.isEmpty() || widthPx <= 0f || heightPx <= 0f) {
        return TutorialCardPlacement(Alignment.BottomCenter, 1f)
    }
    val union = holes.reduce { acc, rect ->
        Rect(
            left = minOf(acc.left, rect.left),
            top = minOf(acc.top, rect.top),
            right = maxOf(acc.right, rect.right),
            bottom = maxOf(acc.bottom, rect.bottom),
        )
    }
    val onLeft = union.center.x < widthPx * 0.55f
    val freeTop = union.top
    val freeBottom = heightPx - union.bottom
    val alignment = when {
        freeBottom >= freeTop -> if (onLeft) Alignment.BottomEnd else Alignment.BottomCenter
        else -> if (onLeft) Alignment.TopEnd else Alignment.TopCenter
    }
    val fraction = if (widthPx > heightPx * 1.15f) 0.48f else 1f
    return TutorialCardPlacement(alignment, fraction)
}
