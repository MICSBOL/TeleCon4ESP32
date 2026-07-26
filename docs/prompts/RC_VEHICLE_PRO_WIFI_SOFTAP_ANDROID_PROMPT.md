# Cursor prompt: RC Vehicle Pro — Wi‑Fi SoftAP control + camera (Android)

Copy into a **new Cursor chat** (Agent mode) on
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`.

---

## Goal

Add a **Wi‑Fi SoftAP** connection mode for **RC Vehicle Pro** so a single ESP32-CAM
can carry **both** live video and RC control (no Bluetooth).

Firmware (already in ESP32 repo):

`TeleCon_RcVehiclePro/ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi/`

| Item | Value |
|------|--------|
| SoftAP SSID | `TeleCon-RC-CAM` |
| SoftAP password | `telecon1234` |
| Base URL | `http://192.168.4.1` |
| Video (preferred) | `GET /stream` MJPEG via `Esp32CameraStreamRepository` |
| Video (fallback) | `GET /capture` uncapped poll |
| Status | `GET /status` → JSON |
| Control | TCP **`192.168.4.1:3333`**, newline-terminated `RC:` text |
| Handshake | `RC:CONNECT,proto,wifi` → `RC:ACK,app,RC` / `RC:NAK,...` |
| Control lines | Same as Classic Simple: `RC:CTRL,...` / `RC:BTN,id,N` |
| Telemetry | `RC:DATA,...` (~2 Hz) |

**Do not** send video over the TCP control socket.
See also: `RC_VEHICLE_PRO_CAMERA_STREAM_ANDROID_PROMPT.md` (smooth MJPEG).

---

## Product UX (final user)

1. Settings: board **ESP32-CAM**, connection **Wi‑Fi SoftAP**.
2. In-app guidance: join Wi‑Fi `TeleCon-RC-CAM` / `telecon1234` (hint: no internet while connected).
3. Connect opens TCP `:3333`, sends handshake, then streams CTRL like Classic Simple.
4. Camera starts `GET /stream` (MJPEG), falls back to `/capture` (`RcVehicleProViewModel`).
5. Disconnect / leave SoftAP → stop TCP + stop camera + fail-safe on ESP32.

---

## Android work items

### 1. New connection mode

Extend `BluetoothConnectionMode` / app settings (or add a parallel “link mode”) with:

- **Wi‑Fi SoftAP** (RC Vehicle Pro / ESP32-CAM)

Keep Classic Simple / Classic Binary / BLE Binary for DevKit / noCam.

Strings **en + es**: title, description (join SoftAP first; control over Wi‑Fi; no BT).

### 2. Wi‑Fi RC transport

Add a data transport (mirror `BluetoothDataTransport` / line framing):

- Connect TCP to `Esp32CameraDefaults.DEFAULT_BASE_URL` host + port **3333**
  (configurable later; default `192.168.4.1:3333`).
- Send/receive `\n`-terminated lines.
- On connect: `ProtocolHandshake.buildConnectLine` with **`proto=wifi`**
  (extend handshake if it only knows `simple`/`binary`).
- Parse `RC:ACK` / `RC:NAK` like Classic (reuse `ProtocolHandshake`).
- After ACK: send `RC:CTRL` / `RC:BTN` from existing RC control state
  (same encoder path as Classic Simple text).
- Parse inbound `RC:DATA` into existing RC telemetry models.
- On socket loss: mark disconnected; stop CTRL; show clear error
  (not the Classic “BLE-only firmware” dialog).

### 3. Wire into RC Vehicle Pro session

- When mode is Wi‑Fi SoftAP, **do not** start Classic/BLE discovery.
- “Connect” = ensure SoftAP reachability (optional `GET /status`) + open TCP + handshake.
- `RcVehicleProScreen` camera uses `/stream` then `/capture` — keep it;
  **do not** gate camera on Bluetooth.
- Failures: SoftAP unreachable vs handshake NAK (`proto_mismatch`) with dedicated copy.

### 4. Settings / help copy

Document:

- SSID / password / URL / TCP port  
- Phone leaves home Wi‑Fi while on SoftAP  
- Classic Simple CAM is **not** supported on one ESP32-CAM (radio conflict)

### 5. Cleartext

`network_security_config` already allows cleartext HTTP; ensure TCP to `192.168.4.1`
is allowed (same LAN/SoftAP).

### 6. Tests

- Unit: handshake `wifi` ACK/NAK parsing  
- Optional: mock TCP line session for CTRL/DATA  

---

## Acceptance

- [ ] Settings → Wi‑Fi SoftAP → join SoftAP → Connect succeeds with `RC:ACK`
- [ ] Sticks send `RC:CTRL`; HUD updates from `RC:DATA`
- [ ] Live `/stream` (or `/capture` fallback) frames in `RcCameraPreview`
- [ ] Wrong proto firmware → NAK dialog, no stuck session
- [ ] Disconnect / leave AP → safe stop; no Classic/BLE error copy
- [ ] en + es strings for new mode and errors

## Do not

- Require Bluetooth for this mode  
- Put JPEG on the TCP control socket  
- Remove Classic/BLE modes for noCam DevKit users  
- Invent a new app prefix (keep **`RC`**)

## Firmware reference

See ESP32 sketch README:
`TeleCon_RcVehiclePro/ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi/README.md`
