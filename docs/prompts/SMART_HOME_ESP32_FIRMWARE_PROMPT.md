# Cursor prompt: TeleCon Smart Home ESP32 firmware (no cam)

Copy everything below into a **new Cursor chat** (Agent mode) on the ESP32 firmware tree
(for example under `/home/miguel/Documents/ESP32_Projects/`).

The Android app (`TeleCon4ESP32`) Smart Home screen is already implemented for **ESP32 DevKit only — no camera**.

---

## Goal

Create **`TeleCon_SmartHome`** Arduino/ESP32 firmware for a DIY home hub:

- **Board:** ESP32 DevKit (WROOM-32) — **no ESP32-CAM, no SoftAP video**
- **Transport:** Classic Bluetooth SPP and/or BLE (match existing TeleCon stacks)
- **Protocols on the same link:**
  - **SIMPLE** — text lines `SH:DATA,...` and `SH:SET,...`
  - **ADVANCED** — binary SH packets (`AA 48` SET / `CC 48` DATA)

Reuse shared modules from an existing TeleCon sketch (e.g. `ESP32_BT_Controller_sp/`):

- `TeleConBluetooth.*` / BLE equivalents
- `TeleConProtocol.*`
- `TeleConBinaryRx.*` (extend for `CC 48` / `AA 48`)
- `TeleConConfig.h`, `TeleConDebug.h`

---

## Android contract (source of truth)

Read these files in the Android repo
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`:

| File | Why |
|------|-----|
| `docs/SIMPLE_PROTOCOL_ESP32.md` | Line framing `APP:TYPE,key,value,...` |
| `docs/BINARY_PROTOCOL_APPS.md` § Smart Home | Binary SET/DATA layout |
| `domain/bluetooth/sh/SmartHomeProtocol.kt` | Room/device/scene string ids |
| `domain/bluetooth/sh/ShBinaryProtocol.kt` | Masks, payload size 14, device flags |
| `ui/smarthome/SmartHomePinMap.kt` | GPIO map shown in app Help |
| `ui/smarthome/SmartHomeViewModel.kt` | What the app sends/expects |
| Help strings `smart_home_guide_*` in `res/values/strings.xml` | Human-readable examples |

App prefix: **`SH`**. Binary app byte: **`0x48` (`H`)**.

---

## Hardware pin map (match Android Help)

Active-high relays unless noted. Reed/PIR = `INPUT_PULLUP` (LOW = active/open/motion as you define — document in code).

```
GPIO  2  → status LED (mirrors “systems OK” / blink on BT connect)
GPIO 16  → living light relay
GPIO 17  → living ambience relay
GPIO 15  → living outlet relay          // boot strapping — use carefully
GPIO  4  → living DHT22 data
GPIO 32  → living window reed
GPIO 19  → kitchen light relay
GPIO 23  → kitchen appliance relay
GPIO  5  → kitchen DHT22 data
GPIO 14  → kitchen water valve
GPIO 18  → bedroom light relay
GPIO 12  → bedroom DHT22 data           // boot strapping — use carefully
GPIO 13  → garage light relay
GPIO 33  → garage door reed
GPIO 25  → garage PIR
GPIO 26  → garage lock relay
GPIO 21  → INA219 SDA (I2C)
GPIO 22  → INA219 SCL (I2C)
GPIO 27  → water flow meter pulse (interrupt)
```

**Climate tile:** average of DHT22 temps on 4/5/12 (skip missing sensors).  
**Security tile:** open if living window (32) or garage door (33) is open.  
Put pin defines in `pins.h` so DIY builders can remap.

**Minimal DIY subset (recommended first flash):** GPIO 2 + light relays 16/19/18/13 + one DHT + one reed. Stub missing sensors (omit keys or send `0`).

---

## SIMPLE protocol

### ESP32 → phone (`SH:DATA`)

Send a compact line **every 1–2 s**. Include device states + system snapshot. Example:

```
SH:DATA,device,ESP32-SH01,status,0,living_light,1,living_ambience,0,living_outlet,0,living_on,1,living_total,3,living_temp,21,kitchen_light,1,kitchen_appliance,0,kitchen_water,0,kitchen_on,1,kitchen_total,3,kitchen_temp,23,bedroom_light,0,bedroom_on,0,bedroom_total,1,bedroom_temp,19,garage_light,1,garage_lock,1,garage_on,2,garage_total,2,garage_temp,18,climate_temp,21,climate_status,0,energy_kw,1.2,power_kw,1.2,security_status,0,water_l,42
```

Rules:

- Keys must be **even** key/value pairs (line codec rejects odd token counts).
- Room ids: `living`, `kitchen`, `bedroom`, `garage`
- Controllable devices: `light`, `ambience`, `outlet`, `appliance`, `water`, `lock`
- Per-device keys: `{room}_{device}` → `0`/`1`
- Counts: `{room}_on`, `{room}_total` (controllable devices only)
- Alerts: `{room}_alert` = `window` | `door` (omit or `none` if clear)
- `status`: `0` all normal, `1` warning
- `climate_status`: `0` comfortable, `1` heating, `2` cooling
- `security_status`: `0` secure, `1` open contact

**Every 30–60 s** also send history + events (can be a second line or merged):

```
SH:DATA,hist_power,0.4|0.3|0.6|1.2|2.4|2.1|1.8|1.5|1.0|0.5,event0,door_locked|09:30,event1,light_on|09:15,event2,motion|08:47
```

Event formats accepted by the app:

- `eventN,<code>|<HH:MM>` **or**
- `eventN,<code>,eventN_t,<HH:MM>`

Codes: `door_locked`, `light_on`, `motion`, `windows`, `water`.

### Phone → ESP32 (`SH:SET`)

```
SH:SET,room,living,device,light,1
SH:SET,room,living,device,ambience,0
SH:SET,room,kitchen,device,appliance,1
SH:SET,room,kitchen,device,water,0
SH:SET,room,garage,device,lock,1
SH:SET,scene,all_lights_off
SH:SET,scene,away
SH:SET,refresh,1
```

After every SET, apply GPIO then immediately send a confirming `SH:DATA` (and binary DATA if in advanced mode).

**Scenes:**

| Scene | Behavior |
|-------|----------|
| `all_lights_off` | All `*_light` (+ living `ambience`) → OFF |
| `away` | All lights, ambience, outlet, appliance, water → OFF; garage `lock` → ON |

---

## Binary protocol (ADVANCED)

### SET (`AA 48` + mask u16 LE + values + checksum)

| Mask bit | Payload |
|----------|---------|
| bit0 `refresh` | u8 0/1 |
| bit1 room device | `room_id` u8, `device_id` u8, `state` u8 |
| bit2 scene | `scene` u8: `0` all_lights_off, `1` away |

**room_id:** 0 living, 1 kitchen, 2 bedroom, 3 garage  
**device_id:** 1 light, 2 ambience, 3 outlet, 4 appliance, 5 water, 6 lock

### DATA (`CC 48`, type `0x01`, payload **14 bytes** LE)

| Offset | Field |
|--------|-------|
| 0 | climate_temp i8 (°C) |
| 1 | climate_status u8 |
| 2–3 | energy×10 u16 (kW × 10) |
| 4 | security_status u8 |
| 5–6 | water_l u16 |
| 7 | status u8 |
| 8 | living_on u8 |
| 9 | kitchen_on u8 |
| 10 | bedroom_on u8 |
| 11 | garage_on u8 |
| 12–13 | device_flags u16 |

**device_flags bits:**  
0 living.light, 1 living.ambience, 2 living.outlet,  
3 kitchen.light, 4 kitchen.appliance, 5 kitchen.water,  
6 bedroom.light, 7 garage.light, 8 garage.lock

Checksum rules: same as other TeleCon apps (`docs/BINARY_PROTOCOL_APPS.md` / `AppBinaryFrame.kt`).

---

## Firmware architecture

```
setup():
  pins + I2C + BT name "TeleCon-SH" (or similar)
  restore relay states from NVS (optional)

loop():
  teleconPollBluetooth()
  readSensors()          // DHT, reeds, PIR, INA219, flow pulses
  updateStatusFlags()    // status, security, climate_status
  if (telemetryDue) sendShTelemetry()
  applyPendingActuators()
```

### Inbound

| Input | Handler |
|-------|---------|
| `SH:SET,...` | `handleShSetSimple()` |
| `AA 48 ...` | `handleShSetBinary()` |

Infer protocol mode from last inbound frame (line vs binary), or always emit SIMPLE and add binary when ADVANCED was seen.

### Outbound

- `sendShDataSimple()` via `teleconSendLine()`
- `sendShDataBinary()` `CC 48` 14-byte payload
- On `refresh=1`: flush full state immediately (devices + hist + events)

### Events

Keep a small ring buffer (5 entries) in RAM/NVS. Push on:

- lock → ON → `door_locked`
- any light → ON → `light_on`
- PIR rising edge → `motion`
- window reed open → `windows`
- flow pulse burst / daily liter step → `water`

---

## Acceptance checklist

- [ ] Phone connects over Classic BT (and BLE if you implement it); app shows **Online**
- [ ] Toggling living/kitchen/bedroom/garage lights and other controllable icons drives the listed GPIOs
- [ ] `SH:DATA` updates temps, energy, security, water, room badges, and events in the app
- [ ] Scenes `all_lights_off` and `away` match the table above
- [ ] Binary SET/DATA works with Android ADVANCED protocol mode
- [ ] No camera / SoftAP video code paths
- [ ] Missing optional sensors do not crash; telemetry still sends

---

## Out of scope

- ESP32-CAM / MJPEG / SoftAP video (Smart Home is DevKit-only in the app)
- Alexa/Google Home cloud bridges
- Per-room Wi‑Fi mesh — single ESP32 hub is enough for this DIY kit
