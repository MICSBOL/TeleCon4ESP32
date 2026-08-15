# Cursor prompt: RC Vehicle Pro — steering center trim (all connection modes)

Copy everything below into a **new Cursor chat** (Agent mode) on the ESP32 firmware
repo for TeleCon RC Vehicle Pro (all Classic / BLE / SoftAP variants that drive a
steering servo or differential mix).

The Android app (`TeleCon4ESP32`) already implements **steer center trim tickers** on
the RC Vehicle Pro screen:

- Tune icon enters trim mode (stick held at visual zero)
- ◀ / ▶ nudge by **±1 channel unit** (`rx` ±1 in −100…100)
- Check locks center: sends save command, hides tickers
- Live `rx` already includes the app trim while nudging

---

## Goal

Make firmware treat **stick `rx = 0` as mechanical straight** after the user locks
center. Support **every** RC Vehicle Pro connection path that carries control:

| Mode | Transport | Control stream | Save-center command |
|------|-----------|----------------|---------------------|
| Classic + Simple | Classic SPP | `RC:CTRL,...` | `RC:SET,steer_center,1,rx,<n>` |
| Classic + Binary | Classic SPP | `AA 55` sticks | `BB 66` id **`0x10`** (+ optional text SET) |
| BLE + Binary | BLE NUS | `AA 55` sticks | `BB 66` id **`0x10`** (+ optional text SET) |
| SoftAP + Simple (Kit A / text) | TCP `:3333` | `RC:CTRL,...` | `RC:SET,steer_center,1,rx,<n>` |
| SoftAP + Binary | TCP `:3333` | `AA 55` | `BB 66` id **`0x10`** (+ optional text SET) |

Do **not** invent a new app prefix. Stay on **`RC`**.

---

## Android contract (source of truth)

Repo: `/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`

| Topic | File |
|-------|------|
| Trim step / clamp | `ui/rc_vehicle_pro/RcSteerTrim.kt` (`STEP = 0.01f` → channel ±1) |
| UI | `ui/rc_vehicle_pro/RcVehicleProScreen.kt`, `components/RcControlAndActionBar.kt` |
| SIMPLE save line | `SimpleProtocolEncoder.buildSteerCenterSaveLine(rx)` |
| Binary button id | `ButtonEvent.STEER_CENTER_SAVE` = `0x10` |
| Send path | `BluetoothViewModel.saveSteerCenter()` |
| Stick packing | `RcPacketEncoder.stickTo12Bit` (−100…100 → 0…4095) |

### Wire examples

**Simple / SoftAP text**

```text
RC:SET,steer_center,1,rx,-3
```

- `steer_center,1` — commit request  
- `rx` — live channel (−100…100) currently applied (includes phone trim)

**Binary / BLE / SoftAP binary**

```text
BB 66 10 10
```

(`0xBB 0x66`, id `0x10`, repeat id — same layout as other `BB 66` buttons)

Android may also send the text `RC:SET,...` line on binary links; accept **either**.

---

## Firmware behavior

### 1. Steering map (always)

Keep a persisted **mechanical center** in NVS (µs for servo, or mix bias for diff):

```text
steerUs = map(rx, -100..100, centerUs - travelUs, centerUs + travelUs)
```

or equivalent for differential:

```text
bias = map(rx, -100..100, -maxBias, +maxBias)
left  = throttle + bias
right = throttle - bias
```

Default `centerUs` ≈ 1500 before first calibration.

### 2. On save-center command

When `RC:SET,steer_center,1` **or** `BB 66` / `0x10` is received:

1. Read the **current steering output** actually applied to the hardware
   (the PWM/mix that resulted from the latest `rx`, including phone trim).
2. Store that as new `centerUs` / `bias0` in NVS.
3. From then on, **`rx = 0` must produce that same straight output**.
4. Optional ACK (Simple): `RC:ACK,steer_center,1` (nice-to-have; Android does not wait yet).

Do **not** require the stick to be non-zero. During trim mode Android holds
visual stick at 0 and only changes trim → `rx` is the fine offset.

### 3. Fail-safe

- BT/BLE/TCP drop → motors/servo to safe idle; **keep** saved center in NVS.
- Emergency stop from app sends sticks 0 — that must be true straight after save.

### 4. Apply in every sketch variant

Update **all** TeleCon RC Vehicle Pro firmwares that steer:

- Classic Simple / Classic Binary / BLE Binary  
- CAM and noCam  
- SoftAP TCP Simple and Binary (if present)

Share one `steer_center` module (NVS + map) across variants.

---

## Acceptance criteria

- [ ] Before save: rough mechanical mount; small `rx` trim from app moves servo by tiny steps
- [ ] After ◀/▶ until wheels look straight, Android Check → firmware receives SET and/or `0x10`
- [ ] NVS survives reboot; `rx = 0` keeps wheels straight
- [ ] Works on Classic Simple, Classic Binary, BLE Binary, SoftAP Simple, SoftAP Binary
- [ ] Wrong/unknown SET keys ignored safely
- [ ] No new app prefix; no video over BT; no change to `AA 55` stick layout

---

## Do not

- Rely only on Android trim without NVS (other phones would not get the center)
- Change stick scale (−100…100 / 12-bit) without updating Android
- Block `loop()` on NVS writes for long periods

---

## When done

Document in each sketch README:

1. How to run Android trim (Tune → ticks → Check)  
2. Which connection mode to select  
3. That center is stored in NVS on the vehicle  

Reference: `docs/prompts/RC_VEHICLE_PRO_ESP32_FIRMWARE_PROMPT.md`,
`docs/SIMPLE_PROTOCOL_ESP32.md`, `docs/BINARY_PROTOCOL_APPS.md`.
