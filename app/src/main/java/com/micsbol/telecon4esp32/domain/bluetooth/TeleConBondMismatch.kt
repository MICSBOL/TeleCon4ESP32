package com.micsbol.telecon4esp32.domain.bluetooth

/**
 * Detects when a bonded ESP32/TeleCon device is advertising a different Bluetooth name,
 * which usually means the board was reflashed (e.g. BLE → Classic) while the old bond remains.
 */
object TeleConBondMismatch {

    fun looksLikeTeleConDevice(name: String?): Boolean {
        val normalized = name?.trim()?.uppercase().orEmpty()
        if (normalized.isEmpty()) return false
        return normalized.contains("TELECON") || normalized.contains("ESP32")
    }

    /**
     * True when the same MAC is bonded under one TeleCon-like name but is now
     * advertising a different non-blank name.
     */
    fun isStaleFirmwareBond(bondedName: String?, advertisedName: String?): Boolean {
        val bonded = bondedName?.trim().orEmpty()
        val advertised = advertisedName?.trim().orEmpty()
        if (bonded.isEmpty() || advertised.isEmpty()) return false
        if (bonded.equals(advertised, ignoreCase = true)) return false
        return looksLikeTeleConDevice(bonded) || looksLikeTeleConDevice(advertised)
    }

    fun findStaleFirmwareDevice(
        scannedDevices: List<RemoteDevice>,
        pairedDevices: List<RemoteDevice>,
    ): RemoteDevice? {
        val pairedByAddress = pairedDevices.associateBy { it.address.uppercase() }
        return scannedDevices.firstOrNull { scanned ->
            val paired = pairedByAddress[scanned.address.uppercase()] ?: return@firstOrNull false
            isStaleFirmwareBond(paired.name, scanned.name)
        }
    }
}
