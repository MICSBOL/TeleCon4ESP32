package com.micsbol.telecon4esp32.domain.bluetooth.sh

/**
 * Room / device ids shared by SIMPLE `SH:SET`/`SH:DATA`, binary SH frames, and the UI.
 * Keep in sync with help strings and the ESP32 firmware prompt.
 */
object SmartHomeProtocol {
    const val ROOM_LIVING = "living"
    const val ROOM_KITCHEN = "kitchen"
    const val ROOM_BEDROOM = "bedroom"
    const val ROOM_GARAGE = "garage"

    const val DEVICE_LIGHT = "light"
    const val DEVICE_AMBIENCE = "ambience"
    const val DEVICE_OUTLET = "outlet"
    const val DEVICE_APPLIANCE = "appliance"
    const val DEVICE_WATER = "water"
    const val DEVICE_LOCK = "lock"

    const val SCENE_ALL_LIGHTS_OFF = "all_lights_off"
    const val SCENE_AWAY = "away"

    /** Binary SET / DATA device_id bytes (1-based). */
    fun deviceIdByte(deviceId: String): Int = when (deviceId) {
        DEVICE_LIGHT -> 1
        DEVICE_AMBIENCE -> 2
        DEVICE_OUTLET -> 3
        DEVICE_APPLIANCE -> 4
        DEVICE_WATER -> 5
        DEVICE_LOCK -> 6
        else -> 0
    }

    fun deviceIdFromByte(value: Int): String? = when (value) {
        1 -> DEVICE_LIGHT
        2 -> DEVICE_AMBIENCE
        3 -> DEVICE_OUTLET
        4 -> DEVICE_APPLIANCE
        5 -> DEVICE_WATER
        6 -> DEVICE_LOCK
        else -> null
    }

    fun roomIndex(roomId: String): Int = when (roomId) {
        ROOM_LIVING -> 0
        ROOM_KITCHEN -> 1
        ROOM_BEDROOM -> 2
        ROOM_GARAGE -> 3
        else -> -1
    }

    fun roomIdFromIndex(index: Int): String? = when (index) {
        0 -> ROOM_LIVING
        1 -> ROOM_KITCHEN
        2 -> ROOM_BEDROOM
        3 -> ROOM_GARAGE
        else -> null
    }

    /** Bit positions in binary DATA device flags (u16 LE). */
    fun deviceFlagBit(roomId: String, deviceId: String): Int? = when (roomId to deviceId) {
        ROOM_LIVING to DEVICE_LIGHT -> 0
        ROOM_LIVING to DEVICE_AMBIENCE -> 1
        ROOM_LIVING to DEVICE_OUTLET -> 2
        ROOM_KITCHEN to DEVICE_LIGHT -> 3
        ROOM_KITCHEN to DEVICE_APPLIANCE -> 4
        ROOM_KITCHEN to DEVICE_WATER -> 5
        ROOM_BEDROOM to DEVICE_LIGHT -> 6
        ROOM_GARAGE to DEVICE_LIGHT -> 7
        ROOM_GARAGE to DEVICE_LOCK -> 8
        else -> null
    }
}
