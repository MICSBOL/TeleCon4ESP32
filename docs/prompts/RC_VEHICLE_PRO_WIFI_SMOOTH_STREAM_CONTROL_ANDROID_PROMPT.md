# Cursor prompt: RC Vehicle Pro — Smooth SoftAP stream + control (Android)

Copy into a **new Cursor chat** (Agent mode) on
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`.

**Scope: Kit A SoftAP** (`CameraLinkProfile.WIFI_SOFTAP` / `BluetoothConnectionMode.WIFI_SOFTAP`).

Firmware companion:

`/home/miguel/Documents/ESP32_Projects/TeleCon_RcVehiclePro/docs/prompts/RC_VEHICLE_PRO_WIFI_SMOOTH_STREAM_CONTROL_ESP32_PROMPT.md`

Related (already done / extend — do not redo):

- SoftAP connect UX: [`RC_VEHICLE_PRO_WIFI_SOFTAP_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_WIFI_SOFTAP_ANDROID_PROMPT.md)
- Decode / MJPEG / SoftAP TCP perf: [`RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_ANDROID_PROMPT.md)
- Performance presets + `/camconfig` Phase 2: [`RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_SETTINGS_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_SETTINGS_ANDROID_PROMPT.md)
- Firmware `/camconfig`: TeleCon_RcVehiclePro `RC_VEHICLE_PRO_WIFI_SOFTAP_CAMCONFIG_ESP32_PROMPT.md`

---

## Research verdict (Xiaomi / Poco SoftAP)

| Practice | Why | Source pattern |
|----------|-----|----------------|
| Bind sockets to SoftAP `Network` (not cellular) | Android 10+ routes “no internet” SoftAP via mobile data | [arduino-esp32#4423](https://github.com/espressif/arduino-esp32/issues/4423), `ConnectivityManager.bindProcessToNetwork` / per-socket bind |
| `WifiLock` high-performance / low-latency | Keeps STA radio awake; power save kills SoftAP RX latency | [AOSP Wi-Fi low-latency mode](https://source.android.com/docs/core/connect/wifi-low-latency) |
| Custom MJPEG parser + Bitmap decode (not ExoPlayer) | ExoPlayer does not support MJPEG multipart | ExoPlayer issues / Android media docs |
| Downsample HUD + cap publish FPS while still draining TCP | SoftAP stalls if you stop reading JPEG parts | Existing SoftAP perf work in this app |
| Push Smooth bitrate to CAM via `/camconfig` | Android-only decode does not shrink airtime | Firmware `/camconfig` + presets prompt |
| Moderate CTRL rate on mid-range (≈150 ms) | SoftAP airtime shared with VGA MJPEG; stay under ~750 ms fail-safe | RC SoftAP designs; this app’s SoftAP CTRL period |
| Prefer Smooth preset on mid-range devices | Poco-class SoCs + MIUI Wi-Fi PS struggle with uncapped VGA | Field reports + ESP32 SoftAP latency guides |

**Do not** use WebView-as-player as the primary path, process-wide SoftAP bind unless unavoidable, or flood CTRL faster than needed.

---

## Problem

On SoftAP, mid-range phones (e.g. **Xiaomi Poco X3**) often show: choppy video, delayed sticks, or stream OK in desktop browser but bad in-app. Causes stack:

1. Phone Wi-Fi power save / MIUI aggressiveness
2. Traffic leaving SoftAP via cellular (no internet on ESP AP)
3. Uncapped VGA MJPEG saturating SoftAP
4. Decode + UI + CTRL competing on one mid-range CPU

---

## Goal

Make SoftAP **feel smooth and controllable** on Poco-class devices by combining:

1. **Radio / routing locks** (WifiLock + SoftAP-bound sockets — already partially present)
2. **Presets** (Smooth / Balanced / High) — Phase 1 Android + Phase 2 `/camconfig`
3. **Drain-never-stall** MJPEG path with HUD FPS / sample-size limits

Ship increments; prefer extending existing SoftAP code over new parallel stacks.

---

## Already implemented — extend

| Layer | File | Role |
|-------|------|------|
| SoftAP network bind | `data/camera/SoftApNetworkResolver.kt` | Per-connection SoftAP bind (prefer keep; avoid process-wide) |
| SoftAP TCP | `data/wifi/WifiSoftApDataTransferService.kt` | Control socket + coalesce |
| Stream | `data/camera/Esp32CameraStreamRepository.kt` | MJPEG + `setFastPreviewDecode` |
| SoftAP CTRL | `ui/bluetooth/BluetoothViewModel.kt` | `SOFTAP_CTRL_PERIOD_MS` |
| Settings presets | see PERF_SETTINGS prompt | Smooth → decode / FPS / CTRL / `/camconfig` |

---

## Work items

### 1. WifiLock for SoftAP sessions (new / missing)

While SoftAP camera stream **or** SoftAP TCP control is active:

- Acquire `WifiManager.WifiLock` with the strongest available mode:
  - Prefer `WIFI_MODE_FULL_LOW_LATENCY` when API / device supports it
  - Else `WIFI_MODE_FULL_HIGH_PERF`
- Release when both SoftAP stream and SoftAP TCP are stopped
- Hold from a clear owner (session / ViewModel / foreground service already used for RC) — no leak across kit switches
- Log once: `[SoftAP] WifiLock acquired mode=...`

Strings: none user-facing required (debug log OK).

### 2. SoftAP routing (verify, fix gaps)

- Keep **per-socket / openConnection** bind via `SoftApNetworkResolver` for `/stream`, `/capture`, `/camconfig`, SoftAP TCP.
- Do **not** call process-wide `bindProcessToNetwork` unless a concrete bug shows SoftAP traffic still leaving on cellular after per-socket bind.
- On SoftAP connect failure / `UnknownHostException` / timeouts: surface existing SoftAP help (join SSID, disable VPN, etc.) — extend copy only if Xiaomi-specific tip is short (en + es).

Optional short tip (en + es): SoftAP has no internet; keep the app in foreground; disable MIUI battery restriction for TeleCon if the link drops when screen dims.

### 3. Smooth preset end-to-end

If PERF_SETTINGS Phase 1 is not done, implement it. If done, complete Phase 2:

| Preset | Android | Firmware `/camconfig` |
|--------|---------|------------------------|
| **Smooth** (Poco) | `inSampleSize=4`, max HUD ~10 FPS, CTRL **150 ms** | `framesize=qvga&quality=22&fps=10` |
| **Balanced** (default) | sample 2, ~15 or uncapped HUD, CTRL **100 ms** | `vga&quality=15&fps=0` (or skip apply) |
| **High quality** | full decode, uncapped-ish, CTRL **100 ms** | `vga&quality=12&fps=0` |

On SoftAP session start (or when preset changes while connected):

1. Apply Android decode / FPS / CTRL immediately.
2. If SoftAP HTTP reachable: `GET http://192.168.4.1/camconfig?...` (or configured SoftAP host).
3. If 404 / connection fail: log once, continue Phase 1-only.
4. After Smooth apply, restart `/stream` if firmware notes clients may drop on framesize change.

### 4. MJPEG path discipline (must)

- Never stop reading multipart parts when skipping HUD publish (FPS cap).
- Prefer RGB_565 + subsample on Smooth; avoid ARGB full-size on mid-range.
- Photo: use `/capture` or last full JPEG bytes — not the soft HUD bitmap alone.

### 5. CTRL path discipline (must)

- SoftAP CTRL period from preset; always ≪ `TELECON_CTRL_TIMEOUT_MS` (~750 ms).
- Keep coalesce / latest-wins on SoftAP TCP (do not queue stick spam).
- Video and control remain separate sockets.

---

## Files to touch (expected)

| Area | Likely files |
|------|----------------|
| WifiLock | SoftAP session owner near `Esp32CameraLinkSession` / RC VM / SoftAP services |
| Stream options | `Esp32CameraStreamRepository.kt`, `CameraStreamRepository.kt` |
| Presets | Settings repo + `RcVehicleCameraWifiSettingsSection` + strings en/es |
| `/camconfig` client | small HTTP helper or existing SoftAP HTTP client |
| CTRL period | `BluetoothViewModel.kt` SoftAP CTRL loop |

Follow TeleCon4ESP32 conventions: Compose + ViewModel, resources en+es, minimal scope.

---

## Manual test plan (Poco SoftAP)

1. Join Kit A SoftAP; open RC Vehicle Pro SoftAP mode.
2. Confirm stream + sticks; log shows WifiLock acquired.
3. Set **Smooth** → HUD softer/slower; sticks still under fail-safe; if firmware ready, `/camconfig` returns ok and SoftAP feels lighter.
4. Toggle cellular on (SoftAP selected): stream/CTRL must still hit `192.168.4.1` (not hang on mobile route).
5. Background briefly / screen off briefly — note MIUI kills; tip string if needed.
6. **Balanced** / **High quality** restore sharper feed on a stronger phone.
7. Greenhouse / BLE kits unchanged.

---

## Do not

- Replace SoftAP TCP with BLE for Kit A
- Use ExoPlayer for MJPEG
- Process-wide SoftAP bind by default
- Hardcode Poco model checks (presets are enough)
- Change ESP32 protocol strings

---

## Acceptance

- [ ] SoftAP session holds WifiLock (low-latency or high-perf) and releases cleanly
- [ ] SoftAP HTTP + TCP stay on SoftAP `Network` with cellular enabled
- [ ] Smooth preset applies Android limits and `/camconfig` when firmware supports it
- [ ] MJPEG drain continues under HUD FPS cap; CTRL period stays safe
- [ ] en + es strings for presets / optional SoftAP tip
- [ ] Non-SoftAP kits unaffected
