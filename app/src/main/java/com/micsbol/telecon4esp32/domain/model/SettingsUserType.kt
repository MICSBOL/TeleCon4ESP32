package com.micsbol.telecon4esp32.domain.model

import com.micsbol.telecon4esp32.domain.bluetooth.BluetoothConnectionMode
import com.micsbol.telecon4esp32.domain.bluetooth.ConnectionLinkFamily

/**
 * Settings experience level for connection options.
 *
 * [NORMAL] shows starter Simple links (DevKit Simple + CAM SoftAP starter).
 * [ADVANCED] shows binary / BLE / Kit A SoftAP options.
 */
enum class SettingsUserType {
    NORMAL,
    ADVANCED,
}

val BluetoothConnectionMode.settingsUserType: SettingsUserType
    get() = when (this) {
        BluetoothConnectionMode.CLASSIC_SIMPLE,
        BluetoothConnectionMode.WIFI_SIMPLE,
        BluetoothConnectionMode.WIFI_CAM_STARTER,
        -> SettingsUserType.NORMAL
        BluetoothConnectionMode.CLASSIC_BINARY,
        BluetoothConnectionMode.BLE_BINARY,
        BluetoothConnectionMode.WIFI_BINARY,
        BluetoothConnectionMode.WIFI_SOFTAP,
        -> SettingsUserType.ADVANCED
    }

fun ApplicationId.availableConnectionModes(
    board: Esp32Board,
    userType: SettingsUserType,
): List<BluetoothConnectionMode> =
    availableConnectionModes(board).filter { it.settingsUserType == userType }

fun ApplicationId.connectionModesForFamily(
    board: Esp32Board,
    family: ConnectionLinkFamily,
    userType: SettingsUserType,
): List<BluetoothConnectionMode> =
    connectionModesForFamily(board, family).filter { it.settingsUserType == userType }

/**
 * Prefers the first usable mode for [family] and [userType], falling back across families
 * when the current link has no option for that experience level.
 */
fun ApplicationId.preferredConnectionMode(
    board: Esp32Board,
    family: ConnectionLinkFamily,
    userType: SettingsUserType,
    canUseAdvanced: Boolean,
): BluetoothConnectionMode? {
    fun firstUsable(modes: List<BluetoothConnectionMode>): BluetoothConnectionMode? =
        modes.firstOrNull { mode ->
            when (mode) {
                BluetoothConnectionMode.CLASSIC_BINARY,
                BluetoothConnectionMode.BLE_BINARY,
                BluetoothConnectionMode.WIFI_BINARY,
                BluetoothConnectionMode.WIFI_SOFTAP,
                -> canUseAdvanced
                else -> true
            }
        } ?: modes.firstOrNull()

    firstUsable(connectionModesForFamily(board, family, userType))?.let { return it }

    ConnectionLinkFamily.entries.forEach { other ->
        if (other == family) return@forEach
        firstUsable(connectionModesForFamily(board, other, userType))?.let { return it }
    }

    return firstUsable(availableConnectionModes(board, userType))
}
