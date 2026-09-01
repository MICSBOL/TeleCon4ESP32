# TeleCon binary protocols (all applications)

Compact binary frames for Classic SPP, BLE (Nordic UART), and **DevKit SoftAP TCP**.
Used when the Android app selects **Classic + Binary**, **BLE + Binary**, or
**Wi‑Fi SoftAP + Binary**. (**Classic + Simple** / **Wi‑Fi SoftAP + Simple** use the
text-line protocol in [SIMPLE_PROTOCOL_ESP32.md](SIMPLE_PROTOCOL_ESP32.md) instead;
BLE is binary-only. Kit A CAM SoftAP uses SIMPLE text with `proto=wifi`, not these frames.)

**Shared framing** (see `AppBinaryFrame.kt`):

| Direction | Layout |
|-----------|--------|
| Phone → ESP32 SET | `AA <app> [mask u16 LE] [values…] [checksum]` |
| ESP32 → Phone DATA | `CC <app> [length u16 LE] [type=0x01] [payload…] [checksum]` |

Checksum = low byte of sum of all bytes after the 2-byte header.

RC Control Panel keeps its legacy layouts (`AA 55`, `CC 11/22/33`) — see existing RC firmware.
Plot packets (`CC 33`) should send **count = 4 or 8** sample bytes (0–255). The phone
treats them as numbered analog channels **CH1…CH8**; the Control Panel still has four
plot widgets (defaults CH1–CH4). Four-byte frames remain valid. Do **not** send legacy
`CC 44` label-config packets; plot/panel/indicator
labels come from Android RC settings. The phone can remap `CC 33` samples and `CC 11`/`CC 22`
fields onto plots, radar, gauges, panels, or LEDs; firmware does not send a channel-map packet.

**Handshake (Classic SPP, BLE, and DevKit SoftAP):** before control/telemetry, the phone sends a text line
`RC:CONNECT,proto,binary` (or `proto,simple` for Classic/DevKit Simple firmware). Reply with
`RC:ACK,app,RC` or `RC:NAK,reason,proto_mismatch|app_mismatch,expected,…,actual,…`.
DevKit SoftAP Binary uses SoftAP `ESP32-TC-RC-WiFi-Binary` / `telecon1234` and TCP
`192.168.4.1:3333` with the **same** `AA 55` / `BB 66` / `CC 11/22/33` frames as Classic/BLE Binary.
See [SIMPLE_PROTOCOL_ESP32.md](SIMPLE_PROTOCOL_ESP32.md) and
[prompts/CONTROL_PANEL_HANDSHAKE_ESP32_PROMPT.md](prompts/CONTROL_PANEL_HANDSHAKE_ESP32_PROMPT.md).

**Bench debug (RC Control Panel):** default firmware simulate mode should echo phone
sticks/knobs/switches into `CC 11` / `CC 22` / `CC 33` (same UX as Classic Simple text
echo). Optional Serial inject forwards typed lines/frames to the phone. Unify Classic
Binary with BLE via
[prompts/CONTROL_PANEL_DEBUG_MODE_ESP32_PROMPT.md](prompts/CONTROL_PANEL_DEBUG_MODE_ESP32_PROMPT.md).

---

## App bytes

| App | Prefix | App byte | Notes |
|-----|--------|----------|-------|
| Greenhouse | `GH` | `0x47` (`G`) | See [BINARY_PROTOCOL_GH.md](BINARY_PROTOCOL_GH.md) |
| Water Tank | `WT` | `0x57` (`W`) | |
| Solar Power | `SP` | `0x53` (`S`) | Live fields only; `hist_*` stays SIMPLE |
| Smart Home | `SH` | `0x48` (`H`) | |
| Smart Door Lock | `DL` | `0x4B` (`K`) | Avoids clash with RC config `CC 44` |
| Smart Lighting | `LT` | `0x4C` (`L`) | SIMPLE prefix is `LT` (not `SL`) |

---

## Water Tank (`WT`)

**SET mask:** bit0 = `pump` (0/1)

**DATA payload (5 B):** `level u8`, `cap u16 LE`, `pump u8`, `status u8`

---

## Solar Power (`SP`)

**SET mask:** bit0 `refresh`, bit1 `period`, bit2 `inverter`, bit3 `reset_day`

**DATA payload (50 B, LE):** live watts, SOC, volt×10, amp×10, kWh×10 counters, panels, status,
inverter, grid_mode, panel_eff, fault, battery info, distribution kWh×10. Chart history is
SIMPLE-only (`hist_prod` / `hist_cons`).

---

## Smart Home (`SH`)

**SET mask:** bit0 `refresh`; bit1 `room_id u8` + `device_id u8` + `state u8`; bit2 `scene u8`
(`0` = all_lights_off, `1` = away)

**device_id:** `1` light, `2` ambience, `3` outlet, `4` appliance, `5` water, `6` lock  
**room_id:** `0` living, `1` kitchen, `2` bedroom, `3` garage

**DATA payload (14 B):** climate_temp i8, climate_status u8, energy×10 u16 LE, security_status u8,
water_l u16 LE, status u8, living_on, kitchen_on, bedroom_on, garage_on, device_flags u16 LE

**device_flags bits:** 0 living.light, 1 living.ambience, 2 living.outlet, 3 kitchen.light,
4 kitchen.appliance, 5 kitchen.water, 6 bedroom.light, 7 garage.light, 8 garage.lock

---

## Smart Door Lock (`DL`)

**SET mask bits:** unlock, lock, pulse, mic, spk, cam, call

**DATA payload (8 B):** lock, relay (0=LOW/1=HIGH), door, mic, spk, cam, sig (i8), online

---

## Smart Lighting (`LT`)

**SET mask:** `all`, `zone_id`+`state`, `auto_away`, `motion`, `sunset`

**DATA payload (5 B):** on_count, total, flags (auto_away|motion|sunset), zones u16 bitfield

---

## Android files

| Area | Path |
|------|------|
| Shared frame helper | `domain/bluetooth/AppBinaryFrame.kt` |
| Per-app codecs | `domain/bluetooth/{wt,sp,sh,dl,lt}/` |
| Inbound routing | `data/bluetooth/AndroidBluetoothController.kt` |
| Frame assembly | `data/bluetooth/BluetoothFrameAssembler.kt` |
