# TeleCon Simple Protocol — ESP32 Reference

Human-readable Bluetooth lines for TeleCon4ESP32. Use this when the app’s **Communication protocol** setting is **Simple (text lines)** for a given application.

**Wire format:** `APP:TYPE,key1,value1,key2,value2,...` + newline (`\n`)

**Bluetooth:** Classic RFCOMM, UUID `00001101-0000-1000-8000-00805F9B34FB` (SPP).

---

## Application prefixes

| Prefix | Application        |
|--------|--------------------|
| `RC`   | Control Panel / RC |
| `GH`   | Greenhouse         |
| `WT`   | Water Tank         |
| `SH`   | Smart Home         |
| `SP`   | Solar Power        |
| `DL`   | Smart Door Lock    |
| `LT`   | Smart Lighting     |
| `CD`   | Custom Dashboard   |

---

## RC (Control Panel)

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

#### `RC:PLOTCFG` — plot series names (send once at startup or when names change)

```
RC:PLOTCFG,n0,RPM,n1,Speed,n2,Temp
```

| Key | Description              |
|-----|--------------------------|
| n0  | Name for plot series 0   |
| n1  | Name for plot series 1   |
| …   | Up to n5 supported       |

The app assigns fixed colors per index (cyan, red, green, yellow, magenta, white).

#### `RC:PLOT` — live plot samples (send periodically, e.g. 10–30 Hz)

```
RC:PLOT,v0,128,v1,200,v2,64
```

| Key | Description                                      |
|-----|--------------------------------------------------|
| v0  | New sample for series 0, integer **0–255**       |
| v1  | New sample for series 1                          |
| …   | One sample per key per line; app keeps last 100 |

Values match the **binary** plot packet scale: `0` = bottom, `255` = top of the graph.

**Typical flow:**

1. On connect: send `RC:PLOTCFG,...` with your series names.
2. In `loop()`: every 50–100 ms send `RC:PLOT,v0,...,vN,...` with current sensor readings mapped to 0–255.
3. Optionally interleave `RC:DATA,...` for gauges and panels at a lower rate.

---

## Greenhouse (`GH`)

**ESP32 → phone:** `GH:DATA,temp,26.2,hum,68,vpd,1.1,soil,42,light,12400,fan,1,heater,0,pump,0,lights,0,vent,40,tank,78,auto,1,target_temp,24,target_hum,65`

**Phone → ESP32:** `GH:SET,fan,1` | `GH:SET,heater,0` | `GH:SET,pump,1` | `GH:SET,lights,1` | `GH:SET,vent,60` | `GH:SET,auto,1` | `GH:SET,target_temp,24,target_hum,65`

Optional history: `hist_temp`, `hist_hum`, `hist_vpd` as pipe-separated floats. See Greenhouse settings in the app for pin map and field guide.

---

## Water Tank (`WT`)

**ESP32 → phone:** `WT:DATA,level,74,cap,500,pump,0,status,0` (`status`: 0=normal, 1=low, 2=critical)

**Phone → ESP32:** `WT:SET,pump,1`

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

void sendPlotConfig() {
  SerialBT.println("RC:PLOTCFG,n0,RPM,n1,Speed");
}

void sendPlotSample(int rpmByte, int speedByte) {
  SerialBT.printf("RC:PLOT,v0,%d,v1,%d\n", rpmByte, speedByte);
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
  sendPlotConfig();
}

void loop() {
  if (SerialBT.available()) {
  String line = SerialBT.readStringUntil('\n');
    line.trim();
    if (line.length() > 0) handleLine(line);
  }

  unsigned long now = millis();

  // Plot updates ~20 Hz
  if (now - lastPlotMs >= 50) {
    lastPlotMs = now;
    float rpm = analogRead(34);   // example
    float speed = analogRead(35);
    sendPlotSample(toPlotByte(rpm, 0, 4095), toPlotByte(speed, 0, 4095));
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
