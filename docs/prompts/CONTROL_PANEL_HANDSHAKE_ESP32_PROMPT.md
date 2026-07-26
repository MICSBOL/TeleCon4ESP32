# Cursor prompt: TeleCon_ControlPanel — RC:CONNECT handshake (Classic Simple / Binary / BLE)

Copy everything below into a **new Cursor chat** (Agent mode) on
`/home/miguel/Documents/ESP32_Projects/TeleCon_ControlPanel/`.

The Android app (`TeleCon4ESP32`) **already** sends a handshake after the socket/GATT link
is up and shows an error dialog on mismatch or timeout. Classic Simple and Classic Binary
Control Panel sketches must answer it the same way Greenhouse does. BLE already has a
handler — tighten it so wrong `proto` is NAK’d.

---

## Goal

Implement **`RC:CONNECT` → `RC:ACK` / `RC:NAK`** on:

| Variant | Path | `TELECON_PROTO_WIRE` |
|---------|------|----------------------|
| Classic + Simple | `ESP32_noCam/Classic_Simple/ESP32_BT_Controller_sp/` | `"simple"` |
| Classic + Binary | `ESP32_noCam/Classic_Binary/ESP32_BT_Controller/` | `"binary"` |
| BLE + Binary | `ESP32_noCam/BLE/TeleCon_ControlPanel_BLE_binary_ESP32/` | `"binary"` |

Mirror Greenhouse:

- Classic Simple: `TeleCon_Greenhouse/.../Classic_Simple/.../GreenhouseHandlers.cpp` + `TeleConBluetooth.cpp`
- Classic Binary: `TeleCon_Greenhouse/.../Classic_Binary/.../GreenhouseHandlers.cpp` + text-line branch in BT RX
- BLE: existing `ControlPanelHandlers.cpp` `handleRcConnect` (fix NAK rules)

---

## Android contract (source of truth)

Read in the Android repo:

- `docs/SIMPLE_PROTOCOL_ESP32.md` — RC handshake section
- `docs/BINARY_PROTOCOL_APPS.md` — RC handshake note
- `app/.../domain/bluetooth/ProtocolHandshake.kt`
- `app/.../ui/bluetooth/BluetoothViewModel.kt` — `startHandshake()` (~2.5 s timeout)
- `app/.../ui/control_panel/ControlPanelScreen.kt` — passes Settings protocol/transport into connect; shows `BluetoothConnectionErrorDialog`

### Phone → ESP32 (after link up)

```
RC:CONNECT,proto,simple\n
```

or

```
RC:CONNECT,proto,binary\n
```

`proto` matches the app connection mode (Classic Simple → `simple`; Classic Binary / BLE Binary → `binary`).

### ESP32 → phone

**Success:**

```
RC:ACK,app,RC\n
```

**Wrong application prefix:**

```
RC:NAK,reason,app_mismatch,expected,RC,actual,<incomingApp>\n
```

**Wrong protocol** (e.g. app set to Binary, board is Simple sketch):

```
RC:NAK,reason,proto_mismatch,expected,simple,actual,binary\n
```

Rules:

- `expected` = this firmware’s capability (`TELECON_PROTO_WIRE` / `TELECON_APP_PREFIX`)
- `actual` = value from the CONNECT line
- Only ACK when `app == RC` **and** `proto` equals `TELECON_PROTO_WIRE` (case-insensitive)
- Missing / unknown `proto` → NAK `proto_mismatch` with `actual,unknown`
- Do **not** ACK both `simple` and `binary` on a single-protocol build (BLE currently ACKs both — **fix that**)

Android maps NAK → dialog (“Protocol doesn’t match” / “Wrong ESP32 firmware”). No ACK/NAK within ~2.5 s → “No handshake reply” and **no** connected session.

---

## Concrete work per firmware

### 1) Classic Simple (`ESP32_BT_Controller_sp`)

1. Add to `TeleConConfig.h`:
   - `#define TELECON_APP_PREFIX "RC"`
   - `#define TELECON_PROTO_WIRE "simple"`
2. Add `handleRcConnect(app, line)` (ACK/NAK helpers like Greenhouse).
3. In the text-line dispatcher (where `RC:CTRL` / `RC:BTN` are handled), route `type == "CONNECT"` to `handleRcConnect`.
4. Optionally gate auto telemetry / plot until handshake is confirmed (recommended).
5. Update Serial banner: waiting for `RC:CONNECT,proto,simple`.

### 2) Classic Binary (`ESP32_BT_Controller`)

Binary control packets stay `AA 55` / `BB 66`, but the handshake is **text on the same SPP stream**.

1. Add a small line parser (or reuse Greenhouse-style `TeleConProtocol` + BT poll) so printable lines ending in `\n` are handled.
2. On `RC:CONNECT,...` call `handleRcConnect` with `TELECON_PROTO_WIRE "binary"`.
3. Keep existing binary RX for control; do not require binary for CONNECT.
4. NAK when app sends `proto,simple`.

Reference: Greenhouse Classic Binary `TeleConBluetooth.cpp` CONNECT branch.

### 3) BLE Binary (`TeleCon_ControlPanel_BLE_binary_ESP32`)

`handleRcConnect` already exists. Change it so:

```text
if proto != TELECON_PROTO_WIRE → NAK proto_mismatch
else → ACK
```

Do **not** ACK `proto,simple` on the BLE binary sketch. Keep CCCD / notify arming before ACK (as today).

---

## Acceptance checklist

- [ ] App Classic Simple ↔ Simple firmware → `RC:ACK`, session connected
- [ ] App Classic Binary ↔ Binary firmware → `RC:ACK`
- [ ] App Classic Binary ↔ Simple firmware → `RC:NAK proto_mismatch` → Android error dialog, not connected
- [ ] App Classic Simple ↔ Binary firmware → same NAK / dialog
- [ ] App Greenhouse CONNECT to RC board → `app_mismatch` NAK / dialog
- [ ] BLE Binary rejects `proto,simple` with NAK
- [ ] No silent “connected” without ACK

---

## Out of scope

- Do not change plot sample count / label-config removal in this prompt (see other Control Panel prompts).
- Do not remove Android parsers; only implement ESP32 replies.
- Do not alter Greenhouse firmwares except as a copy reference.
