# ESP32 prompt — SoftAP `/camconfig` (all ESP32-CAM sketches)

**Implemented in firmware (2026-08+):** `/home/miguel/Documents/ESP32_Projects/TeleCon_RcVehiclePro/`

| Item | Path |
|------|------|
| Shared module | `common/CameraStreamCamConfig.{h,cpp}` |
| P0 Kit A Advanced | `ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi_Binary/` |
| P0 Kit A Normal | `ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi_Simple/` |
| P0 Kit B video | `ESP32_cam/lab/TeleCon_RcVehiclePro_CAM_SoftAP_Video/` |
| P1 BLE CAM (lab) | `ESP32_cam/lab/TeleCon_RcVehiclePro_CAM_BLE_binary/` |

Copy notes: `common/README.md` (`CameraStreamCamConfig`).  
Stream quality on the phone requires this firmware; old sketches without `/camconfig` stay on Android Phase 1 HUD limits (HTTP 404).

**Serial Monitor debug** (`#if TELECON_DEBUG` only — copy into a firmware Cursor chat):

`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/docs/prompts/ESP32_CAM_CAMCONFIG_SERIAL_DEBUG_ESP32_PROMPT.md`

**Historical prompt** (how to implement `/camconfig`; do not re-run unless adding a new CAM sketch):

`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/docs/prompts/ESP32_CAM_CAMCONFIG_RUNTIME_ESP32_PROMPT.md`

Android runtime stream quality (Phase 2 client + live-camera UI):

- `domain/camera/SoftApPerformancePreset.kt`
- `data/camera/SoftApCamConfigClient.kt`
- `ui/camera/SoftApRuntimeStreamQualityOverlay.kt`
