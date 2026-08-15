# Cursor prompt: DevKit noCam Wi‑Fi Simple / Binary — Android + multi-app ESP32 base

Copy everything below into a **new Cursor chat** (Agent mode) on
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`.

Keep a second workspace root open (or read-only) for the Control Panel ESP32
reference firmwares:

`/home/miguel/Documents/ESP32_Projects/TeleCon_ControlPanel/`

Do **not** invent a new transport. SoftAP TCP control already exists for CAM Kit A
(`WifiSoftApDataTransferService`, `192.168.4.1:3333`). This work finishes **DevKit
noCam** Wi‑Fi Simple / Binary so they match the ESP32 sketches and Settings UI.

---

## Goal

1. **Android:** Make `WIFI_SIMPLE` and `WIFI_BINARY` work end-to-end for
   **Control Panel** (and any other **ESP32 noCam / DevKit** app that already offers
   those modes in Settings).
2. **Contract:** DevKit SoftAP uses the **same** text/binary wire formats as Classic
   Simple / Classic Binary / BLE Binary — only the link is SoftAP TCP.
3. **ESP32 base:** Treat Control Panel noCam Wi‑Fi sketches as the **canonical
   template** for other noCam apps (Greenhouse, Solar, Smart Home, …): copy SoftAP +
   TCP transport, swap app prefix / handlers.

---

## ESP32 reference (source of truth for SoftAP identity + proto)

| Mode | Path | SoftAP SSID | Password | TCP | `proto` on CONNECT |
|------|------|-------------|----------|-----|--------------------|
| Wi‑Fi Simple | `ESP32_noCam/WiFi_Simple/ESP32_WiFi_Controller_sp_starter/` | `ESP32-TC-RC-WiFi-Simple` | `telecon1234` | `192.168.4.1:3333` | `simple` (firmware also ACKs `wifi` for transition) |
| Wi‑Fi Binary | `ESP32_noCam/WiFi_Binary/TeleCon_ControlPanel_WiFi_binary_ESP32/` | `ESP32-TC-RC-WiFi-Binary` | `telecon1234` | `192.168.4.1:3333` | `binary` only |

**Do not confuse with CAM Kit A SoftAP:**

| Kit A CAM | SSID `TeleCon-RC-CAM` | `proto=wifi` | SIMPLE text only | may include HTTP `/stream` |

DevKit noCam SoftAPs have **no camera / no HTTP video**. Control is TCP only.

### Device name pattern (all Control Panel noCam links)

| Connection | Advertised / SoftAP name |
|------------|--------------------------|
| Classic Simple | `ESP32-TC-RC-BT-Simple` |
| Classic Binary | `ESP32-TC-RC-BT-Binary` |
| BLE Binary | `ESP32-TC-RC-BLE-Binary` |
| Wi‑Fi Simple | `ESP32-TC-RC-WiFi-Simple` |
| Wi‑Fi Binary | `ESP32-TC-RC-WiFi-Binary` |

Other noCam apps should follow the same pattern with their app token, e.g.
`ESP32-TC-GH-WiFi-Simple`, `ESP32-TC-SP-WiFi-Binary` (document in each firmware
`TeleConConfig.h` / starter header).

---

## Current Android gaps (fix these)

Read and fix:

| Issue | Where | Required behavior |
|-------|--------|-------------------|
| Handshake always sends `proto=wifi` on **any** WIFI transport | `ProtocolHandshake.wireProto()` | **CAM SoftAP / `WIFI_SOFTAP` only** → `wifi`. **`WIFI_SIMPLE`** → `simple`. **`WIFI_BINARY`** → `binary`. |
| Wi‑Fi sessions force SIMPLE CTRL/BTN | `BluetoothViewModel.currentRcProtocolMode()` / `restartRcDataSending()` | Force SIMPLE only for **`WIFI_SOFTAP`** (CAM). For **`WIFI_SIMPLE`** keep SIMPLE text. For **`WIFI_BINARY`** use ADVANCED binary (`AA 55` / `BB 66`) like Classic/BLE Binary. |
| SoftAP connect hardcodes SIMPLE | `connectToWifiSoftAp()` / `ensureWifiSoftApConnected()` | Pass the **requested session** `protocolMode` from Settings (`WIFI_SIMPLE` vs `WIFI_BINARY`), not always `SIMPLE`. |
| SoftAP UI / error copy always says `TeleCon-RC-CAM` | `strings.xml` / `strings-es.xml`, `Esp32SoftApDevice`, handshake timeout body | DevKit modes should mention the **DevKit SSID** (`ESP32-TC-RC-WiFi-Simple` / `…-Binary`) or a neutral “join the ESP32 SoftAP shown in the sketch docs”. Keep CAM strings for Kit A. |
| Unit test assumes all WIFI → `proto,wifi` | `ProtocolHandshakeTest.buildConnectLine_usesWifiWireValueForSoftAp` | Split: CAM SoftAP → `wifi`; DevKit Simple → `simple`; DevKit Binary → `binary`. |

`WifiSoftApDataTransferService` already frames both newline text and binary — keep it.
Do **not** put JPEG on TCP `:3333`.

---

## Android contract after the fix

### Handshake (phone → ESP32)

Use `ProtocolHandshake.buildConnectLine(appPrefix, protocolMode, transport)` but
`wireProto` must depend on **connection mode** (or board + protocol), not “WIFI ⇒ wifi”:

| Settings mode | Wire CONNECT |
|---------------|--------------|
| `WIFI_SOFTAP` (CAM Kit A) | `RC:CONNECT,proto,wifi` |
| `WIFI_SIMPLE` (DevKit) | `RC:CONNECT,proto,simple` |
| `WIFI_BINARY` (DevKit) | `RC:CONNECT,proto,binary` |
| Classic / BLE (unchanged) | `simple` / `binary` from protocol mode |

ESP32 reply (unchanged):

- Success: `RC:ACK,app,RC`
- Wrong app: `RC:NAK,reason,app_mismatch,expected,RC,actual,…`
- Wrong proto: `RC:NAK,reason,proto_mismatch,expected,<firmware>,actual,<phone>`

### Control / telemetry

| Mode | Phone → ESP32 | ESP32 → phone |
|------|---------------|---------------|
| `WIFI_SIMPLE` | `RC:CTRL,…` / `RC:BTN,id,N` (text) | `RC:DATA` / `RC:PLOT` |
| `WIFI_BINARY` | `AA 55` (18 B) / `BB 66` (4 B) | `CC 11` / `CC 22` / `CC 33` |
| `WIFI_SOFTAP` (CAM) | SIMPLE text only (keep heartbeat CTRL) | SIMPLE `RC:DATA` (HUD) |

SoftAP CTRL heartbeat (≤ ~200 ms, fail-safe on firmware) stays for **SIMPLE SoftAP**
modes (`WIFI_SOFTAP`, `WIFI_SIMPLE`). For **`WIFI_BINARY`**, use the same ~50 ms
advanced RC loop as Classic/BLE Binary (or a SoftAP-tuned period if needed) — do **not**
send text `RC:CTRL` on binary SoftAP.

### Connect UX (DevKit)

1. User selects **Wi‑Fi SoftAP + Simple** or **+ Binary** in App Settings (already in
   `ApplicationProtocolSupport` for DevKit).
2. User joins SoftAP in system Wi‑Fi settings (`ESP32-TC-RC-WiFi-Simple` or `…-Binary` /
   `telecon1234`).
3. App **Connect** opens TCP `192.168.4.1:3333` via existing SoftAP path
   (`ApplicationBluetoothSessionHost` → `connectToWifiSoftAp()`), **without** BT picker.
4. Handshake must match the selected mode; mismatch → existing error dialog.

Optional polish (nice-to-have, not blocking): show SoftAP SSID/password hint on the
Control Panel connect banner for DevKit Wi‑Fi modes (reuse / extend strings used for CAM).

---

## Concrete Android work

### 1) Handshake wire proto

Files:

- `domain/bluetooth/ProtocolHandshake.kt`
- `domain/bluetooth/BluetoothConnectionMode.kt` (already documents intended protos)
- Prefer extending `buildConnectLine` to take `BluetoothConnectionMode` **or**
  `(transport, protocolMode, board)` so CAM SoftAP stays `wifi` while DevKit SoftAP
  uses `simple`/`binary`.

Update tests in `ProtocolHandshakeTest.kt`.

### 2) Stop forcing SIMPLE on all Wi‑Fi

Files:

- `ui/bluetooth/BluetoothViewModel.kt`
  - `currentRcProtocolMode()`
  - `restartRcDataSending()`
  - `connectToWifiSoftAp()` / session context creation
- Any RC Vehicle Pro SoftAP path that still assumes “Wi‑Fi ⇒ SIMPLE forever” must
  distinguish **CAM SoftAP** vs **DevKit Wi‑Fi Binary**.

Rule of thumb:

```text
if mode == WIFI_SOFTAP → SIMPLE (+ proto wifi)
if mode == WIFI_SIMPLE → SIMPLE (+ proto simple)
if mode == WIFI_BINARY → ADVANCED (+ proto binary)
```

### 3) SoftAP identity / copy for DevKit

Files:

- `domain/camera/Esp32CameraDefaults.kt` — keep CAM defaults; do **not** overwrite CAM SSID
- Prefer a small **DevKit SoftAP defaults** helper (new) or constants next to
  `Esp32SoftApDevice`, e.g. host/port shared, SSID per mode:
  - Simple → `ESP32-TC-RC-WiFi-Simple`
  - Binary → `ESP32-TC-RC-WiFi-Binary`
  - Password → `telecon1234`
- `res/values/strings.xml` + `res/values-es/strings.xml` — DevKit Wi‑Fi descriptions
  already say `proto=simple` / `proto=binary`; align any “join TeleCon-RC-CAM” text so
  it only applies to CAM Kit A / `WIFI_SOFTAP`.

### 4) Apps in scope (Android Settings already list Wi‑Fi Simple/Binary on DevKit)

Primary: **Control Panel** (`ApplicationId.CONTROL_PANEL`, prefix `RC`).

Same SoftAP TCP stack should work for other noCam apps once handshake uses their
prefix (`GH`, `SP`, …) — verify Greenhouse / others still coerce CAM vs DevKit correctly
in `ApplicationProtocolSupport.kt`. Do not break CAM Kit A for RC Vehicle Pro.

### 5) Docs (Android)

Update briefly (no large rewrite):

- `docs/SIMPLE_PROTOCOL_ESP32.md` — add DevKit SoftAP section: `proto=simple` over TCP
- `docs/BINARY_PROTOCOL_APPS.md` — note SoftAP TCP carries the same binary frames
- Keep CAM SoftAP `proto=wifi` documented as Kit A–only

---

## ESP32 multi-app base (how to reuse Control Panel Wi‑Fi)

When porting to another **noCam** app (e.g. Greenhouse):

### Simple starter (one file)

1. Copy
   `TeleCon_ControlPanel/ESP32_noCam/WiFi_Simple/ESP32_WiFi_Controller_sp_starter/`
2. Rename folder / `.ino` to the app.
3. Change:
   - `AP_SSID` → `ESP32-TC-<APP>-WiFi-Simple` (e.g. `GH`)
   - `APP_PREFIX` → app wire prefix (`GH`, `SP`, …)
   - `PROTO_WIRE` → `"simple"`
   - Handlers: map CTRL/BTN (or app-specific line types) to that app’s actuators
4. Keep SoftAP + TCP `:3333` + line parser + ACK/NAK pattern.

### Binary (full architecture)

1. Copy
   `TeleCon_ControlPanel/ESP32_noCam/WiFi_Binary/TeleCon_ControlPanel_WiFi_binary_ESP32/`
2. Keep transport modules as-is:
   - `TeleConWifi.*` (SoftAP + TCP)
   - `TeleConBinaryRx.*` / `TeleConProtocol.*`
3. Replace Control Panel–specific pieces:
   - `TELECON_AP_SSID` / `TELECON_APP_PREFIX` / `TELECON_PROTO_WIRE "binary"`
   - `ControlPanelHandlers.*` / `ControlPanelControl.*` → app handlers (mirror BLE/Classic Binary for that app)
4. Do **not** add camera HTTP on noCam SoftAP builds.

### Naming / handshake rules for every noCam Wi‑Fi port

- SoftAP SSID must encode app + mode (see pattern above).
- ACK only the firmware’s `TELECON_PROTO_WIRE` (`simple` or `binary`).
- CAM `proto=wifi` is **not** used on DevKit noCam Simple/Binary (Simple may still ACK
  `wifi` temporarily; Binary must NAK `wifi` / `simple`).

---

## Acceptance checklist

### Android ↔ Control Panel Wi‑Fi Simple

- [ ] Settings = Wi‑Fi SoftAP + Simple
- [ ] Join `ESP32-TC-RC-WiFi-Simple` / `telecon1234`
- [ ] Connect → TCP open → `RC:CONNECT,proto,simple` → `RC:ACK,app,RC`
- [ ] Sticks send `RC:CTRL`; UI updates from `RC:DATA` / `RC:PLOT` (bench echo)
- [ ] Wrong firmware (Binary SoftAP) → NAK / mismatch dialog

### Android ↔ Control Panel Wi‑Fi Binary

- [ ] Settings = Wi‑Fi SoftAP + Binary
- [ ] Join `ESP32-TC-RC-WiFi-Binary` / `telecon1234`
- [ ] Connect → `RC:CONNECT,proto,binary` → ACK
- [ ] Sticks send `AA 55` (not text CTRL); telemetry `CC 11/22/33`
- [ ] App Classic Binary settings ↔ this SoftAP Binary board works the same wire format

### Regression — CAM Kit A (RC Vehicle Pro)

- [ ] `WIFI_SOFTAP` still sends `RC:CONNECT,proto,wifi`
- [ ] Still SIMPLE text CTRL heartbeat on `:3333`
- [ ] Video still HTTP `/stream` (not on TCP control)
- [ ] SSID hint still `TeleCon-RC-CAM`

### Tests

- [ ] `ProtocolHandshakeTest` covers `wifi` / `simple` / `binary` SoftAP cases
- [ ] Existing SoftAP ViewModel tests updated (no “always SIMPLE even if ADVANCED requested”
      for **DevKit** `WIFI_BINARY`; keep that rule only for CAM SoftAP if still required)

---

## Out of scope

- Writing `.tex` documentation for ESP32 sketches
- Changing Classic / BLE Bluetooth names (already `ESP32-TC-RC-BT-*` / `…-BLE-Binary`)
- Implementing Greenhouse / other app Wi‑Fi firmwares in this pass (document the copy
  recipe only; optional follow-up chats per app)
- SoftAP + BLE on one ESP32-CAM (unsupported)
- Putting video on TCP `:3333`

---

## Suggested implementation order

1. Fix `ProtocolHandshake` + unit tests (`simple` / `binary` / `wifi` split).
2. Fix `BluetoothViewModel` so `WIFI_BINARY` sends binary and uses session protocol mode.
3. Align SoftAP connect path + DevKit SSID / strings (without breaking CAM defaults).
4. Manual smoke test against Control Panel Wi‑Fi Simple and Wi‑Fi Binary sketches.
5. Short doc updates in `SIMPLE_PROTOCOL_ESP32.md` / `BINARY_PROTOCOL_APPS.md`.
