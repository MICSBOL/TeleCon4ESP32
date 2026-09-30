package com.micsbol.telecon4esp32.ui.cyber.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.micsbol.telecon4esp32.BuildConfig
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.ActiveBluetoothSession
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.CoinWalletState
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.PremiumSource
import com.micsbol.telecon4esp32.domain.model.hasEntryAccess
import com.micsbol.telecon4esp32.domain.model.isApplicationCatalogVisible
import com.micsbol.telecon4esp32.domain.model.isFree
import com.micsbol.telecon4esp32.domain.model.resolveHomeFeaturedApplication
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy
import com.micsbol.telecon4esp32.ui.ads.LocalRewardedAdManager
import com.micsbol.telecon4esp32.ui.applications.ApplicationCatalogItem
import com.micsbol.telecon4esp32.ui.applications.ApplicationEntryDialogs
import com.micsbol.telecon4esp32.ui.applications.HomeModuleHeroCard
import com.micsbol.telecon4esp32.ui.applications.applicationBadge
import com.micsbol.telecon4esp32.ui.applications.applicationTrailingAction
import com.micsbol.telecon4esp32.ui.applications.defaultApplicationCatalog
import com.micsbol.telecon4esp32.ui.applications.handleApplicationClick
import com.micsbol.telecon4esp32.ui.applications.thumbnailRes
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothConnectionErrorDialog
import com.micsbol.telecon4esp32.ui.components.NeoIconButton
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeumorphicBackground
import com.micsbol.telecon4esp32.ui.components.glassSurface
import com.micsbol.telecon4esp32.ui.components.safeHudPadding
import com.micsbol.telecon4esp32.ui.control_panel.EnsureVisibleSystemBars
import com.micsbol.telecon4esp32.ui.control_panel.LockScreenOrientation
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.home.HomeHelpDialog
import com.micsbol.telecon4esp32.ui.home.TutorialAnchor
import com.micsbol.telecon4esp32.ui.home.reportTutorialAnchor
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.theme.Neo
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme
import com.micsbol.telecon4esp32.ui.wallet.CoinBalanceChip
import com.micsbol.telecon4esp32.ui.wallet.LocalWallet
import com.micsbol.telecon4esp32.ui.wallet.WalletViewModel

private val ProGold = Color(0xFFFFD54F)
private val ProGoldDeep = Color(0xFFFFB300)
private val QuickUseGreen = Color(0xFF5EE87A)
private val QuickUseGreenDeep = Color(0xFF1B8F3A)
private const val HomeModuleCardWidthFraction = 0.78f
private const val HomeModuleFrontCardScale = 1.06f

/**
 * Home: shipped modules as primary CTAs.
 * Browse Catalog appears only when [isApplicationCatalogVisible] (more than 3 shipped apps).
 */
@Composable
fun CyberHomeScreen(
    navController: NavHostController,
    isConnecting: Boolean = false,
    activeSession: ActiveBluetoothSession? = null,
    lastApplicationId: ApplicationId? = null,
    errorMessage: String? = null,
    connectFailure: com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectFailure? = null,
    handshakeFailure: com.micsbol.telecon4esp32.domain.bluetooth.HandshakeFailure? = null,
    onOpenApplications: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onPlayFirstConnectionTutorial: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val entitlement = LocalEntitlement.current
    val walletViewModel = hiltViewModel<WalletViewModel>()
    val wallet by walletViewModel.wallet.collectAsState()
    val explorerGiftAvailable by walletViewModel.explorerGiftAvailable.collectAsState()
    val rewardedAdManager = LocalRewardedAdManager.current
    val requiresCoinEntry = entitlement.usesCoinEconomy() || BuildConfig.DEBUG

    var comingSoonAppName by remember { mutableStateOf<String?>(null) }
    var unlockTarget by remember { mutableStateOf<ApplicationCatalogItem?>(null) }
    var explorerGiftTarget by remember { mutableStateOf<ApplicationCatalogItem?>(null) }
    var unlockMessage by remember { mutableStateOf<String?>(null) }
    var showPricingTable by remember { mutableStateOf(false) }

    CyberHomeScreenContent(
        isConnecting = isConnecting,
        activeSession = activeSession,
        lastApplicationId = lastApplicationId,
        errorMessage = errorMessage,
        connectFailure = connectFailure,
        handshakeFailure = handshakeFailure,
        entitlement = entitlement,
        wallet = wallet,
        requiresCoinEntry = requiresCoinEntry,
        explorerGiftAvailable = explorerGiftAvailable,
        onDismissError = onDismissError,
        onOpenApplications = onOpenApplications,
        onShowPricingTable = { showPricingTable = true },
        onProClick = { navController.navigate(Screen.Upgrade.route) },
        onHelpAbout = { navController.navigate(Screen.About.route) },
        onCodesClick = { item ->
            navController.navigate(Screen.ApplicationCodes.createRoute(item.id))
        },
        onUnlockClick = { unlockTarget = it },
        onExplorerSparkleClick = { explorerGiftTarget = it },
        onItemClick = { item, title ->
            handleApplicationClick(
                item = item,
                entitlement = entitlement,
                wallet = wallet,
                requiresCoinEntry = requiresCoinEntry,
                navController = navController,
                onComingSoon = { comingSoonAppName = title },
                onRequestUnlock = { unlockTarget = item },
                onRequestUpgrade = { navController.navigate(Screen.Upgrade.route) },
            )
        },
        onPlayFirstConnectionTutorial = onPlayFirstConnectionTutorial,
    )

    ApplicationEntryDialogs(
        navController = navController,
        wallet = wallet,
        requiresCoinEntry = requiresCoinEntry,
        activity = activity,
        rewardedAdManager = rewardedAdManager,
        unlockTarget = unlockTarget,
        onUnlockTargetChange = { unlockTarget = it },
        explorerGiftTarget = explorerGiftTarget,
        onExplorerGiftTargetChange = { explorerGiftTarget = it },
        comingSoonAppName = comingSoonAppName,
        onComingSoonAppNameChange = { comingSoonAppName = it },
        unlockMessage = unlockMessage,
        onUnlockMessageChange = { unlockMessage = it },
        showPricingTable = showPricingTable,
        onShowPricingTableChange = { showPricingTable = it },
        onUnlockFeature = { feature, option, onResult ->
            walletViewModel.unlockFeature(feature, option, onResult)
        },
        onClaimExplorerGift = { feature, onResult ->
            walletViewModel.claimExplorerGift(feature, onResult)
        },
    )
}

@Composable
fun CyberHomeScreenContent(
    isConnecting: Boolean = false,
    activeSession: ActiveBluetoothSession? = null,
    lastApplicationId: ApplicationId? = null,
    errorMessage: String? = null,
    connectFailure: com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectFailure? = null,
    handshakeFailure: com.micsbol.telecon4esp32.domain.bluetooth.HandshakeFailure? = null,
    entitlement: Entitlement = LocalEntitlement.current,
    wallet: CoinWalletState = LocalWallet.current,
    requiresCoinEntry: Boolean = false,
    explorerGiftAvailable: Boolean = false,
    catalog: List<ApplicationCatalogItem>? = null,
    showBrowseCatalog: Boolean = isApplicationCatalogVisible(),
    onDismissError: () -> Unit = {},
    onOpenApplications: () -> Unit = {},
    onShowPricingTable: () -> Unit = {},
    onProClick: () -> Unit = {},
    onHelpAbout: () -> Unit = {},
    onCodesClick: (ApplicationCatalogItem) -> Unit = {},
    onUnlockClick: (ApplicationCatalogItem) -> Unit = {},
    onExplorerSparkleClick: (ApplicationCatalogItem) -> Unit = {},
    onItemClick: (ApplicationCatalogItem, String) -> Unit = { _, _ -> },
    onPlayFirstConnectionTutorial: () -> Unit = {},
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT)
    EnsureVisibleSystemBars()
    val context = LocalContext.current
    val activity = context as? Activity
    var showHelpDialog by remember { mutableStateOf(false) }

    BackHandler { activity?.finish() }

    val isSessionConnected = activeSession != null
    val showProButton = when (entitlement) {
        is Entitlement.Free -> true
        is Entitlement.Premium -> entitlement.source == PremiumSource.DEBUG_OVERRIDE
    }
    val featuredAppId = resolveHomeFeaturedApplication(
        activeSessionApplicationId = activeSession?.applicationId,
        lastApplicationId = lastApplicationId,
        entitlement = entitlement,
        wallet = wallet,
        requiresCoinEntry = requiresCoinEntry,
    )
    val defaultModules = remember { defaultApplicationCatalog() }
    val modules = catalog ?: defaultModules

    Box(modifier = Modifier.fillMaxSize()) {
        NeumorphicBackground()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeHudPadding(
                    includeTop = true,
                    includeBottom = true,
                    includeHorizontal = true,
                ),
        ) {
            val edgePad = if (maxWidth < 400.dp) 16.dp else 20.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = edgePad, vertical = edgePad),
            ) {
                MinimalHomeTopBar(
                    isConnected = isSessionConnected,
                    showCoinChip = requiresCoinEntry,
                    coinBalance = wallet.balance,
                    onCoinClick = onShowPricingTable,
                    onHelpClick = { showHelpDialog = true },
                    onTitleClick = onHelpAbout,
                )

                Spacer(modifier = Modifier.height(14.dp))

                HomeModuleCardDeck(
                    modules = modules,
                    featuredAppId = featuredAppId,
                    entitlement = entitlement,
                    wallet = wallet,
                    requiresCoinEntry = requiresCoinEntry,
                    explorerGiftAvailable = explorerGiftAvailable,
                    isConnecting = isConnecting,
                    onCodesClick = onCodesClick,
                    onUnlockClick = onUnlockClick,
                    onExplorerSparkleClick = onExplorerSparkleClick,
                    onItemClick = onItemClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(10.dp))

                HomeSecondaryActions(
                    isConnecting = isConnecting,
                    showBrowseCatalog = showBrowseCatalog,
                    showProButton = showProButton,
                    onOpenApplications = onOpenApplications,
                    onQuickUseClick = onPlayFirstConnectionTutorial,
                    onProClick = onProClick,
                    onOpenAbout = onHelpAbout,
                    fillButtonWidth = true,
                )
            }
        }

        if (showHelpDialog) {
            HomeHelpDialog(
                onDismissRequest = { showHelpDialog = false },
                onPlayTutorial = {
                    showHelpDialog = false
                    onPlayFirstConnectionTutorial()
                },
            )
        }

        BluetoothConnectionErrorDialog(
            handshakeFailure = handshakeFailure,
            connectFailure = connectFailure,
            errorMessage = errorMessage,
            onDismiss = onDismissError,
        )

        if (isConnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Neo.Background.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Neo.Accent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (activeSession?.deviceName != null) {
                            stringResource(R.string.home_bluetooth_status_connecting) +
                                " ${activeSession.deviceName}"
                        } else {
                            stringResource(R.string.home_bluetooth_status_connecting)
                        },
                        color = Neo.Accent,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeModuleCardDeck(
    modules: List<ApplicationCatalogItem>,
    featuredAppId: ApplicationId,
    entitlement: Entitlement,
    wallet: CoinWalletState,
    requiresCoinEntry: Boolean,
    explorerGiftAvailable: Boolean,
    isConnecting: Boolean,
    onCodesClick: (ApplicationCatalogItem) -> Unit,
    onUnlockClick: (ApplicationCatalogItem) -> Unit,
    onExplorerSparkleClick: (ApplicationCatalogItem) -> Unit,
    onItemClick: (ApplicationCatalogItem, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val moduleIds = remember(modules) { modules.map { it.id } }
    var deckOrder by remember(moduleIds) {
        mutableStateOf(initialHomeModuleDeckOrder(moduleIds, featuredAppId))
    }
    val byId = remember(modules) { modules.associateBy { it.id } }
    var dragPx by remember { mutableFloatStateOf(0f) }
    val localDensity = LocalDensity.current
    val swipeThresholdPx = with(localDensity) { 64.dp.toPx() }
    val swipeHint = stringResource(R.string.home_module_card_swipe_content_description)
    val cardSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val offsetSpring = spring<Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    Box(
        modifier = modifier
            .pointerInput(moduleIds) {
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
            .semantics {
                contentDescription = swipeHint
            },
    ) {
        deckOrder.asReversed().forEach { id ->
            val item = byId[id] ?: return@forEach
            key(id) {
            val depth = deckOrder.indexOf(id)
            val isFront = depth == 0
            val pose = homeModuleFanPose(depth = depth, count = deckOrder.size)
            val offsetX by animateDpAsState(
                targetValue = pose.offsetX,
                animationSpec = offsetSpring,
                label = "moduleDeckOffsetX",
            )
            val offsetY by animateDpAsState(
                targetValue = pose.offsetY,
                animationSpec = offsetSpring,
                label = "moduleDeckOffsetY",
            )
            val rotation by animateFloatAsState(
                targetValue = pose.rotation,
                animationSpec = cardSpring,
                label = "moduleDeckRotation",
            )
            val scale by animateFloatAsState(
                targetValue = pose.scale,
                animationSpec = cardSpring,
                label = "moduleDeckScale",
            )
            val elevation by animateFloatAsState(
                targetValue = pose.elevation,
                animationSpec = cardSpring,
                label = "moduleDeckElevation",
            )
            HomeModuleHeroSlot(
                item = item,
                featuredAppId = featuredAppId,
                entitlement = entitlement,
                wallet = wallet,
                requiresCoinEntry = requiresCoinEntry,
                explorerGiftAvailable = explorerGiftAvailable,
                isConnecting = isConnecting,
                onCodesClick = onCodesClick,
                onUnlockClick = onUnlockClick,
                onExplorerSparkleClick = onExplorerSparkleClick,
                onItemClick = { clicked, title ->
                    if (isFront) onItemClick(clicked, title)
                },
                isFront = isFront,
                    modifier = Modifier
                    .fillMaxWidth(HomeModuleCardWidthFraction)
                    .fillMaxHeight(0.94f)
                    .align(Alignment.Center)
                    .offset(x = offsetX, y = offsetY)
                    .zIndex((modules.size - depth).toFloat())
                    .graphicsLayer {
                        val drag = if (isFront) dragPx else -dragPx * 0.18f
                        translationX = drag
                        rotationZ = rotation + if (isFront) dragPx / 28f else 0f
                        scaleX = scale
                        scaleY = scale
                        alpha = if (isFront) 1f else 0.62f
                        shadowElevation = elevation
                        cameraDistance = 14f * density
                    },
            )
            }
        }
    }
}

internal data class HomeModuleFanPose(
    val offsetX: Dp,
    val offsetY: Dp,
    val rotation: Float,
    val scale: Float,
    val elevation: Float,
)

internal fun homeModuleFanPose(depth: Int, count: Int): HomeModuleFanPose {
    val previousIndex = (count - 1).coerceAtLeast(0)
    return when {
        depth <= 0 -> HomeModuleFanPose(
            offsetX = 0.dp,
            offsetY = 0.dp,
            rotation = -1.5f,
            scale = HomeModuleFrontCardScale,
            elevation = 24f,
        )
        depth == 1 -> HomeModuleFanPose(
            offsetX = 64.dp,
            offsetY = 28.dp,
            rotation = 16f,
            scale = 0.78f,
            elevation = 6f,
        )
        count > 2 && depth == previousIndex -> HomeModuleFanPose(
            offsetX = (-64).dp,
            offsetY = 28.dp,
            rotation = -16f,
            scale = 0.78f,
            elevation = 6f,
        )
        else -> HomeModuleFanPose(
            offsetX = 78.dp,
            offsetY = 40.dp,
            rotation = 22f,
            scale = 0.7f,
            elevation = 3f,
        )
    }
}

internal fun initialHomeModuleDeckOrder(
    moduleIds: List<ApplicationId>,
    featuredAppId: ApplicationId,
): List<ApplicationId> {
    if (featuredAppId !in moduleIds) return moduleIds
    return listOf(featuredAppId) + moduleIds.filter { it != featuredAppId }
}

internal fun <T> rotateHomeModuleDeckNext(order: List<T>): List<T> {
    if (order.size < 2) return order
    return order.drop(1) + order.first()
}

internal fun <T> rotateHomeModuleDeckPrevious(order: List<T>): List<T> {
    if (order.size < 2) return order
    return listOf(order.last()) + order.dropLast(1)
}

@Composable
private fun HomeModuleHeroSlot(
    item: ApplicationCatalogItem,
    featuredAppId: ApplicationId,
    entitlement: Entitlement,
    wallet: CoinWalletState,
    requiresCoinEntry: Boolean,
    explorerGiftAvailable: Boolean,
    isConnecting: Boolean,
    onCodesClick: (ApplicationCatalogItem) -> Unit,
    onUnlockClick: (ApplicationCatalogItem) -> Unit,
    onExplorerSparkleClick: (ApplicationCatalogItem) -> Unit,
    onItemClick: (ApplicationCatalogItem, String) -> Unit,
    modifier: Modifier = Modifier,
    isFront: Boolean = true,
) {
    val title = stringResource(item.titleRes)
    val isUnlocked = item.id.hasEntryAccess(
        entitlement = entitlement,
        wallet = wallet,
        requiresCoinEntry = requiresCoinEntry,
    )
    val isRecent = item.id == featuredAppId
    HomeModuleHeroCard(
        applicationId = item.id,
        thumbnailRes = item.id.thumbnailRes(),
        title = title,
        subtitle = stringResource(item.subtitleRes),
        badge = applicationBadge(item),
        trailingAction = applicationTrailingAction(
            item = item,
            isUnlocked = isUnlocked,
            requiresCoinEntry = requiresCoinEntry,
        ),
        onUnlockClick = { onUnlockClick(item) },
        onCodesClick = { onCodesClick(item) },
        onClick = {
            if (!isConnecting) onItemClick(item, title)
        },
        showExplorerSparkle = explorerGiftAvailable &&
            !item.id.isFree() &&
            !item.comingSoon &&
            !isUnlocked,
        onExplorerSparkleClick = { onExplorerSparkleClick(item) },
        enabledProHighlight = isRecent ||
            (!item.id.isFree() && !item.comingSoon && isUnlocked),
        animateHologram = true,
        isFront = isFront,
        modifier = modifier.then(
            if (item.id == ApplicationId.CONTROL_PANEL) {
                Modifier.reportTutorialAnchor(TutorialAnchor.HOME_CONTROL_PANEL)
            } else {
                Modifier
            },
        ),
    )
}

@Composable
private fun HomeSecondaryActions(
    isConnecting: Boolean,
    showBrowseCatalog: Boolean,
    showProButton: Boolean,
    onOpenApplications: () -> Unit,
    onQuickUseClick: () -> Unit,
    onProClick: () -> Unit,
    onOpenAbout: () -> Unit,
    fillButtonWidth: Boolean,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HomeQuickUseButton(
            onClick = onQuickUseClick,
            enabled = !isConnecting,
            modifier = Modifier.fillMaxWidth(
                HomeModuleCardWidthFraction * HomeModuleFrontCardScale,
            ),
        )
        if (showProButton) {
            HomeProButton(
                onClick = onProClick,
                enabled = !isConnecting,
                modifier = Modifier.fillMaxWidth(
                    HomeModuleCardWidthFraction * HomeModuleFrontCardScale,
                ),
            )
        }
        if (showBrowseCatalog) {
            NeoPillButton(
                text = stringResource(R.string.home_browse_catalog),
                onClick = onOpenApplications,
                icon = Icons.Filled.GridView,
                enabled = !isConnecting,
                fillMaxWidth = fillButtonWidth,
                compact = true,
                modifier = if (fillButtonWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.widthIn(min = 180.dp, max = 280.dp)
                },
            )
        }
        Text(
            text = stringResource(R.string.home_version_info, BuildConfig.VERSION_NAME),
            color = Neo.TextMuted.copy(alpha = 0.85f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenAbout)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = context.getString(R.string.home_about_content_description)
                },
        )
    }
}

@Composable
private fun MinimalHomeTopBar(
    isConnected: Boolean,
    showCoinChip: Boolean,
    coinBalance: Int,
    onCoinClick: () -> Unit,
    onHelpClick: () -> Unit,
    onTitleClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_telecon4esp32_icon),
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .glassSurface(cornerRadius = 18.dp)
                .clickable(onClick = onTitleClick)
                .padding(4.dp),
        )
        Spacer(modifier = Modifier.size(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onTitleClick),
        ) {
            Text(
                text = stringResource(R.string.home_hud_title),
                color = Neo.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Neo.Positive else Neo.Negative),
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = if (isConnected) {
                        stringResource(R.string.cyber_device_connected)
                    } else {
                        stringResource(R.string.cyber_device_disconnected)
                    },
                    color = if (isConnected) Neo.Positive else Neo.Negative,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showCoinChip) {
            CoinBalanceChip(
                balance = coinBalance,
                modifier = Modifier
                    .clickable(onClick = onCoinClick)
                    .padding(end = 4.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
        }
        NeoIconButton(
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            onClick = onHelpClick,
            contentDescription = stringResource(R.string.home_help),
            size = 40.dp,
        )
    }
}

@Composable
private fun HomeQuickUseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val label = stringResource(R.string.home_help_play_tutorial)
    val pulse = rememberInfiniteTransition(label = "quickUseBlink")
    val blink by pulse.animateFloat(
        initialValue = 0.38f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 720),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "quickUseBlinkAlpha",
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        QuickUseGreen.copy(alpha = 0.16f + 0.22f * blink),
                        QuickUseGreenDeep.copy(alpha = 0.10f + 0.20f * blink),
                    ),
                ),
            )
            .border(1.5.dp, QuickUseGreen.copy(alpha = 0.35f + 0.65f * blink), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = QuickUseGreen.copy(alpha = 0.62f + 0.38f * blink),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun HomeProButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val description = stringResource(R.string.home_pro_button_content_description)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        ProGold.copy(alpha = 0.28f),
                        ProGoldDeep.copy(alpha = 0.18f),
                    ),
                ),
            )
            .border(1.dp, ProGold.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_pro_button_bottom),
            color = ProGold,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Preview(showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CyberHomeScreenPreview() {
    TeleCon4Esp32Theme {
        CyberHomeScreenContent(requiresCoinEntry = true)
    }
}

@Preview(
    showSystemUi = true,
    uiMode = UI_MODE_NIGHT_YES,
    name = "Landscape",
    widthDp = 800,
    heightDp = 360,
)
@Composable
private fun CyberHomeScreenLandscapePreview() {
    TeleCon4Esp32Theme {
        CyberHomeScreenContent(
            lastApplicationId = ApplicationId.CONTROL_PANEL,
            requiresCoinEntry = true,
        )
    }
}

@Preview(showSystemUi = true, uiMode = UI_MODE_NIGHT_YES, name = "Connected")
@Composable
private fun CyberHomeScreenConnectedPreview() {
    TeleCon4Esp32Theme {
        CyberHomeScreenContent(
            activeSession = ActiveBluetoothSession(
                applicationId = ApplicationId.RC_VEHICLE_PRO,
                protocolMode = com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode.SIMPLE,
                deviceName = "ESP32-TeleCon-RC",
                deviceAddress = "AA:BB:CC:DD:EE:FF",
            ),
            lastApplicationId = ApplicationId.RC_VEHICLE_PRO,
            requiresCoinEntry = true,
        )
    }
}

@Preview(showSystemUi = true, uiMode = UI_MODE_NIGHT_YES, name = "Recent RC")
@Composable
private fun CyberHomeScreenRecentRcPreview() {
    TeleCon4Esp32Theme {
        CyberHomeScreenContent(
            lastApplicationId = ApplicationId.RC_VEHICLE_PRO,
            requiresCoinEntry = true,
        )
    }
}
