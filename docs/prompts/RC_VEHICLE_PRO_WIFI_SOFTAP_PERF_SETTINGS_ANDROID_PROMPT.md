# Cursor prompt: RC Vehicle Pro — SoftAP performance presets in settings (Android)

Copy into a **new Cursor chat** (Agent mode) on
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`.

**Scope: Kit A SoftAP** (`CameraLinkProfile.WIFI_SOFTAP` / `BluetoothConnectionMode.WIFI_SOFTAP`).

Firmware companion (Phase 2):

`/home/miguel/Documents/ESP32_Projects/TeleCon_RcVehiclePro/docs/prompts/RC_VEHICLE_PRO_WIFI_SOFTAP_CAMCONFIG_ESP32_PROMPT.md`

(Stub in this repo: [`RC_VEHICLE_PRO_WIFI_SOFTAP_CAMCONFIG_ESP32_PROMPT.md`](./RC_VEHICLE_PRO_WIFI_SOFTAP_CAMCONFIG_ESP32_PROMPT.md))

Related (already done / do not redo):

- Decode / MJPEG / SoftAP TCP perf: [`RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_ANDROID_PROMPT.md)
- SoftAP mode: [`RC_VEHICLE_PRO_WIFI_SOFTAP_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_WIFI_SOFTAP_ANDROID_PROMPT.md)

---

## Problem

Mid-range phones (e.g. **Xiaomi Poco X3**) still feel choppy on SoftAP video + TCP control after Android decode optimizations. Browser on Ubuntu is smooth → SoftAP feed is OK; phone SoftAP RX + decode + CTRL load is the issue.

Android-only downsample does **not** shrink Wi‑Fi airtime (ESP32 still sends VGA). Users need **settings presets** to trade quality for smoothness. Real SoftAP bitrate cuts need firmware `/camconfig` (Phase 2).

---

## Goal (two phases — implement Phase 1 fully; Phase 2 if firmware is ready)

### Phase 1 — Android-only (ship first)

Expose SoftAP **performance presets** in RC Vehicle Pro settings. Wire them into:

- HUD decode (`setFastPreviewDecode` / `inSampleSize` levels)
- Max HUD publish FPS (skip decode/publish; still drain MJPEG so SoftAP does not stall)
- SoftAP CTRL heartbeat period (stay well under ESP32 `TELECON_CTRL_TIMEOUT_MS` ≈ 750 ms)

Persist in DataStore. Default = **Balanced**.

### Phase 2 — Push config to CAM (after ESP32 prompt)

When SoftAP is reachable and preset ≠ Balanced (or always on SoftAP connect), HTTP apply firmware camera params via `/camconfig` so resolution / JPEG quality / stream FPS drop on-air.

Graceful fallback if `/camconfig` missing (old firmware): Phase 1 behaviour only; log once.

---

## Product UX

Settings path: RC Vehicle Pro → Communication / protocol settings (where `RcVehicleCameraWifiSettingsSection` already lives).

Show a new section **when SoftAP HTTP video is armed** — Kit A (`WIFI_SOFTAP`) **and**
Kit B (board CAM + `BLE_BINARY` / `WIFI_CAMERA_DEVKIT_BLE`). Use
`showSoftApPerformanceSettings(...)` (profile `shouldStartCameraStream`), not
`mode == WIFI_SOFTAP` alone. See also
[`RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_KIT_B_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_KIT_B_ANDROID_PROMPT.md).

### Presets (simple; prefer these over many sliders)

| Preset | Use case | Android (Phase 1) | Firmware (Phase 2) |
|--------|----------|-------------------|--------------------|
| **Smooth** | Mid-range (Poco X3) | `inSampleSize=4`, max HUD ~10 FPS, CTRL **150 ms** | QVGA, `jpeg_quality` ~22, stream target ~10 FPS |
| **Balanced** (default) | Most phones | fast preview `inSampleSize=2`, no hard FPS cap (or ~15), CTRL **100 ms** | VGA, quality ~15 (firmware default) |
| **High quality** | Flagship | full decode (`inSampleSize=1` / ARGB), uncapped-ish, CTRL **100 ms** | VGA, quality ~12–15 |

Optional advanced (v1.1, only if presets alone feel insufficient): Max FPS slider, Preview scale 1/2/4, Control rate 100/150/200. Prefer presets only for v1.

Copy **en + es** for section title, preset names, short descriptions (“Lower video load for mid-range phones; may look softer”).

---

## Already implemented — extend

| Layer | File | Role |
|-------|------|------|
| Settings UI hook | `ui/applications/ApplicationProtocolSettingsScreen.kt` → `RcVehicleCameraWifiSettingsSection` | SoftAP info card — extend with presets |
| Settings VM | `ui/applications/ApplicationSettingsViewModel.kt` | board / connection mode |
| DataStore | `data/repository/SettingsRepository.kt` + `ISettingsRepository` | add SoftAP perf keys (app-scoped or RC-only) |
| Stream | `data/camera/Esp32CameraStreamRepository.kt` | `setFastPreviewDecode`; extend with sample size + max FPS |
| Domain | `domain/camera/CameraStreamRepository.kt` | extend API |
| RC VM | `ui/rc_vehicle_pro/RcVehicleProViewModel.kt` | enables fast preview when SoftAP streaming |
| SoftAP CTRL | `ui/bluetooth/BluetoothViewModel.kt` | `SOFTAP_CTRL_PERIOD_MS = 100` — make configurable |
| SoftAP TCP | `data/wifi/WifiSoftApDataTransferService.kt` | already coalesce + SoftAP network bind |

---

## Work items — Phase 1

### 1. Domain model

Add something like:

```kotlin
enum class SoftApPerformancePreset {
    SMOOTH,      // mid-range
    BALANCED,    // default
    HIGH_QUALITY,
}
```

Map each preset to:

- `hudInSampleSize: Int` (4 / 2 / 1)
- `useRgb565: Boolean` (true / true / false)
- `maxHudFps: Int?` (10 / 15-or-null / null)
- `softApCtrlPeriodMs: Long` (150 / 100 / 100)

### 2. Persist

- DataStore key for RC Vehicle Pro SoftAP preset (string enum name).
- Flow + save on `ISettingsRepository` / use cases mirroring board/protocol pattern.
- Default `BALANCED`.

### 3. Wire stream repository

Extend `Esp32CameraStreamRepository` (or apply options from ViewModel each session):

- Replace boolean-only fast preview with **decode options** from preset (`inSampleSize`, `RGB_565` vs default).
- Enforce **max HUD FPS**: when publishing frames, if last publish was too recent, skip decode of this JPEG but **still consume** the JPEG from the MJPEG stream (do not let SoftAP block).
- Photo path: keep full-quality `/capture` or full decode of last JPEG bytes if already stored — do not save soft HUD bitmap as the only photo source if avoidable.

Greenhouse: leave default full quality unless it opts in.

### 4. Wire SoftAP CTRL period

- `BluetoothViewModel` SoftAP heartbeat must read preset (or injected period), not only hardcoded `100L`.
- Never exceed fail-safe margin: period ≤ **200 ms** in UI; firmware timeout ~750 ms.

### 5. Settings UI

In `RcVehicleCameraWifiSettingsSection` (or sibling composable):

- Section title + short body.
- Three selectable options (same visual language as connection mode cards / radio rows).
- Changing preset saves immediately; if stream is active, apply decode/FPS/CTRL on next frames (no full app restart required).

### 6. Strings

`values/strings.xml` + `values-es/strings.xml` matching keys.

---

## Work items — Phase 2 (Android client for `/camconfig`)

Only after firmware implements the ESP32 prompt.

### Contract (match ESP32 prompt)

| Item | Value |
|------|--------|
| Apply | `GET` or `POST` `http://192.168.4.1/camconfig?framesize=qvga&quality=22&fps=10` |
| Read | `GET /camconfig` or extend `GET /status` with current framesize/quality/fps |
| Framesize tokens | `vga` / `hvga` / `qvga` (map to ESP `FRAMESIZE_*`) |
| Quality | 10–30 (higher = smaller JPEG on ESP32 sensor API) |
| FPS | target stream pacing on firmware (idle delay between frames) |

Preset → query map:

| Preset | Query |
|--------|--------|
| Smooth | `framesize=qvga&quality=22&fps=10` |
| Balanced | `framesize=vga&quality=15&fps=0` (0 = uncapped / firmware default) |
| High quality | `framesize=vga&quality=12&fps=0` |

### When to call

- On SoftAP camera session start (before or just after `/stream`), and when user changes preset while SoftAP connected.
- Use `SoftApNetworkResolver.openHttpConnection`.
- HTTP 404 / connection fail → log once, continue with Phase 1 only.
- Do **not** put JPEG on TCP `:3333`.

### Stream restart

If firmware only applies config when `/stream` is not held: stop stream → `/camconfig` → start stream. Prefer firmware that applies between frames or on next `/stream` without killing TCP RC.

---

## Do not

- Change Kit B BLE / Classic paths except shared repository APIs with safe defaults
- Process-wide `bindProcessToNetwork`
- WebView preview
- Reintroduce success-path `delay(150)` as a global FPS cap without preset
- SoftAP CTRL period ≥ 400 ms (fail-safe risk)
- Require firmware for Phase 1 acceptance

---

## Manual test plan

### Phase 1

1. RC settings → SoftAP mode → see three presets; default Balanced.
2. Select **Smooth** on Poco X3 → HUD softer but smoother; sticks remain live (CTRL ~150 ms).
3. Select **High quality** on S23 → sharper HUD; no regression vs current.
4. Change preset while streaming → behaviour updates without crash.
5. Photo still works.
6. Leave screen → stream stops; SoftAP disconnect → ESP32 fail-safe.
7. en + es strings present.
8. Greenhouse SoftAP (if shared repo touched) still works at full decode by default.

### Phase 2

1. Flash CAM with `/camconfig`.
2. Smooth → Serial / `/status` shows QVGA (or equivalent); SoftAP airtime lower; Poco smoother than Phase 1 alone.
3. Old firmware without `/camconfig` → app does not crash; Phase 1 only.

---

## Acceptance

### Phase 1

- [ ] SoftAP performance presets in RC settings (en + es), persisted
- [ ] Smooth / Balanced / High quality map to decode + max FPS + CTRL period
- [ ] MJPEG consumer still drains frames when skipping publish
- [ ] SoftAP CTRL stays under fail-safe timeout
- [ ] Photo + Greenhouse defaults preserved

### Phase 2

- [ ] SoftAP connect / preset change applies `/camconfig` when available
- [ ] Missing `/camconfig` degrades gracefully
- [ ] TCP control remains independent of HTTP video

---

## Suggested order

1. Domain + DataStore + settings UI presets  
2. Repository decode levels + max HUD FPS  
3. Configurable SoftAP CTRL period  
4. (Later) `/camconfig` client after ESP32 prompt is flashed  

Measure on **Poco X3** with Smooth vs Balanced before calling Phase 1 done.
