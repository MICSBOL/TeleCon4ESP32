# Cursor prompt: ESP32-CAM `/camconfig` runtime stream tuning (all TeleCon CAM sketches)

Copy everything below into a **new Cursor chat** (Agent mode) on the ESP32 firmware tree
(for example `/home/miguel/Documents/ESP32_Projects/`).

The Android app (`TeleCon4ESP32`) already sends runtime SoftAP stream-quality changes over HTTP
while the user is on the live camera screen (RC Vehicle Pro + Control Panel camera pane).
Settings only stores protocol/board; **no separate firmware SKU per phone tier**.

---

## Goal

Add a shared HTTP handler **`GET /camconfig`** to every ESP32-CAM sketch that serves
**SoftAP MJPEG** (`/stream`). The handler adjusts sensor framesize, JPEG quality, and optional
stream FPS cap **at runtime** — no reflash when the user picks Smooth / Balanced / High on the phone.

Sketches that only expose **`/capture`** (single JPEG) should still implement `/camconfig` if they
use the same `CameraStream` module; `/capture` must honour the updated params on the next shot.

---

## Android contract (source of truth)

Read these files in the Android repo
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`:

| File | Why |
|------|-----|
| `domain/camera/Esp32CameraDefaults.kt` (`CameraStreamState.kt`) | Base URL, paths: `/stream`, `/capture`, `/camconfig` |
| `domain/camera/SoftApPerformancePreset.kt` | Preset → query string mapping |
| `data/camera/SoftApCamConfigClient.kt` | HTTP client behaviour (GET, timeouts, 404 = Phase 1 only) |
| `data/camera/SoftApCamConfigApplier.kt` | When Android restarts `/stream` (framesize change) |
| `domain/camera/SoftApStreamQualityDefaults.kt` | Which apps use runtime tuning |
| `ui/camera/SoftApRuntimeStreamQualityOverlay.kt` | User-facing presets |

Default SoftAP base: **`http://192.168.4.1`**

---

## HTTP contract

### Request

```
GET /camconfig?framesize=<token>&quality=<0-63>&fps=<n>
```

| Query | Values | Meaning |
|-------|--------|---------|
| `framesize` | `qvga`, `vga`, `svga`, `xga`, `hd`, `uxga`, … | Maps to ESP32 `sensor_t` / `framesize_t` (see table below) |
| `quality` | `0` (best) … `63` (worst) | JPEG quality passed to `esp_camera` |
| `fps` | `0` = uncapped, `5`…`30` | Max MJPEG frames emitted on `/stream` (sketch-local throttle) |

Unknown query keys: ignore. Invalid values: return **400** with short JSON error.

### Presets sent by Android today

| Preset | Query |
|--------|-------|
| Smooth | `framesize=qvga&quality=22&fps=10` |
| Balanced | `framesize=vga&quality=15&fps=0` |
| High quality | `framesize=vga&quality=12&fps=0` |

Source: `SoftApPerformancePreset.camConfigQuery` in Android.

### Response

**200 OK** — params applied (even if some were no-ops):

```json
{
  "ok": true,
  "framesize": "qvga",
  "quality": 22,
  "fps": 10
}
```

**400** — invalid param.

**404** — endpoint missing (old firmware; Android keeps Phase 1 decode limits only).

**405** — only GET supported.

Content-Type: `application/json` (body optional on 404).

### Client behaviour (do not break)

- Android uses **GET**, 3 s connect/read timeout, no redirect follow.
- On **404** or unreachable: silent degrade; Android-side HUD downsample / FPS cap still work.
- On **200** when `framesize` changes (Smooth ↔ VGA): Android **reconnects `/stream`**.
  Ensure the MJPEG handler can stop/restart cleanly without wedging the sensor.
- Idempotent: repeating the same query must be cheap (Android dedupes per session).

---

## Framesize token map

Implement once in shared code (`CameraStreamCamConfig.cpp` or extend existing `CameraStream.*`):

| Token | `framesize_t` | Typical use |
|-------|---------------|-------------|
| `qqvga` | FRAMESIZE_QQVGA | Lab / debug only |
| `qvga` | FRAMESIZE_QVGA | Smooth preset |
| `vga` | FRAMESIZE_VGA | Balanced / High |
| `svga` | FRAMESIZE_SVGA | Optional future |
| `xga` | FRAMESIZE_XGA | Optional future |
| `hd` | FRAMESIZE_HD | Optional future |
| `uxga` | FRAMESIZE_UXGA | Avoid on SoftAP — too heavy |

Default at boot (before any `/camconfig`): **VGA**, quality **15**, fps **0** — matches Balanced.

---

## Shared module (implement once, link everywhere)

Create or extend under each firmware repo that owns CAM code:

```
CameraStreamCamConfig.h
CameraStreamCamConfig.cpp   // parse query, apply sensor, store fps cap
```

Responsibilities:

1. Register **`/camconfig`** on the same `WebServer` / async HTTP server as `/stream` and `/capture`.
2. Thread-safe apply: pause MJPEG stream briefly, `sensor->set_framesize`, `sensor->set_quality`,
   update global fps limit used by the stream loop.
3. Expose `cameraStreamGetFpsCap()`, `cameraStreamGetQuality()`, `cameraStreamGetFramesizeToken()`
   for `/status` (optional but useful for Serial debug).
4. Serial logs for `/camconfig` **only** inside `#if TELECON_DEBUG` (see companion prompt
   `ESP32_CAM_CAMCONFIG_SERIAL_DEBUG_ESP32_PROMPT.md`). Production (`TELECON_DEBUG 0`)
   still applies params; it must not print.

Reuse existing camera init from Greenhouse / RC CAM WiFi — do **not** duplicate `esp_camera_init`.

---

## Sketches to update (by project)

Apply the shared module to every row that serves **`/stream`** on SoftAP or STA.
Priority order: Kit A WiFi → Kit B video-only → BLE binary CAM → others.

### TeleCon_RcVehiclePro (`/home/miguel/Documents/ESP32_Projects/TeleCon_RcVehiclePro/`)

| Sketch path | Role | `/stream` | `/camconfig` priority |
|-------------|------|-----------|------------------------|
| `ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi/` | **Kit A** — SoftAP video + TCP `:3333` | Yes | **P0 — ship first** |
| `ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_SoftAP_Video/` | **Kit B** — SoftAP video only | Yes | **P0** |
| `ESP32_cam/BLE/TeleCon_RcVehiclePro_CAM_BLE_binary/` | BLE RC + Wi‑Fi `/stream` | Yes | **P1** |
| `ESP32_cam/Classic_Simple/TeleCon_RcVehiclePro_CAM_Classic_Simple/` | Classic SPP + `/capture` | Usually no | P2 — `/capture` honours quality/size |
| `ESP32_cam/Classic_Binary/TeleCon_RcVehiclePro_CAM_Classic_Binary/` | Classic binary + `/capture` | Usually no | P2 |
| `ESP32_cam/lab/*` | Lab builds | Varies | P3 |

SoftAP SSID/password (unchanged): `TeleCon-RC-CAM` / `telecon1234` (starter: `TeleCon-RC-CAM-Starter`).

### TeleCon_Greenhouse (`TeleCon_Greenhouse_CAM/`)

| Build | Camera HTTP | Notes |
|-------|-------------|-------|
| `TeleCon_Greenhouse_CAM` | `/capture` (+ optional `/stream` if added) | Android Greenhouse does **not** call `/camconfig` yet; still add handler for shared `CameraStream` and future HUD |

### TeleCon Smart Door Lock (when CAM sketch exists)

| Build | Expected | Notes |
|-------|----------|-------|
| `TeleCon_SmartDoorLock_CAM` or equivalent | `/capture` door viewer | Same shared module; Android runtime UI not wired yet |

### Control Panel (Android app)

No separate firmware — uses the same ESP32-CAM SoftAP profile as RC when board = CAM.
Updating RC Kit A / Kit B sketches covers Control Panel.

---

## Implementation steps (per sketch)

1. **Link** `CameraStreamCamConfig.*` (copy or symlink from RC Kit A once stable).
2. In `setup()` after `cameraInit()` and before `server.begin()`:
   ```cpp
   server.on("/camconfig", HTTP_GET, handleCamConfig);
   ```
3. **`handleCamConfig`**:
   - Parse `framesize`, `quality`, `fps` from `WebServer` args.
   - Call `cameraStreamApplyConfig(token, quality, fps)`.
   - Return JSON 200.
4. **MJPEG loop** (`/stream`):
   - Respect fps cap: track `lastFrameMs`, skip encode if interval not elapsed when `fps > 0`.
   - On framesize change while streaming: set a flag to end current client chunk and reopen sensor settings before next boundary.
5. **`/capture`**: read current quality + framesize from shared state before `esp_camera_fb_get()`.
6. **Do not** block the control path (TCP `:3333` or BLE) inside `/camconfig` — keep handler < 50 ms when possible.

---

## `/status` extension (recommended)

If the sketch already exposes `GET /status`, add:

```json
"cam": {
  "framesize": "qvga",
  "quality": 22,
  "fps_cap": 10
}
```

Helps field debug without Serial.

---

## Testing checklist

For **Kit A** `TeleCon_RcVehiclePro_CAM_WiFi`:

1. Flash firmware; join SoftAP; open `http://192.168.4.1/stream` — VGA stream OK.
2. `curl "http://192.168.4.1/camconfig?framesize=qvga&quality=22&fps=10"` → 200 JSON.
3. Reload `/stream` — visibly smaller/faster video; Serial shows new params.
4. `curl ".../camconfig?framesize=vga&quality=15&fps=0"` → back to VGA.
5. Android RC Vehicle Pro → live screen → **Stream quality** → Smooth → no crash; HUD smoother.
6. Android Control Panel → camera pane → same dialog works on SoftAP CAM board.
7. Old firmware without handler: Android still runs (404 logged once in logcat).

Repeat smoke test on **Kit B** `CAM_SoftAP_Video` (no TCP control port).

---

## Do not

- Create separate firmware binaries per phone model (Poco vs flagship).
- Change SoftAP SSID, password, or `/stream` URL scheme.
- Move stream-quality UI back into Android Settings — runtime only on live camera.
- Break TCP `:3333` Kit A control or BLE Kit B control while applying camconfig.
- Use POST for `/camconfig` (Android client is GET-only).

---

## Deliverables

1. Shared `CameraStreamCamConfig.*` (or equivalent) in the RC repo, documented in a short README section.
2. `/camconfig` wired on **P0** sketches (Kit A WiFi + Kit B SoftAP Video).
3. Optional P1/P2 roll-out to BLE CAM and `/capture`-only sketches.
4. Serial / README note: “Stream quality on phone requires firmware with `/camconfig` (2026-08+).”

Serial Monitor debug (`TELECON_DEBUG` only):  
`docs/prompts/ESP32_CAM_CAMCONFIG_SERIAL_DEBUG_ESP32_PROMPT.md`

When done, update the canonical stub in the Android repo:
`docs/prompts/RC_VEHICLE_PRO_WIFI_SOFTAP_CAMCONFIG_ESP32_PROMPT.md` → point here.
