package com.micsbol.telecon4esp32.ui.cyber.screens

import android.app.Activity
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import com.micsbol.telecon4esp32.BuildConfig
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.ads.AdPolicy
import com.micsbol.telecon4esp32.ui.components.AdBanner
import com.micsbol.telecon4esp32.ui.components.NeoCard
import com.micsbol.telecon4esp32.ui.components.NeoIconBadge
import com.micsbol.telecon4esp32.ui.components.NeoIconButton
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeumorphicBackground
import com.micsbol.telecon4esp32.ui.components.glassSurface
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.domain.bluetooth.ActiveBluetoothSession
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.ui.applications.recentTagRes
import com.micsbol.telecon4esp32.ui.applications.thumbnailRes
import com.micsbol.telecon4esp32.ui.applications.titleRes
import com.micsbol.telecon4esp32.ui.bluetooth.handshakeFailureMessage
import com.micsbol.telecon4esp32.ui.bluetooth.protocolModeLabel
import com.micsbol.telecon4esp32.ui.home.HomeHelpDialog
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.theme.Neo
import com.micsbol.telecon4esp32.ui.wallet.CoinHomeWalletPanel
import com.micsbol.telecon4esp32.ui.wallet.CoinMessageDialog
import com.micsbol.telecon4esp32.ui.wallet.CoinPricingTableDialog
import com.micsbol.telecon4esp32.ui.wallet.LocalWallet
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy
import com.micsbol.telecon4esp32.ui.ads.LocalRewardedAdManager

private data class HomeMenuItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val onClick: () -> Unit,
)

/**
 * Dark glassmorphism home dashboard.
 *
 * Vertical structure:
 *  Header -> Wallet -> Recent Project -> Codes -> Active Session -> About -> Banner
 */
@Composable
fun CyberHomeScreen(
    navController: NavHostController? = null,
    isConnecting: Boolean = false,
    activeSession: ActiveBluetoothSession? = null,
    lastApplicationId: ApplicationId? = null,
    lastDeviceName: String? = null,
    errorMessage: String? = null,
    handshakeFailure: com.micsbol.telecon4esp32.domain.bluetooth.HandshakeFailure? = null,
    onOpenApplications: () -> Unit = {},
    onContinueSession: () -> Unit = {},
    onOpenRecentProject: (ApplicationId) -> Unit = {},
    onDismissError: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val entitlement = LocalEntitlement.current
    val wallet = LocalWallet.current
    val rewardedAdManager = LocalRewardedAdManager.current
    var showHelpDialog by remember { mutableStateOf(false) }
    var showPricingTable by remember { mutableStateOf(false) }
    var coinMessage by remember { mutableStateOf<String?>(null) }
    val adRewardGrantedMessage = stringResource(R.string.coins_ad_reward_granted)
    val adUnavailableMessage = stringResource(R.string.coins_ad_unavailable)
    val showCoinWallet = entitlement.usesCoinEconomy() || BuildConfig.DEBUG

    BackHandler { activity?.finish() }

    val codesItem = HomeMenuItem(
        icon = Icons.Filled.Code,
        title = stringResource(R.string.cyber_grid_codes_title),
        description = stringResource(R.string.cyber_grid_codes_desc),
        onClick = { navController?.navigate(Screen.Codes.route) },
    )

    val aboutItem = HomeMenuItem(
        icon = Icons.Filled.Info,
        title = stringResource(R.string.about_title),
        description = stringResource(R.string.cyber_grid_about_desc),
        onClick = { navController?.navigate(Screen.About.route) },
    )

    val isSessionConnected = activeSession != null

    val resolvedError = when {
        handshakeFailure != null -> handshakeFailureMessage(handshakeFailure)
        errorMessage != null -> errorMessage
        else -> null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NeumorphicBackground()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                HeaderPanel(
                    isConnected = isSessionConnected,
                    onHelpClick = { showHelpDialog = true },
                    onHeaderClick = { navController?.navigate(Screen.About.route) },
                    showCoins = showCoinWallet,
                    coinBalance = wallet.balance,
                    onCoinsClick = { showPricingTable = true },
                )
            }

            if (showCoinWallet) {
                item {
                    CoinHomeWalletPanel(
                        balance = wallet.balance,
                        onWatchAd = {
                            if (activity != null && rewardedAdManager != null) {
                                rewardedAdManager.tryShow(activity) { granted ->
                                    coinMessage = if (granted) {
                                        adRewardGrantedMessage
                                    } else {
                                        adUnavailableMessage
                                    }
                                }
                            } else {
                                coinMessage = adUnavailableMessage
                            }
                        }
                    )
                }
            }

            item {
                RecentProjectPanel(
                    applicationId = lastApplicationId,
                    lastDeviceName = lastDeviceName,
                    onOpenClick = { appId -> onOpenRecentProject(appId) },
                    onBrowseApplications = onOpenApplications,
                )
            }

            item {
                HomeLinkCard(item = codesItem, showFreeBadge = true)
            }

            item {
                ActiveSessionPanel(
                    activeSession = activeSession,
                    isConnecting = isConnecting,
                    onOpenApplications = onOpenApplications,
                    onContinueSession = onContinueSession,
                )
            }

            item {
                HomeLinkCard(item = aboutItem)
            }

            if (AdPolicy.hasBanner(Screen.Home.route, entitlement)) {
                item { AdBanner(modifier = Modifier.fillMaxWidth()) }
            }
        }

        if (showHelpDialog) {
            HomeHelpDialog(onDismissRequest = { showHelpDialog = false })
        }

        if (showPricingTable && showCoinWallet) {
            CoinPricingTableDialog(
                walletBalance = wallet.balance,
                onDismiss = { showPricingTable = false },
                onWatchAd = {
                    if (activity != null && rewardedAdManager != null) {
                        rewardedAdManager.tryShow(activity) { granted ->
                            coinMessage = if (granted) {
                                adRewardGrantedMessage
                            } else {
                                adUnavailableMessage
                            }
                            if (granted) showPricingTable = false
                        }
                    } else {
                        coinMessage = adUnavailableMessage
                    }
                },
            )
        }

        if (coinMessage != null) {
            CoinMessageDialog(
                message = coinMessage.orEmpty(),
                onDismiss = { coinMessage = null },
            )
        }

        if (resolvedError != null) {
            AlertDialog(
                onDismissRequest = onDismissError,
                title = { Text(stringResource(R.string.bluetooth_connection_error)) },
                text = { Text(resolvedError) },
                confirmButton = {
                    TextButton(onClick = onDismissError) {
                        Text(stringResource(R.string.codes_dialog_ok))
                    }
                },
            )
        }

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
                            stringResource(
                                R.string.home_bluetooth_status_connecting,
                            ) + " ${activeSession.deviceName}"
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
private fun HeaderPanel(
    isConnected: Boolean,
    onHelpClick: () -> Unit,
    onHeaderClick: () -> Unit,
    showCoins: Boolean = false,
    coinBalance: Int = 0,
    onCoinsClick: () -> Unit = {},
) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onHeaderClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .glassSurface(cornerRadius = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_telecon4esp32_icon),
                    contentDescription = stringResource(R.string.about_logo_content_description),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_hud_title),
                    color = Neo.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.cyber_header_subtitle),
                    color = Neo.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusLine(isConnected = isConnected)
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (showCoins) {
                com.micsbol.telecon4esp32.ui.wallet.CoinBalanceChip(
                    balance = coinBalance,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onCoinsClick)
                        .padding(horizontal = 4.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            NeoIconButton(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                onClick = onHelpClick,
                contentDescription = stringResource(R.string.home_help),
                size = 44.dp,
            )
        }
    }
}

@Composable
private fun StatusLine(isConnected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isConnected) Neo.Positive else Neo.Negative),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isConnected) {
                stringResource(R.string.cyber_device_connected)
            } else {
                stringResource(R.string.cyber_device_disconnected)
            },
            color = if (isConnected) Neo.Positive else Neo.Negative,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ActiveSessionPanel(
    activeSession: ActiveBluetoothSession?,
    isConnecting: Boolean,
    onOpenApplications: () -> Unit,
    onContinueSession: () -> Unit,
) {
    val primaryText = if (activeSession != null) {
        stringResource(R.string.home_session_continue)
    } else {
        stringResource(R.string.home_session_open_applications)
    }
    val subtitle = if (activeSession != null) {
        stringResource(
            R.string.home_session_continue_subtitle,
            stringResource(activeSession.applicationId.titleRes()),
        )
    } else {
        stringResource(R.string.home_session_none_subtitle)
    }

    NeoCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.home_session_title),
                color = Neo.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (activeSession == null) {
                Text(
                    text = stringResource(R.string.home_session_none),
                    color = Neo.TextSecondary,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_session_none_hint),
                    color = Neo.TextSecondary,
                    fontSize = 12.sp,
                )
            } else {
                SessionDetailRow(
                    label = stringResource(R.string.home_session_app_label),
                    value = stringResource(activeSession.applicationId.titleRes()),
                )
                Spacer(modifier = Modifier.height(4.dp))
                SessionDetailRow(
                    label = stringResource(R.string.home_session_device_label),
                    value = activeSession.deviceName
                        ?: stringResource(R.string.bluetooth_unknown_device),
                )
                Spacer(modifier = Modifier.height(4.dp))
                SessionDetailRow(
                    label = stringResource(R.string.home_session_protocol_label),
                    value = protocolModeLabel(activeSession.protocolMode),
                )
                Spacer(modifier = Modifier.height(8.dp))
                StatusLine(isConnected = true)
            }
            Spacer(modifier = Modifier.height(12.dp))
            NeoPillButton(
                text = primaryText,
                onClick = {
                    if (activeSession != null) onContinueSession() else onOpenApplications()
                },
                icon = if (activeSession != null) Icons.Filled.Rocket else Icons.Filled.GridView,
                enabled = !isConnecting,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = Neo.TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SessionDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Neo.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.weight(0.45f),
        )
        Text(
            text = value,
            color = Neo.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeLinkCard(
    item: HomeMenuItem,
    showFreeBadge: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = 22.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = item.onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        if (showFreeBadge) {
            Text(
                text = stringResource(R.string.applications_badge_free),
                color = Neo.Positive,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (showFreeBadge) 72.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NeoIconBadge(icon = item.icon, size = 44.dp, selected = pressed)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Neo.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    color = Neo.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RecentProjectPanel(
    applicationId: ApplicationId?,
    lastDeviceName: String?,
    onOpenClick: (ApplicationId) -> Unit,
    onBrowseApplications: () -> Unit,
) {
    NeoCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.cyber_recent_project),
            color = Neo.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (applicationId == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.cyber_recent_none),
                        color = Neo.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.cyber_recent_none_hint),
                        color = Neo.TextSecondary,
                        fontSize = 11.sp,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                NeoPillButton(
                    text = stringResource(R.string.home_session_open_applications),
                    onClick = onBrowseApplications,
                    compact = true,
                )
            }
        } else {
            val isRcVehicle = applicationId == ApplicationId.RC_VEHICLE_PRO
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 84.dp, height = 64.dp)
                        .glassSurface(cornerRadius = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(applicationId.thumbnailRes()),
                        contentDescription = stringResource(applicationId.titleRes()),
                        contentScale = if (isRcVehicle) ContentScale.Fit else ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .padding(if (isRcVehicle) 6.dp else 0.dp),
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(applicationId.titleRes()),
                        color = Neo.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = lastDeviceName
                            ?: stringResource(R.string.cyber_recent_open_to_connect),
                        color = Neo.TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tag(stringResource(applicationId.recentTagRes()))
                        Tag(stringResource(R.string.cyber_tag_esp32))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                NeoPillButton(
                    text = stringResource(R.string.cyber_open),
                    onClick = { onOpenClick(applicationId) },
                    compact = true,
                )
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Neo.Accent.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            color = Neo.Accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CyberHomeScreenPreview() {
    CyberHomeScreen()
}

@Preview(showSystemUi = true, uiMode = UI_MODE_NIGHT_YES, name = "Connected")
@Composable
private fun CyberHomeScreenConnectedPreview() {
    CyberHomeScreen(
        activeSession = ActiveBluetoothSession(
            applicationId = ApplicationId.GREENHOUSE,
            protocolMode = com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode.SIMPLE,
            deviceName = "ESP32-TeleCon-GH",
            deviceAddress = "AA:BB:CC:DD:EE:FF",
        ),
        lastApplicationId = ApplicationId.GREENHOUSE,
        lastDeviceName = "ESP32-TeleCon-GH",
    )
}

@Preview(showSystemUi = true, uiMode = UI_MODE_NIGHT_YES, name = "Recent RC")
@Composable
private fun CyberHomeScreenRecentRcPreview() {
    CyberHomeScreen(
        lastApplicationId = ApplicationId.RC_VEHICLE_PRO,
        lastDeviceName = "ESP32-TeleCon-RC",
    )
}





