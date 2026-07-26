# Cursor prompt: TeleCon_ControlPanel — Classic vs BLE transport mismatches

Copy into a **new Cursor chat** on `/home/miguel/Documents/ESP32_Projects/TeleCon_ControlPanel/`
if you need firmware-side docs or Serial banners clarified. **Most of this mismatch is
handled on Android** (link fails before `RC:CONNECT`).

---

## What the user saw

App Settings = **Classic + Simple**, ESP32 = **BLE-only** Control Panel firmware.

Classic SPP (RFCOMM) cannot open against a BLE-only stack → Android shows
**“Classic Bluetooth link failed”** (not a handshake NAK). Handshake never runs.

| App setting | ESP32 firmware | What happens |
|-------------|----------------|--------------|
| Classic Simple | Classic Simple | RFCOMM OK → `RC:CONNECT,proto,simple` → ACK |
| Classic Binary | Classic Binary | RFCOMM OK → `RC:CONNECT,proto,binary` → ACK |
| BLE Binary | BLE Binary | GATT/NUS OK → `RC:CONNECT,proto,binary` → ACK |
| Classic Simple/Binary | BLE only | **RFCOMM fails** (link error) — no CONNECT |
| BLE Binary | Classic only | **BLE open fails** (link error) — no CONNECT |
| Classic Binary | Classic Simple | RFCOMM OK → CONNECT → **NAK proto_mismatch** |
| Classic Simple | Classic Binary | RFCOMM OK → CONNECT → **NAK proto_mismatch** |
| Wrong app (e.g. GH) | RC firmware | CONNECT → **NAK app_mismatch** |
| No ACK/NAK | Any | **Handshake timeout** dialog |

---

## Android (already updated)

- Classic RFCOMM failure → `BluetoothConnectFailure.ClassicLinkFailed` → clear Spanish/English copy telling user to pick **BLE Binary** or flash Classic firmware.
- BLE GATT failure → `BleLinkFailed` → pick Classic mode or flash BLE firmware.
- Handshake NAK / timeout → existing handshake dialogs.

No ESP32 change is required for the Classic-settings + BLE-firmware case: the board
correctly has no Classic SPP listener.

---

## Optional ESP32 polish (this prompt)

### BLE sketch (`ESP32_noCam/BLE/...`)

1. Serial banner on boot:
   - `This build is BLE-only. In the Android app select: BLE Binary.`
   - `Classic Simple / Classic Binary will fail at RFCOMM (expected).`
2. Keep `TELECON_PROTO_WIRE "binary"` and NAK `proto,simple` on CONNECT (see handshake prompt).

### Classic Simple / Classic Binary sketches

1. Serial banner:
   - Simple: `App Settings must be Classic Simple (not BLE).`
   - Binary: `App Settings must be Classic Binary (not BLE / not Simple).`
2. Implement `RC:CONNECT` ACK/NAK per `CONTROL_PANEL_HANDSHAKE_ESP32_PROMPT.md`.

### Do not

- Do not try to make BLE firmware accept Classic RFCOMM.
- Do not soft-ACK wrong `proto` on a single-protocol build.

---

## Quick test plan (intentional mismatches)

1. BLE firmware + Classic Simple in app → Classic link failed dialog (no socket dump).
2. Classic Simple firmware + BLE Binary in app → BLE link failed dialog.
3. Classic Simple firmware + Classic Binary in app → Protocol doesn’t match (NAK).
4. Matching pair → ACK and connected session.
