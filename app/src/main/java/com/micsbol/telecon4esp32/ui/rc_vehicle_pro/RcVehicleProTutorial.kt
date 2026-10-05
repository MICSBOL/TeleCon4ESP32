package com.micsbol.telecon4esp32.ui.rc_vehicle_pro

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothViewModel
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.safeHudPadding
import com.micsbol.telecon4esp32.ui.home.LocalTutorialAnchors
import com.micsbol.telecon4esp32.ui.home.TutorialAnchor
import com.micsbol.telecon4esp32.ui.home.TutorialDimScrim
import com.micsbol.telecon4esp32.ui.home.TutorialOutsideHoleBlocker
import com.micsbol.telecon4esp32.ui.home.TutorialTouchPolicy
import com.micsbol.telecon4esp32.ui.theme.AppGlass
import com.micsbol.telecon4esp32.ui.theme.Neo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RcVehicleProTutorialStep(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val icon: ImageVector,
    val anchors: List<TutorialAnchor>,
    val touchPolicy: TutorialTouchPolicy,
    val showsNextButton: Boolean,
) {
    CAMERA(
        titleRes = R.string.rc_vehicle_tutorial_camera_title,
        bodyRes = R.string.rc_vehicle_tutorial_camera_body,
        icon = Icons.Filled.Videocam,
        anchors = emptyList(),
        touchPolicy = TutorialTouchPolicy.All,
        showsNextButton = true,
    ),
    TOP_BAR(
        titleRes = R.string.rc_vehicle_tutorial_top_bar_title,
        bodyRes = R.string.rc_vehicle_tutorial_top_bar_body,
        icon = Icons.Filled.Speed,
        anchors = listOf(TutorialAnchor.RC_TOP_BAR),
        touchPolicy = TutorialTouchPolicy.All,
        showsNextButton = true,
    ),
    THROTTLE(
        titleRes = R.string.rc_vehicle_tutorial_throttle_title,
        bodyRes = R.string.rc_vehicle_tutorial_throttle_body,
        icon = Icons.Filled.SwapVert,
        anchors = listOf(TutorialAnchor.RC_THROTTLE),
        touchPolicy = TutorialTouchPolicy.HolesOnly,
        showsNextButton = false,
    ),
    STEERING(
        titleRes = R.string.rc_vehicle_tutorial_steering_title,
        bodyRes = R.string.rc_vehicle_tutorial_steering_body,
        icon = Icons.Filled.SwapHoriz,
        anchors = listOf(TutorialAnchor.RC_STEERING),
        touchPolicy = TutorialTouchPolicy.HolesOnly,
        showsNextButton = false,
    ),
    STICK_SETUP(
        titleRes = R.string.rc_vehicle_tutorial_stick_setup_title,
        bodyRes = R.string.rc_vehicle_tutorial_stick_setup_body,
        icon = Icons.Filled.Tune,
        anchors = listOf(TutorialAnchor.RC_THROTTLE, TutorialAnchor.RC_STEERING),
        touchPolicy = TutorialTouchPolicy.HolesOnly,
        showsNextButton = false,
    ),
    TRIM(
        titleRes = R.string.rc_vehicle_tutorial_trim_title,
        bodyRes = R.string.rc_vehicle_tutorial_trim_body,
        icon = Icons.Filled.OpenWith,
        anchors = listOf(TutorialAnchor.RC_THROTTLE, TutorialAnchor.RC_STEERING),
        touchPolicy = TutorialTouchPolicy.All,
        showsNextButton = true,
    ),
    CENTER_ACTIONS(
        titleRes = R.string.rc_vehicle_tutorial_center_title,
        bodyRes = R.string.rc_vehicle_tutorial_center_body,
        icon = Icons.Filled.CameraAlt,
        anchors = listOf(TutorialAnchor.RC_CENTER),
        touchPolicy = TutorialTouchPolicy.All,
        showsNextButton = true,
    ),
    TELEMETRY(
        titleRes = R.string.rc_vehicle_tutorial_plot_title,
        bodyRes = R.string.rc_vehicle_tutorial_plot_body,
        icon = Icons.AutoMirrored.Filled.ShowChart,
        anchors = listOf(TutorialAnchor.RC_PLOT),
        touchPolicy = TutorialTouchPolicy.All,
        showsNextButton = true,
    ),
    CAMERA_PAN(
        titleRes = R.string.rc_vehicle_tutorial_pan_title,
        bodyRes = R.string.rc_vehicle_tutorial_pan_body,
        icon = Icons.Filled.CenterFocusStrong,
        anchors = listOf(TutorialAnchor.RC_CAMERA_PAN),
        touchPolicy = TutorialTouchPolicy.All,
        showsNextButton = true,
    ),
    ;

    val isLast: Boolean
        get() = this == CAMERA_PAN
}

val LocalRcVehicleProTutorial =
    compositionLocalOf<RcVehicleProTutorialViewModel?> { null }

@HiltViewModel
class RcVehicleProTutorialViewModel @Inject constructor() : ViewModel() {

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val _step = MutableStateFlow(RcVehicleProTutorialStep.CAMERA)
    val step: StateFlow<RcVehicleProTutorialStep> = _step.asStateFlow()

    fun start() {
        _step.value = RcVehicleProTutorialStep.CAMERA
        _active.value = true
    }

    fun dismiss() {
        _active.value = false
    }

    fun show(step: RcVehicleProTutorialStep) {
        _step.value = step
    }

    /** Stick options menu opened while the stick-setup step is showing. */
    fun onStickOptionsOpened() {
        if (_active.value && _step.value == RcVehicleProTutorialStep.STICK_SETUP) {
            show(RcVehicleProTutorialStep.TRIM)
        }
    }
}

@Composable
fun RcVehicleProTutorialOverlay(
    step: RcVehicleProTutorialStep,
    navController: NavHostController,
    bluetoothViewModel: BluetoothViewModel,
    tutorialViewModel: RcVehicleProTutorialViewModel,
) {
    var coverScreen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        var seenRc = false
        navController.currentBackStackEntryFlow.collect { entry ->
            val route = entry.destination.route.orEmpty()
            val onRc = route == Screen.RcVehiclePro.route
            val alongside = route.startsWith("app_settings/${ApplicationId.RC_VEHICLE_PRO.name}") ||
                route == Screen.Bluetooth.route
            if (onRc) seenRc = true
            coverScreen = onRc
            if (seenRc && !onRc && !alongside) {
                tutorialViewModel.dismiss()
            }
        }
    }
    if (!coverScreen) return

    BackHandler(onBack = tutorialViewModel::dismiss)

    val configuration = LocalConfiguration.current
    val cardMaxHeight = (configuration.screenHeightDp * 0.5f).dp.coerceIn(160.dp, 300.dp)
    val closeDescription = stringResource(R.string.first_connection_tutorial_close)
    var handling by remember(step) { mutableStateOf(false) }
    val cardInteraction = remember { MutableInteractionSource() }
    val registry = LocalTutorialAnchors.current
    val density = LocalDensity.current
    val holePadPx = with(density) { 8.dp.toPx() }
    val holes = step.anchors.mapNotNull { anchor ->
        registry.bounds[anchor]?.inflate(holePadPx)
    }
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(step) {
        watchRcVehicleTutorial(
            step = step,
            bluetoothViewModel = bluetoothViewModel,
            tutorialViewModel = tutorialViewModel,
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(21f)
            .onGloballyPositioned { overlayOrigin = it.positionInRoot() },
    ) {
        TutorialDimScrim(holes = holes)
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        if (step.touchPolicy == TutorialTouchPolicy.HolesOnly) {
            val localHoles = holes.map { hole ->
                hole.translate(-overlayOrigin.x, -overlayOrigin.y)
            }
            TutorialOutsideHoleBlocker(
                holes = localHoles,
                widthPx = widthPx,
                heightPx = heightPx,
            )
        }

        val placement = rcTutorialCardPlacement(
            holes = holes,
            widthPx = widthPx,
            heightPx = heightPx,
        )
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
                            RcVehicleProTutorialStep.entries.size,
                        ),
                        color = Neo.Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.rc_vehicle_tutorial_stage),
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

            Text(
                text = stringResource(step.bodyRes),
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
                                R.string.first_connection_tutorial_next
                            },
                        ),
                        onClick = {
                            if (handling) return@NeoPillButton
                            handling = true
                            advanceRcVehicleTutorial(step, tutorialViewModel)
                        },
                        compact = true,
                    )
                }
            }
        }
    }
}

private suspend fun watchRcVehicleTutorial(
    step: RcVehicleProTutorialStep,
    bluetoothViewModel: BluetoothViewModel,
    tutorialViewModel: RcVehicleProTutorialViewModel,
) {
    when (step) {
        RcVehicleProTutorialStep.THROTTLE -> {
            bluetoothViewModel.rcLeftStickPosition.collect { (x, y) ->
                if (abs(x) > 0.12f || abs(y) > 0.12f) {
                    tutorialViewModel.show(RcVehicleProTutorialStep.STEERING)
                }
            }
        }
        RcVehicleProTutorialStep.STEERING -> {
            bluetoothViewModel.rcRightStickPosition.collect { (x, y) ->
                if (abs(x) > 0.12f || abs(y) > 0.12f) {
                    tutorialViewModel.show(RcVehicleProTutorialStep.STICK_SETUP)
                }
            }
        }
        else -> Unit
    }
}

private fun advanceRcVehicleTutorial(
    step: RcVehicleProTutorialStep,
    tutorialViewModel: RcVehicleProTutorialViewModel,
) {
    val next = RcVehicleProTutorialStep.entries.getOrNull(step.ordinal + 1)
    if (next == null) {
        tutorialViewModel.dismiss()
    } else {
        tutorialViewModel.show(next)
    }
}

private data class RcTutorialCardPlacement(
    val alignment: Alignment,
    val widthFraction: Float,
)

/** Keeps the explanation card on the side with more room outside the clear areas. */
private fun rcTutorialCardPlacement(
    holes: List<Rect>,
    widthPx: Float,
    heightPx: Float,
): RcTutorialCardPlacement {
    if (holes.isEmpty() || widthPx <= 0f || heightPx <= 0f) {
        return RcTutorialCardPlacement(Alignment.BottomCenter, 0.72f)
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
    val fraction = if (widthPx > heightPx * 1.15f) 0.48f else 0.92f
    return RcTutorialCardPlacement(alignment, fraction)
}
