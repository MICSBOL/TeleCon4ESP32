# Cursor prompt: TeleCon_ControlPanel — eight analog channels (CH1–CH8)

Copy everything below into a **new Cursor chat** (Agent mode) on the ESP32 firmware tree
under `/home/miguel/Documents/ESP32_Projects/TeleCon_ControlPanel/`.

The Android app (`TeleCon4ESP32`) now treats `RC:PLOT` / `CC 33` samples as numbered analog
sources **CH1–CH8** (`v0`…`v7`). The Control Panel still has **four plot widgets**; extra
channels are map sources (bind them to a plot, radar, or gauge). Analog / battery on
`RC:DATA` / `CC 22` are unchanged.

**Four-sample sketches keep working.** This change is additive: send 8 samples when you can;
unused extras should be `0`.

Do **not** send `RC:PLOTCFG` or binary `CC 44`. Do **not** add `RC:CH` / `RC:RADAR` packets.
Do **not** change control (`RC:CTRL` / `AA 55`) or telemetry rates (plots ~20 Hz, `RC:DATA` ~2 Hz).

---

## Goal

Update Control Panel firmware so each plot tick can carry **8** analog samples (0–255):

| Index | Wire key / byte | Android label |
|-------|-----------------|---------------|
| 0 | `v0` / first sample | CH1 |
| 1 | `v1` | CH2 |
| 2 | `v2` | CH3 |
| 3 | `v3` | CH4 |
| 4 | `v4` | CH5 |
| 5 | `v5` | CH6 |
| 6 | `v6` | CH7 |
| 7 | `v7` | CH8 |

Canonical I/O lives in `common/` and is **copied** into each sketch. Change `common/` first,
then the copies, then Simple `snprintf` / Binary `sendPlotPacket` call sites.

| Variant | Path |
|---------|------|
| Shared I/O | `common/TeleConUserIo.cpp` / `.h` / `TeleConUserPins.h` |
| Classic + Simple | `ESP32_noCam/Classic_Simple/ESP32_BT_Controller_sp_starter/` |
| Classic + Binary | `ESP32_noCam/Classic_Binary/ESP32_BT_Controller/` |
| BLE + Binary | `ESP32_noCam/BLE/TeleCon_ControlPanel_BLE_binary_ESP32/` |
| Wi‑Fi Simple | `ESP32_noCam/WiFi_Simple/ESP32_WiFi_Controller_sp_starter/` |
| Wi‑Fi Binary | `ESP32_noCam/WiFi_Binary/TeleCon_ControlPanel_WiFi_binary_ESP32/` |
| CAM Wi‑Fi Simple | `ESP32_cam/WiFi/TeleCon_ControlPanel_CAM_WiFi_Simple/` |
| CAM Wi‑Fi Binary | `ESP32_cam/WiFi/TeleCon_ControlPanel_CAM_WiFi_Binary/` |
| CAM pins | `common/TeleConUserPins_CAM.h` (+ copies) |

Keep panel / indicator / control / button traffic unchanged. **RC Vehicle Pro is out of scope**
(it can keep four-sample `CC 33`).

---

## Protocol contract (Android source of truth)

Read in the Android repo:

- `docs/SIMPLE_PROTOCOL_ESP32.md` — `RC:PLOT,v0,…,v7,…`
- `docs/BINARY_PROTOCOL_APPS.md` — `CC 33` count = 4 **or** 8
- `app/.../domain/model/UserSettings.kt` — `ANALOG_CHANNEL_COUNT = 8`

### Classic / Wi‑Fi Simple (text)

```
RC:PLOT,v0,128,v1,200,v2,64,v3,180,v4,0,v5,0,v6,0,v7,0
```

Each `vN` is an integer **0–255**. Send all eight keys every plot tick when possible.

**Buffer size:** existing `char plot[64]` is too tight for eight 3-digit values. Use at least
`char plot[96]` (128 is safer).

### Classic / BLE / Wi‑Fi Binary (same frame)

```
CC 33 | length u16 LE | count=8 | b0 … b7 | checksum
```

- `length` = `1 + count` = `9` (count byte + 8 samples)
- Each `bN` is `uint8` 0–255
- Checksum = low byte of sum of bytes from index 2 through last payload byte (same as today)
- Frame size ≈ 14 bytes (`CC 33` + len + count + 8 + checksum). Fits today’s 16-byte plot
  buffers **and** BLE NUS 20-byte notify. Do **not** split the plot packet.

Do **not** send `CC 44` config / label packets.

---

## Concrete code changes

### 1) Shared I/O — `common/TeleConUserIo.*` and pin headers

- Change `teleconReadPlotSamples(uint8_t out[4], …)` → `out[8]` (or `uint8_t* out, size_t n`
  with `n == 8`).
- Fill CH5–CH8 with `0` when unused.
- Optional: add `PIN_PLOT4_ADC` … `PIN_PLOT7_ADC` in `TeleConUserPins.h` /
  `TeleConUserPins_CAM.h` (default `PIN_NONE`), attach them in `teleconUserIoBegin()`, and
  map them in `teleconReadPlotSamples` the same way as PLOT0–3.
- Suggested bench echo when pins are `PIN_NONE` (demo only; real projects use sensors):

  | CH | Example source |
  |----|----------------|
  | 1 | Left knob → 0–255 (or radar angle if I2C range is live) |
  | 2 | Left stick X (or radar range) |
  | 3 | Right knob |
  | 4 | Right stick Y |
  | 5 | Left stick Y |
  | 6 | Right stick X |
  | 7 | `0` (or a spare ADC) |
  | 8 | `0` (or a spare ADC) |

- Copy the same I/O changes into **every** sketch-local `TeleConUserIo.cpp` / `.h` /
  `TeleConUserPins.h` (they are duplicates of `common/`).

### 2) Simple sketches (`snprintf` / `sendLine`)

- `uint8_t v[8]; teleconReadPlotSamples(v, echo);`
- Emit `v0`…`v7`. Grow the plot line buffer as noted above.
- Serial banners / inject examples: show the eight-key `RC:PLOT` line. Inject may still
  forward a 4-key line typed by the user (phone accepts it).

### 3) Binary sketches (`sendPlotPacket`)

- Change `sendPlotPacket(uint8_t v0..v3)` → eight arguments **or** `const uint8_t v[8]`.
- Set `count = 8` and write eight sample bytes. Confirm local `buf[16]` (or similar) still
  fits (~14 bytes).
- Classic Binary `telemetry.cpp` / `telemetry_source`: same count=8 path, including
  `DBG_PLOT` / Serial inject if present.
- BLE: keep handshake / `AA 55` / `BB 66` / `CC 11` / `CC 22` unchanged. One plot notify
  per tick.

---

## Acceptance checklist

- [ ] Simple: each auto plot line includes `v0`…`v7`
- [ ] Classic / Wi‑Fi / BLE Binary: `CC 33` frames have `count == 8` and eight sample bytes
- [ ] BLE frame still fits one NUS notify (no extra 20 Hz `CC 11`/`CC 22`)
- [ ] No `RC:PLOTCFG` / `CC 44` on connect
- [ ] Four-sample inject / old lines still parse on the phone (do not break that)
- [ ] Unused extras are `0`, not omitted randomly (prefer a stable 8-key / count=8 layout)
- [ ] On the phone: Panel settings → Telemetry channels lists CH1–CH8; plot widgets still
      default to CH1–CH4; binding Plot 1 to CH8 shows the eighth sample

---

## Out of scope

- Do not change Greenhouse or RC Vehicle Pro.
- Do not require firmware for the Android app to run (4-sample devices stay valid).
- Do not channelize control sticks/knobs/switches.
- Do not raise `RC:DATA` / `CC 11`/`CC 22` to plot rate.
