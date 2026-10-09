package com.micsbol.telecon4esp32.ui.feature_tour

import android.content.pm.ActivityInfo
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.control_panel.LockScreenOrientation
import com.micsbol.telecon4esp32.ui.cyber.screens.homeModuleFanPose
import com.micsbol.telecon4esp32.ui.cyber.screens.rotateHomeModuleDeckNext
import com.micsbol.telecon4esp32.ui.cyber.screens.rotateHomeModuleDeckPrevious
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.theme.AppGlass
import com.micsbol.telecon4esp32.ui.theme.Neo
import kotlin.math.abs

@Composable
fun ModuleFeatureMenuScreen(
    applicationId: ApplicationId,
    navController: NavController,
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT)
    val features = remember(applicationId) { moduleFeatures(applicationId) }
    val titleRes = when (applicationId) {
        ApplicationId.CONTROL_PANEL -> R.string.app_control_panel_title
        ApplicationId.RC_VEHICLE_PRO -> R.string.app_rc_vehicle_title
        else -> R.string.app_control_panel_title
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Neo.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.about_back),
                    tint = Neo.TextPrimary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(titleRes),
                    color = Neo.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.feature_menu_subtitle),
                    color = Neo.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FeatureCardDeck(
            applicationId = applicationId,
            features = features,
            onOpen = { feature ->
                navController.navigate(
                    Screen.FeaturePractice.createRoute(applicationId, feature.id),
                )
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun FeatureCardDeck(
    applicationId: ApplicationId,
    features: List<ModuleFeature>,
    onOpen: (ModuleFeature) -> Unit,
    modifier: Modifier = Modifier,
) {
    val orderSaver = remember(applicationId) {
        listSaver<List<ModuleFeatureId>, String>(
            save = { order -> order.map { it.name } },
            restore = { names ->
                val ids = moduleFeatures(applicationId).map { it.id }
                val restored = names.mapNotNull { name -> ids.firstOrNull { it.name == name } }
                if (restored.size == ids.size) restored else ids
            },
        )
    }
    var deckOrder by rememberSaveable(applicationId, stateSaver = orderSaver) {
        mutableStateOf(moduleFeatures(applicationId).map { it.id })
    }
    var dragPx by remember { mutableFloatStateOf(0f) }
    val byId = remember(features) { features.associateBy { it.id } }
    val currentOrder = rememberUpdatedState(deckOrder)
    val currentById = rememberUpdatedState(byId)
    val currentOnOpen = rememberUpdatedState(onOpen)
    val swipeThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
    val swipeHint = stringResource(R.string.feature_menu_swipe)
    val cardSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val offsetSpring = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    Box(
        modifier = modifier
            .pointerInput(features) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalX = 0f
                    var totalY = 0f
                    var dragging = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val delta = change.positionChange()
                            totalX += delta.x
                            totalY += delta.y
                            if (!dragging && (abs(totalX) > slop || abs(totalY) > slop)) {
                                dragging = abs(totalX) > abs(totalY)
                            }
                            if (dragging) {
                                change.consume()
                                dragPx += delta.x
                            }
                        }
                    } finally {
                        if (dragging && abs(totalX) >= swipeThresholdPx) {
                            dragPx = 0f
                            deckOrder = if (totalX < 0f) {
                                rotateHomeModuleDeckNext(currentOrder.value)
                            } else {
                                rotateHomeModuleDeckPrevious(currentOrder.value)
                            }
                        } else {
                            dragPx = 0f
                            if (abs(totalX) <= slop && abs(totalY) <= slop) {
                                currentOrder.value.firstOrNull()
                                    ?.let { currentById.value[it] }
                                    ?.let { currentOnOpen.value(it) }
                            }
                        }
                    }
                }
            }
            .semantics { contentDescription = swipeHint },
    ) {
        deckOrder.asReversed().forEach { id ->
            val feature = byId[id] ?: return@forEach
            key(id) {
                val depth = deckOrder.indexOf(id)
                val isFront = depth == 0
                val pose = homeModuleFanPose(depth = depth, count = deckOrder.size)
                val offsetX by animateDpAsState(pose.offsetX, offsetSpring, label = "featureDeckX")
                val offsetY by animateDpAsState(pose.offsetY, offsetSpring, label = "featureDeckY")
                val rotation by animateFloatAsState(pose.rotation, cardSpring, label = "featureDeckRot")
                val scale by animateFloatAsState(pose.scale, cardSpring, label = "featureDeckScale")
                FeatureLessonCard(
                    feature = feature,
                    isFront = isFront,
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .fillMaxHeight(0.78f)
                        .align(Alignment.Center)
                        .offset(x = offsetX, y = offsetY)
                        .zIndex((features.size - depth).toFloat())
                        .graphicsLayer {
                            val drag = if (isFront) dragPx else -dragPx * 0.18f
                            translationX = drag
                            rotationZ = rotation + if (isFront) dragPx / 28f else 0f
                            scaleX = scale
                            scaleY = scale
                            shadowElevation = pose.elevation
                            cameraDistance = 14f * density
                        },
                )
            }
        }
    }
}

@Composable
private fun FeatureLessonCard(
    feature: ModuleFeature,
    isFront: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(AppGlass.BackgroundMid)
            .border(
                width = if (isFront) 2.dp else 1.dp,
                color = if (isFront) Neo.Accent else Neo.TextMuted.copy(alpha = 0.55f),
                shape = shape,
            )
            .padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = feature.icon,
            contentDescription = null,
            tint = Neo.Accent,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Neo.Accent.copy(alpha = 0.16f))
                .padding(14.dp),
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(feature.titleRes),
            color = Neo.TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(feature.subtitleRes),
            color = Neo.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
