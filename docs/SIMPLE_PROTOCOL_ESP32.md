# TeleCon Simple Protocol — ESP32 Reference

Human-readable Bluetooth lines for TeleCon4ESP32. Use this when the app’s connection
setting is **Classic + Simple** for a given application. (BLE is binary-only; see
[BINARY_PROTOCOL_APPS.md](BINARY_PROTOCOL_APPS.md) and
[prompts/BLE_ESP32_FIRMWARE_PROMPT.md](prompts/BLE_ESP32_FIRMWARE_PROMPT.md).)

**Wire format:** `APP:TYPE,key1,value1,key2,value2,...` + newline (`\n`)

**Bluetooth:** Classic RFCOMM, UUID `00001101-0000-1000-8000-00805F9B34FB` (SPP).

---

## Application prefixes

| Prefix | Application        |
|--------|--------------------|
| `RC`   | Control Panel **and** RC Vehicle Pro (same wire protocol) |
| `GH`   | Greenhouse         |
| `WT`   | Water Tank         |
| `SH`   | Smart Home         |
| `SP`   | Solar Power        |
| `DL`   | Smart Door Lock    |
| `LT`   | Smart Lighting     |
| `CD`   | Custom Dashboard   |

---

## RC (Control Panel / RC Vehicle Pro)

Same `RC:` lines for both Android screens. Vehicle HUD maps `left` panel → speed×10,
`batt` → battery %, `analog` → motor temp gauge. Full vehicle + Wi‑Fi CAM firmware prompt:
[prompts/RC_VEHICLE_PRO_ESP32_FIRMWARE_PROMPT.md](prompts/RC_VEHICLE_PRO_ESP32_FIRMWARE_PROMPT.md).

### Handshake (required on connect)

After the RFCOMM / BLE link is up, the phone sends:

```
RC:CONNECT,proto,simple
```

or

```
RC:CONNECT,proto,binary
```

depending on **RC Settings → connection mode** (Classic Simple vs Classic/BLE Binary).

**DevKit SoftAP (Wi‑Fi Simple):** same text handshake over TCP `192.168.4.1:3333`
after joining SoftAP `ESP32-TC-RC-WiFi-Simple` / `telecon1234` (other apps use their
prefix, e.g. `ESP32-TC-GH-WiFi-Simple`):

```
RC:CONNECT,proto,simple
```

Wire lines (`RC:CTRL`, `RC:BTN`, `RC:DATA`, `RC:PLOT`) are identical to Classic Simple;
only the link is SoftAP TCP. Firmware may also ACK `proto,wifi` during transition.

**Kit A CAM SoftAP** (RC Vehicle Pro only) uses `RC:CONNECT,proto,wifi` on the same
TCP port with SoftAP SSID `TeleCon-RC-CAM` — do not mix with DevKit SoftAP SSIDs.

| ESP32 reply | Meaning |
|-------------|---------|
| `RC:ACK,app,RC` | App + protocol match — session continues |
| `RC:NAK,reason,proto_mismatch,expected,simple,actual,binary` | Wrong mode in the app (or wrong firmware build) |
| `RC:NAK,reason,app_mismatch,expected,RC,actual,GH` | Wrong application firmware |

`expected` = firmware capability; `actual` = value from the CONNECT line. The Android dialog
shows these mismatches and does **not** leave a connected session.

**Bench debug:** with auto-telemetry / simulate echo enabled, map `RC:CTRL` sticks and knobs
into `RC:DATA` / `RC:PLOT` (four series) so the Control Panel UI updates without extra wiring.
Optional Serial inject forwards typed protocol lines to the phone. Same UX as Classic/BLE
Binary — see
[prompts/CONTROL_PANEL_DEBUG_MODE_ESP32_PROMPT.md](prompts/CONTROL_PANEL_DEBUG_MODE_ESP32_PROMPT.md).

---

### Phone → ESP32

#### `RC:CTRL` — joystick / switches / knobs (sent on UI change only)

```
RC:CTRL,lx,-50,ly,100,rx,0,ry,0,lk,512,rk,256,sw,03
```

| Key | Range        | Description                          |
|-----|--------------|--------------------------------------|
| lx  | -100 … 100   | Left stick X                         |
| ly  | -100 … 100   | Left stick Y                         |
| rx  | -100 … 100   | Right stick X                        |
| ry  | -100 … 100   | Right stick Y                        |
| lk  | 0 … 1023     | Left knob                            |
| rk  | 0 … 1023     | Right knob                           |
| sw  | hex 2 chars  | Switch bitfield (S1=bit0 … S8=bit7) |

#### `RC:BTN` — momentary button

```
RC:BTN,id,1
```

| id | Button              |
|----|---------------------|
| 1  | Center top left     |
| 2  | Center top right    |
| 3  | Center bottom left  |
| 4  | Center bottom right |
| 16 | Steer center save (`0x10`) — RC Vehicle Pro trim lock |

#### `RC:SET` — steering center save (RC Vehicle Pro)

```
RC:SET,steer_center,1,rx,-3
```

| Key | Description |
|-----|-------------|
| `steer_center` | `1` = store current steering output as mechanical zero |
| `rx` | Live right-stick X (−100…100) including phone trim |

Firmware should persist the PWM/mix that corresponds to that output in NVS so later
`rx = 0` drives straight. See
`docs/prompts/RC_VEHICLE_PRO_STEER_CENTER_TRIM_ESP32_PROMPT.md`.

Phone-side **drive assist** (dual-rate, expo, reverse, deadzone) reshapes sticks
*before* `RC:CTRL` / binary TX. Firmware must not re-apply those curves. See
`docs/prompts/RC_VEHICLE_PRO_DRIVE_ASSIST_ESP32_PROMPT.md`.

---

### ESP32 → Phone

#### `RC:DATA` — panels, indicators, LEDs

```
RC:DATA,left,1234,right,5678,lo,1,ro,0,lg,1,rg,0,analog,42,batt,88,led,0F,lt,RPM,rt,Speed,at,Load,bt,Batt
```

| Key    | Description                    |
|--------|--------------------------------|
| left   | Left seven-segment value       |
| right  | Right seven-segment value      |
| lo, ro | Panel on (1/0)                 |
| lg, rg | Panel color green (1) / red (0)|
| lt, rt | Panel titles                   |
| analog | Analog gauge 0–255             |
| batt   | Battery gauge 0–255            |
| at, bt | Indicator titles               |
| led    | LED byte (hex, e.g. `0F`)      |

#### `RC:PLOT` — live plot samples (send periodically, e.g. 10–30 Hz)

```
RC:PLOT,v0,128,v1,200,v2,64,v3,180
```

| Key | Description                                      |
|-----|--------------------------------------------------|
| v0  | New sample for series 0, integer **0–255**       |
| v1  | New sample for series 1                          |
| v2  | New sample for series 2                          |
| v3  | New sample for series 3                          |
| …   | One sample per key per line; app keeps last 100 |

Values match the **binary** plot packet scale: `0` = bottom, `255` = top of the graph.

The app shows **four** channels (two traces in the top pane, two in the bottom). Colors are
fixed by index (cyan, red, green, yellow, …). **Plot / panel / indicator labels are set in
the Android RC settings** — do **not** send `RC:PLOTCFG` or binary `CC 44` config packets
from firmware.

**Typical flow:**

1. In `loop()`: every 50–100 ms send `RC:PLOT,v0,...,v3,...` with current sensor readings mapped to 0–255.
2. Optionally interleave `RC:DATA,...` for gauges and panels at a lower rate.

---

## Greenhouse (`GH`)

**ESP32 → phone:** `GH:DATA,temp,26.2,hum,68,vpd,1.1,soil,42,light,12400,fan,1,heater,0,pump,0,lights,0,vent,40,tank,78,auto,1,target_temp,24,target_hum,65,cam,0`

**Phone → ESP32:** `GH:SET,fan,1` | `GH:SET,heater,0` | `GH:SET,pump,1` | `GH:SET,lights,1` | `GH:SET,vent,60` | `GH:SET,auto,1` | `GH:SET,target_temp,24,target_hum,65` | `GH:SET,cam_pan,50,cam_tilt,50`

`cam`: `0` = no ESP32-CAM module; `1` = camera available (Wi‑Fi stream separate). Binary layout: see [BINARY_PROTOCOL_GH.md](BINARY_PROTOCOL_GH.md).

`cam_pan` / `cam_tilt`: gimbal aim on the live camera screen, **0–100** with **50 = center**. Suggested ESP32-CAM free pins: pan GPIO 13, tilt GPIO 12 (keep climate I/O on the DevKit).

Optional history: `hist_temp`, `hist_hum`, `hist_vpd` as pipe-separated floats. See Greenhouse settings in the app for pin map and field guide.

---

## Water Tank (`WT`)

**ESP32 → phone:** `WT:DATA,level,74,cap,500,pump,0,status,0` (`status`: 0=normal, 1=low, 2=critical)

**Phone → ESP32:** `WT:SET,pump,1`

---

## Solar Power (`SP`)

**ESP32 → phone:**

```
SP:DATA,solar_w,480,load_w,310,batt_w,170,grid_w,120,batt_pct,89,volt,24.8,amp,19.4,
      today_kwh,725,month_kwh,523,total_kwh,1800,cons_week_kwh,472,cons_day_kwh,68,
      prod_kwh,492,export_kwh,183,batt_used_kwh,72,
      panels,4,status,0,device,ESP32-SP01,panel_name,LinCore,
      batt_cap_kwh,2000,charge_eta_min,272,total_charge_kwh,112.9,
      hist_prod,6|14|9|8|18|11|7|16|10|9|20|13|10|22|14|7|15|9|5|12|8,
      hist_cons,12|7|9|14|8|11|16|9|12|11|7|10|15|8|11|13|6|9|10|5|7
```

For **week** view the app expects **21** sub-daily samples (3 per day × 7 days). For **today** view it expects **24** hourly samples (3 per hour × 8 blocks labelled 00:00–21:00). If the ESP32 only stores **7 daily totals** (week) or **8 block totals** (day), send those in `hist_prod` and `hist_cons`; the app expands each group into 3 sub-bars automatically.

| Key | Description |
|-----|-------------|
| `solar_w` | Solar production (W) |
| `load_w` | Home load (W) |
| `batt_w` | Battery charge/discharge (W, positive = charging) |
| `grid_w` | Grid flow (W, positive = export, negative = import) |
| `batt_pct` | Battery state of charge (0–100) |
| `volt` | DC bus voltage (V) |
| `amp` | DC bus current (A) |
| `today_kwh` | Energy generated today (kWh) |
| `month_kwh` | Energy generated this month (kWh) |
| `total_kwh` | Lifetime energy generated (kWh) |
| `cons_week_kwh` | Electricity consumed this week (kWh) |
| `cons_day_kwh` | Electricity consumed today (kWh) |
| `prod_kwh` | Energy produced in selected period (kWh) |
| `export_kwh` | Energy exported to grid (kWh) |
| `batt_used_kwh` | Energy stored/used from battery (kWh) |
| `panels` | Number of panel strings monitored |
| `status` | 0=normal, 1=fault |
| `device` | Device identifier shown in the app |
| `panel_name` | User-facing panel/inverter label |
| `batt_cap_kwh` | Battery capacity (kWh) |
| `charge_eta_min` | Estimated minutes to full charge |
| `total_charge_kwh` | Energy charged into battery in period (kWh) |
| `hist_prod` | Produced kWh per chart bar (upward blue bars). 21 values (week) or 24 (today), or 7 daily totals |
| `hist_cons` | Consumed kWh per chart bar (downward purple bars). Same length as `hist_prod` |
| `hist_cons_home` | Optional: home-use portion per bar (summed with batt/grid if `hist_cons` omitted) |
| `hist_cons_batt` | Optional: battery-use portion per bar |
| `hist_cons_grid` | Optional: grid-use portion per bar |
| `inverter` | 1=inverter on, 0=off |
| `grid_mode` | 0=idle, 1=export, 2=import |
| `panel_eff` | Panel efficiency percent |
| `fault` | Fault code (0=none) |
| `to_home_kwh` | Energy routed to home load |
| `to_batt_kwh` | Energy routed to battery |
| `to_grid_kwh` | Energy routed to grid |
| `max_solar_w` | Peak array watts for UI background scaling |
| `low_batt_pct` | Low-battery warning threshold |

Send `SP:DATA` at ~1–2 Hz for live watts and every 30–60 s for cumulative kWh counters. History keys can be sent less often or after a refresh request.

**Phone → ESP32:**

| Command | Description |
|---------|-------------|
| `SP:SET,refresh,1` | Ask ESP32 to resend counters and history |
| `SP:SET,period,1` | Select chart period (`1` = today, `7` = week) |
| `SP:SET,inverter,1` | Turn inverter on |
| `SP:SET,inverter,0` | Turn inverter off |
| `SP:SET,reset_day,1` | Reset daily kWh counters in ESP32 NVS |

Typical ESP32 inputs: INA219/ACS712 for current, voltage divider for bus voltage, pulse counter or inverter Modbus/serial for kWh totals. Store daily/monthly counters in NVS and reset `today_kwh` at midnight.

---

## Arduino / ESP32 example (RC + plots)

```cpp
#include "BluetoothSerial.h"

BluetoothSerial SerialBT;

// Map a sensor reading to 0–255 for the center plot
int toPlotByte(float value, float minVal, float maxVal) {
  if (maxVal <= minVal) return 0;
  float t = (value - minVal) / (maxVal - minVal);
  t = constrain(t, 0.f, 1.f);
  return (int)(t * 255.f + 0.5f);
}

void sendPlotSample(int v0, int v1, int v2, int v3) {
  SerialBT.printf("RC:PLOT,v0,%d,v1,%d,v2,%d,v3,%d\n", v0, v1, v2, v3);
}

void sendRcData(int analogVal, int battVal) {
  SerialBT.printf("RC:DATA,analog,%d,batt,%d\n", analogVal, battVal);
}

void handleLine(const String& line) {
  if (line.startsWith("RC:CTRL,")) {
    // Parse lx, ly, rx, ry, lk, rk, sw from comma pairs
    // Example: move motors from stick values
  }
  if (line.startsWith("RC:BTN,")) {
    // Parse id — trigger one-shot actions
  }
  if (line.startsWith("GH:SET,")) { /* optional multi-app firmware */ }
}

unsigned long lastPlotMs = 0;
unsigned long lastDataMs = 0;

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32-BT");
  delay(500);
  // Plot labels are configured in the Android RC settings screen.
}

void loop() {
  if (SerialBT.available()) {
  String line = SerialBT.readStringUntil('\n');
    line.trim();
    if (line.length() > 0) handleLine(line);
  }

  unsigned long now = millis();

  // Plot updates ~20 Hz — four channels for the dual-pane center graph
  if (now - lastPlotMs >= 50) {
    lastPlotMs = now;
    float a = analogRead(34);
    float b = analogRead(35);
    float c = analogRead(32);
    float d = analogRead(33);
    sendPlotSample(
      toPlotByte(a, 0, 4095),
      toPlotByte(b, 0, 4095),
      toPlotByte(c, 0, 4095),
      toPlotByte(d, 0, 4095));
  }

  // Slower telemetry ~2 Hz
  if (now - lastDataMs >= 500) {
    lastDataMs = now;
    sendRcData(analogRead(32) / 16, 85);  // example analog + battery %
  }
}
```

---

## Coexistence with binary protocol

If the Control Panel uses **Advanced (binary)**, keep using `0xAA`/`0xCC` packets for RC. Simple lines (`RC:`, `GH:`, …) and binary frames can share one socket; branch on the first byte (`0xCC` vs ASCII letter).

---

## Boolean values

Accepted as `1`/`0`, `true`/`false`, `on`/`off`, or any non-zero integer.
