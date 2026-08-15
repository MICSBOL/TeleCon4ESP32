package com.micsbol.telecon4esp32.domain.bluetooth

/**
 * User-facing link choice for an application.
 *
 * SoftAP TCP modes are shared across apps (Control Panel, RC Vehicle, Greenhouse, …):
 * - [WIFI_SIMPLE] / [WIFI_BINARY] — SoftAP TCP with `proto=simple` / `proto=binary`
 * - On [com.micsbol.telecon4esp32.domain.model.Esp32Board.CAM] + camera apps, those modes
 *   also attach SoftAP HTTP video (same SoftAP, different sketch SSID).
 * - [WIFI_CAM_STARTER] — Normal-user CAM SoftAP with starter SSID + `proto=simple`
 * - [WIFI_SOFTAP] — legacy RC Kit A text SoftAP (`proto=wifi`); coerce to [WIFI_BINARY]
 *
 * Classic offers text and binary; BLE always uses binary (NUS).
 */
enum class BluetoothConnectionMode {
    /** Classic SPP + SIMPLE text lines. */
    CLASSIC_SIMPLE,

    /** Classic SPP + ADVANCED binary packets. */
    CLASSIC_BINARY,

    /** BLE (NUS) + ADVANCED binary packets. */
    BLE_BINARY,

    /**
     * Normal-user ESP32-CAM SoftAP starter firmware.
     * SoftAP TCP + SIMPLE text (`proto=simple`); distinct starter SSID.
     */
    WIFI_CAM_STARTER,

    /**
     * Legacy CAM SoftAP Kit A text control (`proto=wifi`).
     * Prefer [WIFI_BINARY] for new Advanced CAM SoftAP (binary + video).
     */
    WIFI_SOFTAP,

    /** SoftAP TCP + SIMPLE text (`proto=simple`). DevKit or CAM (board selects SSID / camera). */
    WIFI_SIMPLE,

    /** SoftAP TCP + ADVANCED binary (`proto=binary`). DevKit or CAM (board selects SSID / camera). */
    WIFI_BINARY,
    ;

    val transport: BluetoothTransportType
        get() = when (this) {
            CLASSIC_SIMPLE, CLASSIC_BINARY -> BluetoothTransportType.CLASSIC
            BLE_BINARY -> BluetoothTransportType.BLE
            WIFI_CAM_STARTER, WIFI_SOFTAP, WIFI_SIMPLE, WIFI_BINARY -> BluetoothTransportType.WIFI
        }

    val protocolMode: BluetoothProtocolMode
        get() = when (this) {
            CLASSIC_SIMPLE, WIFI_CAM_STARTER, WIFI_SOFTAP, WIFI_SIMPLE -> BluetoothProtocolMode.SIMPLE
            CLASSIC_BINARY, BLE_BINARY, WIFI_BINARY -> BluetoothProtocolMode.ADVANCED
        }

    val isWifiLink: Boolean
        get() = transport == BluetoothTransportType.WIFI

    val isBluetoothLink: Boolean
        get() = !isWifiLink

    /** SoftAP TCP control that needs a SIMPLE CTRL heartbeat (not binary SoftAP). */
    val needsSoftApCtrlHeartbeat: Boolean
        get() = when (this) {
            WIFI_CAM_STARTER, WIFI_SOFTAP, WIFI_SIMPLE -> true
            else -> false
        }

    /**
     * SoftAP TCP modes that carry camera HTTP when the board is CAM + camera app.
     * Shared base for RC Vehicle, Greenhouse, etc.
     */
    val isSoftApTcp: Boolean
        get() = when (this) {
            WIFI_CAM_STARTER, WIFI_SOFTAP, WIFI_SIMPLE, WIFI_BINARY -> true
            else -> false
        }

    /**
     * Historical CAM SoftAP modes that use camera SoftAP SSIDs (starter / legacy Kit A).
     * SoftAP Binary on CAM also uses the main CAM SSID — pass [Esp32Board] when choosing copy.
     * Prefer [isSoftApTcp] + board for new UI.
     */
    val isCamSoftApControl: Boolean
        get() = this == WIFI_CAM_STARTER || this == WIFI_SOFTAP

    companion object {
        fun from(
            transport: BluetoothTransportType,
            protocolMode: BluetoothProtocolMode,
        ): BluetoothConnectionMode = when (transport) {
            // Ambiguous vs CAM starter; callers with a board should prefer
            // [com.micsbol.telecon4esp32.domain.model.effectiveConnectionMode].
            BluetoothTransportType.WIFI -> when (protocolMode) {
                BluetoothProtocolMode.ADVANCED -> WIFI_BINARY
                BluetoothProtocolMode.SIMPLE -> WIFI_SIMPLE
            }
            BluetoothTransportType.BLE -> BLE_BINARY
            BluetoothTransportType.CLASSIC -> when (protocolMode) {
                BluetoothProtocolMode.ADVANCED -> CLASSIC_BINARY
                BluetoothProtocolMode.SIMPLE -> CLASSIC_SIMPLE
            }
        }

        fun fromStored(value: String?): BluetoothConnectionMode? =
            value?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}

/** Top-level link family shown for DevKit settings (Bluetooth vs Wi‑Fi). */
enum class ConnectionLinkFamily {
    BLUETOOTH,
    WIFI,
}

val BluetoothConnectionMode.linkFamily: ConnectionLinkFamily
    get() = if (isWifiLink) ConnectionLinkFamily.WIFI else ConnectionLinkFamily.BLUETOOTH
