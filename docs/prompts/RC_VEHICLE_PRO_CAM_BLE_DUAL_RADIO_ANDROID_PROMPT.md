# Cursor prompt: RC Vehicle Pro — SoftAP video + BLE control (Android)

> **Superseded.** Single-board SoftAP+BLE on one ESP32-CAM is **not** a supported product mode.
>
> Use instead:  
> [`RC_VEHICLE_PRO_SUPPORTED_KITS_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_SUPPORTED_KITS_ANDROID_PROMPT.md)
>
> - **Kit A:** one ESP32-CAM SoftAP (video + TCP)  
> - **Kit B:** ESP32-CAM SoftAP video + ESP32 DevKit BLE (two boards)

The content below is retained for history only (single-board dual-radio experiments).

---

Copy everything below into a **new Cursor chat** (Agent mode) on
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`.

Firmware companion (updated for dual-radio coexistence):

`/home/miguel/Documents/ESP32_Projects/TeleCon_RcVehiclePro/ESP32_cam/BLE/TeleCon_RcVehiclePro_CAM_BLE_binary/`

---

## Problem (user report)

On one ESP32-CAM board:

- SoftAP HTTP camera works alone
- BLE control works alone
- **Together they fail**: video shows ~1 frame then dies, and/or BLE will not connect while SoftAP is up

ESP32 has **one** 2.4 GHz radio. SoftAP MJPEG and BLE GATT time-share. Long-lived `/stream` + BLE notifies is fragile. Firmware now prefers **`/capture` while BLE is linked** (`TELECON_CAPTURE_WHEN_BLE 1` → `/stream` returns HTTP 503).

Android must treat **CAM + BLE** as a first-class path: SoftAP for video only, BLE for control, no stream restart storms, SoftAP network binding, and `/capture`-first when BLE is connected.

Do **not** invent TCP `:3333` for this profile. Do **not** send video over BLE.

---

## Goal

Make `CameraLinkProfile.WIFI_CAMERA_BLE_CONTROL` reliable:

| Link | Role |
|------|------|
| SoftAP Wi‑Fi `TeleCon-RC-CAM` / `telecon1234` | HTTP JPEG video only |
| BLE NUS `TeleCon-BLE-RC-V-CAM` | Binary RC control + telemetry |

### Required phone order

1. Join SoftAP `TeleCon-RC-CAM` / `telecon1234`
2. Open RC Vehicle Pro → arm SoftAP video (“Connect stream” if shown)
3. Connect BLE → `TeleCon-BLE-RC-V-CAM` → `RC:CONNECT,proto,binary`

Video must **keep running** (via `/capture` polling) after BLE connects.

---

## Firmware contract (match exactly)

| Item | Value |
|------|--------|
| SoftAP SSID / pass | `TeleCon-RC-CAM` / `telecon1234` |
| Base URL | `http://192.168.4.1` |
| Preferred with BLE linked | `GET /capture` → `image/jpeg` |
| Preferred when BLE idle | `GET /stream` → MJPEG multipart |
| Status | `GET /status` → JSON includes `"mode":"ble_binary"`, `"ble":0/1`, `"capture_when_ble":1` |
| `/stream` while BLE connected | May return **503** `"use /capture while BLE connected"` — treat as expected |
| Control | BLE NUS only — **no** SoftAP TCP `:3333` |
| Handshake | `RC:CONNECT,proto,binary` → `RC:ACK,app,RC` |
| Phone → ESP32 | `AA 55` control, `BB 66` buttons |
| ESP32 → phone | `CC 11` / `CC 22` (optional `CC 33`; telemetry may be sparse while streaming) |

Camera defaults already in `Esp32CameraDefaults` (`SOFTAP_SSID`, `DEFAULT_BASE_URL`, paths).

---

## Files to change / verify

| Layer | File | Job |
|-------|------|-----|
| Profile | `domain/camera/CameraLinkProfile.kt` | `WIFI_CAMERA_BLE_CONTROL` for Board=CAM + BLE Binary |
| Session | `domain/camera/Esp32CameraLinkSession.kt` | **Never** re-`startStream()` while already streaming |
| Repository | `data/camera/Esp32CameraStreamRepository.kt` | SoftAP bind; `/capture`-first when BLE up; handle `/stream` 503 |
| SoftAP net | `data/camera/SoftApNetworkResolver.kt` | HTTP via SoftAP Wi‑Fi `Network`, not cellular |
| VM | `ui/rc_vehicle_pro/RcVehicleProViewModel.kt` | Keep `bleStreamArmed` across BLE connect / DataStore saves |
| Screen | `ui/rc_vehicle_pro/RcVehicleProScreen.kt` | No SoftAP TCP auto-connect for BLE profile |
| BLE | `data/bluetooth/BleDataTransferService.kt` | NUS + CCCD + MTU; handshake after notify ON |
| UI copy | `strings.xml` / `values-es` | Idle / connect-stream hints for CAM+BLE |

---

## Work items (do these)

### 1. Stop killing video on BLE connect (critical)

Already partially fixed — verify end-to-end:

- [ ] `Esp32CameraLinkSession`: `startStream` only when transitioning to streaming; ignore duplicate `setProfile` / `setCameraEnabled` / `onVisible`
- [ ] `Esp32CameraStreamRepository.startStream`: idempotent if same `baseUrl` already active
- [ ] `RcVehicleProViewModel`: do **not** set `bleStreamArmed = false` on DataStore re-emits of the same `WIFI_CAMERA_BLE_CONTROL` profile
- [ ] Saving last BLE device must **not** call `stopStream()`

Regression: open stream → connect BLE → video must stay on `Frame` (or briefly `Connecting` then `Frame` via `/capture`), never stuck Idle after one frame.

### 2. Prefer `/capture` when BLE control is linked

In `Esp32CameraStreamRepository` (or a small policy helper):

```
if (camera profile == WIFI_CAMERA_BLE_CONTROL && remoteController.isConnected):
  skip /stream (or try once); poll /capture continuously
else:
  try /stream; on failure or HTTP 503 → /capture
```

Handle firmware 503 on `/stream` as **expected** when BLE is up — log at debug, do not show a fatal “camera unavailable” toast every poll.

Optional: observe `remoteController.isConnected` and switch mode without tearing SoftAP join.

### 3. SoftAP network binding

Keep / harden `SoftApNetworkResolver`:

- Prefer Wi‑Fi network whose link address is `192.168.4.x`
- Fallback: any `TRANSPORT_WIFI`
- Open connections with `Network.openConnection(url)` (API 23+)
- Do **not** use process-wide `bindProcessToNetwork` unless necessary (breaks other traffic)

### 4. UX / arming for CAM+BLE

- [ ] Show clear idle copy: join SoftAP, then Connect stream, then Connect BLE
- [ ] “Connect stream” arms SoftAP HTTP only (does not start BLE)
- [ ] Bluetooth chip / connect sheet discovers `TeleCon-BLE-RC-V-CAM`
- [ ] Do **not** call `ensureWifiSoftApConnected` (TCP `:3333`) for `WIFI_CAMERA_BLE_CONTROL`

### 5. BLE control loop under SoftAP

- [ ] Advanced RC TX loop (~50 ms `AA 55`) is OK
- [ ] Expect sparse / delayed `CC 11` / `CC 22` while SoftAP video is active — HUD may fall back to local stick speed (already does when no telemetry)
- [ ] Fail-safe: BLE disconnect zeros motors on ESP32; Android should stop sending or show disconnected

### 6. Logging (debug)

Log tags:

- `Esp32CameraStream` — stream/capture mode switches, HTTP status (including 503)
- `SoftApNetwork` — which `Network` was chosen
- BLE existing tags — connect / CCCD / handshake

---

## Acceptance criteria

- [ ] SoftAP video alone: continuous frames via `/stream` or `/capture`
- [ ] BLE alone (no SoftAP join): discovers `TeleCon-BLE-RC-V-CAM`, handshake ACK, sticks work
- [ ] SoftAP **then** BLE: video continues (typically `/capture`); BLE session stays up; sticks move vehicle echo / HUD
- [ ] BLE connect does **not** blank the camera after one frame
- [ ] No SoftAP TCP `:3333` attempts in CAM+BLE profile
- [ ] Emulator still skips SoftAP HTTP
- [ ] EN + ES strings updated together if UI text changes

---

## Do not

- Send JPEG over BLE NUS
- Auto-open SoftAP TCP control for `WIFI_CAMERA_BLE_CONTROL`
- Restart `startStream()` on every DataStore / Compose recomposition
- Require internet / cellular while on SoftAP
- Change binary packet layouts (`AA55` / `BB66` / `CC11` / `CC22`)

---

## Manual test plan

1. Flash latest `TeleCon_RcVehiclePro_CAM_BLE_binary` (Serial 115200).
2. Phone Wi‑Fi → `TeleCon-RC-CAM` / `telecon1234`.
3. App Settings → Board **ESP32-CAM**, Connection **BLE + Binary**.
4. Open RC Vehicle Pro → Connect stream → confirm live video.
5. Connect BLE → confirm handshake on Serial (`[HS]`) and sticks.
6. Confirm video still updates after BLE connect (may be lower FPS via `/capture`).
7. Disconnect BLE → fail-safe on Serial; SoftAP video still works.
8. Logcat: filter `Esp32CameraStream` / `SoftApNetwork`.

---

## Reference

- Firmware path: `TeleCon_RcVehiclePro/ESP32_cam/BLE/TeleCon_RcVehiclePro_CAM_BLE_binary/`
- SoftAP-only (no BLE): `.../ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi/` — do not mix TCP control into BLE profile
- Existing prompts: `RC_VEHICLE_PRO_CAMERA_STREAM_ANDROID_PROMPT.md`, `RC_VEHICLE_PRO_CAM_BLE_BINARY_ESP32_PROMPT.md`
