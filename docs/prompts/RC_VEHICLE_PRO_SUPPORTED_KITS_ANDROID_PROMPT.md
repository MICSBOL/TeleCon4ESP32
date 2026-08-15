# Cursor prompt: RC Vehicle Pro — supported hardware kits (Android)

Copy everything below into a **new Cursor chat** (Agent mode) on
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`.

---

## Product decision (locked)

**Do not** support SoftAP video + BLE control on **one** ESP32-CAM
(`WIFI_CAMERA_BLE_CONTROL` as single-board dual-radio). That mode is unreliable
(one 2.4 GHz radio) and is abandoned as a user-facing option.

Support **only** these cheap/reliable kits:

| Kit | Hardware | App mode | Control | Video |
|-----|----------|----------|---------|-------|
| **A — One board** | ESP32-CAM only | Wi‑Fi SoftAP | SoftAP TCP `:3333` (`proto=wifi`) | SoftAP HTTP `/stream` + `/capture` |
| **B — Two boards** | ESP32-CAM SoftAP **video only** + ESP32 **DevKit** BLE | SoftAP video + BLE Binary | BLE NUS on DevKit (`proto=binary`) | SoftAP HTTP on CAM (no BLE on CAM) |

Consumer bulb cams (Dimax, Tuya, V380, etc.) are **out of scope**.

---

## Goal

Refactor RC Vehicle Pro settings, `CameraLinkProfile`, HUD, and copy so:

1. **Kit A** stays the primary single-CAM path (`CameraLinkProfile.WIFI_SOFTAP`).
2. **Kit B** is SoftAP HTTP video from a **video-only CAM** + BLE control to a **DevKit** (two radios = two boards).
3. Single-board SoftAP+BLE (`TeleCon-BLE-RC-V-CAM` on the CAM) is **removed or hidden** from settings and docs.

Do **not** invent new wire formats. App prefix stays **`RC`**.

---

## Firmware companions (ESP32 repo)

Base path: `/home/miguel/Documents/ESP32_Projects/TeleCon_RcVehiclePro/`

| Kit | Flash |
|-----|--------|
| **A** | `ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi/` — SoftAP video + TCP `:3333` |
| **B — camera** | `ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_SoftAP_Video/` — SoftAP **video only** (same SSID/HTTP, **no** TCP `:3333`, **no** BLE) |
| **B — control** | `ESP32_noCam/BLE/TeleCon_RcVehiclePro_BLE_binary/` — BLE name `TeleCon-BLE-RC-V` |

| SoftAP (both kits) | Value |
|--------------------|--------|
| SSID / password | `TeleCon-RC-CAM` / `telecon1234` |
| IP | `192.168.4.1` |
| Video | `GET /stream` (MJPEG preferred), `GET /capture` |
| Status | `GET /status` (optional) |

| Kit A control | Value |
|---------------|--------|
| TCP | `192.168.4.1:3333` |
| Handshake | `RC:CONNECT,proto,wifi` → `RC:ACK,app,RC` |

| Kit B control | Value |
|---------------|--------|
| BLE name | `TeleCon-BLE-RC-V` (DevKit — **not** `…-CAM`) |
| Handshake | `RC:CONNECT,proto,binary` → `RC:ACK,app,RC` |
| Frames | `AA 55` / `BB 66` / `CC 11` / `CC 22` |

---

## Target Android behaviour

### Kit A — One board (SoftAP only)

**Settings**

- Board: **ESP32-CAM**
- Connection: **Wi‑Fi SoftAP**

**Runtime**

1. User joins SoftAP `TeleCon-RC-CAM`.
2. HUD starts SoftAP HTTP video (`/stream` → `/capture`).
3. When camera online → auto-open SoftAP TCP control (`ensureWifiSoftApConnected` / `autoConnectSoftApControlWhenCameraOnline`).
4. No Bluetooth discovery.

Profile: `CameraLinkProfile.WIFI_SOFTAP` (keep).

### Kit B — Two boards (SoftAP video + DevKit BLE)

**Settings (recommended UX)**

Pick one clear model and implement it end-to-end (prefer **Option 1**):

#### Option 1 — Setup / kit selector (preferred)

Add RC Vehicle Pro setting **Hardware kit** (or reuse board + connection with clearer copy):

| Kit | Stored meaning |
|-----|----------------|
| One ESP32-CAM (SoftAP) | Board=CAM, mode=WIFI_SOFTAP |
| CAM video + DevKit BLE | SoftAP camera **enabled** + control transport **BLE Binary** on DevKit |

When kit = dual:

- SoftAP HTTP video always armed when screen visible (same defaults URL).
- Connect / device picker targets **BLE DevKit** (`TeleCon-BLE-RC-V`).
- **Never** auto SoftAP TCP `:3333`.
- **Never** expect BLE on the CAM board.

#### Option 2 — Board = DevKit + “SoftAP camera” toggle

- Board **DevKit**, connection **BLE Binary** → control only.
- Extra toggle: **Use SoftAP camera** → start `Esp32CameraLinkSession` with `Esp32CameraDefaults`.
- Copy explains: flash CAM SoftAP video-only + DevKit BLE.

**Runtime (Kit B)**

1. Join SoftAP for video.
2. Open RC Vehicle Pro → SoftAP video runs (`/stream` preferred; full MJPEG OK because CAM is not doing BLE).
3. Tap Connect → BLE to **DevKit** → binary handshake.
4. Video must keep running after BLE connect (idempotent stream session — already partially fixed).

### Profile model

Repurpose or replace `WIFI_CAMERA_BLE_CONTROL`:

| Old meaning (remove) | New meaning |
|----------------------|-------------|
| Single ESP32-CAM SoftAP + BLE on same board | **Two boards**: SoftAP video (CAM) + BLE control (DevKit) |

Suggested rename (optional but clearer):

- `WIFI_CAMERA_BLE_CONTROL` → `WIFI_CAMERA_DEVKIT_BLE`  
  or keep enum name but rewrite KDoc / strings so users never flash BLE on the CAM.

`resolveCameraLinkProfile` must **not** imply single-board dual-radio firmware.

`CONTROL_ONLY` remains DevKit / no SoftAP camera.

---

## Files to change / verify

| Layer | File | Job |
|-------|------|-----|
| Modes | `domain/model/ApplicationProtocolSupport.kt` | CAM board modes: SoftAP + dual-kit BLE path; no Classic on CAM |
| Profile | `domain/camera/CameraLinkProfile.kt` | SoftAP-only vs SoftAP-video+DevKit-BLE; update KDoc |
| Session | `domain/camera/Esp32CameraLinkSession.kt` | Idempotent start; SoftAP video for both kits |
| Repository | `data/camera/Esp32CameraStreamRepository.kt` | SoftAP bind; prefer `/stream` for both kits (no `/capture`-only for single-CAM BLE) |
| Defaults | `Esp32CameraDefaults` | Keep SSID/URL; document kit B |
| Settings UI | `ApplicationProtocolSettings*` / RC settings | Kit A vs Kit B copy; hide single-board SoftAP+BLE-on-CAM; SoftAP video performance presets for both kits |
| VM | `RcVehicleProViewModel.kt` | SoftAP auto TCP only for Kit A; Kit B arms SoftAP video + BLE separately |
| Screen | `RcVehicleProScreen.kt` | Auto SoftAP TCP only when `WIFI_SOFTAP` |
| BLE | Existing NUS client | Kit B → DevKit name `TeleCon-BLE-RC-V` |
| Strings | `values/strings.xml` + `values-es/strings.xml` | EN+ES kit names, setup steps, idle hints; SoftAP perf copy for Kit A+B |
| Tests | `CameraLinkProfileTest`, protocol support tests, VM tests | Both kits; SoftAP perf visibility; no single-board dual-radio expectations |
| Docs | Deprecate `RC_VEHICLE_PRO_CAM_BLE_DUAL_RADIO_*` single-board guidance | Point to this prompt |

---

## Work items

### 1. Settings: two kits only (user-facing)

- [ ] Present **Kit A** and **Kit B** clearly (titles + short descriptions).
- [ ] Remove / rewrite copy that says BLE Binary on a **single** ESP32-CAM does SoftAP+BLE dual-radio.
- [ ] Kit B BLE target name hint: `TeleCon-BLE-RC-V` (DevKit).
- [ ] Kit A SoftAP hint: join `TeleCon-RC-CAM` / `telecon1234`, TCP `:3333`.
- [ ] EN + ES strings together.

### 2. Profile resolution

- [ ] Kit A → `WIFI_SOFTAP` → video + SoftAP TCP auto-connect.
- [ ] Kit B → SoftAP video + BLE Binary to DevKit → **no** SoftAP TCP.
- [ ] DevKit without SoftAP camera → `CONTROL_ONLY` (no SoftAP HTTP).
- [ ] Stale DataStore: coerce away from obsolete single-board SoftAP+BLE-on-CAM.

### 3. Video path

- [ ] Both kits: SoftAP HTTP via `SoftApNetworkResolver` (not cellular).
- [ ] Prefer `/stream` then `/capture` (Kit B CAM is video-only — MJPEG is fine).
- [ ] Remove product reliance on `/stream` 503 “BLE linked” single-board behaviour.
- [ ] Do not restart stream on BLE connect / DataStore saves (`Esp32CameraLinkSession` idempotent).

### 4. Control path

- [ ] Kit A: SoftAP TCP only; handshake `proto=wifi`.
- [ ] Kit B: BLE Binary only; handshake `proto=binary`; device `TeleCon-BLE-RC-V`.
- [ ] Kit B: never call `ensureWifiSoftApConnected`.

### 5. HUD / idle copy

- [ ] Kit A idle: join SoftAP; Connect opens Wi‑Fi control.
- [ ] Kit B idle: join SoftAP for video; Connect BLE DevKit for sticks.
- [ ] Optional “Connect stream” only if SoftAP video still needs explicit arming — prefer auto-start when SoftAP joined and kit expects camera.

### 6. Cleanup

- [ ] Mark single-board CAM BLE Binary firmware as **unsupported / lab only** in Android docs.
- [ ] Update `RC_VEHICLE_PRO_CAMERA_STREAM_ANDROID_PROMPT.md` firmware table to Kit A + Kit B only.
- [ ] Keep Greenhouse / other apps unchanged unless they share broken CAM+BLE single-board assumptions.

---

## Acceptance criteria

### Kit A (one ESP32-CAM)

- [ ] Settings: Board CAM + Wi‑Fi SoftAP
- [ ] Join SoftAP → live `/stream` (or `/capture`)
- [ ] TCP `:3333` + `RC:CONNECT,proto,wifi` → ACK; sticks work
- [ ] No BLE required

### Kit B (CAM SoftAP video + DevKit BLE)

- [ ] Settings clearly describe **two boards**
- [ ] Join SoftAP → live video
- [ ] Connect BLE → discovers `TeleCon-BLE-RC-V` → binary handshake
- [ ] Video keeps running after BLE connect
- [ ] No SoftAP TCP `:3333` attempts
- [ ] Sticks / buttons over BLE; SoftAP carries video only

### Negative

- [ ] No user path that implies SoftAP + BLE on **one** ESP32-CAM
- [ ] No Classic SPP offered for single CAM SoftAP kits
- [ ] No consumer IP-cam / Dimax integration

---

## Do not

- Revive single-board SoftAP+BLE as a supported product mode
- Send video over BLE
- Change `AA55` / `BB66` / `CC11` / `CC22` or SoftAP TCP SIMPLE line layouts
- Require internet / cellular while on SoftAP
- Add RTSP / Tuya / bulb-cam players in this task

---

## Manual test plan

1. **Kit A:** Flash `TeleCon_RcVehiclePro_CAM_WiFi` → SoftAP join → video + TCP drive.
2. **Kit B:** Flash CAM SoftAP **video-only** + DevKit `TeleCon_RcVehiclePro_BLE_binary` → SoftAP video → BLE connect DevKit → drive while video runs.
3. Switch kits in Settings → coerce modes → reconnect; no crash / no TCP on Kit B.
4. EN and ES settings strings reviewed.

---

## Reference

- SoftAP Android: `docs/prompts/RC_VEHICLE_PRO_WIFI_SOFTAP_ANDROID_PROMPT.md`
- Camera stream: `docs/prompts/RC_VEHICLE_PRO_CAMERA_STREAM_ANDROID_PROMPT.md`
- Obsolete single-board dual-radio: `docs/prompts/RC_VEHICLE_PRO_CAM_BLE_DUAL_RADIO_ANDROID_PROMPT.md` (superseded by this doc)
- Firmware: Kit A `…/CAM_WiFi/`; Kit B `…/CAM_SoftAP_Video/` + `…/BLE_binary/`; lab single-board under `ESP32_cam/lab/`
