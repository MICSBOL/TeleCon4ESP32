# Cursor prompt: RC Vehicle Pro — smooth SoftAP camera (Android)

Copy into a **new Cursor chat** (Agent mode) on
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`.

Firmware companions (ESP32 repo):

| Kit | Sketch |
|-----|--------|
| **A** SoftAP video + TCP (`WIFI_SOFTAP`) | `ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi/` |
| **B** SoftAP video-only CAM + DevKit BLE (`WIFI_CAMERA_DEVKIT_BLE`) | `ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_SoftAP_Video/` + `ESP32_noCam/BLE/TeleCon_RcVehiclePro_BLE_binary/` (`TeleCon-BLE-RC-V`) |

Single-board SoftAP+BLE on one ESP32-CAM is **unsupported** (lab only). See
[`RC_VEHICLE_PRO_SUPPORTED_KITS_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_SUPPORTED_KITS_ANDROID_PROMPT.md).

Both SoftAP sketches expose `GET /stream` (preferred) and `GET /capture`. Kit B has **no** TCP `:3333` on the CAM.

---

## Goal

Keep the RC Vehicle Pro landscape HUD video **as smooth as SoftAP allows**:

1. Prefer continuous **MJPEG** `GET /stream`
2. Fall back to uncapped **`GET /capture`** polling (no 150 ms artificial delay)
3. Compose draws frames with minimal extra work (`remember` ImageBitmap)

Do **not** use WebView. Do **not** send video over Bluetooth.

---

## Firmware contract (flash WiFi CAM sketch)

| Item | Value |
|------|--------|
| Soft-AP SSID | `TeleCon-RC-CAM` |
| Soft-AP password | `telecon1234` |
| Base URL | `http://192.168.4.1` |
| Preferred video | `GET /stream` → `multipart/x-mixed-replace; boundary=frame` |
| Still / photo source | Latest decoded frame **or** `GET /capture` → `image/jpeg` |
| Status | `GET /status` |
| Control | TCP `:3333` (independent of HTTP video) |

Camera: VGA JPEG, `jpeg_quality ≈ 15`, `fb_count = 2`, `CAMERA_GRAB_LATEST` when available.
While `/stream` is serving a client, firmware must keep calling `rcWifiControlPoll()` (idle hook).

---

## Already implemented — verify / extend, do not rewrite

| Layer | File | Role |
|-------|------|------|
| Stream client | `data/camera/Esp32CameraStreamRepository.kt` | MJPEG `/stream` first; `/capture` fallback with `yield()` only |
| Defaults | `domain/camera/CameraStreamState.kt` → `Esp32CameraDefaults` | `STREAM_PATH`, `streamUrl()`, `CAPTURE_PATH` |
| Domain | `CameraStreamRepository`, `CameraStreamState` | Idle / Connecting / Frame / Error |
| DI | `di/AppModule.kt` | binds repository |
| VM | `ui/rc_vehicle_pro/RcVehicleProViewModel.kt` | `onScreenVisible` / `onScreenHidden` |
| Composable | `ui/rc_vehicle_pro/components/RcCameraPreview.kt` | full-bleed `Image` + `remember(bitmap) { asImageBitmap() }` |
| Cleartext | `res/xml/network_security_config.xml` | HTTP to SoftAP allowed |

### Repository behaviour (required)

```
startStream(baseUrl)
  loop:
    try consumeMjpegStream(/stream)   // SOI/EOI JPEG scan on InputStream
    on failure / non-200 → pollCaptureFrames(/capture) with no FRAME_POLL delay
    on error → short ERROR_RETRY (~250 ms), retry /stream
stopStream → cancel job, recycle bitmaps, Idle
```

- Log tag: `Esp32CameraStream`
- Do **not** reintroduce `FRAME_POLL_INTERVAL_MS = 150` (or any FPS cap on success)
- Keep one-frame bitmap recycle lag so Compose can finish drawing
- Greenhouse / other apps sharing this repository: `/stream` missing → automatic `/capture` fallback

### Compose behaviour (required)

- `RcCameraPreview`: when `Frame`, use `remember(cameraState.bitmap) { bitmap.asImageBitmap() }`
- `ContentScale.Crop`, full-bleed behind HUD glass
- Connecting / Idle / Error unchanged (SSID + password hint, open Wi‑Fi settings)

---

## Work items (if regressing or extending)

1. [ ] Confirm `Esp32CameraDefaults.streamUrl()` exists and repository prefers it.
2. [ ] Confirm success path has **no** `delay(150)` (only `yield()` / error retry).
3. [ ] Confirm photo button still snapshots latest `CameraStreamState.Frame` bitmap.
4. [ ] Emulator: keep no-spam Idle / skip stream (existing ViewModel guard).
5. [ ] Strings en + es if you change user-visible camera copy.
6. [ ] Optional later: OkHttp keep-alive (only if profiling shows connect overhead on `/capture` fallback).

---

## Manual test plan

1. Flash `TeleCon_RcVehiclePro_CAM_WiFi`; Serial shows SoftAP + camera OK.
2. Phone joins **`TeleCon-RC-CAM` / `telecon1234`**.
3. Browser: `http://192.168.4.1/stream` → continuous MJPEG; `/capture` → one JPEG.
4. Open **RC Vehicle Pro** → HUD feed updates smoothly (visibly faster than old ~6–7 FPS poll).
5. Move sticks while video runs → Serial `[PANEL RX]` / motors (control must not freeze).
6. Photo → gallery; leave screen → stream stops (Logcat quiet).
7. BT disconnected: video still works.

Logcat filter: `Esp32CameraStream` — expect MJPEG path; “falling back to /capture” only if `/stream` missing.

---

## Do not

- Cap successful frames with a fixed poll interval “for battery” without measuring
- Require Bluetooth / handshake before starting Wi‑Fi video
- Send frames over Classic SPP / BLE
- Rewrite preview as WebView
- Block RC TCP by serving `/stream` without a firmware idle hook (ESP32 side)

---

## Acceptance

- [ ] On SoftAP, HUD uses `/stream` when available and feels continuous
- [ ] Old CAM firmware without `/stream` still works via `/capture` (uncapped)
- [ ] Stick control remains live during video
- [ ] Photo uses last frame; leave screen stops the job
- [ ] No cleartext / ATS regressions

---

## Parity

Greenhouse camera UI uses the same `Esp32CameraStreamRepository` — keep `/capture` fallback so Greenhouse SoftAP builds without `/stream` keep working.
