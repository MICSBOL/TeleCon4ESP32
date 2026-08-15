# Cursor prompt: TeleCon Greenhouse ESP32 firmware

Copy everything below into a **new Cursor chat** (Agent mode) to implement the ESP32 firmware. The Android app (`TeleCon4ESP32`) is already implemented on the other side of this protocol.

---

## Goal

Create **two Arduino/ESP32 firmware projects** for the TeleCon Greenhouse application:

1. **`TeleCon_Greenhouse`** — ESP32 DevKit (sensors + actuators, **no camera**)
2. **`TeleCon_Greenhouse_CAM`** — Same as above + ESP32-CAM module support (or second board) for Wi‑Fi MJPEG

Both must speak **Classic Bluetooth SPP** (not BLE) and support **two protocols on the same socket**:

- **SIMPLE** — text lines `GH:DATA,...` and `GH:SET,...`
- **ADVANCED** — binary GH packets (premium users on Android)

Reuse shared transport modules from `/home/miguel/Documents/ESP32_Projects/ESP32_BT_Controller_sp/`:

- `TeleConBluetooth.*`
- `TeleConProtocol.*`
- `TeleConBinaryRx.*` (extend for `CC 47` / `AA 47`)
- `TeleConConfig.h`, `TeleConDebug.h`

---

## Android protocol contract (source of truth)

Read these files in the Android repo:

- `docs/SIMPLE_PROTOCOL_ESP32.md` — SIMPLE `GH:` lines
- `docs/BINARY_PROTOCOL_GH.md` — binary `AA 47` / `CC 47` layout
- `app/src/main/java/.../domain/bluetooth/gh/GhPacketEncoder.kt`
- `app/src/main/java/.../domain/bluetooth/gh/GhBinaryTelemetryMapper.kt`

### SIMPLE protocol (always required)

**ESP32 → phone (telemetry, ~1–2 Hz):**

```
GH:DATA,temp,26.2,hum,68,vpd,1.1,soil,42,light,12400,fan,1,heater,0,pump,0,lights,0,vent,40,tank,78,auto,1,target_temp,24,target_hum,65,cam,0
```

**Phone → ESP32 (commands, on demand):**

```
GH:SET,fan,1
GH:SET,vent,60
GH:SET,target_temp,24,target_hum,65
GH:SET,cam_pan,50,cam_tilt,50
```

### Binary protocol (premium / advanced)

**Phone → ESP32 SET:** `0xAA 0x47` + mask u16 LE + values + checksum  
**ESP32 → phone DATA:** `0xCC 0x47` + length u16 LE + `0x01` + 17-byte payload + checksum  

Full field layout is in `docs/BINARY_PROTOCOL_GH.md`. SET mask bits 8–9 are `cam_pan` / `cam_tilt` (0–100, 50 = center).

### Camera flag

| Firmware | `cam` in GH:DATA / binary flags bit 6 |
|----------|---------------------------------------|
| DevKit only | `0` |
| With ESP32-CAM | `1` |

Camera video is **Wi‑Fi HTTP**, not Bluetooth: `GET http://192.168.4.1/capture` (MJPEG/JPEG poll). Android uses `Esp32CameraStreamRepository`. Camera aim (`cam_pan` / `cam_tilt`) rides the control link (BT/BLE/SoftAP).

---

## Hardware (suggested pins — from Android strings)

```
I2C SDA=21, SCL=22 → BME280 (climate) + BH1750 (lux)
GPIO 4 → DS18B20 outside temperature
GPIO 34 → soil moisture (analog)
GPIO 27/33 → tank ultrasonic (trig/echo)
GPIO 13 → fan relay | 14 → heater | 16 → pump
GPIO 17 → grow lights | 19 → vent servo PWM
GPIO 32 → flow sensor pulse (optional)
ESP32-CAM (CAM variant / second board): Wi‑Fi AP/stream at 192.168.4.1
  free pins: GPIO 13 → pan servo | GPIO 12 → tilt servo
```

Adjust pins in `pins.h` per board.

---

## Architecture requirements

```
loop():
  teleconPollBluetooth()     // lines + binary demux
  readSensors()
  if (due) sendGhTelemetry() // SIMPLE and/or binary based on last RX mode OR always both
  applyActuators()
```

### Inbound routing

Extend `TeleConBinaryRx` / `onLineReceived`:

| Input | Handler |
|-------|---------|
| `GH:SET,...` line | `handleGhSet()` |
| `AA 47 ...` binary | `handleGhSetBinary()` |

### Outbound

Implement:

- `sendGhDataSimple()` — `GH:DATA,...` via `teleconSendLine()`
- `sendGhDataBinary()` — `CC 47` packet
- On connect: send telemetry in the mode the app uses, or send both until mode is known

**Pragmatic approach:** Always send SIMPLE lines at 1 Hz; when binary mode detected (or always at 2 Hz), also send binary DATA. Android ADVANCED users decode binary; SIMPLE users ignore unknown `CC 47` if not parsed — **but** Android already parses `CC 47` into messages regardless of mode. So:

- **Option A (recommended):** Firmware stores `protocolMode` from a handshake line `GH:SET,proto,binary` or infers from first inbound frame type (line vs `AA 47`).
- **Option B:** Send only one format; user must match app settings.

Implement **Option A**: if last command was binary SET, telemetry replies in binary only; if last was SIMPLE SET or on fresh connect, default SIMPLE until binary SET received.

### Actuator SET fields

`fan`, `heater`, `pump`, `lights`, `auto`, `vent`, `target_temp`, `target_hum`, `cam_pan`, `cam_tilt` — same semantics as Android `GreenhouseViewModel` / `GreenhouseCameraViewModel`.

---

## Project layout

```
TeleCon_Greenhouse/
  TeleCon_Greenhouse.ino
  pins.h
  GreenhouseSensors.cpp / .h
  GreenhouseActuators.cpp / .h
  GreenhouseHandlers.cpp / .h   // SIMPLE + binary encode/decode
  TeleConBluetooth.cpp          // copied or symlinked from _sp
  TeleConProtocol.cpp
  TeleConBinaryRx.cpp
  TeleConConfig.h

TeleCon_Greenhouse_CAM/
  (same +)
  CameraStream.cpp / .h         // ESP32-CAM HTTP /capture
  cam flag = 1 in telemetry
```

Bluetooth device names:

- `ESP32-TeleCon-GH`
- `ESP32-TeleCon-GH-CAM`

---

## Acceptance criteria

- [ ] Pairs with Android app (SPP UUID `00001101-0000-1000-8000-00805F9B34FB`)
- [ ] SIMPLE: `GH:DATA` updates Greenhouse screen; `GH:SET` toggles relays
- [ ] Binary: Android Advanced mode shows same values; SET packets drive actuators
- [ ] DevKit build: `cam,0`, no camera HTTP server
- [ ] CAM build: `cam,1`, `/capture` returns JPEG frames
- [ ] Coexists with line parser (no binary/text corruption)
- [ ] Serial debug optional via `TELECON_DEBUG`

---

## Reference ESP32 projects on this machine

- `/home/miguel/Documents/ESP32_Projects/ESP32_BT_Controller_sp/` — multi-app SIMPLE demo with GH stubs
- `/home/miguel/Documents/ESP32_Projects/ESP32_BT_Controller/` — production RC binary firmware

Start by copying `TeleCon*` modules from `_sp`, then replace simulated `TeleConTelemetry` GH state with real sensor/actuator code.

---

## Do not

- Use BLE/GATT
- Send camera over Bluetooth
- Change Android packet layouts without updating `docs/BINARY_PROTOCOL_GH.md` and Kotlin encoders

---

When done, provide a short README in each project with wiring, flash steps, and which protocol mode to select in the Android Greenhouse settings.
