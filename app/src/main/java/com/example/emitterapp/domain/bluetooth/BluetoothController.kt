package com.example.emitterapp.domain.bluetooth

/**
 * @deprecated This interface is dead code. [RemoteController] is the active abstraction.
 * [AndroidBluetoothController] implements [RemoteController], not this interface.
 * This file is kept only to avoid git blame confusion; it should be deleted.
 */
@Deprecated(
    message = "Dead code. Use RemoteController instead.",
    replaceWith = ReplaceWith("RemoteController", "com.example.emitterapp.domain.bluetooth.RemoteController")
)
interface BluetoothController
