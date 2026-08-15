package com.micsbol.telecon4esp32.ui.smartdoorlock

import com.micsbol.telecon4esp32.domain.camera.CameraLinkProfile
import com.micsbol.telecon4esp32.domain.camera.CameraStreamState

enum class DoorLockState {
    LOCKED,
    UNLOCKED,
}

enum class RelayPinState {
    LOW,
    HIGH,
}

data class SmartDoorLockUiState(
    val cameraLinkProfile: CameraLinkProfile = CameraLinkProfile.CONTROL_ONLY,
    val cameraState: CameraStreamState = CameraStreamState.Idle,
    val isCameraOnline: Boolean = false,
    val isEsp32Online: Boolean = false,
    val isCallActive: Boolean = false,
    val callDurationSeconds: Int = 0,
    val doorLockState: DoorLockState = DoorLockState.LOCKED,
    val wifiSignalDbm: Int = -42,
    val relayPinState: RelayPinState = RelayPinState.LOW,
    val isMicEnabled: Boolean = true,
    val isSpeakerEnabled: Boolean = true,
    val isCameraEnabled: Boolean = true,
    val isRelayPulseActive: Boolean = false,
    val lastSignalMessage: String? = null,
)
