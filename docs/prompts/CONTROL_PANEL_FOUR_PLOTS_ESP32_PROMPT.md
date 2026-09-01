# Cursor prompt: TeleCon_ControlPanel — four plot channels (Classic Simple / Binary / BLE)

**Superseded for analog count:** Android now accepts **8** numbered analog channels (CH1–CH8).
Use [CONTROL_PANEL_EIGHT_ANALOG_CHANNELS_ESP32_PROMPT.md](CONTROL_PANEL_EIGHT_ANALOG_CHANNELS_ESP32_PROMPT.md)
for new firmware work. This prompt remains valid as a 4-sample baseline.

Copy everything below into a **new Cursor chat** (Agent mode) on the ESP32 firmware tree
under `/home/miguel/Documents/ESP32_Projects/TeleCon_ControlPanel/`.

The Android app (`TeleCon4ESP32`) already shows **four** center-graph channels:

| Index | Wire key / byte | Center pane |
|-------|-----------------|-------------|
| 0 | `v0` / first sample | Top |
| 1 | `v1` / second sample | Top |
| 2 | `v2` / third sample | Bottom |
| 3 | `v3` / fourth sample | Bottom |

Labels are set only in **Android RC Controller Settings → Plot labels**. Do **not** send
`RC:PLOTCFG` or binary `CC 44` label-config packets.

---

## Goal

Update **all three** Control Panel firmwares so each plot update carries **exactly 4**
samples (0–255), at ~10–20 Hz:

| Variant | Path |
|---------|------|
| Classic + Simple | `ESP32_noCam/Classic_Simple/ESP32_BT_Controller_sp/` |
| Classic + Binary | `ESP32_noCam/Classic_Binary/ESP32_BT_Controller/` |
| BLE + Binary | `ESP32_noCam/BLE/TeleCon_ControlPanel_BLE_binary_ESP32/` |

Keep panel / indicator / control / button traffic unchanged.

---

## Protocol contract (Android source of truth)

Read in the Android repo:

- `docs/SIMPLE_PROTOCOL_ESP32.md` — `RC:PLOT,v0,…,v3,…`
- `docs/BINARY_PROTOCOL_APPS.md` — RC legacy `CC 33` plot frames
- `app/.../domain/bluetooth/SimpleProtocolTelemetryMapper.kt` — `MAX_PLOT_SERIES = 4`
- `app/.../ui/control_panel/CenterDisplay.kt` — top = series 0–1, bottom = series 2–3

### Classic Simple (text)

```
RC:PLOT,v0,128,v1,200,v2,64,v3,180
```

Each `vN` is an integer **0–255**. Send all four keys every plot tick when possible.

### Classic Binary & BLE Binary (same frame)

```
CC 33 | length u16 LE | count=4 | b0 | b1 | b2 | b3 | checksum
```

- `length` = `1 + count` = `5` (count byte + 4 samples)
- Each `bN` is `uint8` 0–255
- Checksum = low byte of sum of bytes from index 2 through last payload byte (same as today)
- BLE: identical bytes over Nordic UART TX notify (same wire format as Classic Binary)

Do **not** send `CC 44` config / label packets.

---

## Concrete code changes

### 1) Classic Simple — `ESP32_BT_Controller_sp`

- In `TeleConPlot.cpp`, change `kPlotSeriesCount` from `3` to `4`.
- Produce a fourth sample (map another control/sensor, e.g. right stick Y or a spare ADC).
- `sendRcPlot(values, 4)` must emit `v0`…`v3`.
- Update Serial debug banner examples to include `v3`.
- Keep `TELECON_AUTO_PLOT`; still **no** `RC:PLOTCFG` on connect.

### 2) Classic Binary — `ESP32_BT_Controller`

- Add `getPlot4()` in `telemetry_source.h` / `.cpp` (mirror `getPlot1`–`3`, including `DBG_PLOT` Serial test path that reads **four** values).
- In `telemetry.cpp` plot send block: set `count = 4` and append `v1,v2,v3,v4` (four bytes).
- Ensure buffer size still fits (16 B is enough for header + len + count + 4 + checksum).

### 3) BLE Binary — `TeleCon_ControlPanel_BLE_binary_ESP32`

- Change `sendPlotPacket(uint8_t v1, uint8_t v2, uint8_t v3)` → four arguments (or array of 4).
- Set payload `count = 4` and write four sample bytes.
- Update `controlPanelSendTelemetryIfDue()` call site (`readPlot1`…`readPlot4`).
- Add `readPlot4()` next to existing plot readers (demo/sim is fine).
- Keep handshake / `AA 55` / `BB 66` / `CC 11` / `CC 22` unchanged.

---

## Suggested sample mapping (demo firmwares)

Reuse stick/knob state so all four traces move without extra sensors:

| Channel | Example source |
|---------|----------------|
| 0 | Left knob → 0–255 |
| 1 | Left stick X → 0–255 |
| 2 | Right knob → 0–255 |
| 3 | Right stick Y (or X) → 0–255 |

Real projects should replace these with sensor readings scaled to 0–255.

---

## Acceptance checklist

- [ ] Simple: each auto plot line includes `v0`…`v3`
- [ ] Classic Binary: `CC 33` frames have `count == 4` and four sample bytes
- [ ] BLE Binary: same `CC 33` layout over NUS notify
- [ ] No `RC:PLOTCFG` / `CC 44` on connect
- [ ] On phone RC screen: top pane shows traces 1–2, bottom pane 3–4; Plot labels from settings apply to all four
- [ ] `DBG_PLOT` / Serial inject paths (if present) accept four values

---

## Out of scope

- Do not change Greenhouse or other apps.
- Do not remove Android parsers for legacy 3-sample frames (app still accepts `count < 4`).
- Do not reintroduce firmware-driven legend names.
