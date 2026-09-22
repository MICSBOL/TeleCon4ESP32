package com.micsbol.telecon4esp32.ui.codes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
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
import com.micsbol.telecon4esp32.ui.applications.titleRes
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoDialog
import com.micsbol.telecon4esp32.ui.components.NeoDialogBody
import com.micsbol.telecon4esp32.ui.components.NeoDialogTitle
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeoScaffold
import com.micsbol.telecon4esp32.ui.components.NeoSecondaryButton
import com.micsbol.telecon4esp32.ui.cyber.screens.homeModuleFanPose
import com.micsbol.telecon4esp32.ui.cyber.screens.rotateHomeModuleDeckNext
import com.micsbol.telecon4esp32.ui.cyber.screens.rotateHomeModuleDeckPrevious
import com.micsbol.telecon4esp32.ui.theme.AppGlass
import com.micsbol.telecon4esp32.ui.theme.Neo
import kotlin.math.abs

private enum class PdfDialogKind {
    NoReader,
    CopyFailed,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodesScreen(
    navController: NavController,
    applicationId: ApplicationId,
) {
    val context = LocalContext.current
    var pdfDialogKind by remember { mutableStateOf<PdfDialogKind?>(null) }
    val documentationAssets = remember { documentationAssetsFor(currentCodeAssetLanguage()) }

    pdfDialogKind?.let { kind ->
        NeoDialog(
            onDismissRequest = { pdfDialogKind = null },
            title = {
                NeoDialogTitle(
                    text = stringResource(
                        when (kind) {
                            PdfDialogKind.NoReader -> R.string.codes_pdf_no_reader_title
                            PdfDialogKind.CopyFailed -> R.string.codes_pdf_open_failed_title
                        }
                    )
                )
            },
            subtitle = {
                NeoDialogBody(
                    text = stringResource(
                        when (kind) {
                            PdfDialogKind.NoReader -> R.string.codes_pdf_no_reader_message
                            PdfDialogKind.CopyFailed -> R.string.codes_pdf_open_failed_message
                        }
                    )
                )
            },
            actions = {
                when (kind) {
                    PdfDialogKind.NoReader -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NeoSecondaryButton(
                                text = stringResource(R.string.codes_pdf_cancel),
                                onClick = { pdfDialogKind = null },
                                compact = true,
                            )
                            NeoPillButton(
                                text = stringResource(R.string.codes_pdf_find_reader),
                                onClick = {
                                    context.launchPlayStorePdfReaderSearch()
                                    pdfDialogKind = null
                                },
                                compact = true,
                            )
                        }
                    }

                    PdfDialogKind.CopyFailed -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            NeoPillButton(
                                text = stringResource(R.string.codes_dialog_ok),
                                onClick = { pdfDialogKind = null },
                                compact = true,
                            )
                        }
                    }
                }
            }
        )
    }

    val navigateBack: () -> Unit = { navController.navigateUp() }

    BackHandler(onBack = navigateBack)

    NeoScaffold(
        title = stringResource(applicationId.titleRes()),
        onNavigateBack = navigateBack,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            if (documentationAssets.isEmpty()) {
                NeoCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
                    Text(
                        text = stringResource(R.string.application_codes_empty_title),
                        color = Neo.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.application_codes_empty_message),
                        color = Neo.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.codes_documents_desk_title),
                    color = Neo.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )
                DocumentationCardDeck(
                    assets = documentationAssets,
                    onAssetClick = { asset ->
                        val result = when {
                            asset.remoteUrlRes != null ->
                                openPdfUrlExternally(
                                    context,
                                    context.getString(asset.remoteUrlRes),
                                )

                            asset.assetFileName != null ->
                                openPdfAssetExternally(context, asset.assetFileName)

                            else -> PdfOpenResult.CopyFailed
                        }
                        when (result) {
                            PdfOpenResult.Success -> Unit
                            PdfOpenResult.NoActivity ->
                                pdfDialogKind = PdfDialogKind.NoReader

                            PdfOpenResult.CopyFailed ->
                                pdfDialogKind = PdfDialogKind.CopyFailed
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.codes_documents_swipe_hint),
                    color = Neo.TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DocumentationCardDeck(
    assets: List<CodeAssetInfo>,
    onAssetClick: (CodeAssetInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val assetKeys = remember(assets) {
        assets.map { it.assetFileName ?: it.titleRes.toString() }
    }
    var deckOrder by remember(assetKeys) { mutableStateOf(assetKeys) }
    val byKey = remember(assets) {
        assets.associateBy { it.assetFileName ?: it.titleRes.toString() }
    }
    var dragPx by remember { mutableFloatStateOf(0f) }
    val localDensity = LocalDensity.current
    val swipeThresholdPx = with(localDensity) { 64.dp.toPx() }
    val swipeHint = stringResource(R.string.codes_documents_swipe_content_description)
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
            .pointerInput(assetKeys) {
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var dragging = false
                    var accumulated = 0f
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val delta = change.positionChange().x
                            accumulated += delta
                            if (!dragging && abs(accumulated) > slop) {
                                dragging = true
                            }
                            if (dragging) {
                                change.consume()
                                dragPx += delta
                            }
                        }
                    } finally {
                        if (dragging) {
                            val dx = dragPx
                            dragPx = 0f
                            deckOrder = when {
                                dx <= -swipeThresholdPx -> rotateHomeModuleDeckNext(deckOrder)
                                dx >= swipeThresholdPx -> rotateHomeModuleDeckPrevious(deckOrder)
                                else -> deckOrder
                            }
                        } else {
                            dragPx = 0f
                        }
                    }
                }
            }
            .semantics { contentDescription = swipeHint },
    ) {
        deckOrder.asReversed().forEach { keyId ->
            val asset = byKey[keyId] ?: return@forEach
            key(keyId) {
                val depth = deckOrder.indexOf(keyId)
                val isFront = depth == 0
                val pose = homeModuleFanPose(depth = depth, count = deckOrder.size)
                val offsetX by animateDpAsState(
                    targetValue = pose.offsetX,
                    animationSpec = offsetSpring,
                    label = "docDeckOffsetX",
                )
                val offsetY by animateDpAsState(
                    targetValue = pose.offsetY,
                    animationSpec = offsetSpring,
                    label = "docDeckOffsetY",
                )
                val rotation by animateFloatAsState(
                    targetValue = pose.rotation,
                    animationSpec = cardSpring,
                    label = "docDeckRotation",
                )
                val scale by animateFloatAsState(
                    targetValue = pose.scale,
                    animationSpec = cardSpring,
                    label = "docDeckScale",
                )
                val elevation by animateFloatAsState(
                    targetValue = pose.elevation,
                    animationSpec = cardSpring,
                    label = "docDeckElevation",
                )
                DocumentationGuideCard(
                    asset = asset,
                    isFront = isFront,
                    onClick = {
                        if (isFront) onAssetClick(asset)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .fillMaxHeight(0.92f)
                        .align(Alignment.Center)
                        .offset(x = offsetX, y = offsetY)
                        .zIndex((assets.size - depth).toFloat())
                        .graphicsLayer {
                            val drag = if (isFront) dragPx else -dragPx * 0.18f
                            translationX = drag
                            rotationZ = rotation + if (isFront) dragPx / 28f else 0f
                            scaleX = scale
                            scaleY = scale
                            alpha = if (isFront) 1f else 0.68f
                            shadowElevation = elevation
                            cameraDistance = 14f * density
                        },
                )
            }
        }
    }
}

@Composable
private fun DocumentationGuideCard(
    asset: CodeAssetInfo,
    isFront: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(asset.titleRes)
    val subtitle = asset.subtitleRes?.let { stringResource(it) }
    val illustration = asset.illustrationRes
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        label = "docCardPress",
    )
    val cardShape = RoundedCornerShape(18.dp)
    val innerShape = RoundedCornerShape(12.dp)
    val borderColor = if (isFront) Neo.AccentHighlight else Neo.TextMuted.copy(alpha = 0.45f)
    val titleColor = if (isFront) Neo.TextPrimary else Neo.TextMuted
    val bodyColor = if (isFront) Neo.TextSecondary else Neo.TextMuted.copy(alpha = 0.7f)
    val borderWidth = if (isFront) 3.dp else 1.5.dp

    Column(
        modifier = modifier
            .clip(cardShape)
            .background(if (isFront) AppGlass.BackgroundMid else AppGlass.BackgroundTop)
            .border(width = borderWidth, color = borderColor, shape = cardShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(if (isFront) 14.dp else 10.dp),
    ) {
        Text(
            text = stringResource(R.string.codes_documents_desk_title),
            color = if (isFront) Neo.AccentHighlight else Neo.TextMuted,
            fontSize = if (isFront) 10.sp else 8.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = titleColor,
            fontSize = if (isFront) 20.sp else 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(min = 0.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .scale(pressScale)
                .clip(innerShape)
                .background(
                    if (isFront) {
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color(0xFFE3F2FD),
                                androidx.compose.ui.graphics.Color(0xFFBBDEFB),
                            ),
                        )
                    } else {
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color(0xFF455A64),
                                androidx.compose.ui.graphics.Color(0xFF37474F),
                            ),
                        )
                    },
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (illustration != null) {
                Image(
                    painter = painterResource(illustration),
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .fillMaxHeight(0.92f),
                )
            }
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = subtitle,
                color = bodyColor,
                fontSize = if (isFront) 13.sp else 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isFront) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.codes_documents_open_pdf),
                color = Neo.AccentHighlight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
