package com.micsbol.telecon4esp32.domain.camera

/**
 * Shared SoftAP HTTP camera lifecycle for Kit A ([CameraLinkProfile.WIFI_SOFTAP])
 * and Kit B ([CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE]).
 *
 * Call [onVisible] / [onHidden] from the screen lifecycle, and [setProfile] when
 * board / transport settings change so DevKit stops polling SoftAP.
 *
 * Important: [apply] must not call [CameraStreamRepository.startStream] again while
 * already streaming — that cancels the HTTP client and leaves the HUD on one frame
 * (e.g. after BLE connect on Kit B).
 */
class Esp32CameraLinkSession(
    private val repository: CameraStreamRepository,
    private val baseUrl: String = Esp32CameraDefaults.DEFAULT_BASE_URL,
    private val skipOnEmulator: Boolean = false,
) {
    private var screenVisible: Boolean = false
    private var profile: CameraLinkProfile = CameraLinkProfile.CONTROL_ONLY
    private var cameraEnabled: Boolean = true
    private var streaming: Boolean = false

    fun setProfile(profile: CameraLinkProfile) {
        if (this.profile == profile) return
        this.profile = profile
        apply()
    }

    /**
     * Optional UI toggle (e.g. Smart Door Lock camera mute). When false, stream
     * stops even if the profile would otherwise start SoftAP HTTP video.
     */
    fun setCameraEnabled(enabled: Boolean) {
        if (cameraEnabled == enabled) return
        cameraEnabled = enabled
        apply()
    }

    fun onVisible() {
        if (screenVisible) {
            apply()
            return
        }
        screenVisible = true
        apply()
    }

    fun onHidden() {
        screenVisible = false
        apply()
    }

    fun stop() {
        screenVisible = false
        streaming = false
        repository.stopStream()
    }

    /**
     * Force-stop then re-start SoftAP HTTP when firmware `/camconfig` may drop
     * an in-flight `/stream` client (e.g. Smooth QVGA framesize change).
     */
    fun restartIfStreaming() {
        if (!streaming) return
        repository.stopStream()
        streaming = false
        apply()
    }

    private fun apply() {
        if (skipOnEmulator) {
            if (streaming) {
                streaming = false
                repository.stopStream()
            }
            return
        }
        val shouldStream = screenVisible &&
            cameraEnabled &&
            profile.shouldStartCameraStream
        if (shouldStream) {
            if (!streaming) {
                streaming = true
                repository.startStream(baseUrl)
            }
        } else if (streaming) {
            streaming = false
            repository.stopStream()
        }
    }
}
