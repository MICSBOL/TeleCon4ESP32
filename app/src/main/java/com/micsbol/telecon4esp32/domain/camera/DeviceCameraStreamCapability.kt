package com.micsbol.telecon4esp32.domain.camera

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Whether this phone is likely to struggle with SoftAP camera decode + HUD
 * (Wi‑Fi SoftAP RX, JPEG decode, Compose HUD) while controlling an ESP32-CAM.
 */
enum class DeviceCameraStreamRisk {
    /** Flagship / high-memory devices — SoftAP camera should be fine. */
    OK,

    /** Mid/low-end or low-RAM — warn; prefer Smooth preset / lower HUD FPS. */
    AT_RISK,
}

data class DeviceCameraStreamSignals(
    val memoryClassMb: Int,
    val isLowRamDevice: Boolean,
    val sdkInt: Int,
)

fun assessDeviceCameraStreamRisk(signals: DeviceCameraStreamSignals): DeviceCameraStreamRisk {
    if (signals.isLowRamDevice) return DeviceCameraStreamRisk.AT_RISK
    if (signals.sdkInt < Build.VERSION_CODES.O) return DeviceCameraStreamRisk.AT_RISK
    // Heap class ≤384 MB is typical of mid/low-end phones that struggle on SoftAP + HUD.
    if (signals.memoryClassMb <= 384) return DeviceCameraStreamRisk.AT_RISK
    return DeviceCameraStreamRisk.OK
}

fun Context.deviceCameraStreamRisk(): DeviceCameraStreamRisk {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return assessDeviceCameraStreamRisk(
        DeviceCameraStreamSignals(
            memoryClassMb = activityManager.memoryClass,
            isLowRamDevice = activityManager.isLowRamDevice,
            sdkInt = Build.VERSION.SDK_INT,
        ),
    )
}

fun Context.isCameraStreamAtRisk(): Boolean =
    deviceCameraStreamRisk() == DeviceCameraStreamRisk.AT_RISK
