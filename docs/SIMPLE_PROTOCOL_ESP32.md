# TeleCon Simple Protocol — ESP32 Reference

Human-readable Bluetooth lines for TeleCon4ESP32. Use this when the app’s connection
setting is **Classic + Simple** for a given application. (BLE is binary-only; see
[BINARY_PROTOCOL_APPS.md](BINARY_PROTOCOL_APPS.md).)

**Wire format:** `APP:TYPE,key1,value1,key2,value2,...` + newline (`\n`)

**Bluetooth:** Classic RFCOMM, UUID `00001101-0000-1000-8000-00805F9B34FB` (SPP).

---

## Application prefixes

| Prefix | Application        |
|--------|--------------------|
| `RC`   | Control Panel **and** RC Vehicle Pro (same wire protocol) |

---

## RC (Control Panel / RC Vehicle Pro)

Same `RC:` lines for both Android screens. Vehicle HUD maps `left` panel → speed×10,
`batt` → battery %, `analog` → motor temp gauge.

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

**Role B CAM SoftAP (Wi‑Fi Simple, RC Vehicle Pro only):** same text handshake over TCP `192.168.4.1:3333`
after joining SoftAP `TeleCon-RC-CAM-Starter` / `telecon1234`:

```
RC:CONNECT,proto,simple
```

Wire lines (`RC:CTRL`, `RC:BTN`, `RC:DATA`, `RC:PLOT`) are identical to Classic Simple;
only the link is SoftAP TCP. Firmware may also ACK `proto,wifi` during transition.

DevKit SoftAP Simple is withdrawn. No-camera DevKit Wi‑Fi is **Binary only**
(`ESP32-TC-RC-WiFi-Binary` — see [BINARY_PROTOCOL_APPS.md](BINARY_PROTOCOL_APPS.md)).

**Legacy Kit A CAM SoftAP** uses `RC:CONNECT,proto,wifi` on the same TCP port with
SoftAP SSID `TeleCon-RC-CAM` — do not mix with DevKit SoftAP SSIDs.

| ESP32 reply | Meaning |
|-------------|---------|
| `RC:ACK,app,RC` | App + protocol match — session continues |
| `RC:NAK,reason,proto_mismatch,expected,simple,actual,binary` | Wrong mode in the app (or wrong firmware build) |
| `RC:NAK,reason,app_mismatch,expected,RC,actual,XX` | Wrong application firmware |

`expected` = firmware capability; `actual` = value from the CONNECT line. The Android dialog
shows these mismatches and does **not** leave a connected session.

**Bench debug:** with auto-telemetry / simulate echo enabled, map `RC:CTRL` sticks and knobs
into `RC:DATA` / `RC:PLOT` (CH1…CH8 analog samples; four-sample echo is still fine) so the Control Panel UI updates without extra wiring.
Optional Serial inject forwards typed protocol lines to the phone. Same UX as Classic/BLE Binary.

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
`rx = 0` drives straight.

Phone-side **drive assist** (dual-rate, expo, reverse, deadzone) reshapes sticks
*before* `RC:CTRL` / binary TX. Firmware must not re-apply those curves.

---

### ESP32 → Phone

#### `RC:DATA` — panels, indicators, LEDs

```
RC:DATA,left,1234,right,5678,analog,42,batt,88,led,0F
```

| Key    | Description                    |
|--------|--------------------------------|
| left   | Left seven-segment value       |
| right  | Right seven-segment value      |
| analog | Analog gauge 0–255             |
| batt   | Battery gauge 0–255            |
| led    | LED byte (hex, e.g. `0F`)      |

Panel **on/off** and **green/red** color are configured in the Android Panel settings screen, not in firmware. Optional legacy keys (`lo`, `ro`, `lg`, `rg`, `lt`, `rt`, `at`, `bt`) are ignored for appearance.

**Android channel map:** the phone can retarget these fields (and `RC:PLOT` `v0`…`v7` = CH1…CH8) onto plots, radar, gauges, panels, or LEDs. Firmware keeps sending the same keys; do **not** add `RC:RADAR` / `RC:CH` packets. Four-sample sketches (`v0`…`v3`) remain valid; unused CH5–CH8 stay at 0 on the phone.

#### `RC:PLOT` — live analog samples (send periodically, e.g. 10–30 Hz)

```
RC:PLOT,v0,128,v1,200,v2,64,v3,180,v4,0,v5,0,v6,0,v7,0
```

| Key | Phone label | Description |
|-----|-------------|-------------|
| v0  | CH1 | Sample 0, integer **0–255** |
| v1  | CH2 | Sample 1 |
| v2  | CH3 | Sample 2 |
| v3  | CH4 | Sample 3 |
| v4  | CH5 | Sample 4 (optional; send `0` if unused) |
| v5  | CH6 | Sample 5 (optional) |
| v6  | CH7 | Sample 6 (optional) |
| v7  | CH8 | Sample 7 (optional) |
| …   | | One sample per key per line; app keeps last 100 |

Values match the **binary** plot packet scale: `0` = bottom, `255` = top of the graph.

The Control Panel shows **four plot widgets** (two traces in the top pane, two in the bottom). Those widgets default to CH1–CH4; CH5–CH8 are extra analog sources in the channel map (bind them to a plot, radar, or gauge). Colors are fixed by analog index (cyan, red, green, yellow, magenta, white, orange, periwinkle). **Plot / panel / indicator labels and the telemetry channel map are set in the Android RC settings** — do **not** send `RC:PLOTCFG` or binary `CC 44` config packets from firmware. Radar still uses analog samples (defaults CH1 / CH2); the user can retarget those in the channel map.

**Typical flow:**

1. In `loop()`: every 50–100 ms send `RC:PLOT,v0,...,v7,...` with current sensor readings mapped to 0–255. Sending only `v0`…`v3` is still accepted.
2. Optionally interleave `RC:DATA,...` for gauges and panels at a lower rate.

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

void sendPlotSample(int v0, int v1, int v2, int v3,
                    int v4 = 0, int v5 = 0, int v6 = 0, int v7 = 0) {
  SerialBT.printf("RC:PLOT,v0,%d,v1,%d,v2,%d,v3,%d,v4,%d,v5,%d,v6,%d,v7,%d\n",
                  v0, v1, v2, v3, v4, v5, v6, v7);
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

If the Control Panel uses **Advanced (binary)**, keep using `0xAA`/`0xCC` packets for RC. Simple lines (`RC:`) and binary frames can share one socket; branch on the first byte (`0xCC` vs ASCII letter).

---

## Boolean values

Accepted as `1`/`0`, `true`/`false`, `on`/`off`, or any non-zero integer.
