# Cursor prompt: TeleCon BLE ESP32 firmware (.ino)

Copy everything below into a **new Cursor chat** (Agent mode) — or use it directly in the Arduino IDE with an AI assistant — to implement the ESP32 BLE firmware. The Android app (`TeleCon4ESP32`) is already implemented on the other side of this protocol.

---

## Goal

Create **one Arduino IDE sketch** named **`TeleCon_BLE.ino`** for an ESP32 that talks to the TeleCon4ESP32 Android app over **Bluetooth Low Energy (GATT)** instead of Classic SPP.

- Target: any ESP32 with BLE (original ESP32, ESP32-C3, ESP32-C6, ESP32-S3, …). Do **not** use `BluetoothSerial` (that is Classic only).
- Use the Arduino **`BLEDevice` / NimBLE** stack (`#include <BLEDevice.h>`, or `NimBLEDevice.h` if the NimBLE-Arduino library is installed — prefer NimBLE for lower RAM).
- One sketch, selectable application via a `#define` (see below). Default to `APP_GREENHOUSE`.

## BLE service contract (must match the Android app exactly)

The app connects as a GATT client to the **Nordic UART Service (NUS)**:

| Item | UUID |
|------|------|
| Service | `6E400001-B5A3-F393-E0A9-E50E24DCCA9E` |
| RX characteristic (phone → ESP32, Write / Write No Response) | `6E400002-B5A3-F393-E0A9-E50E24DCCA9E` |
| TX characteristic (ESP32 → phone, Notify) | `6E400003-B5A3-F393-E0A9-E50E24DCCA9E` |

Requirements:

- Advertise the NUS service UUID and a device name of the form `TeleCon-BLE-GH` (suffix per application: `RC`, `GH`, `WT`, `SP`, `SH`, `DL`, `LT`).
- TX characteristic must have a **CCCD (0x2902)** descriptor; the app writes it to enable notifications and only then considers the session ready.
- Accept an MTU of **247** (the app requests it; `BLEDevice::setMTU(247)`), but never assume it — chunk outgoing notifications to `negotiatedMTU - 3` bytes.
- The byte stream carried over RX/TX is **identical to the Classic SPP wire format**. Treat RX writes as if they were `SerialBT.read()` bytes and TX notifications as `SerialBT.write()` bytes. Reuse the same line/packet parser as the SPP firmware.

## Protocol contract (source of truth in the Android repo)

Read these files:

- `docs/SIMPLE_PROTOCOL_ESP32.md` — text-line protocol for every application prefix
- `docs/BINARY_PROTOCOL_GH.md` — Greenhouse binary (`AA 47` / `CC 47`)
- `app/src/main/java/.../domain/bluetooth/RcPacketEncoder.kt` — RC binary control packet (`AA 55`, 18 bytes)
- `app/src/main/java/.../domain/bluetooth/gh/GhPacketEncoder.kt` and `GhBinaryTelemetryMapper.kt`
- `app/src/main/java/.../data/bluetooth/BleDataTransferService.kt` — the Android BLE client this sketch must interoperate with

### Handshake (all applications)

On connect the app sends a text line, e.g.:

```
GH:CONNECT,proto,binary\n
```

Reply within 2.5 s:

- `GH:ACK,app,GH\n` when the sketch serves that application and protocol
- `GH:NAK,reason,app_mismatch,expected,GH,actual,WT\n` when it does not

### Protocol rule for BLE

**BLE sessions use the binary protocol only.** The Android app offers three connection modes:

| Mode | Transport | Protocol |
|------|-----------|----------|
| Classic + Simple | SPP | Text lines |
| Classic + Binary | SPP | Binary packets |
| BLE + Binary | NUS | Binary packets (`proto,binary` handshake) |

| Application | Prefix | Over BLE |
|-------------|--------|----------|
| Control Panel / RC | `RC` | Binary: phone→ESP32 `AA 55` control (18 B) and `BB 66` buttons; ESP32→phone `CC 11/22/33/44` telemetry |
| Greenhouse | `GH` | Binary: phone→ESP32 `AA 47` SET; ESP32→phone `CC 47` DATA (17-B payload) |
| Water Tank | `WT` | Binary: `AA 57` / `CC 57` — see `docs/BINARY_PROTOCOL_APPS.md` |
| Solar Power | `SP` | Binary live: `AA 53` / `CC 53` (history charts remain SIMPLE on Classic) |
| Smart Home | `SH` | Binary: `AA 48` / `CC 48` |
| Smart Door Lock | `DL` | Binary: `AA 4B` / `CC 4B` (`K` avoids RC `CC 44` clash) |
| Smart Lighting | `LT` | Binary: `AA 4C` / `CC 4C` |

Implement at least **GH** (binary + SIMPLE on Classic) and **WT** (SIMPLE on Classic + binary) fully; stub the others behind the application `#define` with simulated data so any app can be demoed.

### Framing reminder (shared with SPP)

- Text lines end with `\n` (CR optional, stripped).
- Binary frames start with `0xCC` (ESP32→phone) followed by a subtype byte: `0x11` panel (8 B total), `0x22` indicator (6 B), `0x33` plot / `0x47` GH DATA (length-prefixed: `u16 LE` length, payload, low-byte checksum of bytes 2…end-1). Do **not** send legacy RC `0x44` label-config packets; labels come from Android RC settings.
- Phone→ESP32 binary starts with `0xAA` (`0x55` RC control, `0x47` GH SET) or `0xBB 0x66` (RC buttons).
- A notification chunk may contain a partial frame or several frames — buffer bytes and parse incrementally, exactly like the SPP stream.

## Sketch structure

```
TeleCon_BLE.ino
```

1. `#define TELECON_APP APP_GREENHOUSE` — one of `APP_RC, APP_GREENHOUSE, APP_WATER_TANK, APP_SOLAR, APP_SMART_HOME, APP_DOOR_LOCK, APP_LIGHTING`
2. BLE setup: NUS service, advertising, connection callbacks, MTU callback
3. `onWrite(RX)` → feed bytes into the shared incremental parser (handshake lines, `SET` lines, binary SET/control)
4. Telemetry task: every 500–1000 ms (2 s is fine for WT) build either the binary DATA packet (GH) or the SIMPLE `XX:DATA,...` line, then notify in MTU-sized chunks
5. Deep-sleep hook (optional, commented): example of `esp_deep_sleep` between advertising windows for battery builds
6. `TELECON_DEBUG` macro that mirrors traffic to `Serial` at 115200

## Power-saving notes (why BLE)

- Call `esp_bt_controller_disable()` is NOT needed (no Classic); instead use `setPower(ESP_PWR_LVL_N0)` or NimBLE TX-power APIs where sensible.
- Stop advertising while a phone is connected; restart on disconnect.
- Keep telemetry ≤ 2 Hz for sensor apps; only notify when a value changed where possible.

## Acceptance criteria

- [ ] App (Connection type = **BLE + Binary** in application settings) discovers `TeleCon-BLE-GH`, connects, enables notifications
- [ ] `GH:CONNECT,proto,binary` is answered with `GH:ACK,app,GH`
- [ ] Greenhouse screen shows live binary telemetry (`CC 47`); toggles send `AA 47` SET packets that switch relays/LEDs
- [ ] With `TELECON_APP APP_WATER_TANK`: `CC 57` / `AA 57` (Binary) work end-to-end
- [ ] Frames split across notifications reassemble correctly (test with MTU 23 by skipping the MTU request)
- [ ] Same sketch compiles for ESP32 and ESP32-C3 targets in the Arduino IDE

## Do not

- Use `BluetoothSerial` / Classic SPP in this sketch
- Invent new UUIDs or a new wire format — reuse the SPP protocol bytes over NUS
- Send more than `MTU-3` bytes in one notification
- Change Android packet layouts; if something is missing, flag it instead

When done, provide a short README section (comment header in the .ino) with: required board package/library versions, how to select the application `#define`, wiring for the demo pins, and which settings to pick in the Android app (Connection type = BLE; protocol is chosen automatically).
