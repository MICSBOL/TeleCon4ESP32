# Cursor prompt: RC Vehicle Pro — ESP32-CAM + BLE Binary firmware

> **Lab only / superseded as a product path.** SoftAP + BLE on **one** ESP32-CAM is
> **not** a supported kit. Prefer:
> - Kit A: `TeleCon_RcVehiclePro_CAM_WiFi` (SoftAP video + TCP)
> - Kit B: SoftAP video-only CAM + DevKit `TeleCon_RcVehiclePro_BLE_binary` (`TeleCon-BLE-RC-V`)
>
> Android guidance: [`RC_VEHICLE_PRO_SUPPORTED_KITS_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_SUPPORTED_KITS_ANDROID_PROMPT.md)
> (profile `WIFI_CAMERA_DEVKIT_BLE` = two boards, not this sketch).

Copy everything below into a **new Cursor chat** (Agent mode) on
`/home/miguel/Documents/ESP32_Projects/TeleCon_RcVehiclePro/`.

Historical Android mapping for this lab firmware was SoftAP HTTP video + BLE on the
same CAM (`TeleCon-BLE-RC-V-CAM`). Do **not** present that as a user-facing option.

Do **not** implement Wi‑Fi SoftAP TCP control (`proto=wifi`) here — that is
`TeleCon_RcVehiclePro_CAM_WiFi`. Do **not** invent a new app prefix — always **`RC`**.

---

## Goal

Implement one Arduino IDE sketch:

| Item | Value |
|------|--------|
| Path | `ESP32_cam/BLE/TeleCon_RcVehiclePro_CAM_BLE_binary/` |
| Board | AI-Thinker **ESP32-CAM** (`CAMERA_MODEL_AI_THINKER`) |
| Control | **BLE NUS** + **binary RC** (`proto=binary`) |
| Video | SoftAP HTTP **`GET /stream`** (preferred) + **`GET /capture`** fallback |
| Device name | `TeleCon-BLE-RC-V-CAM` |
| SoftAP SSID / password | `TeleCon-RC-CAM` / `telecon1234` |
| SoftAP IP | `192.168.4.1` (HTTP port **80** only — **no** TCP `:3333`) |

**Dual-radio on one CAM board:** BLE for control/telemetry, Wi‑Fi SoftAP for JPEG/MJPEG only.
Video bytes must **never** ride on BLE.

Prefer **NimBLE** (`NimBLEDevice.h`) for RAM headroom next to the camera frame buffer.

---

## Android contract (already done — match this)

Android repo: `/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`

| Topic | Files |
|-------|--------|
| CAM + BLE profile | `domain/camera/CameraLinkProfile.kt` → `WIFI_CAMERA_BLE_CONTROL` |
| Settings | Board **ESP32-CAM**, connection **BLE + Binary** |
| Handshake | `ProtocolHandshake.kt` → `RC:CONNECT,proto,binary` |
| BLE NUS client | `data/bluetooth/BleDataTransferService.kt` |
| Binary RC | `RcPacketEncoder.kt`, `docs/BINARY_PROTOCOL_APPS.md` |
| Camera client | `Esp32CameraStreamRepository.kt` — `/stream` then `/capture` |
| Defaults | `Esp32CameraDefaults` — `http://192.168.4.1`, SSID `TeleCon-RC-CAM` |
| HUD mapping | `docs/prompts/RC_VEHICLE_PRO_ESP32_FIRMWARE_PROMPT.md` (control + telemetry tables) |
| BLE base | `docs/prompts/BLE_ESP32_FIRMWARE_PROMPT.md` |
| SoftAP-only (do not mix) | `ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi/` |

### Phone setup (manual test)

1. Flash this sketch; open **Serial Monitor @ 115200**.
2. Android → RC Vehicle Pro **Settings**: device **ESP32-CAM**, connection **BLE + Binary**.
3. Join Wi‑Fi **`TeleCon-RC-CAM` / `telecon1234`** (phone loses internet while on SoftAP).
4. Open RC Vehicle Pro → video should start (`/stream` or `/capture`).
5. Tap Connect → discover **`TeleCon-BLE-RC-V-CAM`** → BLE session + handshake.

---

## BLE service (must match Android exactly)

Nordic UART Service:

| Item | UUID |
|------|------|
| Service | `6E400001-B5A3-F393-E0A9-E50E24DCCA9E` |
| RX (phone → ESP32, Write / Write NR) | `6E400002-B5A3-F393-E0A9-E50E24DCCA9E` |
| TX (ESP32 → phone, Notify) | `6E400003-B5A3-F393-E0A9-E50E24DCCA9E` |

- Advertise NUS UUID + name `TeleCon-BLE-RC-V-CAM`.
- TX needs CCCD `0x2902`; session ready only after notifications enabled.
- Request / accept MTU **247**; chunk notifies to `negotiatedMTU - 3`.
- Byte stream = Classic Binary RC over NUS (same parsers as Control Panel BLE Binary).

### Handshake

Phone sends (text line over NUS):

```text
RC:CONNECT,proto,binary
```

Reply within **2.5 s**:

| Case | Reply |
|------|--------|
| OK | `RC:ACK,app,RC\n` |
| Wrong proto | `RC:NAK,reason,proto_mismatch,expected,binary,actual,<got>\n` |
| Wrong app | `RC:NAK,reason,app_mismatch,expected,RC,actual,<got>\n` |

Set `TELECON_PROTO_WIRE` = `"binary"`. Only ACK when `proto` is `binary`.
Do **not** ACK `proto=wifi` or `proto=simple` in this sketch.

### Phone → ESP32 (after ACK)

| Frame | Meaning |
|-------|---------|
| `AA 55` (18-byte RC control) | Sticks / knobs / switches (`RcPacketEncoder`) |
| `BB 66` + button id | Action buttons (buzzer **2**, camera-front **4**, …) |

### ESP32 → phone

| Frame | Meaning |
|-------|---------|
| `CC 11` | Panel telemetry (speed×10 in left panel) |
| `CC 22` | Indicator (battery 0–100 preferred, motor temp analog 0–255) |
| `CC 33` | Optional 4 plot samples (bench/debug) |

Do **not** send `CC 44` / `RC:PLOTCFG`. No video over NUS.

---

## SoftAP camera (HTTP only)

| Item | Value |
|------|--------|
| SoftAP | SSID `TeleCon-RC-CAM`, password `telecon1234`, IP `192.168.4.1` |
| Preferred video | `GET /stream` → `multipart/x-mixed-replace; boundary=frame` (MJPEG) |
| Fallback / still | `GET /capture` → single `image/jpeg` |
| Status (optional) | `GET /status` → JSON `{ "cam":1, "mode":"ble_binary" }` |
| Control TCP | **None** — do not open `:3333` |

Reuse Greenhouse / CAM WiFi camera modules where possible:

- Greenhouse: `TeleCon_Greenhouse/.../CameraStream.*`
- RC SoftAP WiFi sketch: `ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi/` (camera + SoftAP only; strip TCP control)

Camera: VGA JPEG, `jpeg_quality ≈ 12–15`, `fb_count = 2`, `CAMERA_GRAB_LATEST` when available.
While serving `/stream`, keep calling BLE poll + motor fail-safe from an idle hook / `loop()`.

---

## Reuse (do not rewrite parsers)

Start from:

| Need | Source under ESP32 projects |
|------|------------------------------|
| BLE Binary RC | `TeleCon_ControlPanel/ESP32_noCam/BLE/TeleCon_ControlPanel_BLE_binary_ESP32/` |
| Vehicle pin / output mapping | `RC_VEHICLE_PRO_ESP32_FIRMWARE_PROMPT.md` + any existing RC-V BLE noCam sketch |
| Camera HTTP SoftAP | Greenhouse CAM or `TeleCon_RcVehiclePro_CAM_WiFi` camera portion |
| Debug macros | Control Panel `TeleConDebug.h` / `TELECON_DEBUG` / `TELECON_SERIAL_INJECT` |

Copy/adapt: NUS setup, `RC:CONNECT` handler, `AA 55` / `BB 66` RX, `CC 11/22/33` TX,
fail-safe on disconnect, then add SoftAP + `CameraStream` for AI-Thinker pins.

---

## Suggested sketch layout

```text
TeleCon_RcVehiclePro_CAM_BLE_binary/
  TeleCon_RcVehiclePro_CAM_BLE_binary.ino
  TeleConConfig.h          // names, TELECON_DEBUG, TELECON_USE_HW_IO, SoftAP creds
  TeleConDebug.h           // Serial helpers
  TeleConBle.*             // NUS + MTU + RX buffer → protocol
  TeleConProtocol.* / BinaryRx.*
  RcVehicleHandlers.*      // AA55 / BB66 → motors, pan, lights, buzzer
  CameraStream.cpp / .h    // SoftAP + /stream + /capture
  pins.h                   // CAM-safe GPIOs only
  README.md
```

### `loop()` (non-blocking)

```text
loop():
  teleconBlePoll()           // NUS RX parse, notify flush
  applyRcOutputs()           // motors / pan / lights from last AA55
  if (telemetryDue) sendCcTelemetry()
  cameraStreamPoll()         // handleClient + stream idle hook
  // BLE disconnect → motors zero within one iteration
```

### ESP32-CAM pin caution

Do **not** reuse camera GPIOs (0, 2, 4, 5, 12, 13, 14, 15, 16, … board-specific).
Document a **minimal** drive map for bench (e.g. status LED + optional servo on free pins)
and a full vehicle map for wiring off-board drivers if needed. Prefer simulation mode
(`TELECON_USE_HW_IO 0`) for first bring-up.

---

## Serial Monitor debug (Arduino IDE) — required

All logging on **`Serial` at 115200** baud. Enable with:

```c
#define TELECON_DEBUG 1
#define TELECON_SERIAL_INJECT 1   // optional: type lines → phone over NUS
#define TELECON_USE_HW_IO 0       // 0 = bench echo / safe defaults
```

### Boot banner (print once in `setup()`)

```text
======== TeleCon RC-V CAM BLE Binary ========
Board: ESP32-CAM (AI-Thinker)
BLE name: TeleCon-BLE-RC-V-CAM
Proto: binary (ACK only proto=binary)
SoftAP: TeleCon-RC-CAM / telecon1234 @ 192.168.4.1
HTTP: GET /stream (MJPEG), GET /capture (JPEG)
Control: BLE NUS only — NO TCP :3333
Android: Board=ESP32-CAM, Connection=BLE + Binary
TELECON_DEBUG=1  SERIAL_INJECT=1  HW_IO=0
============================================
```

### Required log lines (`TELECON_DEBUG`)

| Event | Example Serial line |
|-------|---------------------|
| SoftAP up | `[CAM] SoftAP up SSID=TeleCon-RC-CAM IP=192.168.4.1` |
| Camera init OK/fail | `[CAM] sensor OK` / `[CAM] init FAIL <code>` |
| HTTP client | `[CAM] GET /stream` / `[CAM] GET /capture` |
| BLE advertising | `[BLE] advertising TeleCon-BLE-RC-V-CAM` |
| BLE connect / disconnect | `[BLE] connected` / `[BLE] disconnected → FAILSAFE` |
| CCCD notify enabled | `[BLE] notifications ON` |
| Handshake RX | `[HS] RX RC:CONNECT,proto,binary` |
| Handshake TX | `[HS] TX RC:ACK,app,RC` or `[HS] TX RC:NAK,...` |
| Control RX | `[RC] AA55 ly=.. rx=.. rk=.. sw=..` (throttle rates if noisy) |
| Button RX | `[RC] BB66 id=2` |
| Telemetry TX | `[TX] CC11 left=186` / `[TX] CC22 batt=76 analog=90` (≤2 Hz summary) |
| Fail-safe | `[SAFE] motors zero (BLE lost)` |
| Inject | `[INJ] forwarded <n> bytes to NUS` |

Keep per-frame JPEG logs **off** by default (too spammy). Optional
`TELECON_DEBUG_CAM_VERBOSE 0/1` for `/capture` timing.

### Serial inject (bench)

With `TELECON_SERIAL_INJECT 1`, lines typed in Serial Monitor are encoded/forwarded to the
phone over NUS (same idea as Control Panel BLE Binary), e.g.:

```text
RC:DATA,left,186,right,0,lo,1,ro,0,lg,1,rg,0,analog,90,batt,76,led,01
```

Sketch may accept SIMPLE text and convert to `CC 11` / `CC 22` before notify — prefer that
for tutorial parity. Document the inject format in the sketch README.

### Bench / simulate mode (`TELECON_USE_HW_IO 0`)

Echo phone controls into telemetry (match Control Panel BLE debug):

| Input | Telemetry |
|-------|-----------|
| Throttle / left stick Y | Speed×10 → `CC 11` left |
| Right knob | Battery or analog (document choice) |
| Switch / lights | `CC 22` led / bits |
| Buttons 2 / 4 | Log + optional brief buzzer / center pan flag |

Motors stay safe (outputs off or soft PWM demo LED only) until `TELECON_USE_HW_IO 1`.

---

## Acceptance criteria

- [ ] Serial @ 115200 shows boot banner, SoftAP up, camera OK, BLE advertising
- [ ] Phone joins SoftAP → browser or app sees `/stream` or `/capture`
- [ ] Android Board=CAM, Connection=**BLE + Binary** → discovers `TeleCon-BLE-RC-V-CAM`
- [ ] `RC:CONNECT,proto,binary` → `RC:ACK,app,RC` (Serial `[HS]` lines)
- [ ] Wrong proto (`simple` / `wifi`) → NAK `proto_mismatch`; Android shows mismatch dialog
- [ ] Sticks send `AA 55`; Serial shows `[RC] AA55…`; HUD / echo telemetry updates
- [ ] Buzzer button id **2**, camera-front id **4** logged / handled
- [ ] BLE disconnect → fail-safe within one loop; Serial `[SAFE]`
- [ ] No TCP `:3333`; no video on BLE; no `CC 44`; app prefix always `RC`
- [ ] `TELECON_SERIAL_INJECT` can push a DATA/plot line to the phone for HUD smoke test
- [ ] README: flash steps, Android settings, SoftAP creds, Serial baud, pin map, HW_IO flag

---

## Do not

- Open SoftAP **TCP control** (`proto=wifi` / port 3333) in this sketch
- Use `BluetoothSerial` / Classic SPP on the CAM (RAM + radio conflict with SoftAP)
- Send JPEG/MJPEG over NUS
- Invent `RV:` or other prefixes
- Block `loop()` on long `esp_camera_fb_get` without feeding BLE + fail-safe
- Change Android packet layouts; flag gaps instead

---

## When done

1. Short **README.md** in the sketch folder (board, SoftAP, BLE name, Android settings,
   Serial 115200 debug legend, inject examples, pin map, HW_IO flag).
2. Cross-link from the RC Vehicle Pro Android docs if needed:
   Lab only — product Kit B uses `WIFI_CAMERA_DEVKIT_BLE` with DevKit
   `TeleCon_RcVehiclePro_BLE_binary`, not this single-board CAM sketch.
3. Manual checklist: SoftAP video alone → BLE connect → drive echo → disconnect fail-safe,
   with Serial Monitor open the whole time.

Reference prompts:

- `docs/prompts/RC_VEHICLE_PRO_ESP32_FIRMWARE_PROMPT.md` (full six-variant context)
- `docs/prompts/BLE_ESP32_FIRMWARE_PROMPT.md` (NUS contract)
- `docs/prompts/CONTROL_PANEL_DEBUG_MODE_ESP32_PROMPT.md` (Serial inject / simulate)
- `docs/prompts/CONTROL_PANEL_HANDSHAKE_ESP32_PROMPT.md`
- `docs/prompts/RC_VEHICLE_PRO_CAMERA_STREAM_ANDROID_PROMPT.md` (`/stream` preference)
