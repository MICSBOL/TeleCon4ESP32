package com.micsbol.emitterapp.ui.home

import android.app.Activity
import com.micsbol.emitterapp.BuildConfig
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.micsbol.emitterapp.R
import com.micsbol.emitterapp.ui.components.ConnectionStatusDot
import com.micsbol.emitterapp.ui.components.EmitterAppScaffold
import com.micsbol.emitterapp.ui.components.EmitterHeaderBrandLogoSizeHome
import com.micsbol.emitterapp.ui.components.EmitterIconContainer
import com.micsbol.emitterapp.ui.components.EmitterQuickStartButton
import com.micsbol.emitterapp.ui.components.EmitterStyledCard
import com.micsbol.emitterapp.ui.components.brandPrimary
import com.micsbol.emitterapp.ui.components.mutedTextColor
import com.micsbol.emitterapp.ui.ads.AdPolicy
import com.micsbol.emitterapp.ui.entitlement.LocalEntitlement
import com.micsbol.emitterapp.ui.navigation.Screen
import com.micsbol.emitterapp.ui.theme.EmitterAppTheme
import com.micsbol.emitterapp.ui.theme.StatusConnected
import com.micsbol.emitterapp.ui.theme.StatusDisconnected

data class HomeItem(
    val icon: ImageVector,
    val title: String,
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
    val context = LocalContext.current
    val activity = context as? Activity
    var showHelpDialog by remember { mutableStateOf(false) }

    BackHandler {
        activity?.finish()
    }

    val homeItems = listOf(
        HomeItem(
            Icons.Default.Apps,
            stringResource(R.string.home_item_applications),
            Screen.Applications.route,
        ),
        HomeItem(
            Icons.Default.Bluetooth,
            stringResource(R.string.home_item_bluetooth),
            Screen.Bluetooth.route,
        ),
        HomeItem(
            Icons.Default.Code,
            stringResource(R.string.home_codes_documents),
            Screen.Codes.route,
        ),
        HomeItem(
            Icons.Default.VideoLibrary,
            stringResource(R.string.home_item_tutorial),
            Screen.Tutorial.route,
        ),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        EmitterAppScaffold(
            title = stringResource(R.string.home_title),
            subtitle = stringResource(R.string.app_header_subtitle),
            brandLogoSize = EmitterHeaderBrandLogoSizeHome,
            showAdBanner = AdPolicy.hasBanner(Screen.Home.route, entitlement),
            onNavigateBack = { activity?.finish() },
            actions = {
                IconButton(onClick = { showHelpDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Help,
                        contentDescription = stringResource(R.string.home_help),
                        tint = brandPrimary()
                    )
                }
            }
        ) { paddingValues ->
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeOptionsList(
                        homeItems = homeItems,
                        navController = navController,
                        isConnecting = isConnecting,
                        isBluetoothConnected = isBluetoothConnected,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                    HomeSecondaryContent(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        imageModifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        lastDeviceName = lastDeviceName,
                        onStartClick = onStartClick,
                        onAboutClick = { navController?.navigate(Screen.About.route) }
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HomeOptionsList(
                        homeItems = homeItems,
                        navController = navController,
                        isConnecting = isConnecting,
                        isBluetoothConnected = isBluetoothConnected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    HomeSecondaryContent(
                        modifier = Modifier
                            .weight(0.7f)
                            .fillMaxWidth(),
                        imageModifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        lastDeviceName = lastDeviceName,
                        onStartClick = onStartClick,
                        onAboutClick = { navController?.navigate(Screen.About.route) }
                    )
                }
            }
        }

        if (showHelpDialog) {
            HomeHelpDialog(onDismissRequest = { showHelpDialog = false })
        }

        if (errorMessage != null) {
            AlertDialog(
                onDismissRequest = onDismissError,
                title = { Text(stringResource(R.string.bluetooth_connection_error)) },
                text = { Text(errorMessage) },
                confirmButton = {
                    TextButton(onClick = onDismissError) { Text(stringResource(R.string.codes_dialog_ok)) }
                }
            )
        }

        if (isConnecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
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
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeOptionsList(
    homeItems: List<HomeItem>,
    navController: NavHostController?,
    isConnecting: Boolean,
    isBluetoothConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val bluetoothStatusText = when {
        isConnecting -> stringResource(R.string.home_bluetooth_status_connecting)
        isBluetoothConnected -> stringResource(R.string.home_bluetooth_status_connected)
        else -> stringResource(R.string.home_bluetooth_status_disconnected)
    }
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(homeItems) { item ->
            HomeItemCard(
                icon = item.icon,
                title = item.title,
                onClick = {
                    if (item.route.isNotEmpty()) {
                        navController?.navigate(item.route)
                    }
                },
                subtitle = if (item.route == Screen.Bluetooth.route) bluetoothStatusText else null,
                isBluetoothConnected = isBluetoothConnected,
            )
        }
    }
}

@Composable
private fun HomeSecondaryContent(
    modifier: Modifier = Modifier,
    imageModifier: Modifier,
    lastDeviceName: String? = null,
    onStartClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.car_bouncing01),
            contentDescription = stringResource(R.string.home_car_image_description),
            contentScale = ContentScale.Fit,
            modifier = imageModifier
        )
        Spacer(modifier = Modifier.height(12.dp))
        EmitterQuickStartButton(
            text = stringResource(R.string.home_start_button),
            onClick = onStartClick,
            icon = Icons.Default.RocketLaunch,
            modifier = Modifier.fillMaxWidth(0.92f),
            supportingText = if (lastDeviceName != null) {
                stringResource(R.string.home_last_devices) + " " + lastDeviceName
            } else {
                stringResource(R.string.home_scan_a_device)
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.home_version_info, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelSmall,
            color = brandPrimary(),
            modifier = Modifier
                .semantics {
                    role = Role.Button
                    contentDescription = context.getString(R.string.home_about_content_description)
                }
                .clickable(onClick = onAboutClick)
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun HomeItemCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    isBluetoothConnected: Boolean = false,
) {
    EmitterStyledCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EmitterIconContainer(
                icon = icon,
                contentDescription = title,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ConnectionStatusDot(connected = isBluetoothConnected)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isBluetoothConnected) StatusConnected else StatusDisconnected,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    EmitterAppTheme {
        HomeScreen()
    }
}

@Preview(
    showSystemUi = true,
    name = "Landscape",
    device = "spec:width=840dp,height=420dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape"
)
@Composable
fun HomeScreenLandscapePreview() {
    EmitterAppTheme {
        HomeScreen()
    }
}

@Preview(showSystemUi = true, name = "Dark Mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun HomeScreenDarkPreview() {
    EmitterAppTheme {
        HomeScreen()
    }
}

