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
    val manufacturer: String = "",
    val model: String = "",
    /** Physical RAM from [ActivityManager.MemoryInfo.totalMem], in MiB. 0 if unknown. */
    val totalRamMb: Long = 0,
    /** [Build.VERSION.MEDIA_PERFORMANCE_CLASS], or 0 if unavailable / unmet. */
    val mediaPerformanceClass: Int = 0,
)

/** Android 12 media performance class — S23 Ultra and similar flagships report 33+. */
private const val MIN_OK_MEDIA_PERFORMANCE_CLASS = 31

/**
 * 8 GB-class phones report ~7 GB+ as [ActivityManager.MemoryInfo.totalMem];
 * 6 GB mid-range phones report less than 6 GB after firmware reservation.
 */
private const val MIN_OK_RAM_MB = 6 * 1024L

/** Fallback only when physical RAM is unknown. */
private const val AT_RISK_HEAP_CLASS_MB = 384

/** MIUI / Poco SoftAP clients buffer TCP MJPEG and show a delayed picture. */
fun isKnownSoftApLaggyClient(manufacturer: String, model: String): Boolean {
    val mfr = manufacturer.lowercase()
    val mdl = model.lowercase()
    return mfr.contains("xiaomi") ||
        mfr.contains("redmi") ||
        mfr.contains("poco") ||
        mdl.contains("poco") ||
        mdl.startsWith("m2102") ||
        mdl.startsWith("m2007j20")
}

fun assessDeviceCameraStreamRisk(signals: DeviceCameraStreamSignals): DeviceCameraStreamRisk {
    if (signals.isLowRamDevice) return DeviceCameraStreamRisk.AT_RISK
    if (signals.sdkInt < Build.VERSION_CODES.O) return DeviceCameraStreamRisk.AT_RISK
    if (isKnownSoftApLaggyClient(signals.manufacturer, signals.model)) {
        return DeviceCameraStreamRisk.AT_RISK
    }
    // Heap class is the per-app ART limit, not device capacity. Flagships like
    // S23 Ultra often report 256 MB even with 8–12 GB RAM — do not warn on that.
    if (signals.mediaPerformanceClass >= MIN_OK_MEDIA_PERFORMANCE_CLASS) {
        return DeviceCameraStreamRisk.OK
    }
    if (signals.totalRamMb >= MIN_OK_RAM_MB) {
        return DeviceCameraStreamRisk.OK
    }
    if (signals.totalRamMb in 1 until MIN_OK_RAM_MB) {
        return DeviceCameraStreamRisk.AT_RISK
    }
    if (signals.memoryClassMb <= AT_RISK_HEAP_CLASS_MB) return DeviceCameraStreamRisk.AT_RISK
    return DeviceCameraStreamRisk.OK
}

fun Context.deviceCameraStreamRisk(): DeviceCameraStreamRisk {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memInfo)
    val mediaPerformanceClass =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.VERSION.MEDIA_PERFORMANCE_CLASS
        } else {
            0
        }
    return assessDeviceCameraStreamRisk(
        DeviceCameraStreamSignals(
            memoryClassMb = activityManager.memoryClass,
            isLowRamDevice = activityManager.isLowRamDevice,
            sdkInt = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            totalRamMb = memInfo.totalMem / (1024L * 1024L),
            mediaPerformanceClass = mediaPerformanceClass,
        ),
    )
}

fun Context.isCameraStreamAtRisk(): Boolean =
    deviceCameraStreamRisk() == DeviceCameraStreamRisk.AT_RISK
