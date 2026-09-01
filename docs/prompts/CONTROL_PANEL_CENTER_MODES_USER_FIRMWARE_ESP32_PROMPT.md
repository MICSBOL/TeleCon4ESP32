# Cursor prompt: TeleCon_ControlPanel — default user firmware (DevKit + ESP32-CAM)

Copy everything below into a **new Cursor chat** (Agent mode) on the ESP32 firmware tree
`/home/miguel/Documents/ESP32_Projects/TeleCon_ControlPanel/`.

The Android app (`TeleCon4ESP32`) Control Panel **center pane** already switches between
three modes. Default user sketches for **two boards** must keep those modes working.
Do **not** change Android.

---

## Goal

Update (or create) the **default user code** for **exactly two devices**:

| Device | Role | Path to own / create |
|--------|------|----------------------|
| **ESP32 DevKit (noCam)** | Control + plots + radar samples | `ESP32_noCam/` default / starter sketch (Classic Simple is the user default; keep existing Binary / BLE / Wi‑Fi variants in sync if they share modules) |
| **ESP32-CAM** | Same control/telemetry **plus** SoftAP live video | `ESP32_cam/` default / starter sketch (create if missing) |

Both must speak the existing **RC** Control Panel protocol so the phone center composable can show:

1. **Plots** — four live traces (always available)
2. **Camera** — SoftAP MJPEG (CAM board only; noCam leaves the phone placeholder)
3. **Radar** — servo bearing + range drawn from the **same four plot channels** (phone-side visualization)

The phone chooses the center mode locally. Firmware must **not** try to detect Plots vs Camera vs Radar. Always send plot samples; CAM always serves HTTP video.

**Radar / extra hardware:** do not treat native GPIOs as the long-term expansion path.
Prefer **I2C** (shared SDA/SCL + chip addresses). Use the follow-up prompt
[`CONTROL_PANEL_I2C_EXPANSION_RADAR_ESP32_PROMPT.md`](CONTROL_PANEL_I2C_EXPANSION_RADAR_ESP32_PROMPT.md)
when migrating radar off `PIN_RADAR_SERVO` / TRIG / ECHO / ADC. This file still
allows optional GPIO slots so a first flash needs no extra chips.

---

## Pin assignment rule (mandatory)

**Do not hard-code GPIO numbers for any user feature** (radar servo, ultrasonic, extra ADCs, LEDs, relays, buttons, etc.) in `.ino` / `.cpp` / `TeleConConfig.h`.

Create **one clear file** that the user fills in for their wiring. Default every feature pin to disabled so the sketch **compiles and runs with no extra hardware**.

Suggested name (use the same file in both sketches):

```
TeleConUserPins.h
```

Put it next to the sketch `.ino` (and copy into shared `common/` if modules are shared). The rest of the firmware only reads those macros.

### Required shape of `TeleConUserPins.h`

- English comments only.
- One section per **optional** feature that the center pane (or surrounding panel) can use.
- Every pin `#define` defaults to **`PIN_NONE` (`-1`)**.
- Helper: `#define PIN_IS_SET(p) ((p) >= 0)`
- No example GPIO numbers as assigned values. Comments may say *what the pin is for* and *which GPIOs the user must not steal on ESP32-CAM* (camera module pins), but they must **not** pick a board for the user.
- Camera **sensor** pins on the CAM sketch are a board *model* (`CAMERA_MODEL_AI_THINKER` or a commented list of models). That is the module pinout, not a user feature. Keep it out of `TeleConUserPins.h`.

Template (adapt names to existing style; keep `PIN_NONE` defaults):

```cpp
#pragma once
// TeleConUserPins.h — the only file where YOU assign GPIOs.
// Leave PIN_NONE to disable that feature. Firmware must run with every pin unset.

#ifndef PIN_NONE
#define PIN_NONE (-1)
#endif
#define PIN_IS_SET(p) ((p) >= 0)

// --- Radar (center Radar pane) ---
// Phone default: plot series 0 = servo angle, series 1 = range.
// 0 = left extreme of the scan, 255 = right extreme; range 0 = near, 255 = far.
// Preferred extras: I2C (PCA9685 + ToF) — see CONTROL_PANEL_I2C_EXPANSION_RADAR_ESP32_PROMPT.md
// GPIO radar below is optional/legacy; leave PIN_NONE unless you must use native pins.
#define PIN_RADAR_SERVO       PIN_NONE  // PWM sweep servo (legacy GPIO)
#define PIN_RADAR_TRIG        PIN_NONE  // ultrasonic trigger (optional, not recommended)
#define PIN_RADAR_ECHO        PIN_NONE  // ultrasonic echo (optional, not recommended)
#define PIN_RADAR_RANGE_ADC   PIN_NONE  // analog range instead of ultrasonic (optional)

// --- Extra plot inputs (Plots pane; radar can remap series 0–3 on the phone) ---
#define PIN_PLOT0_ADC         PIN_NONE
#define PIN_PLOT1_ADC         PIN_NONE
#define PIN_PLOT2_ADC         PIN_NONE
#define PIN_PLOT3_ADC         PIN_NONE

// --- Optional panel / gauge / LED hardware (not required for center modes) ---
#define PIN_STATUS_LED        PIN_NONE
#define PIN_ANALOG_GAUGE_ADC  PIN_NONE
#define PIN_BATT_ADC          PIN_NONE

// Add more commented slots if the sketch already drives outputs from RC:CTRL / AA 55.
// Still default each to PIN_NONE. Never invent a required pin map.
```

**Runtime rule:** if a pin is `PIN_NONE`, skip `pinMode` / PWM / ADC for that feature. Fall back to existing **simulate / echo** so Plots and Radar still move from sticks/knobs on the bench (`TELECON_USE_HW_IO` equivalent = “any user pin set”).

Do **not** scatter `#define PIN_SERVO 13` in other files.

---

## Android center composable (source of truth — read, do not edit)

Repo: `/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`

| File | Why |
|------|-----|
| `ui/control_panel/CenterDisplay.kt` | Center host: Plots / Camera / Radar |
| `ui/control_panel/ControlPanelCenterMode.kt` | `PLOTS`, `CAMERA`, `RADAR` |
| `ui/control_panel/CartesianPlot` in `CenterDisplay.kt` | Four series: top = 0–1, bottom = 2–3 |
| `ui/control_panel/ControlPanelRadarDisplay.kt` | Sweep from plot samples |
| `ui/control_panel/RadarSectorGeometry.kt` | 0–1 (or 0–255) → bearing; range fraction |
| `ui/control_panel/ControlPanelRadarSettingsDialog.kt` | Phone-only: 180°/270°, beam, which plot is angle vs range |
| `ui/control_panel/ControlPanelCameraDisplay.kt` | Camera pane + Smooth / Balanced / High overlay |
| `ui/control_panel/ControlPanelCameraStreamViewModel.kt` | Applies `GET /camconfig` while the camera pane is visible |
| `domain/camera/CameraStreamState.kt` (`Esp32CameraDefaults`) | `http://192.168.4.1` `/stream` `/capture` `/camconfig` |
| `domain/camera/SoftApPerformancePreset.kt` | Query strings below |
| `docs/SIMPLE_PROTOCOL_ESP32.md` | `RC:PLOT` `v0`…`v3` |
| `docs/BINARY_PROTOCOL_APPS.md` | `CC 33` count = 4 |

Handshake / four-plot / bench echo already specified:

- `docs/prompts/CONTROL_PANEL_HANDSHAKE_ESP32_PROMPT.md`
- `docs/prompts/CONTROL_PANEL_FOUR_PLOTS_ESP32_PROMPT.md`
- `docs/prompts/CONTROL_PANEL_DEBUG_MODE_ESP32_PROMPT.md`
- CAM HTTP: `docs/prompts/ESP32_CAM_CAMCONFIG_RUNTIME_ESP32_PROMPT.md`
- CAM Serial logs: `docs/prompts/ESP32_CAM_CAMCONFIG_SERIAL_DEBUG_ESP32_PROMPT.md`

---

## Center modes ↔ firmware

Phone UI is Pro-gated for Camera / Radar. Firmware always provides the data; it does not implement unlocks.

### 1) Plots (both boards)

Send **exactly four** samples, ~10–20 Hz:

**Classic / Wi‑Fi Simple**

```
RC:PLOT,v0,128,v1,200,v2,64,v3,180
```

Each `vN` is integer **0–255**.

**Classic Binary / BLE Binary / Wi‑Fi Binary**

```
CC 33 | length u16 LE | count=4 | b0 | b1 | b2 | b3 | checksum
```

Do **not** send `RC:PLOTCFG` or `CC 44` (labels are Android RC Settings).

When user plot ADCs are `PIN_NONE`, keep the current bench mapping (knobs / sticks → `v0`…`v3`). When a plot ADC pin is set, that channel reads that pin scaled to 0–255.

### 2) Radar (both boards — no extra wire protocol)

Radar is **only** a phone drawing of plot data. Defaults on the phone:

| Radar use | Default plot series | Firmware meaning |
|-----------|---------------------|------------------|
| Servo / sweep angle | 0 (`v0`) | 0 = left extreme, 128 ≈ north / center, 255 = right extreme |
| Range / blip distance | 1 (`v1`) | 0 = near, 255 = far |

The user can remap series 0–3 in the radar settings dialog. Firmware just keeps sending four samples.

If `PIN_RADAR_SERVO` is set: sweep that servo across the user’s mechanical range and publish the **current command / measured angle** as `v0` (0–255). Do **not** assume 180° vs 270° — that span is phone-only. Full travel = 0…255.

If `PIN_RADAR_TRIG` + `PIN_RADAR_ECHO` (or `PIN_RADAR_RANGE_ADC`) are set: measure distance, scale to 0–255, publish as `v1`. Clamp / timeout → a safe value (e.g. 255 = no echo).

If radar pins are `PIN_NONE`: echo knobs into `v0`/`v1` (or existing simulate) so the radar pane is not stuck on “Waiting for plot data…”.

`v2` / `v3` remain available for other sensors or simulate.

### 3) Camera (ESP32-CAM only)

Video is **HTTP on SoftAP**, not Bluetooth.

| Item | Value |
|------|--------|
| Base | `http://192.168.4.1` |
| Preferred video | `GET /stream` — `multipart/x-mixed-replace; boundary=frame` |
| Snapshot | `GET /capture` — `image/jpeg` |
| Stream quality | `GET /camconfig?framesize=…&quality=…&fps=…` |
| Control (same board) | TCP `:3333` with the **same** RC lines/frames as noCam |
| Starter SSID / pass | `TeleCon-RC-CAM-Starter` / `telecon1234` (Normal / default user) |
| Advanced SSID | `TeleCon-RC-CAM` / `telecon1234` (only if this sketch is the Advanced CAM build) |

Presets the phone sends (apply even if the live preview is still a placeholder on the phone):

| Phone preset | Query |
|--------------|--------|
| Smooth | `framesize=qvga&quality=22&fps=10` |
| Balanced | `framesize=vga&quality=15&fps=0` |
| High quality | `framesize=vga&quality=12&fps=0` |

Boot default before any `/camconfig`: VGA, quality 15, fps 0 (Balanced).

Reuse `CameraStreamCamConfig.*` from RC Vehicle Pro if present. Serial logs for camconfig **only** inside `#if TELECON_DEBUG`.

**noCam sketch:** do **not** start SoftAP HTTP video. Camera pane on the phone stays on the waiting hint — that is correct.

Do not block TCP `:3333` / Bluetooth inside `/stream` or `/camconfig`. Poll the HTTP server from `loop()` without starving control RX.

---

## Two default user sketches

Keep folder layout under `TeleCon_ControlPanel/`. Prefer extending existing starters over duplicating parsers.

### A) DevKit noCam — default user

Canonical starter today:

- `ESP32_noCam/Classic_Simple/ESP32_BT_Controller_sp/`  
  (published Android zip is Classic Simple/Binary for DevKit)

Must:

1. Add `TeleConUserPins.h` (all `PIN_NONE`).
2. Keep `RC:CONNECT` → `RC:ACK` / `RC:NAK` (`TELECON_PROTO_WIRE "simple"`).
3. Keep four-channel `RC:PLOT`.
4. Optional radar servo / range **only** when those pins are set.
5. Boot Serial banner: which center modes this board supports (Plots + Radar data; no camera HTTP).
6. Device name unchanged (`ESP32-TC-RC-BT-Simple` or whatever the sketch already advertises).

If Wi‑Fi Simple starter exists (`ESP32_noCam/WiFi_Simple/ESP32_WiFi_Controller_sp_starter/`), share the same `TeleConUserPins.h` + plot/radar helpers. SoftAP identity stays `ESP32-TC-RC-WiFi-Simple` / `telecon1234` — **no** `/stream`.

### B) ESP32-CAM — default user

Create if missing, e.g.:

- `ESP32_cam/WiFi/TeleCon_ControlPanel_CAM_WiFi_Simple/`  
  (default user = SoftAP Simple + video)

Must:

1. Same `TeleConUserPins.h` (feature pins all `PIN_NONE`).
2. Camera model define only (AI-Thinker by default, other models commented).
3. SoftAP + `/stream` + `/capture` + `/camconfig`.
4. TCP `:3333` RC Simple (`RC:CONNECT,proto,simple` → ACK). Starter SSID `TeleCon-RC-CAM-Starter`.
5. Four-channel plots (simulate or user pins), including radar-ready `v0`/`v1`.
6. **Never** assign CAM-safe leftover GPIOs in code. Document in `TeleConUserPins.h` comments that the user must avoid the camera module pins for *their* board model — still leave every feature pin `PIN_NONE`.
7. Boot banner: join SoftAP, open Control Panel Camera pane, Stream quality chips call `/camconfig`.

Do **not** ship a dual-radio BLE+MJPEG default as the user sketch (fragile). Advanced Kit B stays a separate sketch if it already exists.

---

## Shared behaviour (both devices)

| Topic | Rule |
|-------|------|
| App prefix | `RC` (same as Control Panel / RC Vehicle Pro) |
| Handshake | ACK only matching `proto`; NAK `proto_mismatch` / `app_mismatch` |
| Panels / gauges / LEDs | Keep existing `RC:DATA` / `CC 11` / `CC 22` if already present |
| Bench | With all pins `PIN_NONE`, sticks/knobs still echo to plots (and thus radar) |
| Debug Serial | `#if TELECON_DEBUG` only; 115200 |
| Labels | Android-only |

---

## Implementation notes

1. One helper, e.g. `teleconReadPlotSamples(uint8_t out[4])`:
   - If `PIN_PLOTN_ADC` set → analog scaled 0–255
   - Else if radar pins feed series 0/1 → use those
   - Else simulate / last `RC:CTRL` knobs
2. One helper, e.g. `teleconRadarPoll()`:
   - No-op when radar pins are `PIN_NONE`
   - Otherwise sweep + range at a modest rate (do not stall `loop()`)
3. Gate hardware with `PIN_IS_SET`, not a second pin-map header.
4. Copy `TeleConUserPins.h` into **both** default sketches so the user sees the same file.
5. Short README next to each sketch: flash steps, Android connection mode, and “edit `TeleConUserPins.h` only”.

---

## Do not

- Assign GPIO numbers for radar, ultrasonic, plots, LEDs, or any other feature in firmware source (only `TeleConUserPins.h`, and only if the **user** edits it; stock file is all `PIN_NONE`).
- Invent a new radar packet type or camera-over-Bluetooth path.
- Send plot/panel labels from firmware.
- Change SoftAP SSIDs / passwords / `/stream` URLs the Android app already knows.
- Mix DevKit SoftAP SSIDs (`ESP32-TC-RC-WiFi-*`) with CAM SSIDs (`TeleCon-RC-CAM*`).
- Require extra sensors for a first successful flash.
- Change Android.
- Put MJPEG on TCP `:3333`.

---

## Acceptance checklist

### DevKit noCam

- [ ] Flash with unmodified `TeleConUserPins.h` (all `PIN_NONE`) → handshake ACK, four plot traces move from knobs/sticks
- [ ] Control Panel → Radar: beam follows plot series 0; blips use series 1
- [ ] Camera pane: no crash; no CAM SoftAP from this sketch
- [ ] Setting radar pins in `TeleConUserPins.h` can drive GPIO radar if present; **recommended** extras (servo + range) are I2C — see `CONTROL_PANEL_I2C_EXPANSION_RADAR_ESP32_PROMPT.md`
- [ ] No GPIO literals for those features in `.cpp` / `.ino`

### ESP32-CAM

- [ ] Join `TeleCon-RC-CAM-Starter` / `telecon1234`
- [ ] Browser `http://192.168.4.1/stream` works; `/capture` returns JPEG
- [ ] `GET /camconfig?framesize=qvga&quality=22&fps=10` → 200 JSON; stream follows
- [ ] TCP `:3333` still ACKs `RC:CONNECT,proto,simple` and accepts `RC:CTRL` / `RC:PLOT` out
- [ ] Four plot channels still update (simulate or user pins)
- [ ] `TeleConUserPins.h` still all `PIN_NONE` in the shipped default; camera model is the only board-specific define

---

## Deliverables

1. `TeleConUserPins.h` in **both** default user sketches — documented, all features `PIN_NONE`.
2. DevKit noCam default sketch updated for four plots + optional radar hardware via that file.
3. ESP32-CAM default user sketch with `/stream` `/capture` `/camconfig` + same plot/radar helpers + TCP control.
4. README note: user assigns pins only in `TeleConUserPins.h`; firmware does not ship a fixed feature pinout.

When done, mention in the sketch README:

> Control Panel center: Plots and Radar use `RC:PLOT` / `CC 33` (four samples). Camera is SoftAP HTTP on the CAM sketch. Assign GPIOs in `TeleConUserPins.h` only. For extra hardware (radar servo + range, later ADC/PWM/GPIO), prefer I2C — see `CONTROL_PANEL_I2C_EXPANSION_RADAR_ESP32_PROMPT.md`.
