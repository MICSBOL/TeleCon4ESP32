# Cursor prompt: RC Vehicle Pro — SoftAP Wi‑Fi stream + control performance (Android)

Copy into a **new Cursor chat** (Agent mode) on
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`.

**Scope: Kit A only** — SoftAP HTTP video + SoftAP TCP control (`CameraLinkProfile.WIFI_SOFTAP`).

| Item | Value |
|------|--------|
| Firmware | `TeleCon_RcVehiclePro/ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi/` |
| SoftAP | `TeleCon-RC-CAM` / `telecon1234` → `http://192.168.4.1` |
| Video | `GET /stream` MJPEG (preferred); `GET /capture` uncapped fallback |
| Control | TCP `192.168.4.1:3333`, `RC:CONNECT,proto,wifi`, `RC:CTRL` / `RC:BTN` / `RC:DATA` |

Do **not** change Kit B BLE paths, Classic SPP, or lab single-board SoftAP+BLE unless a shared file forces a careful, non-regressing touch.

Related prompts (do not duplicate their goals):

- Smooth MJPEG basics: [`RC_VEHICLE_PRO_CAMERA_STREAM_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_CAMERA_STREAM_ANDROID_PROMPT.md)
- SoftAP connect UX: [`RC_VEHICLE_PRO_WIFI_SOFTAP_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_WIFI_SOFTAP_ANDROID_PROMPT.md)
- Kits: [`RC_VEHICLE_PRO_SUPPORTED_KITS_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_SUPPORTED_KITS_ANDROID_PROMPT.md)
- **Next:** SoftAP performance **presets in settings** (+ optional `/camconfig`): [`RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_SETTINGS_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_SETTINGS_ANDROID_PROMPT.md)

---

## Problem (observed)

- Browser / Ubuntu on SoftAP `http://192.168.4.1/stream` looks **smooth**.
- Samsung S23 HUD is acceptable.
- Mid-range phones (e.g. **Xiaomi Poco**) show **choppy video** and less smooth stick response in the app while using **Wi‑Fi SoftAP only** (cam + control).

Conclusion: SoftAP bitrate / firmware feed is good enough; optimize the **Android SoftAP path** (decode, MJPEG parse, frame publish, SoftAP TCP).

---

## Goal

Make Kit A SoftAP **video + control** feel smoother on mid-range devices without harming S23 / flagship, without rewriting the architecture, and without WebView / BLE video.

Priority order:

1. Cheaper HUD JPEG decode (preview)
2. Faster MJPEG SOI/EOI scan
3. Drop / skip frames under decode load (prefer latest)
4. SoftAP TCP: bind to SoftAP `Network` + reduce CTRL send backlog
5. Keep Compose preview cheap (`remember` ImageBitmap)

**Do not** reintroduce `FRAME_POLL_INTERVAL_MS = 150` on the success path.  
**Do not** change firmware in this prompt (Android-only). Optional firmware bitrate tweaks are out of scope unless the user asks separately.

---

## Already implemented — extend, do not rewrite

| Layer | File | Role |
|-------|------|------|
| Stream client | `data/camera/Esp32CameraStreamRepository.kt` | MJPEG `/stream` → `/capture`; byte SOI/EOI scan; full `BitmapFactory.decodeByteArray`; one-frame recycle lag |
| SoftAP HTTP bind | `data/camera/SoftApNetworkResolver.kt` | `Network.openConnection` for SoftAP Wi‑Fi |
| Preview | `ui/rc_vehicle_pro/components/RcCameraPreview.kt` | full-bleed `Image` + `remember(bitmap) { asImageBitmap() }` |
| SoftAP TCP | `data/wifi/WifiSoftApDataTransferService.kt` | TCP `:3333`, `tcpNoDelay`, `Channel.UNLIMITED`, **`delay(10)` after every flush** |
| CTRL heartbeat | `ui/bluetooth/BluetoothViewModel.kt` | SoftAP CTRL every **100 ms** (`SOFTAP_CTRL_PERIOD_MS`) |
| Auto-connect | `ui/rc_vehicle_pro/RcVehicleProScreen.kt` | when camera online → `ensureWifiSoftApConnected` |
| Session | `domain/camera/Esp32CameraLinkSession.kt` | idempotent `startStream` |

Greenhouse / other apps share `Esp32CameraStreamRepository` — keep `/stream` → `/capture` behaviour; any decode options must remain safe for those callers (defaults OK for HUD; photo path must stay high quality where the app snapshots).

---

## Work items

### 1. Cheaper HUD decode (highest impact)

In `Esp32CameraStreamRepository` (or a small helper used by it):

- For **live HUD frames** (`/stream` and `/capture` poll), decode with `BitmapFactory.Options`:
  - `inSampleSize = 2` (VGA → ~QVGA effective for HUD), and/or
  - `inPreferredConfig = Bitmap.Config.RGB_565`
- Photo / save path must still get a usable still:
  - Prefer: keep a separate full-quality snapshot path (e.g. one `/capture` decode without downsample when user taps photo), **or**
  - Document if photo uses last HUD frame (slightly softer) and only change if product accepts it.
- Do **not** force downsample on Greenhouse if product quality there matters — gate by caller flag / default that RC Vehicle Pro HUD opts into “fast preview”.

Log once at stream start which decode options are active (`Esp32CameraStream`).

### 2. Faster MJPEG scan

Replace per-byte `InputStream.read()` SOI/EOI loops in `readNextJpeg()` with **chunked buffer scans** over the existing `BufferedInputStream` (64 KB), searching for `FF D8` / `FF D9` in bulk.

Keep:

- Max JPEG size guard (`MAX_JPEG_BYTES`)
- Resync behaviour on oversized / corrupt frames
- Coroutine `yield()` between published frames (no artificial success delay)

### 3. Frame drop under load

While consuming MJPEG:

- Prefer **latest frame latency** over decoding every JPEG if the previous frame was just published and decode is falling behind.
- Simple approach: if a decode/publish is in flight or the last publish was &lt; N ms ago under backlog, skip decode of intermediate JPEGs (still advance the stream reader so SoftAP does not stall).
- Do **not** queue multiple bitmaps for Compose.

Keep one-frame `pendingRecycle` lag so Compose can finish drawing.

### 4. SoftAP TCP control (Kit A only)

In `WifiSoftApDataTransferService` / SoftAP connect path:

- Open the control `Socket` on the SoftAP `Network` from `SoftApNetworkResolver` (same idea as HTTP — MIUI / dual-SIM often need this). Keep `tcpNoDelay = true`.
- Reduce send backlog under video load:
  - Drop or shorten the post-flush `delay(10)` if safe, **or**
  - Coalesce CTRL: keep at most one pending CTRL payload (drop older) so `Channel.UNLIMITED` cannot grow.
- Do **not** lower SoftAP CTRL heartbeat below what firmware fail-safe needs (~750 ms timeout → keep period well under that, e.g. still ~100 ms unless measured otherwise).

Log tag: keep / extend `WifiSoftApTransport` / `RcWifiSoftAp`.

### 5. Compose (verify only)

- `RcCameraPreview`: keep `remember(cameraState.bitmap) { asImageBitmap() }`, `ContentScale.Crop`, no extra per-frame effects.
- No WebView.

### 6. Strings / DI

- User-visible copy: only if you add a setting (prefer **no** new user setting for v1 — apply fast preview for SoftAP HUD automatically).
- If EN/ES strings are needed, update `values/strings.xml` + `values-es/strings.xml` together.

---

## Out of scope / Do not

- Firmware `FRAMESIZE` / `jpeg_quality` changes (separate ESP32 task)
- Kit B BLE Binary control changes
- SoftAP+BLE on one CAM board
- Cap success FPS with a fixed poll interval “for battery”
- WebView preview
- Video over TCP `:3333` or Bluetooth
- Process-wide `bindProcessToNetwork` (keep per-connection SoftAP bind)
- Drive-by refactors outside SoftAP stream/control path

---

## Manual test plan

### Baseline (prove SoftAP is fine)

1. Flash Kit A `TeleCon_RcVehiclePro_CAM_WiFi`.
2. Join SoftAP on Ubuntu → browser `http://192.168.4.1/stream` still smooth.
3. Note: `/stream` is **single-client** — close browser before app test (or vice versa).

### Android SoftAP (Kit A)

1. Join SoftAP on **Xiaomi Poco** (or similar mid-range) and on **Samsung S23** (or flagship).
2. Open RC Vehicle Pro → mode Wi‑Fi SoftAP / board CAM.
3. Logcat `Esp32CameraStream`: **`MJPEG /stream connected`** (not stuck on `/capture`).
4. HUD video should feel closer to browser smoothness on Poco (fewer hitch/stutters).
5. Move sticks while video runs → Serial `[PANEL RX]` / motors live; no multi-second CTRL lag.
6. Photo still works (acceptable quality).
7. Leave screen → stream stops; disconnect SoftAP → fail-safe on ESP32.
8. S23: no regression vs current feel.
9. Greenhouse SoftAP camera (if touched shared repo): still starts; `/capture` fallback intact.

Optional: Logcat `SoftApNetwork` / `WifiSoftApTransport` — SoftAP network used for HTTP **and** TCP.

---

## Acceptance

- [ ] Kit A SoftAP HUD uses cheaper preview decode (documented in log) without success-path FPS cap
- [ ] MJPEG marker scan is chunked (no per-byte read hot path)
- [ ] Under load, app prefers latest frame (no multi-frame bitmap queue)
- [ ] SoftAP TCP opens via SoftAP `Network` when available; CTRL does not backlog under video
- [ ] Poco SoftAP video+sticks feel clearly smoother than before; S23 not worse
- [ ] Photo + leave-screen stop + Greenhouse shared repository behaviour preserved
- [ ] No Kit B / Classic regressions; en+es only if copy changed

---

## Suggested implementation order

1. Preview `BitmapFactory.Options` for SoftAP HUD  
2. Chunked `readNextJpeg`  
3. Frame skip under load  
4. SoftAP TCP network bind + CTRL coalesce / TX pacing  

Measure on Poco after each step when possible (Logcat frame counts / subjective stick lag).
