# Cursor prompt: TeleCon_ControlPanel — I2C expansion (radar first)

Copy everything below into a **new Cursor chat** (Agent mode) on the ESP32 firmware tree
`/home/miguel/Documents/ESP32_Projects/TeleCon_ControlPanel/`.

The Android app (`TeleCon4ESP32`) does **not** speak I2C. It only sends `RC:CTRL` / `AA 55`
and receives `RC:PLOT` / `CC 33` (and `RC:DATA` / `CC 11` / `CC 22`). Radar on the phone is
already a drawing of plot series 0 (angle) and 1 (range). Do **not** change Android.

This prompt **replaces GPIO-based extra features** (radar servo PWM, HC-SR04 TRIG/ECHO,
native ADC plot pins) as the **recommended** way for advanced users to add hardware.
Keep the existing RC protocol and bench simulate path.

Related (read, do not re-do unless needed):

- `docs/prompts/CONTROL_PANEL_CENTER_MODES_USER_FIRMWARE_ESP32_PROMPT.md` — center panes, four plots, CAM HTTP
- `docs/SIMPLE_PROTOCOL_ESP32.md` — `RC:PLOT` `v0`…`v3`
- `docs/BINARY_PROTOCOL_APPS.md` — `CC 33` count = 4

---

## Goal

1. Treat **I2C as the principal expansion bus** for optional Control Panel extras
   (radar, later extra ADC / PWM / GPIO chips). ESP32 native GPIOs used for extras:
   **SDA + SCL only** (plus power/GND).
2. **Migrate radar** off assumed ESP32 pins (`PIN_RADAR_SERVO`, `PIN_RADAR_TRIG`,
   `PIN_RADAR_ECHO`, `PIN_RADAR_RANGE_ADC`) onto I2C devices.
3. Document clearly for the **advanced user**: wire chips on the I2C bus, set
   **addresses / channels** in one header — do not spend a GPIO per feature.
4. Stock sketches still run with **no extra hardware** (bus disabled → simulate / echo
   knobs into plots, so Radar still moves on the phone).

Default user sketches to update (same two devices as the center-modes prompt):

| Device | Where extras belong |
|--------|---------------------|
| **ESP32 DevKit (noCam)** | Own the I2C bus. Preferred board for radar + future expanders. |
| **ESP32-CAM** | Same I2C *option* if the user has leftover SDA/SCL, but **do not** recommend radar/expanders on CAM (camera pins + SoftAP). Prefer DevKit / two-device kit. |

---

## Why not native GPIOs for radar

Current user-pin headers assume:

- Servo = ESP32 LEDC / `Servo` on a GPIO
- Range = HC-SR04 `TRIG`/`ECHO` pulse timing, or `analogRead` on a GPIO

That does not scale: every new center feature steals more pins (CAM has almost none).
A GPIO expander (MCP23017) **cannot** time HC-SR04 echo in microseconds. Native PWM
for one servo also blocks a pin that a PCA9685 would free.

**Recommended radar kit (I2C):**

| Function | Chip | Notes |
|----------|------|--------|
| Sweep servo | **PCA9685** (16× PWM, typical addr `0x40`) | Channel 0 = radar servo. Remaining channels reserved for later extras. |
| Distance | **VL53L0X / VL53L1X** (typical `0x29`) | Native I2C millimetres. Scale to 0–255 for `v1`. |

**Do not** route classic HC-SR04 through MCP23017. If the user must keep ultrasonic
pulse hardware, use a **small helper MCU** on the radar head that owns TRIG/ECHO +
servo PWM and appears on the bus as one I2C slave returning angle + range bytes.
That helper is optional; default docs and code should push **PCA9685 + ToF**.

**Power:** servos want a **5 V** rail and common GND. I2C stays **3.3 V** (level
shifter if a 5 V I2C module needs it). Do not power an SG90 from the ESP32 3.3 V pin.

---

## Protocol (unchanged)

No new radar packet. No I2C addresses on Bluetooth / Wi‑Fi.

Phone defaults (already implemented):

| Plot | Meaning | 0 … 255 |
|------|---------|---------|
| `v0` | Sweep angle | left extreme … right extreme |
| `v1` | Range | near … far (timeout / no echo → **255**) |
| `v2`,`v3` | Other sensors or simulate | unchanged |

Firmware still sends ~10–20 Hz:

```
RC:PLOT,v0,<angle>,v1,<range>,v2,…,v3,…
```

or binary `CC 33` with count = 4.

Do **not** send chip dumps, millimetre integers, or register reads to the phone.
Scale on the ESP32. 180° vs 270° scan drawing stays **phone-only**; firmware only
reports full mechanical travel as 0…255.

Phone → ESP32 remains `RC:CTRL` / `AA 55`. Optional later: map a knob to PCA9685
channel 0 if you do not auto-sweep. Default radar behaviour: firmware **auto-sweeps**
the servo and publishes the current command as `v0` (same as today’s GPIO radar).

Do **not** invent `RC:I2C` / raw register write lines in this change. Addresses stay
in the user header. A future tagged `RC:EXP` slot protocol is out of scope unless
four plot bytes are already insufficient (they are enough for radar).

---

## User config file (replace GPIO extras)

Keep **one** user-edited header (existing `TeleConUserPins.h` or rename only if both
sketches already share it — prefer **extend in place**, do not create a second
conflicting pin file).

**Remove or stop recommending** as the primary radar path:

```cpp
#define PIN_RADAR_SERVO       …
#define PIN_RADAR_TRIG        …
#define PIN_RADAR_ECHO        …
#define PIN_RADAR_RANGE_ADC   …
#define PIN_PLOT0_ADC         …  // native ADC; prefer I2C ADC later
```

If you keep those macros for one release, default them all to `PIN_NONE` and comment
them as **legacy / not recommended**. New radar code must not require them.

**Add** an I2C expansion section. Defaults disable the bus so a first flash needs
no wiring. **Do not** assign real GPIO numbers in the shipped file (same rule as
before: `PIN_NONE` / address `0` = off). Comments may name typical ESP32 I2C pins
(e.g. 21/22) as *examples in comments only*, never as assigned values.

Template (adapt names to existing style):

```cpp
#pragma once
// TeleConUserPins.h — YOU assign the I2C bus and device addresses.
// Leave PIN_NONE / 0 to disable. Firmware must run with everything unset.

#ifndef PIN_NONE
#define PIN_NONE (-1)
#endif
#define PIN_IS_SET(p) ((p) >= 0)

// --- I2C expansion bus (principal way to add extras) ---
// Only native ESP32 pins for extras: SDA and SCL. All radar / ADC / PWM / GPIO
// chips hang on this bus. Advanced users: set these two pins and the addresses below.
#define PIN_I2C_SDA           PIN_NONE
#define PIN_I2C_SCL           PIN_NONE

// 0 = device absent (skip). Typical values are 7-bit addresses.
#define I2C_ADDR_PWM          0    // PCA9685, often 0x40
#define I2C_ADDR_RANGE        0    // VL53L0X / VL53L1X, often 0x29
#define I2C_ADDR_ADC          0    // optional ADS1115, often 0x48 — future plots
#define I2C_ADDR_GPIO         0    // optional MCP23017, often 0x20 — future bits

#define PWM_CH_RADAR_SERVO    0    // PCA9685 channel for the sweep servo (0–15)

// Radar pulse ultrasonic on ESP32 GPIOs is NOT recommended (see README).
// Legacy GPIO radar (disabled):
#define PIN_RADAR_SERVO       PIN_NONE
#define PIN_RADAR_TRIG        PIN_NONE
#define PIN_RADAR_ECHO        PIN_NONE
#define PIN_RADAR_RANGE_ADC   PIN_NONE
```

**Runtime:**

- If `PIN_I2C_SDA` / `PIN_I2C_SCL` are `PIN_NONE` → do not call `Wire.begin`, do not
  probe chips, keep existing **simulate / last `RC:CTRL` knobs** for `v0`…`v3`.
- If the bus is up but `I2C_ADDR_PWM` is 0 → do not drive a servo; still simulate `v0`
  unless you have a real angle source.
- If `I2C_ADDR_RANGE` is 0 → simulate `v1` (or 255).
- Probe once at setup (optional). If a chip NACKs, log under `#if TELECON_DEBUG` and
  fall back to simulate for that slot — do not stall `loop()` or fail the handshake.
- Never block on `pulseIn` for the recommended path.

Copy the same header into **both** default sketches.

---

## Firmware behaviour

### Bus

- `Wire` at 100 kHz default (400 kHz only if all modules allow it).
- Init only when both SDA and SCL are set.
- Poll radar at a modest rate from `loop()` (same ~10–20 Hz as plots). No long
  `delay()`. Camera sketches must not starve TCP `:3333` / `/stream`.

### Radar helpers (replace GPIO `teleconRadarPoll` assumptions)

Example split (names may match existing code):

1. `teleconI2cBegin()` — `Wire.begin` when pins set.
2. `teleconRadarPoll()`:
   - If PCA9685 present: sweep PWM channel `PWM_CH_RADAR_SERVO` across the user’s
     mechanical min/max pulse (document typical 500–2500 µs in comments; do not
     hard-require 180°). Publish sweep position as `v0` 0–255.
   - If ToF present: read mm, map into 0–255 with a documented max range (e.g. 0 mm → 0,
     clamp max → 254, timeout → 255). Publish as `v1`.
   - If neither present: existing bench echo.
3. `teleconReadPlotSamples(uint8_t out[4])` — fill `v0`/`v1` from radar I2C when
   live; `v2`/`v3` simulate or future `I2C_ADDR_ADC`.

**Legacy:** if you keep GPIO radar macros, only use them when I2C radar addresses
are 0 **and** those pins are set. README must say I2C is preferred.

### Future extras (implement slots, not full features)

Do not build new Android panes. Do prepare the bus so the next chips are obvious:

- `I2C_ADDR_ADC` — reserved; no requirement to stream extra ADCs in this task.
- `I2C_ADDR_GPIO` — reserved; do not bit-bang HC-SR04 on it.
- Unused PCA9685 channels — leave idle; comment that extra servos/PWM map from
  `RC:CTRL` later.

---

## README (required)

Next to each default sketch, tell the **advanced user** in English:

1. Extra Control Panel hardware (radar and later ADC/PWM/GPIO) should use **I2C**,
   not a new ESP32 pin per part.
2. Recommended radar: PCA9685 (servo) + VL53L0X/VL53L1X (range) on SDA/SCL.
3. Set `PIN_I2C_SDA`, `PIN_I2C_SCL`, `I2C_ADDR_PWM`, `I2C_ADDR_RANGE` in
   `TeleConUserPins.h` only.
4. Android Radar pane needs no extra packet — it already uses plot `v0` / `v1`.
5. HC-SR04 on ESP32 GPIOs is legacy; GPIO expanders cannot replace ECHO timing.
6. Prefer DevKit for the bus; CAM leftover pins are for the camera.
7. 5 V servo supply, 3.3 V I2C, common GND.
8. Stock file (all unset) still handshakes and moves plots/radar from knobs.

Boot Serial banner (`#if TELECON_DEBUG` or existing banner): I2C on/off, which
addresses probed OK — no pin literals.

---

## Do not

- Put PCA9685 / VL53 register maps or I2C addresses in the **Android** protocol.
- Require extra chips for a first successful flash.
- Use MCP23017 for ultrasonic echo.
- Hard-code SDA/SCL GPIO numbers in `.ino` / `.cpp` (only the user header, and stock
  remains `PIN_NONE`).
- Change SoftAP SSIDs, `/stream`, handshake, or plot count.
- Change Android.
- Make CAM the default radar wiring example.
- Stall `loop()` on I2C or ToF ranging.

---

## Acceptance checklist

### DevKit noCam

- [ ] Unmodified header (I2C pins `PIN_NONE`, all addresses `0`) → ACK, four plots
      move from knobs; Radar pane still follows `v0`/`v1` (simulate)
- [ ] No PCA9685 / ToF libraries required at link time if you stub when addresses
      are 0; **or** libraries linked but fully skipped when disabled (must compile
      without hardware)
- [ ] With user-set SDA/SCL + `I2C_ADDR_PWM` + `I2C_ADDR_RANGE`: `v0` tracks sweep,
      `v1` tracks range; phone Radar beam/blips move
- [ ] Missing chip (NACK) → debug log, simulate that channel, handshake still OK
- [ ] README recommends I2C radar; GPIO TRIG/ECHO not presented as the main path
- [ ] No GPIO literals for radar/expanders in `.cpp` / `.ino`

### ESP32-CAM

- [ ] Same header defaults; I2C radar optional and documented as “prefer DevKit”
- [ ] `/stream` `/capture` `/camconfig` and TCP `:3333` unchanged
- [ ] Enabling I2C must not block the HTTP stream loop

---

## Deliverables

1. Updated `TeleConUserPins.h` (both default sketches): I2C bus + addresses;
   GPIO radar macros unused or clearly legacy/`PIN_NONE`.
2. Radar poll path uses PCA9685 + I2C ToF when configured; otherwise simulate.
3. Sketch README: I2C is the principal expansion path; recommended radar BOM;
   Android still uses `RC:PLOT` / `CC 33`.
4. Debug banner: bus and probed devices.

When done, mention in the sketch README:

> Extra hardware (radar servo + range, later ADC/PWM/GPIO) should hang on I2C
> (SDA/SCL in `TeleConUserPins.h`). Radar still publishes angle/range as plot
> `v0`/`v1` (`RC:PLOT` / `CC 33`). Do not spend an ESP32 GPIO per feature.
> Prefer PCA9685 + VL53L0X/VL53L1X on the DevKit. The phone does not speak I2C.
