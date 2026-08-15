# Cursor prompt: RC Vehicle Pro — SoftAP video + BLE control (Android)

> **Superseded.** Single-board SoftAP+BLE on one ESP32-CAM is **not** supported.
>
> Use [`RC_VEHICLE_PRO_SUPPORTED_KITS_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_SUPPORTED_KITS_ANDROID_PROMPT.md):
> - **Kit A:** `WIFI_SOFTAP`
> - **Kit B:** `WIFI_CAMERA_DEVKIT_BLE` (CAM SoftAP video + DevKit BLE)
>
> Also superseded by [`RC_VEHICLE_PRO_CAM_BLE_DUAL_RADIO_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_CAM_BLE_DUAL_RADIO_ANDROID_PROMPT.md)
> (itself superseded by the supported-kits prompt).

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

## Status (Android — implemented)

- [x] `Esp32CameraLinkSession` — no duplicate `startStream` while already streaming
- [x] `Esp32CameraStreamRepository.startStream` — idempotent for same `baseUrl`
- [x] `RcVehicleProViewModel` — `bleStreamArmed` survives DataStore re-emits of same profile
- [x] `/capture`-first when CAM+BLE and `remoteController.isConnected`
- [x] HTTP **503** on `/stream` treated as expected (debug log, fall through to `/capture`)
- [x] `SoftApNetworkResolver` — bind HTTP to SoftAP Wi‑Fi `Network`
- [x] No SoftAP TCP `:3333` for `WIFI_CAMERA_BLE_CONTROL` (only SoftAP-only profile)
- [x] EN + ES idle / Connect stream copy

### Manual re-verify

1. SoftAP video alone → continuous frames
2. SoftAP **then** BLE → video continues via `/capture`; sticks work
3. Logcat: `Esp32CameraStream` / `SoftApNetwork`

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

---

## Do not

- Send JPEG over BLE NUS
- Auto-open SoftAP TCP control for `WIFI_CAMERA_BLE_CONTROL`
- Restart `startStream()` on every DataStore / Compose recomposition
- Require internet / cellular while on SoftAP
- Change binary packet layouts (`AA55` / `BB66` / `CC11` / `CC22`)

## Reference

- Firmware: `TeleCon_RcVehiclePro/ESP32_cam/BLE/TeleCon_RcVehiclePro_CAM_BLE_binary/`
- SoftAP-only (no BLE): `.../ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi/`
- Related: `RC_VEHICLE_PRO_CAMERA_STREAM_ANDROID_PROMPT.md`, `RC_VEHICLE_PRO_CAM_BLE_BINARY_ESP32_PROMPT.md`
