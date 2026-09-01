package com.micsbol.telecon4esp32.ui.home

import android.app.Activity
import com.micsbol.telecon4esp32.ui.components.safeHudPadding
import com.micsbol.telecon4esp32.BuildConfig
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.ui.ads.AdPolicy
import com.micsbol.telecon4esp32.ui.ads.LocalRewardedAdManager
import com.micsbol.telecon4esp32.ui.bluetooth.BluetoothConnectionErrorDialog
import com.micsbol.telecon4esp32.ui.components.AdBanner
import com.micsbol.telecon4esp32.ui.components.EmitterBrandLogo
import com.micsbol.telecon4esp32.ui.components.HoloRcTurntableVideo
import com.micsbol.telecon4esp32.ui.components.ShellPlasticBackground
import com.micsbol.telecon4esp32.ui.components.ShellPlasticDisplayPanel
import com.micsbol.telecon4esp32.ui.components.ShellPlasticMenuCard
import com.micsbol.telecon4esp32.ui.components.ShellPlasticStartButton
import com.micsbol.telecon4esp32.ui.components.ShellPlasticStatusBar
import com.micsbol.telecon4esp32.ui.components.brandPrimary
import com.micsbol.telecon4esp32.ui.control_panel.components.ControlPanelPlasticColors
import com.micsbol.telecon4esp32.ui.control_panel.components.ControlPanelPlasticIconButton
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.theme.TeleCon4Esp32Theme
import com.micsbol.telecon4esp32.ui.wallet.CoinBalanceChip
import com.micsbol.telecon4esp32.ui.wallet.CoinHomeWalletPanel
import com.micsbol.telecon4esp32.ui.wallet.CoinMessageDialog
import com.micsbol.telecon4esp32.ui.wallet.CoinPricingTableDialog
import com.micsbol.telecon4esp32.ui.wallet.LocalWallet
import com.micsbol.telecon4esp32.domain.model.CoinEconomy
import com.micsbol.telecon4esp32.domain.model.CoinWalletState
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.isApplicationCatalogVisible
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy

data class HomeItem(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val route: String,
)

@Composable
fun HomeScreen(
    navController: NavHostController? = null,
    isConnecting: Boolean = false,
    isBluetoothConnected: Boolean = false,
    errorMessage: String? = null,
    lastDeviceName: String? = null,
    onStartClick: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val entitlement = LocalEntitlement.current
    val wallet = LocalWallet.current
    val rewardedAdManager = LocalRewardedAdManager.current
    val context = LocalContext.current
    val activity = context as? Activity
    var showHelpDialog by remember { mutableStateOf(false) }
    var showPricingTable by remember { mutableStateOf(false) }
    var coinMessage by remember { mutableStateOf<String?>(null) }
    val adRewardGrantedMessage = stringResource(R.string.coins_ad_reward_granted)
    val adUnavailableMessage = stringResource(R.string.coins_ad_unavailable)
    val showCoinWallet = entitlement.usesCoinEconomy() || BuildConfig.DEBUG

    BackHandler {
        activity?.finish()
    }

    val homeItems = buildList {
        add(
            HomeItem(
                Icons.Default.Bluetooth,
                stringResource(R.string.home_item_bluetooth),
                stringResource(R.string.home_hud_menu_bluetooth_desc),
                Screen.Bluetooth.route,
            ),
        )
        if (isApplicationCatalogVisible()) {
            add(
                HomeItem(
                    Icons.Default.Apps,
                    stringResource(R.string.home_item_applications),
                    stringResource(R.string.home_hud_menu_applications_desc),
                    Screen.Applications.route,
                ),
            )
        }
        add(
            HomeItem(
                Icons.Default.Code,
                stringResource(R.string.home_codes_documents),
                stringResource(R.string.home_hud_menu_codes_desc),
                Screen.Codes.route,
            ),
        )
        add(
            HomeItem(
                Icons.Default.VideoLibrary,
                stringResource(R.string.home_item_tutorial),
                stringResource(R.string.home_hud_menu_tutorial_desc),
                Screen.Tutorial.route,
            ),
        )
    }

    val bluetoothStatusText = when {
        isConnecting -> stringResource(R.string.home_bluetooth_status_connecting)
        isBluetoothConnected -> stringResource(R.string.home_bluetooth_status_connected)
        else -> stringResource(R.string.home_bluetooth_status_disconnected)
    }

    val signalLabel = if (isBluetoothConnected) {
        stringResource(R.string.home_hud_footer_signal_online)
    } else {
        stringResource(R.string.home_hud_footer_signal_offline)
    }

    val startSubtitle = if (lastDeviceName != null) {
        stringResource(R.string.home_last_devices) + " " + lastDeviceName
    } else {
        stringResource(R.string.home_hud_launch_subtitle)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ShellPlasticBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeHudPadding(
                    includeTop = true,
                    includeBottom = true,
                    includeHorizontal = true,
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            HomePlasticHeaderRow(
                onHelpClick = { showHelpDialog = true },
                onAboutClick = { navController?.navigate(Screen.About.route) },
                showCoins = showCoinWallet,
                coinBalance = wallet.balance,
                onCoinsClick = { showPricingTable = true },
            )

            if (showCoinWallet) {
                Spacer(modifier = Modifier.height(10.dp))
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
                    },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HomePlasticMenuPanel(
                        homeItems = homeItems,
                        navController = navController,
                        isBluetoothConnected = isBluetoothConnected,
                        bluetoothStatusText = bluetoothStatusText,
                        modifier = Modifier.weight(1.1f),
                    )
                    HomePlasticHeroPanel(
                        onStartClick = onStartClick,
                        startSubtitle = startSubtitle,
                        isConnecting = isConnecting,
                        modifier = Modifier.weight(0.9f),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    homeItems.forEach { item ->
                        ShellPlasticMenuCard(
                            icon = item.icon,
                            title = item.title,
                            description = item.description,
                            onClick = {
                                if (item.route.isNotEmpty()) {
                                    navController?.navigate(item.route)
                                }
                            },
                            statusText = if (item.route == Screen.Bluetooth.route) {
                                bluetoothStatusText
                            } else {
                                null
                            },
                            statusConnected = isBluetoothConnected,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isLandscape) {
                ShellPlasticStartButton(
                    title = stringResource(R.string.home_blueprint_start),
                    subtitle = startSubtitle,
                    onClick = onStartClick,
                    enabled = !isConnecting,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            ShellPlasticStatusBar(
                signalLabel = signalLabel,
                modeLabel = stringResource(R.string.home_hud_footer_mode),
                powerLabel = stringResource(R.string.home_hud_footer_power),
            )

            if (AdPolicy.hasBanner(Screen.Home.route, entitlement)) {
                Spacer(modifier = Modifier.height(6.dp))
                AdBanner(modifier = Modifier.fillMaxWidth())
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

        BluetoothConnectionErrorDialog(
            handshakeFailure = null,
            errorMessage = errorMessage,
            onDismiss = onDismissError,
        )

        if (isConnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ControlPanelPlasticColors.RimDark.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = brandPrimary())
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (lastDeviceName != null) {
                            stringResource(R.string.home_bluetooth_status_connecting) + " $lastDeviceName"
                        } else {
                            stringResource(R.string.home_bluetooth_status_connecting)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = ControlPanelPlasticColors.AccentGlow,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomePlasticHeaderRow(
    onHelpClick: () -> Unit,
    onAboutClick: () -> Unit,
    showCoins: Boolean = false,
    coinBalance: Int = 0,
    onCoinsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShellPlasticDisplayPanel(
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = context.getString(R.string.home_about_content_description)
                }
                .clickable(onClick = onAboutClick),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmitterBrandLogo(size = 44.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.home_hud_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ControlPanelPlasticColors.AccentGlow,
                    )
                    Text(
                        text = stringResource(R.string.home_hud_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF78909C),
                    )
                }
            }
        }
        if (showCoins) {
            Spacer(modifier = Modifier.width(8.dp))
            CoinBalanceChip(
                balance = coinBalance,
                modifier = Modifier.clickable(onClick = onCoinsClick),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        ControlPanelPlasticIconButton(
            onClick = onHelpClick,
            contentDescription = stringResource(R.string.home_help),
            icon = Icons.AutoMirrored.Filled.Help,
        )
    }
}

@Composable
private fun HomePlasticMenuPanel(
    homeItems: List<HomeItem>,
    navController: NavHostController?,
    isBluetoothConnected: Boolean,
    bluetoothStatusText: String,
    modifier: Modifier = Modifier,
) {
    ShellPlasticDisplayPanel(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            homeItems.forEach { item ->
                ShellPlasticMenuCard(
                    icon = item.icon,
                    title = item.title,
                    description = item.description,
                    onClick = {
                        if (item.route.isNotEmpty()) {
                            navController?.navigate(item.route)
                        }
                    },
                    statusText = if (item.route == Screen.Bluetooth.route) {
                        bluetoothStatusText
                    } else {
                        null
                    },
                    statusConnected = isBluetoothConnected,
                )
            }
        }
    }
}

@Composable
private fun HomePlasticHeroPanel(
    onStartClick: () -> Unit,
    startSubtitle: String,
    isConnecting: Boolean,
    modifier: Modifier = Modifier,
) {
    ShellPlasticDisplayPanel(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HoloRcTurntableVideo(
                contentDescription = stringResource(R.string.home_car_image_description),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            ShellPlasticStartButton(
                title = stringResource(R.string.home_blueprint_start),
                subtitle = startSubtitle,
                onClick = onStartClick,
                enabled = !isConnecting,
            )
        }
    }
}

@Composable
private fun HomeScreenPreviewContent(
    isConnecting: Boolean = false,
    isBluetoothConnected: Boolean = false,
    errorMessage: String? = null,
    lastDeviceName: String? = null,
    wallet: CoinWalletState = CoinWalletState(balance = CoinEconomy.DEBUG_STARTING_BALANCE),
    entitlement: Entitlement = Entitlement.Free,
) {
    CompositionLocalProvider(
        LocalEntitlement provides entitlement,
        LocalWallet provides wallet,
    ) {
        HomeScreen(
            isConnecting = isConnecting,
            isBluetoothConnected = isBluetoothConnected,
            errorMessage = errorMessage,
            lastDeviceName = lastDeviceName,
        )
    }
}

@Preview(showSystemUi = true, name = "Home Portrait", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPortraitPreview() {
    TeleCon4Esp32Theme {
        HomeScreenPreviewContent()
    }
}

@Preview(showSystemUi = true, name = "Home Connected", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenConnectedPreview() {
    TeleCon4Esp32Theme {
        HomeScreenPreviewContent(
            isBluetoothConnected = true,
            lastDeviceName = "RC Car Pro",
        )
    }
}

@Preview(showSystemUi = true, name = "Home Connecting", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenConnectingPreview() {
    TeleCon4Esp32Theme {
        HomeScreenPreviewContent(
            isConnecting = true,
            lastDeviceName = "RC Car Pro",
        )
    }
}

@Preview(showSystemUi = true, name = "Home Low Coins", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenLowCoinsPreview() {
    TeleCon4Esp32Theme {
        HomeScreenPreviewContent(
            wallet = CoinWalletState(balance = 5),
        )
    }
}

@Preview(
    showSystemUi = true,
    name = "Home Landscape Connected",
    uiMode = UI_MODE_NIGHT_YES,
    device = "spec:width=840dp,height=420dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape",
)
@Composable
private fun HomeScreenLandscapeConnectedPreview() {
    TeleCon4Esp32Theme {
        HomeScreenPreviewContent(
            isBluetoothConnected = true,
            lastDeviceName = "RC Car Pro",
        )
    }
}
