# Cursor prompt: Remove RC telemetry label-config from TeleCon_ControlPanel ESP32

Copy everything below into a **new Cursor chat** (Agent mode) opened on the ESP32
firmware tree. The Android app (`TeleCon4ESP32`) already uses **RC Controller Settings**
for plot / panel / indicator labels — firmware must stop pushing those labels over Bluetooth.

---

## Goal

In **`TeleCon_ControlPanel`**, delete all logic that **sends telemetry configuration /
label packets** after Bluetooth connect, for both Classic firmwares:

| Variant | Path |
|---------|------|
| Classic + Binary | `ESP32_noCam/Classic_Binary/ESP32_BT_Controller/` |
| Classic + Simple | `ESP32_noCam/Classic_Simple/ESP32_BT_Controller_sp/` |

Keep live telemetry (panels, indicators, plot **values**) unchanged.

---

## Why

Labels are configured on the phone:

- RC Settings → **Display labels** (panels / indicators)
- RC Settings → **Plot labels** (center graph legend)

Firmware must only stream numeric telemetry. Sending names from the ESP32 is redundant and
overrides the user’s app settings.

---

## What to remove

### Classic Binary (`ESP32_BT_Controller`)

1. Delete **`sendConfigTelemetry()`** entirely (builds `0xCC 0x44` with sections
   `0x01` plot names, `0x02` panel names, `0x03` indicator names).
2. Remove its declaration from `telemetry.h`.
3. In `sendTelemetryIfDue()`:
   - Remove `configSent` and the “send config once after connection” block.
   - Still return early when `!SerialBT.hasClient()`.
4. Keep sending:
   - `CC 11` panel
   - `CC 22` indicator
   - `CC 33` plot samples

Do **not** send `CC 44` anymore.

### Classic Simple (`ESP32_BT_Controller_sp`)

1. Stop sending **`RC:PLOTCFG,...`** on connect (e.g. remove `sendPlotConfig()` /
   `sendRcPlotConfig(...)` and any call from `teleconPlotTick()` when BT becomes connected).
2. Remove unused helpers / caches that only exist for PLOTCFG.
3. Update debug banners / `TeleConConfig.h` comments so they no longer mention PLOTCFG on connect.
4. Keep sending:
   - `RC:DATA,...`
   - `RC:PLOT,v0,...` (and `TELECON_AUTO_PLOT` plot samples)

Do **not** document or auto-send `RC:PLOTCFG` from firmware.

---

## Protocol contract (Android repo — source of truth)

Read:

- `docs/SIMPLE_PROTOCOL_ESP32.md` — RC section: plot **samples** only; labels are app-side
- `docs/BINARY_PROTOCOL_APPS.md` — RC legacy layouts are `AA 55`, `CC 11/22/33` (no `CC 44`)
- Android still **accepts** old `RC:PLOTCFG` / `CC 44` if an old sketch sends them, but new
  firmware must not send them.

---

## Acceptance checklist

- [ ] After connect, Serial Monitor does **not** print `Telemetry config sent...`
- [ ] Binary: no `CC 44` bytes on the wire; `CC 11` / `CC 22` / `CC 33` still flow
- [ ] Simple: no `RC:PLOTCFG` line on connect; `RC:PLOT` / `RC:DATA` still flow
- [ ] In the app, plot/panel/indicator titles follow **RC Controller Settings**
- [ ] No leftover unused functions or `#include`s from the removed config path

---

## Out of scope

- Do not change motor/control parsing (`RC:CTRL`, `RC:BTN`, `AA 55`, button packets).
- Do not remove Android parsers for legacy config (backward compatibility).
- Do not touch Greenhouse / other app firmwares.
