# Greenhouse binary protocol (`GH`)

Compact binary frames for the Greenhouse application. Used when the Android app’s **Communication protocol** is **Advanced (binary GH)** for `ApplicationId.GREENHOUSE` (premium users).

**Transport:** Classic Bluetooth SPP, UUID `00001101-0000-1000-8000-00805F9B34FB`, or BLE Nordic UART Service (`6E400001-B5A3-F393-E0A9-E50E24DCCA9E`) with the same frames. BLE sessions always use this binary protocol.  
**Coexistence:** SIMPLE text lines and binary frames share the same Classic SPP socket or BLE NUS stream. See [BINARY_PROTOCOL_APPS.md](BINARY_PROTOCOL_APPS.md) for all application binary layouts.

See also: [SIMPLE_PROTOCOL_ESP32.md](SIMPLE_PROTOCOL_ESP32.md)

---

## App byte

| App        | Byte | ASCII |
|------------|------|-------|
| Greenhouse | `0x47` | `G`   |

---

## Phone → ESP32: `GH:SET` binary

**Header:** `0xAA 0x47`

| Offset | Field      | Description |
|--------|------------|-------------|
| 0–1    | Header     | `AA 47` |
| 2–3    | Mask       | `uint16` LE — which fields follow |
| 4…     | Values     | One byte per set bit, **in bit order** |
| last   | Checksum   | Low byte of sum(bytes 2 … last−1) |

### Mask bits

| Bit | Key           | Value range |
|-----|---------------|-------------|
| 0   | `fan`         | 0 / 1 |
| 1   | `heater`      | 0 / 1 |
| 2   | `pump`        | 0 / 1 |
| 3   | `lights`      | 0 / 1 |
| 4   | `auto`        | 0 / 1 |
| 5   | `vent`        | 0–100 |
| 6   | `target_temp` | 0–255 (°C) |
| 7   | `target_hum`  | 0–255 (%) |

**Example:** `fan=1` → `AA 47 01 00 01 CS`

---

## ESP32 → Phone: `GH:DATA` binary

**Header:** `0xCC 0x47`

| Offset | Field      | Description |
|--------|------------|-------------|
| 0–1    | Header     | `CC 47` |
| 2–3    | Length     | `uint16` LE — payload + type byte length (not including checksum) |
| 4      | Type       | `0x01` = DATA |
| 5…     | Payload    | 17 bytes (see below) |
| last   | Checksum   | Low byte of sum(bytes 2 … last−1) |

### DATA payload (17 bytes, little-endian)

| Offset | Field         | Type        | Maps to SIMPLE key |
|--------|---------------|-------------|--------------------|
| 0–1    | temp × 10     | `int16`     | `temp` |
| 2      | humidity      | `uint8`     | `hum` |
| 3–4    | vpd × 100     | `uint16`    | `vpd` |
| 5      | soil          | `uint8`     | `soil` |
| 6–9    | light lux     | `uint32`    | `light` |
| 10     | vent %        | `uint8`     | `vent` |
| 11     | tank %        | `uint8`     | `tank` |
| 12     | target temp   | `uint8`     | `target_temp` |
| 13     | target hum    | `uint8`     | `target_hum` |
| 14     | flags         | `uint8`     | see below |
| 15     | delta × 10    | `int8`      | `delta` |
| 16     | last irr min  | `uint8`     | `last_irr` |

### Flags byte (offset 14)

| Bit | SIMPLE key | Meaning |
|-----|------------|---------|
| 0   | `fan`      | 1 = on |
| 1   | `heater`   | 1 = on |
| 2   | `pump`     | 1 = on |
| 3   | `lights`   | 1 = on |
| 4   | `auto`     | 1 = on |
| 5   | `stable`   | 1 = stable |
| 6   | `cam`      | 1 = ESP32-CAM module present |
| 7   | reserved   | 0 |

Set `cam=1` on firmware **with** ESP32-CAM; `cam=0` on DevKit-only boards. The Android app shows the live camera button only when `cam=1`.

---

## Firmware variants

| Variant | Bluetooth name (suggested) | `cam` flag | Wi‑Fi camera |
|---------|---------------------------|------------|--------------|
| DevKit only | `ESP32-TeleCon-GH` | 0 | No |
| DevKit + ESP32-CAM | `ESP32-TeleCon-GH-CAM` | 1 | HTTP `/capture` at `http://192.168.4.1` |

Both variants must implement **SIMPLE** and **binary** GH protocols on the same SPP socket.

---

## Android implementation

| File | Role |
|------|------|
| `GhPacketEncoder.kt` | Outbound SET |
| `GhBinaryTelemetryMapper.kt` | Inbound DATA → key map |
| `GreenhouseViewModel.kt` | Mode branch SIMPLE / ADVANCED |
| `AndroidBluetoothController.kt` | Routes `CC 47` to `messages` |

---

## Typical rates

| Direction | SIMPLE | Binary |
|-----------|--------|--------|
| Telemetry | 1–2 Hz | 2–5 Hz |
| SET       | on user action | on user action |

Binary reduces bytes per update (~25 B vs ~80+ B text).
