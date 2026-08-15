package com.micsbol.telecon4esp32.ui.cyber.screens

import android.app.Activity
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.GridView
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.micsbol.telecon4esp32.BuildConfig
import com.micsbol.telecon4esp32.R
import com.micsbol.telecon4esp32.domain.bluetooth.ActiveBluetoothSession
import com.micsbol.telecon4esp32.domain.model.ApplicationId
import com.micsbol.telecon4esp32.domain.model.Entitlement
import com.micsbol.telecon4esp32.domain.model.PremiumSource
import com.micsbol.telecon4esp32.domain.model.resolveHomeFeaturedApplication
import com.micsbol.telecon4esp32.domain.model.usesCoinEconomy
import com.micsbol.telecon4esp32.ui.applications.thumbnailRes
import com.micsbol.telecon4esp32.ui.applications.titleRes
import com.micsbol.telecon4esp32.ui.bluetooth.connectFailureMessage
import com.micsbol.telecon4esp32.ui.bluetooth.handshakeFailureMessage
import com.micsbol.telecon4esp32.ui.components.HoloTurntableFlipbook
import com.micsbol.telecon4esp32.ui.components.NeoIconButton
import com.micsbol.telecon4esp32.ui.components.NeoPillButton
import com.micsbol.telecon4esp32.ui.components.NeumorphicBackground
import com.micsbol.telecon4esp32.ui.components.glassSurface
import com.micsbol.telecon4esp32.ui.components.holoTurntableAssetDir
import com.micsbol.telecon4esp32.ui.components.safeHudPadding
import com.micsbol.telecon4esp32.ui.entitlement.LocalEntitlement
import com.micsbol.telecon4esp32.ui.home.HomeHelpDialog
import com.micsbol.telecon4esp32.ui.navigation.Screen
import com.micsbol.telecon4esp32.ui.theme.Neo
import com.micsbol.telecon4esp32.ui.wallet.LocalWallet

private val ProGold = Color(0xFFFFD54F)
private val ProGoldDeep = Color(0xFFFFB300)

/**
 * Minimal home: compact chrome, last-module hologram, one primary CTA.
 * Landscape uses a side-by-side layout to fill horizontal space.
 */
@Composable
fun CyberHomeScreen(
    navController: NavHostController? = null,
    isConnecting: Boolean = false,
    activeSession: ActiveBluetoothSession? = null,
    lastApplicationId: ApplicationId? = null,
    lastDeviceName: String? = null,
    errorMessage: String? = null,
    connectFailure: com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectFailure? = null,
    handshakeFailure: com.micsbol.telecon4esp32.domain.bluetooth.HandshakeFailure? = null,
    onOpenApplications: () -> Unit = {},
    onContinueSession: () -> Unit = {},
    onOpenRecentProject: (ApplicationId) -> Unit = {},
    onDismissError: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var showHelpDialog by remember { mutableStateOf(false) }

    BackHandler { activity?.finish() }

    val isSessionConnected = activeSession != null
    val resolvedError = when {
        handshakeFailure != null -> handshakeFailureMessage(handshakeFailure)
        connectFailure != null -> connectFailureMessage(connectFailure)
        errorMessage != null -> errorMessage
        else -> null
    }

    val entitlement = LocalEntitlement.current
    val wallet = LocalWallet.current
    val requiresCoinEntry = entitlement.usesCoinEconomy() || BuildConfig.DEBUG
    // Debug builds force Premium via DEBUG_OVERRIDE — still show PRO so subscription UI is reachable.
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
    val deviceSubtitle = when {
        activeSession?.deviceName != null -> activeSession.deviceName
        featuredAppId == lastApplicationId && !lastDeviceName.isNullOrBlank() -> lastDeviceName
        else -> null
    }
    val primaryLabel = stringResource(R.string.home_session_open_applications)

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
            val isLandscape = maxWidth > maxHeight
            val edgePad = if (maxWidth < 400.dp) 16.dp else 20.dp
            val landscapeHologramSize = minOf(maxWidth * 0.38f, maxHeight * 0.72f)
                .coerceIn(160.dp, 300.dp)
            val portraitHologramSize = minOf(maxWidth * 0.72f, maxHeight * 0.42f)
                .coerceIn(180.dp, 320.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = edgePad, vertical = edgePad),
            ) {
                MinimalHomeTopBar(
                    isConnected = isSessionConnected,
                    showProButton = showProButton,
                    onProClick = { navController?.navigate(Screen.Upgrade.route) },
                    onHelpClick = { showHelpDialog = true },
                    onTitleClick = { navController?.navigate(Screen.About.route) },
                )

                Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 8.dp))

                if (isLandscape) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        HomeFeaturedPanel(
                            featuredAppId = featuredAppId,
                            deviceSubtitle = deviceSubtitle,
                            hologramSize = landscapeHologramSize,
                            isConnecting = isConnecting,
                            onOpenRecentProject = onOpenRecentProject,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart,
                            textAlign = TextAlign.Start,
                            nameBesideImage = true,
                        )
                        HomeActionsPanel(
                            primaryLabel = primaryLabel,
                            isConnecting = isConnecting,
                            onOpenApplications = onOpenApplications,
                            onOpenAbout = { navController?.navigate(Screen.About.route) },
                            modifier = Modifier.align(Alignment.BottomEnd),
                            fillButtonWidth = false,
                            contentAlignment = Alignment.BottomEnd,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        HomeFeaturedPanel(
                            featuredAppId = featuredAppId,
                            deviceSubtitle = deviceSubtitle,
                            hologramSize = portraitHologramSize,
                            isConnecting = isConnecting,
                            onOpenRecentProject = onOpenRecentProject,
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                            textAlign = TextAlign.Center,
                        )
                    }
                    HomeActionsPanel(
                        primaryLabel = primaryLabel,
                        isConnecting = isConnecting,
                        onOpenApplications = onOpenApplications,
                        onOpenAbout = { navController?.navigate(Screen.About.route) },
                        modifier = Modifier.fillMaxWidth(),
                        fillButtonWidth = true,
                        contentAlignment = Alignment.Center,
                    )
                }
            }
        }

        if (showHelpDialog) {
            HomeHelpDialog(onDismissRequest = { showHelpDialog = false })
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
private fun FeaturedAppHologram(
    featuredAppId: ApplicationId,
    hologramSize: Dp,
    isConnecting: Boolean,
    onOpenRecentProject: (ApplicationId) -> Unit,
) {
    val contentDescription = stringResource(featuredAppId.titleRes())
    val shape = RoundedCornerShape(28.dp)
    val hologramModifier = Modifier
        .size(hologramSize)
        .clip(shape)
        .border(
            1.dp,
            Neo.AccentHighlight.copy(alpha = 0.35f),
            shape,
        )
        .clickable(enabled = !isConnecting) {
            onOpenRecentProject(featuredAppId)
        }

    val turntableDir = featuredAppId.holoTurntableAssetDir()
    if (turntableDir != null) {
        HoloTurntableFlipbook(
            assetDir = turntableDir,
            contentDescription = contentDescription,
            modifier = hologramModifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Image(
            painter = painterResource(featuredAppId.thumbnailRes()),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = hologramModifier,
        )
    }
}

@Composable
private fun HomeFeaturedPanel(
    featuredAppId: ApplicationId?,
    deviceSubtitle: String?,
    hologramSize: Dp,
    isConnecting: Boolean,
    onOpenRecentProject: (ApplicationId) -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    textAlign: TextAlign = TextAlign.Center,
    nameBesideImage: Boolean = false,
) {
    Box(
        modifier = modifier,
        contentAlignment = contentAlignment,
    ) {
        if (featuredAppId != null && nameBesideImage) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                FeaturedAppHologram(
                    featuredAppId = featuredAppId,
                    hologramSize = hologramSize,
                    isConnecting = isConnecting,
                    onOpenRecentProject = onOpenRecentProject,
                )
                Column(
                    modifier = Modifier.widthIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(featuredAppId.titleRes()),
                        color = Neo.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!deviceSubtitle.isNullOrBlank()) {
                        Text(
                            text = deviceSubtitle,
                            color = Neo.TextMuted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Start,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        } else {
            Column(
                horizontalAlignment = when (textAlign) {
                    TextAlign.End -> Alignment.End
                    TextAlign.Start -> Alignment.Start
                    else -> Alignment.CenterHorizontally
                },
            ) {
                if (featuredAppId != null) {
                    FeaturedAppHologram(
                        featuredAppId = featuredAppId,
                        hologramSize = hologramSize,
                        isConnecting = isConnecting,
                        onOpenRecentProject = onOpenRecentProject,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(featuredAppId.titleRes()),
                        color = Neo.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = textAlign,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = hologramSize + 24.dp),
                    )
                    if (!deviceSubtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = deviceSubtitle,
                            color = Neo.TextMuted,
                            fontSize = 13.sp,
                            textAlign = textAlign,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = hologramSize + 24.dp),
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.cyber_recent_none),
                        color = Neo.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = textAlign,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_session_none_hint),
                        color = Neo.TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = textAlign,
                        modifier = Modifier.widthIn(max = 280.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeActionsPanel(
    primaryLabel: String,
    isConnecting: Boolean,
    onOpenApplications: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    fillButtonWidth: Boolean,
    contentAlignment: Alignment = Alignment.Center,
) {
    val context = LocalContext.current
    val horizontalAlignment = when (contentAlignment) {
        Alignment.BottomEnd, Alignment.CenterEnd, Alignment.TopEnd -> Alignment.End
        Alignment.BottomStart, Alignment.CenterStart, Alignment.TopStart -> Alignment.Start
        else -> Alignment.CenterHorizontally
    }
    Box(
        modifier = modifier,
        contentAlignment = contentAlignment,
    ) {
        Column(
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NeoPillButton(
                text = primaryLabel,
                onClick = onOpenApplications,
                icon = Icons.Filled.GridView,
                enabled = !isConnecting,
                fillMaxWidth = fillButtonWidth,
                compact = false,
                modifier = if (fillButtonWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.widthIn(min = 200.dp, max = 320.dp)
                },
            )
            Text(
                text = stringResource(R.string.home_version_info, BuildConfig.VERSION_NAME),
                color = Neo.TextMuted.copy(alpha = 0.85f),
                fontSize = 12.sp,
                textAlign = when (horizontalAlignment) {
                    Alignment.End -> TextAlign.End
                    Alignment.Start -> TextAlign.Start
                    else -> TextAlign.Center
                },
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
}

@Composable
private fun MinimalHomeTopBar(
    isConnected: Boolean,
    showProButton: Boolean,
    onProClick: () -> Unit,
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
        if (showProButton) {
            HomeProButton(onClick = onProClick)
            Spacer(modifier = Modifier.size(8.dp))
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
private fun HomeProButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.home_pro_button_content_description)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        ProGold.copy(alpha = 0.28f),
                        ProGoldDeep.copy(alpha = 0.18f),
                    ),
                ),
            )
            .border(1.dp, ProGold.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = description
            }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_pro_button),
            color = ProGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Preview(showSystemUi = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CyberHomeScreenPreview() {
    CyberHomeScreen()
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
    CyberHomeScreen(
        lastApplicationId = ApplicationId.CONTROL_PANEL,
        lastDeviceName = null,
    )
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
