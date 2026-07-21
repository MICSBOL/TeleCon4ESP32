# TeleCon binary protocols (all applications)

Compact binary frames for Classic SPP and BLE (Nordic UART). Used when the Android app
selects **Classic + Binary** or **BLE + Binary**. (**Classic + Simple** uses the text-line
protocol in [SIMPLE_PROTOCOL_ESP32.md](SIMPLE_PROTOCOL_ESP32.md) instead; BLE is binary-only.)

**Shared framing** (see `AppBinaryFrame.kt`):

| Direction | Layout |
|-----------|--------|
| Phone → ESP32 SET | `AA <app> [mask u16 LE] [values…] [checksum]` |
| ESP32 → Phone DATA | `CC <app> [length u16 LE] [type=0x01] [payload…] [checksum]` |

Checksum = low byte of sum of all bytes after the 2-byte header.

RC Control Panel keeps its legacy layouts (`AA 55`, `CC 11/22/33/44`) — see existing RC firmware.

---

## App bytes

| App | Prefix | App byte | Notes |
|-----|--------|----------|-------|
| Greenhouse | `GH` | `0x47` (`G`) | See [BINARY_PROTOCOL_GH.md](BINARY_PROTOCOL_GH.md) |
| Water Tank | `WT` | `0x57` (`W`) | |
| Solar Power | `SP` | `0x53` (`S`) | Live fields only; `hist_*` stays SIMPLE |
| Smart Home | `SH` | `0x48` (`H`) | |
| Smart Door Lock | `DL` | `0x4B` (`K`) | Avoids clash with RC config `CC 44` |
| Smart Lighting | `LT` | `0x4C` (`L`) | SIMPLE prefix is `LT` (not `SL`) |

---

## Water Tank (`WT`)

**SET mask:** bit0 = `pump` (0/1)

**DATA payload (5 B):** `level u8`, `cap u16 LE`, `pump u8`, `status u8`

---

## Solar Power (`SP`)

**SET mask:** bit0 `refresh`, bit1 `period`, bit2 `inverter`, bit3 `reset_day`

**DATA payload (50 B, LE):** live watts, SOC, volt×10, amp×10, kWh×10 counters, panels, status,
inverter, grid_mode, panel_eff, fault, battery info, distribution kWh×10. Chart history is
SIMPLE-only (`hist_prod` / `hist_cons`).

---

## Smart Home (`SH`)

**SET mask:** bit0 `refresh`; bit1 `room_id u8` + `device_id u8` + `state u8`

**DATA payload (12 B):** climate_temp, climate_status, energy×10, security_status, water_l,
status, living_on, kitchen_on, bedroom_on, garage_on

---

## Smart Door Lock (`DL`)

**SET mask bits:** unlock, lock, pulse, mic, spk, cam, call

**DATA payload (8 B):** lock, relay (0=LOW/1=HIGH), door, mic, spk, cam, sig (i8), online

---

## Smart Lighting (`LT`)

**SET mask:** `all`, `zone_id`+`state`, `auto_away`, `motion`, `sunset`

**DATA payload (5 B):** on_count, total, flags (auto_away|motion|sunset), zones u16 bitfield

---

## Android files

| Area | Path |
|------|------|
| Shared frame helper | `domain/bluetooth/AppBinaryFrame.kt` |
| Per-app codecs | `domain/bluetooth/{wt,sp,sh,dl,lt}/` |
| Inbound routing | `data/bluetooth/AndroidBluetoothController.kt` |
| Frame assembly | `data/bluetooth/BluetoothFrameAssembler.kt` |
