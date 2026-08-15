# Cursor prompt: RC Vehicle Pro — SoftAP video quality presets for Kit B (Android)

Copy into a **new Cursor chat** (Agent mode) on
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`.

**Scope: Kit B** — SoftAP HTTP video on video-only CAM + BLE Binary on DevKit
(`CameraLinkProfile.WIFI_CAMERA_DEVKIT_BLE` / `BluetoothConnectionMode.BLE_BINARY`
with board **ESP32-CAM**).

Related (already done — extend, do not rewrite):

- Kits: [`docs/prompts/RC_VEHICLE_PRO_SUPPORTED_KITS_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_SUPPORTED_KITS_ANDROID_PROMPT.md)
- Presets (Kit A only UI originally): [`docs/prompts/RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_SETTINGS_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_SETTINGS_ANDROID_PROMPT.md)
- Stream / decode: SoftAP perf Android prompts + `Esp32CameraStreamRepository`
- `/camconfig` client: `data/camera/SoftApCamConfigClient.kt`

Firmware (optional Phase 2 companion — separate ESP32 chat if missing):

- Video-only CAM: `ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_SoftAP_Video/`
- Kit A already has `/camconfig`; Kit B video sketch may still **404** — Android must stay Phase-1-safe.

---

## Problem

User selects **Kit B — CAM video + DevKit BLE** in RC Vehicle Pro settings.
There was **no SoftAP video quality / performance section** for that option because
settings gated on `connectionMode == WIFI_SOFTAP` only.

Runtime already applied `SoftApPerformancePreset` HUD options whenever
`profile.shouldStartCameraStream` (Kit A **and** Kit B). Users needed the same UI.

---

## Goal

Expose existing SoftAP video performance presets when Kit B SoftAP camera is selected.
Reuse `SoftApPerformancePreset`, DataStore, `SoftApPerformanceSettingsSection`,
`RcVehicleProViewModel`, and `SoftApCamConfigClient`. Visibility + copy only — no parallel stack.

---

## Visibility rule

```kotlin
fun showSoftApPerformanceSettings(
    applicationId: ApplicationId,
    board: Esp32Board,
    mode: BluetoothConnectionMode,
): Boolean {
    if (applicationId != ApplicationId.RC_VEHICLE_PRO) return false
    val profile = resolveCameraLinkProfile(applicationId, board, mode)
    return profile.shouldStartCameraStream
}
```

| Mode | Show presets? |
|------|----------------|
| Kit A — `WIFI_SOFTAP` | Yes |
| Kit B — board CAM + `BLE_BINARY` | Yes |
| DevKit / no SoftAP camera | No |

Kit B note: SoftAP **CTRL period** in the preset is Kit A–only (SoftAP TCP). For Kit B,
only HUD decode / max FPS + optional `/camconfig` matter.

---

## Acceptance

- [x] Kit B (CAM + BLE Binary) shows SoftAP video performance presets
- [x] Preset change persists and applies to Kit B SoftAP HUD decode / max FPS
- [x] `/camconfig` still best-effort; 404 does not break Kit B
- [x] No SoftAP TCP for Kit B
- [x] Kit A behaviour unchanged
- [x] EN + ES strings updated together
- [x] Tests cover visibility for Kit A + Kit B
