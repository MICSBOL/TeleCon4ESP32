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
    if (stored == BluetoothProtocolMode.SIMPLE) return BluetoothProtocolMode.SIMPLE
    return if (
        applicationId.supportsBinaryProtocol() &&
        canUseAdvancedProtocol(applicationId, wallet, requiresCoinEntry)
    ) {
        BluetoothProtocolMode.ADVANCED
    } else {
        BluetoothProtocolMode.SIMPLE
    }
}

fun Entitlement.effectiveConnectionMode(
    applicationId: ApplicationId,
    transport: BluetoothTransportType,
    storedProtocol: BluetoothProtocolMode,
    board: Esp32Board = Esp32Board.DEV_KIT,
    wallet: CoinWalletState = CoinWalletState.Empty,
    requiresCoinEntry: Boolean = usesCoinEconomy(),
): BluetoothConnectionMode {
    if (transport == BluetoothTransportType.WIFI) {
        // CAM SoftAP: Default / Simple stays starter. Advanced protocol may pick SoftAP Binary.
        if (applicationId.usesCamera() && board == Esp32Board.CAM) {
            return if (
                storedProtocol == BluetoothProtocolMode.ADVANCED &&
                canUseConnectionMode(
                    applicationId,
                    BluetoothConnectionMode.WIFI_BINARY,
                    wallet,
                    requiresCoinEntry,
                )
            ) {
                BluetoothConnectionMode.WIFI_BINARY
            } else {
                BluetoothConnectionMode.WIFI_CAM_STARTER
            }
        }
        val protocol = effectiveProtocolMode(
            applicationId,
            storedProtocol,
            wallet,
            requiresCoinEntry,
        )
        return if (protocol == BluetoothProtocolMode.ADVANCED) {
            BluetoothConnectionMode.WIFI_BINARY
        } else {
            BluetoothConnectionMode.WIFI_SIMPLE
        }
    }
    val protocol = effectiveProtocolMode(
        applicationId,
        storedProtocol,
        wallet,
        requiresCoinEntry,
    )
    val mode = BluetoothConnectionMode.from(transport, protocol)
    // BLE always implies binary when the user is allowed to use advanced.
    if (
        transport == BluetoothTransportType.BLE &&
        canUseAdvancedProtocol(applicationId, wallet, requiresCoinEntry)
    ) {
        return BluetoothConnectionMode.BLE_BINARY
    }
    // BLE without premium falls back to Classic Simple for the UI selection.
    if (
        transport == BluetoothTransportType.BLE &&
        !canUseAdvancedProtocol(applicationId, wallet, requiresCoinEntry)
    ) {
        return BluetoothConnectionMode.CLASSIC_SIMPLE
    }
    return mode
}

fun Entitlement.canUseConnectionMode(
    applicationId: ApplicationId,
    mode: BluetoothConnectionMode,
    wallet: CoinWalletState = CoinWalletState.Empty,
    requiresCoinEntry: Boolean = usesCoinEconomy(),
): Boolean = when (mode) {
    BluetoothConnectionMode.CLASSIC_SIMPLE,
    BluetoothConnectionMode.WIFI_CAM_STARTER,
    BluetoothConnectionMode.WIFI_SIMPLE,
    -> true
    BluetoothConnectionMode.WIFI_SOFTAP,
    BluetoothConnectionMode.CLASSIC_BINARY,
    BluetoothConnectionMode.BLE_BINARY,
    BluetoothConnectionMode.WIFI_BINARY,
    -> canUseAdvancedProtocol(applicationId, wallet, requiresCoinEntry)
}

/**
 * Connection modes offered in app settings for the selected board.
 *
 * Role B — one ESP32-CAM ([Esp32Board.CAM]):
 * - [WIFI_CAM_STARTER] — SoftAP Simple (video + TCP `:3333`)
 * - [WIFI_BINARY] — Advanced SoftAP Binary (video + TCP)
 *
 * Role A — DevKit Bluetooth + optional SoftAP camera overlay:
 * Default: Classic Simple (ESP32-TC-RC-BT-Simple) with video-only CAM.
 * Advanced: Classic Binary and BLE Binary stay available. Enabling
 * SoftAP camera does not change this list (overlay is not a connection mode).
 *
 * [Esp32Board.CAM_AND_DEV_KIT] (legacy dual-board storage): same three Bluetooth
 * modes as Role A. SoftAP video is an overlay, never BLE-only.
 *
 * DevKit / non-camera apps: Classic Simple/Binary, BLE Binary, Wi‑Fi Simple/Binary.
 */
fun ApplicationId.availableConnectionModes(board: Esp32Board): List<BluetoothConnectionMode> {
    if (usesCamera() && board.isKitBDual) {
        return listOf(
            BluetoothConnectionMode.CLASSIC_SIMPLE,
            BluetoothConnectionMode.CLASSIC_BINARY,
            BluetoothConnectionMode.BLE_BINARY,
        )
    }
    if (usesCamera() && board == Esp32Board.CAM) {
        return listOf(
            BluetoothConnectionMode.WIFI_CAM_STARTER,
            BluetoothConnectionMode.WIFI_BINARY,
        )
    }
    return listOf(
        BluetoothConnectionMode.CLASSIC_SIMPLE,
        BluetoothConnectionMode.CLASSIC_BINARY,
        BluetoothConnectionMode.BLE_BINARY,
        BluetoothConnectionMode.WIFI_SIMPLE,
        BluetoothConnectionMode.WIFI_BINARY,
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

    // Legacy Kit A text SoftAP / stale BLE on CAM → SoftAP Binary or Starter.
    if (
        applicationId.usesCamera() &&
        board == Esp32Board.CAM &&
        (
            mode.transport == BluetoothTransportType.WIFI ||
                mode == BluetoothConnectionMode.BLE_BINARY
            )
    ) {
        val preferred = when {
            mode == BluetoothConnectionMode.WIFI_CAM_STARTER -> mode
            mode == BluetoothConnectionMode.WIFI_BINARY -> mode
            mode.settingsUserType == SettingsUserType.NORMAL ->
                BluetoothConnectionMode.WIFI_CAM_STARTER
            mode == BluetoothConnectionMode.WIFI_SOFTAP ||
                mode == BluetoothConnectionMode.BLE_BINARY -> {
                if (allowed(BluetoothConnectionMode.WIFI_BINARY)) {
                    BluetoothConnectionMode.WIFI_BINARY
                } else {
                    BluetoothConnectionMode.WIFI_CAM_STARTER
                }
            }
            else -> BluetoothConnectionMode.WIFI_CAM_STARTER
        }
        if (preferred in visible && allowed(preferred)) {
            return preferred
        }
    }

    // SoftAP kit / starter mode while on DevKit → DevKit Wi‑Fi Simple / Binary.
    if (
        board == Esp32Board.DEV_KIT &&
        (
            mode == BluetoothConnectionMode.WIFI_SOFTAP ||
                mode == BluetoothConnectionMode.WIFI_CAM_STARTER
            )
    ) {
        val wifi = if (allowed(BluetoothConnectionMode.WIFI_BINARY) &&
            mode == BluetoothConnectionMode.WIFI_SOFTAP
        ) {
            BluetoothConnectionMode.WIFI_BINARY
        } else {
            BluetoothConnectionMode.WIFI_SIMPLE
        }
        if (wifi in visible && allowed(wifi)) {
            return wifi
        }
    }

    // On CAM, keep Default on SoftAP starter; only Advanced modes pick SoftAP Binary.
    if (applicationId.usesCamera() && board == Esp32Board.CAM) {
        if (mode.settingsUserType == SettingsUserType.NORMAL) {
            val starter = BluetoothConnectionMode.WIFI_CAM_STARTER
            if (starter in visible && allowed(starter)) {
                return starter
            }
        }
        val binary = BluetoothConnectionMode.WIFI_BINARY
        if (binary in visible && allowed(binary)) {
            return binary
        }
        val starter = BluetoothConnectionMode.WIFI_CAM_STARTER
        if (starter in visible && allowed(starter)) {
            return starter
        }
    }

    return visible.firstOrNull { allowed(it) }
        ?: BluetoothConnectionMode.CLASSIC_SIMPLE
}
