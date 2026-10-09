package com.micsbol.telecon4esp32.domain.model

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothProtocolMode
import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothTransportType
import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionLinkFamily
import com.micsbol.telecon4esp32.domain.bluetooth.linkFamily

/** Applications that support an ADVANCED (binary) protocol in addition to SIMPLE lines. */
fun ApplicationId.supportsBinaryProtocol(): Boolean = true

/**
 * Binary / BLE / Kit advanced options require Pro or a coin grant.
 *
 * Unlocked when:
 * - [PremiumFeature.ADVANCED_PROTOCOL] is granted / subscribed, or
 * - this app’s own [premiumFeature] is granted / subscribed (paid modules include Advanced).
 *
 * Control Panel camera/radar/session-CSV extras do **not** unlock Advanced. Default (Simple)
 * CAM settings stay available after extras are unlocked; Advanced stays behind
 * the settings lock until [PremiumFeature.ADVANCED_PROTOCOL] is granted.
 *
 * When [requiresCoinEntry] is true (free tier or debug coin mode), DEBUG Premium alone
 * does not unlock Advanced — same rule as Catalog entry and Control Panel center extras.
 */
fun Entitlement.canUseAdvancedProtocol(
    applicationId: ApplicationId,
    wallet: CoinWalletState = CoinWalletState.Empty,
    requiresCoinEntry: Boolean = usesCoinEconomy(),
): Boolean {
    if (
        hasFeatureAccess(
            entitlement = this,
            feature = PremiumFeature.ADVANCED_PROTOCOL,
            wallet = wallet,
            requiresCoinEntry = requiresCoinEntry,
        )
    ) {
        return true
    }
    val appFeature = applicationId.premiumFeature() ?: return false
    return hasFeatureAccess(
        entitlement = this,
        feature = appFeature,
        wallet = wallet,
        requiresCoinEntry = requiresCoinEntry,
    )
}

fun Entitlement.effectiveProtocolMode(
    applicationId: ApplicationId,
    stored: BluetoothProtocolMode,
    wallet: CoinWalletState = CoinWalletState.Empty,
    requiresCoinEntry: Boolean = usesCoinEconomy(),
): BluetoothProtocolMode {
    if (!applicationId.supportsBinaryProtocol() && stored == BluetoothProtocolMode.ADVANCED) {
        return BluetoothProtocolMode.SIMPLE
    }
    return stored
}

fun Entitlement.effectiveConnectionMode(
    applicationId: ApplicationId,
    transport: BluetoothTransportType,
    storedProtocol: BluetoothProtocolMode,
    board: Esp32Board = Esp32Board.DEV_KIT,
    wallet: CoinWalletState = CoinWalletState.Empty,
    requiresCoinEntry: Boolean = usesCoinEconomy(),
): BluetoothConnectionMode {
    val protocol = effectiveProtocolMode(
        applicationId,
        storedProtocol,
        wallet,
        requiresCoinEntry,
    )
    return when (transport) {
        BluetoothTransportType.BLE -> BluetoothConnectionMode.BLE_BINARY
        BluetoothTransportType.CLASSIC -> when (protocol) {
            BluetoothProtocolMode.ADVANCED -> BluetoothConnectionMode.CLASSIC_BINARY
            BluetoothProtocolMode.SIMPLE -> BluetoothConnectionMode.CLASSIC_SIMPLE
        }
        // DevKit Wi-Fi control and one-board CAM control are withdrawn.
        // A fresh or leftover Wi-Fi transport starts on Classic Simple.
        BluetoothTransportType.WIFI -> BluetoothConnectionMode.CLASSIC_SIMPLE
    }
}

fun Entitlement.canUseConnectionMode(
    applicationId: ApplicationId,
    mode: BluetoothConnectionMode,
    wallet: CoinWalletState = CoinWalletState.Empty,
    requiresCoinEntry: Boolean = usesCoinEconomy(),
): Boolean = mode in applicationId.availableConnectionModes(Esp32Board.DEV_KIT)

/**
 * Connection modes offered in app settings for the selected board.
 *
 * Role B — one ESP32-CAM ([Esp32Board.CAM]):
 * - [WIFI_CAM_STARTER] — SoftAP Simple (video + TCP `:3333`)
 * - [WIFI_BINARY] — Advanced SoftAP Binary (video + TCP)
 * Wi‑Fi control-only ([WIFI_BINARY] on a DevKit) does not enable the camera.
 *
 * Role A — DevKit Bluetooth + optional SoftAP camera overlay:
 * Default: Classic Simple (ESP32-TC-RC-BT-Simple) with video-only CAM.
 * Advanced: Classic Binary and BLE Binary stay available. Enabling
 * SoftAP camera does not change this list (overlay is not a connection mode).
 *
 * [Esp32Board.CAM_AND_DEV_KIT] (legacy dual-board storage): same three Bluetooth
 * modes as Role A. SoftAP video is an overlay, never BLE-only.
 *
 * DevKit / non-camera apps: Classic Simple/Binary, BLE Binary, and Advanced
 * Wi‑Fi SoftAP Binary (no camera). DevKit Wi‑Fi Simple is withdrawn.
 */
/** One-board ESP32-CAM control is withdrawn. Camera video uses the SoftAP overlay. */
fun ApplicationId.supportsCamVideoControl(): Boolean = false

fun ApplicationId.availableConnectionModes(board: Esp32Board): List<BluetoothConnectionMode> {
    return listOf(
        BluetoothConnectionMode.CLASSIC_SIMPLE,
        BluetoothConnectionMode.CLASSIC_BINARY,
        BluetoothConnectionMode.BLE_BINARY,
    )
}

fun ApplicationId.connectionModesForFamily(
    board: Esp32Board,
    family: ConnectionLinkFamily,
): List<BluetoothConnectionMode> =
    availableConnectionModes(board).filter { it.linkFamily == family }

fun ApplicationId.isConnectionModeAvailable(
    board: Esp32Board,
    mode: BluetoothConnectionMode,
): Boolean = mode in availableConnectionModes(board)

/**
 * Last saved Bluetooth mode, or Classic Simple when nothing selectable is stored.
 *
 * Classic Simple, Classic Binary, and BLE Binary are restored as saved.
 * A missing value or a withdrawn Wi-Fi mode starts on Classic Simple, even if an
 * older transport or protocol key still says binary.
 */
fun BluetoothConnectionMode?.orDefaultClassicSimple(): BluetoothConnectionMode = when (this) {
    BluetoothConnectionMode.CLASSIC_SIMPLE,
    BluetoothConnectionMode.CLASSIC_BINARY,
    BluetoothConnectionMode.BLE_BINARY,
    -> this
    else -> BluetoothConnectionMode.CLASSIC_SIMPLE
}

/**
 * Connection mode for live connect buttons.
 *
 * A saved Classic Simple, Classic Binary, or BLE Binary pick wins over older
 * transport and protocol keys.
 */
@Suppress("UNUSED_PARAMETER")
fun liveControlConnectionMode(
    applicationId: ApplicationId,
    board: Esp32Board,
    storedMode: BluetoothConnectionMode?,
    transport: BluetoothTransportType,
    protocol: BluetoothProtocolMode,
): BluetoothConnectionMode = storedMode.orDefaultClassicSimple()

/**
 * Maps a stored mode onto one valid for [board], preferring SoftAP on CAM and
 * Classic Simple on DevKit when the current choice is hidden.
 */
fun Entitlement.coerceConnectionModeForBoard(
    applicationId: ApplicationId,
    board: Esp32Board,
    mode: BluetoothConnectionMode,
    wallet: CoinWalletState = CoinWalletState.Empty,
    requiresCoinEntry: Boolean = usesCoinEconomy(),
): BluetoothConnectionMode {
    fun allowed(candidate: BluetoothConnectionMode): Boolean =
        canUseConnectionMode(applicationId, candidate, wallet, requiresCoinEntry)

    val visible = applicationId.availableConnectionModes(board)
    if (mode in visible && allowed(mode)) return mode

    return liveControlConnectionMode(
        applicationId = applicationId,
        board = board,
        storedMode = mode,
        transport = mode.transport,
        protocol = mode.protocolMode,
    )
}
