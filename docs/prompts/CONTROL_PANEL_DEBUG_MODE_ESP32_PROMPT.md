# Cursor prompt: TeleCon_ControlPanel — unify bench debug (Classic = BLE)

Copy everything below into a **new Cursor chat** (Agent mode) on
`/home/miguel/Documents/ESP32_Projects/TeleCon_ControlPanel/`.

Android (`TeleCon4ESP32`) Control Panel UI and tutorials already treat Classic Simple,
Classic Binary, and BLE Binary the same for bench testing: move sticks/knobs → see
panels / gauges / LEDs / four plot traces; optional Serial Monitor inject. **Firmware
must match that UX.** BLE already does. Classic Simple is close. Classic Binary still
uses the old `TELEMETRY_DEBUG_MODE` Serial-prompt path and must be brought to parity.

---

## Goal

Make **bench / simulate debug** identical across:

| Variant | Path |
|---------|------|
| Classic + Simple | `ESP32_noCam/Classic_Simple/ESP32_BT_Controller_sp/` |
| Classic + Binary | `ESP32_noCam/Classic_Binary/ESP32_BT_Controller/` |
| BLE + Binary (reference) | `ESP32_noCam/BLE/TeleCon_ControlPanel_BLE_binary_ESP32/` |

Reference implementation: BLE `TeleConConfig.h` + `ControlPanelHandlers.cpp` +
`ControlPanelControl.cpp` when `TELECON_USE_HW_IO = 0`.

---

## Android contract (already done — do not change Android for this)

- Tutorials (`tutorial_rc_*_telemetry_test_description`) describe one bench flow for all three modes.
- Plot labels come from RC Settings only (no `RC:PLOTCFG` / `CC 44`).
- Plot packets: **four** samples (`v0`…`v3` / `CC 33` count=4).
- Handshake: `RC:CONNECT` → `RC:ACK` / `RC:NAK` (see `CONTROL_PANEL_HANDSHAKE_ESP32_PROMPT.md`).
- Control Panel screen telemetry UI is the same regardless of Simple vs Binary vs BLE.

---

## Target behavior (parity checklist)

### 1. Simulate / echo mode (default for bench)

When hardware I/O is off (or Classic Simple auto-telemetry is on):

| Phone control | Telemetry out |
|---------------|---------------|
| Left stick X | Left panel (0–9999) |
| Right stick X | Right panel (0–9999) |
| Left / right stick activity | Panel numeric values |
| Left knob | Analog gauge + plot `v0` |
| Right knob | Battery gauge + plot `v2` |
| Left stick X | Plot `v1` |
| Right stick Y | Plot `v3` |
| Switch byte | LED mask (`CC 22` / `RC:DATA` `led`) |
| Action buttons | Control events only (panel on/color are app settings) |

Send rates (match BLE):

- Panel / indicator: ~500 ms (or on change for panel)
- Plot: ~50 ms (~20 Hz), **exactly 4** samples

### 2. Serial inject (optional, default on for debug builds)

`TELECON_SERIAL_INJECT = 1`: lines typed in Serial Monitor @ 115200 are forwarded toward
the phone (Classic SPP or BLE NUS), same idea as BLE `TeleConBle` inject path.

Classic Simple already documents:

```text
RC:DATA,left,1200,right,3400,analog,42,batt,88,led,0F
RC:PLOT,v0,128,v1,200,v2,64,v3,180
```

Classic Binary should accept either:

- Hex / raw binary frames for `CC 11` / `CC 22` / `CC 33`, **or**
- Simple text lines that the sketch encodes to binary before TX (prefer text for tutorial parity)

### 3. Hardware mode (optional, off by default)

`TELECON_USE_HW_IO = 1`: read ADC/GPIO for telemetry and drive outputs (Classic Binary pin map).
Bench users leave this at **0**.

---

## Work by sketch

### A. BLE — reference only

Already correct. Do not regress:

- `TELECON_USE_HW_IO` (default 0)
- `TELECON_DEBUG` / `TELECON_SERIAL_INJECT`
- Echo mapping in `ControlPanelHandlers.cpp` (`readPanel*` / `readPlot*` when `!TELECON_USE_HW_IO`)
- Banner explains: App Settings = **BLE Binary**

### B. Classic Simple — align naming + docs

Already echoes via `teleconTelemetryOnRcCtrl` when `TELECON_AUTO_TELEMETRY = 1`.

Tighten to BLE parity:

1. Prefer documenting / optionally aliasing:
   - `TELECON_AUTO_TELEMETRY` ≈ simulate echo (keep flag if renaming would break docs)
   - Ensure mapping matches BLE table above (panels, gauges, LEDs, four plots).
2. Keep `TELECON_SERIAL_INJECT` and boot banner with example `RC:DATA` / `RC:PLOT` lines.
3. On button (`RC:BTN`), keep sending control events only (panel on/color are app settings).
4. Banner: App Settings = **Classic Simple** (not BLE / not Binary).

### C. Classic Binary — main work

Replace reliance on `debug_config.h` `TELEMETRY_DEBUG_MODE` (`DBG_PANEL` /
`DBG_INDICATOR` / `DBG_PLOT` exclusive Serial prompts) for everyday Control Panel testing.

1. Add BLE-style flags (in a shared config header, e.g. `TeleConConfig.h` or evolve `debug_config.h`):

```c
#define TELECON_DEBUG 1
#define TELECON_USE_HW_IO 0          // 0 = echo sticks → CC 11/22/33
#define TELECON_SERIAL_INJECT 1      // Serial Monitor → phone
#define TELECON_PROTO_WIRE "binary"
```

2. Store last `AA 55` control state; when `TELECON_USE_HW_IO == 0`, feed `CC 11` /
   `CC 22` / `CC 33` from that state using the **same mapping as BLE**.
3. Keep `TELEMETRY_DEBUG_MODE` only if needed for advanced Serial-only tests, or remove
   it and route Serial inject through one path (prefer remove / deprecate).
4. Boot banner: how to use echo + inject; App Settings = **Classic Binary**.
5. Do **not** send `CC 44` plot-label config (labels are Android-only).

Mirror structure from BLE where practical:

- `ControlPanelControl` — hold last RC state / apply HW when enabled
- `ControlPanelHandlers` — CONNECT + telemetry send-if-due
- Binary TX over Classic SPP instead of NUS notify

---

## Do not

- Do not require exclusive `DBG_PANEL` vs `DBG_INDICATOR` vs `DBG_PLOT` for basic bench.
- Do not send plot config packets (`CC 44` / `RC:PLOTCFG`).
- Do not change wire formats Android already parses.
- Do not make Classic firmware speak BLE GATT or vice versa.

---

## Quick test plan

1. Flash Classic Binary with `TELECON_USE_HW_IO 0`, app = Classic Binary → handshake ACK.
2. Move left/right sticks → seven-seg panels update.
3. Turn knobs → analog + battery gauges update; LEDs follow switches.
4. Center plot shows **four** live traces.
5. With inject on, type a panel/plot line (or hex) in Serial → phone updates without touching sticks.
6. Repeat with Classic Simple and BLE Binary (same phone gestures, matching Connection type).
7. Set `TELECON_USE_HW_IO 1` only when real ADC/GPIO are wired.

---

## Related prompts

- `CONTROL_PANEL_HANDSHAKE_ESP32_PROMPT.md`
- `CONTROL_PANEL_FOUR_PLOTS_ESP32_PROMPT.md`
- `CONTROL_PANEL_REMOVE_TELEMETRY_CONFIG_PROMPT.md`
- `CONTROL_PANEL_TRANSPORT_MISMATCH_ESP32_PROMPT.md`
