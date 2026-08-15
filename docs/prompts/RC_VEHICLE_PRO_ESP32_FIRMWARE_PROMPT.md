# Cursor prompt: TeleCon RC Vehicle Pro ESP32 firmware

Copy everything below into a **new Cursor chat** (Agent mode) on
`/home/miguel/Documents/ESP32_Projects/TeleCon_RcVehiclePro/`.

The Android app (`TeleCon4ESP32`) **RC Vehicle Pro** screen is already implemented:
full-bleed Wi‑Fi camera HUD, throttle/steering, camera pan knob, lights / buzzer /
front-cam / emergency stop, Classic Simple + Classic Binary + BLE Binary over the
shared **`RC`** protocol (same wire format as Control Panel).

Folder stubs already exist (`ESP32_noCam/` and `ESP32_cam/` × Classic_Simple /
Classic_Binary / BLE). Fill them with working sketches.

---

## Goal

Create **six firmware variants** (reuse modules; do not invent a new app prefix):

| Variant | Path under `TeleCon_RcVehiclePro/` | Transport | Protocol | Camera |
|---------|-------------------------------------|-----------|----------|--------|
| Classic Simple noCam | `ESP32_noCam/Classic_Simple/TeleCon_RcVehiclePro_Classic_Simple/` | Classic SPP | SIMPLE text | No |
| Classic Binary noCam | `ESP32_noCam/Classic_Binary/TeleCon_RcVehiclePro_Classic_Binary/` | Classic SPP | Binary RC | No |
| BLE Binary noCam | `ESP32_noCam/BLE/TeleCon_RcVehiclePro_BLE_binary/` | BLE NUS | Binary RC | No |
| Classic Simple CAM | `ESP32_cam/Classic_Simple/TeleCon_RcVehiclePro_CAM_Classic_Simple/` | Classic SPP | SIMPLE | Wi‑Fi `/capture` |
| Classic Binary CAM | `ESP32_cam/Classic_Binary/TeleCon_RcVehiclePro_CAM_Classic_Binary/` | Classic SPP | Binary RC | Wi‑Fi `/capture` |
| BLE Binary CAM | `ESP32_cam/BLE/TeleCon_RcVehiclePro_CAM_BLE_binary/` | BLE NUS | Binary RC | Wi‑Fi `/stream` + `/capture` |

**Focused CAM + BLE Binary prompt (Serial debug included):**
`docs/prompts/RC_VEHICLE_PRO_CAM_BLE_BINARY_ESP32_PROMPT.md` in the Android repo.

**Steering center trim (tickers + NVS save, all connection modes):**
`docs/prompts/RC_VEHICLE_PRO_STEER_CENTER_TRIM_ESP32_PROMPT.md` in the Android repo.

**Application prefix is always `RC`** (same as Control Panel). Android
`ApplicationId.RC_VEHICLE_PRO` and `CONTROL_PANEL` both use prefix `"RC"`.

Suggested Bluetooth / BLE names:

| Variant | Device name |
|---------|-------------|
| Classic noCam | `ESP32-TeleCon-RC-V` |
| Classic CAM | `ESP32-TeleCon-RC-V-CAM` |
| BLE noCam | `TeleCon-BLE-RC-V` |
| BLE CAM | `TeleCon-BLE-RC-V-CAM` |

---

## Android contract (source of truth)

Read these files in the Android repo
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`:

| Topic | Files |
|-------|--------|
| SIMPLE RC lines | `docs/SIMPLE_PROTOCOL_ESP32.md` (RC section) |
| Binary RC framing | `docs/BINARY_PROTOCOL_APPS.md` (RC legacy note), `RcPacketEncoder.kt` |
| Handshake | `ProtocolHandshake.kt`, `docs/prompts/CONTROL_PANEL_HANDSHAKE_ESP32_PROMPT.md` |
| BLE NUS | `docs/prompts/BLE_ESP32_FIRMWARE_PROMPT.md`, `BleDataTransferService.kt` |
| Vehicle UI mapping | `ui/rc_vehicle_pro/RcVehicleProScreen.kt`, `RcVehicleProViewModel.kt` |
| Wi‑Fi camera client | `data/camera/Esp32CameraStreamRepository.kt` → `GET {base}/capture` every ~150 ms |
| Default cam URL | `Esp32CameraDefaults.DEFAULT_BASE_URL` = `http://192.168.4.1` |

### Connection modes in Android (RC Vehicle settings)

| App setting | Handshake | Phone → ESP32 control |
|-------------|-----------|------------------------|
| Classic + Simple | `RC:CONNECT,proto,simple` | `RC:CTRL,...` / `RC:BTN,id,N` |
| Classic + Binary | `RC:CONNECT,proto,binary` | `AA 55` 18-byte + `BB 66` buttons |
| BLE + Binary | `RC:CONNECT,proto,binary` | Same binary over Nordic UART |

Reply within **2.5 s**:

- Success: `RC:ACK,app,RC\n`
- Wrong proto: `RC:NAK,reason,proto_mismatch,expected,<this>,actual,<got>\n`
- Wrong app: `RC:NAK,reason,app_mismatch,expected,RC,actual,<got>\n`

Set `TELECON_PROTO_WIRE` to `"simple"` or `"binary"` per sketch. Only ACK when
`proto` matches.

---

## Control mapping (RcVehicleProScreen → RC wire)

Android layout (landscape HUD):

| UI control | RcState / wire | Suggested ESP32 action |
|------------|----------------|------------------------|
| Left stick Y (throttle) | `ly` / leftStickY | Drive motors forward/reverse |
| Right stick X (steering) | `rx` / rightStickX | Steering servo or differential |
| Right knob (camera pan) | `rk` 0…1023 | Pan servo (center ≈ 512) |
| Lights toggle | left switch 0 (`sw` bit0 / switch1) | Headlights relay/PWM |
| Buzzer | `RC:BTN` id **2** / `BB 66` `0x02` (`CENTER_TOP_RIGHT`) | Horn pulse |
| Camera front | knob → 0.5 then `RC:BTN` id **4** / `0x04` (`CENTER_BOTTOM_RIGHT`) | Center pan servo |
| Emergency STOP | sticks forced to 0 | Immediate motor stop |
| Steer trim lock | `RC:SET,steer_center,1,rx,N` / `BB 66` `0x10` | Save current steer PWM as center (NVS) |
| Dual-rate / expo / reverse / deadzone | *(already applied on phone)* | Pass through mapped sticks — see `RC_VEHICLE_PRO_DRIVE_ASSIST_ESP32_PROMPT.md` |
| Photo / Record | **Local Android only** (gallery JPEG / UI flag) | Do **not** handle over BT |

Sticks are −100…100 on SIMPLE; binary packs them as 12-bit 0…4095
(`RcPacketEncoder.stickTo12Bit`). Android may send values already scaled by travel/expo.

---

## Telemetry mapping (ESP32 → HUD)

RcVehiclePro reads shared RC telemetry:

| HUD field | SIMPLE | Binary | Firmware convention |
|-----------|--------|--------|---------------------|
| Speed km/h | `RC:DATA` **`left`** | `CC 11` left panel | **speed×10** (186 → 18.6 km/h). Send `lo,1` when valid. |
| Battery % | `batt` | `CC 22` battery byte | Prefer **0–100**. (App also accepts 0–255 gauge.) |
| Motor temp °C | `analog` | `CC 22` analog | **0–255** gauge; app maps ≈ 25–76 °C |
| (optional) plots | `RC:PLOT,v0…v3` | `CC 33` count=4 | Debug traces; not shown on vehicle HUD |

Do **not** send legacy `RC:PLOTCFG` / `CC 44` label packets — labels come from Android RC settings.

Example SIMPLE telemetry (~2 Hz):

```
RC:DATA,left,186,right,0,lo,1,ro,0,lg,1,rg,0,analog,90,batt,76,led,01
```

---

## Wi‑Fi camera (CAM variants only)

Video is **not** sent over Bluetooth.

1. Soft-AP (or STA) so the phone can reach **`http://192.168.4.1`**.
2. HTTP server on port 80 with **`GET /capture`** returning a single **JPEG**
   (`image/jpeg`). Optional `GET /stream` MJPEG is nice-to-have; Android polls `/capture`.
3. Reuse Greenhouse CAM pattern:
   `/home/miguel/Documents/ESP32_Projects/TeleCon_Greenhouse/ESP32_cam/Classic_Binary/TeleCon_Greenhouse_CAM/CameraStream.*`
4. Typical AI-Thinker ESP32-CAM pin map; `#define CAMERA_MODEL_AI_THINKER`.
5. Call `cameraStreamPoll()` / `server.handleClient()` from `loop()` without starving BT RX.

**Architecture options (pick one and document in README):**

- **A (recommended for dual-board):** ESP32 DevKit = motors + BT/BLE; ESP32-CAM =
  Wi‑Fi only (`/capture`). Same AP SSID documented for the phone.
- **B (single ESP32-CAM):** One board runs BT Classic **or** BLE **and** Wi‑Fi AP +
  camera. Watch RAM (prefer NimBLE if BLE+CAM). Classic SPP + Wi‑Fi is heavy —
  test on real hardware.

Phone must join the CAM AP (or LAN) while Bluetooth stays connected to the
controller. Document SSID / password (e.g. `TeleCon-RC-CAM` / `telecon1234`).

---

## Reuse existing Control Panel firmware

Start from (do not rewrite parsers from scratch):

| Need | Source |
|------|--------|
| Classic Simple RC | `TeleCon_ControlPanel/ESP32_noCam/Classic_Simple/ESP32_BT_Controller_sp/` |
| Classic Binary RC | `TeleCon_ControlPanel/ESP32_noCam/Classic_Binary/ESP32_BT_Controller/` |
| BLE Binary RC | `TeleCon_ControlPanel/ESP32_noCam/BLE/TeleCon_ControlPanel_BLE_binary_ESP32/` |
| Camera HTTP | Greenhouse `CameraStream.cpp` / `.h` |
| Handshake | `CONTROL_PANEL_HANDSHAKE_ESP32_PROMPT.md` behavior |

Shared modules to copy/symlink: `TeleConBluetooth.*`, `TeleConProtocol.*`,
`TeleConBinaryRx.*`, `TeleConConfig.h`, `TeleConDebug.h`, Control Panel handlers
for `RC:CONNECT` / CTRL / BTN / `AA 55` / `BB 66` / `CC 11/22/33`.

Then specialize:

- Pin map for vehicle (drive, steer, pan, lights, buzzer)
- Device name suffixes (`-RC-V`, `-RC-V-CAM`)
- Telemetry → speed×10 / batt% / motor analog
- CAM variants: add `CameraStream`

---

## Suggested pins (adjust in `pins.h`)

```
Drive L PWM / dir  → GPIO 25 / 26
Drive R PWM / dir  → GPIO 27 / 14   (or single throttle + steer servo)
Steer servo        → GPIO 13
Camera pan servo   → GPIO 12
Lights             → GPIO 15
Buzzer             → GPIO 2
Status LED         → GPIO 4
Battery ADC        → GPIO 34
Motor temp ADC     → GPIO 35
```

ESP32-CAM: keep camera pins free; put BT on a second board if using option A.

---

## `loop()` requirements

```
loop():
  teleconPollBluetooth()   // or BLE notify pump
  applyRcOutputs()         // motors / servos / lights from last CTRL
  if (telemetryDue) sendRcTelemetry()  // SIMPLE and/or CC 11/22
  if (CAM) cameraStreamPoll()
```

Fail-safe: if BT/BLE drops, zero motors within one loop iteration.

---

## Acceptance criteria

- [ ] Classic Simple: app Connection = Classic + Simple → ACK; sticks move motors; HUD battery/speed update
- [ ] Classic Binary: Classic + Binary → ACK; `AA 55` / `BB 66` work; `CC 11/22` update HUD
- [ ] BLE Binary: Connection = BLE → discovers `TeleCon-BLE-RC-V*`; same binary as Classic Binary
- [ ] Wrong proto → NAK `proto_mismatch` and Android error dialog (no stuck session)
- [ ] CAM builds: phone on CAM Wi‑Fi sees live video on RcVehiclePro; Photo saves JPEG on phone
- [ ] noCam builds: HUD shows camera offline / idle hint; drive still works
- [ ] Lights = switch1; buzzer = button 2; camera front = button 4 centers pan
- [ ] STOP / disconnect → motors off
- [ ] No camera frames over Bluetooth; no new app prefix; no `CC 44`

---

## Do not

- Invent `RV:` or other prefixes — Android only speaks **`RC`** for this screen
- Send video over SPP/BLE
- Change `RcPacketEncoder` layouts without updating Android docs
- Block `loop()` on long camera captures (use non-blocking `/capture`)

---

## When done

Add a short README in each sketch folder with: board, wiring, SSID (CAM), which
**RC Vehicle settings** Connection type to select, and flash steps.

Reference parity prompts: `CONTROL_PANEL_HANDSHAKE_ESP32_PROMPT.md`,
`CONTROL_PANEL_DEBUG_MODE_ESP32_PROMPT.md`, `BLE_ESP32_FIRMWARE_PROMPT.md`,
`GREENHOUSE_ESP32_FIRMWARE_PROMPT.md` (camera section only).
