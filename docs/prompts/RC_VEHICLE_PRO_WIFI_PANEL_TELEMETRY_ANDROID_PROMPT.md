# Cursor prompt: RC Vehicle Pro — SoftAP panel TX + telemetry RX (Android)

Copy into a **new Cursor chat** (Agent mode) on
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`.

---

## Goal

Make **Wi‑Fi SoftAP** mode reliably:

1. Send **panel control** as **SIMPLE text** `RC:CTRL` / `RC:BTN` to ESP32 TCP `:3333`
2. Receive **telemetry** `RC:DATA` from ESP32 and update the RC Vehicle Pro HUD
3. Optionally Logcat-debug TX/RX (parity with ESP32 Serial `[PANEL RX]` / `[TELEM TX]`)

ESP32 firmware (already updated):

`TeleCon_RcVehiclePro/ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi/`

- SoftAP `TeleCon-RC-CAM` / `telecon1234`
- TCP `192.168.4.1:3333`
- Handshake `RC:CONNECT,proto,wifi` → `RC:ACK,app,RC`
- Always emits `RC:DATA,...` ~every 500 ms after ACK
- Serial shows `[PANEL RX]` and `[TELEM TX]`

---

## Critical bug to fix

SoftAP TCP firmware **only speaks SIMPLE text lines**.

If `BluetoothViewModel.currentRcProtocolMode()` / sending loops use
`BluetoothProtocolMode.ADVANCED`, the app sends **`AA 55` binary** over TCP.
ESP32 cannot parse that as `RC:CTRL` → **no `[PANEL RX]` on Serial**, no motors.

### Required

When `activeSession.transport == WIFI` **or** connection mode is `WIFI_SOFTAP`:

- Force **SIMPLE** for `sendRcControl` / `sendRcButton` / handshake already uses `proto=wifi`
- Do **not** start the ADVANCED 50 ms binary loop on SoftAP sessions
- Persisted “Classic Binary” setting must not override SoftAP text protocol

Check: `startSendingRcData()`, `restartRcDataSending()`, `currentRcProtocolMode()`,
`connectToWifiSoftAp()`, `sendRcControl` helpers.

---

## Work items

### 1. Force SIMPLE over WIFI transport

- `currentRcProtocolMode()` → if session/transport is WIFI, return `SIMPLE`
- Or branch `restartRcDataSending()` on `transport == WIFI` → always `startSimpleRcSendingOnChange()`

### 2. Ensure CTRL is sent while HUD is open

- `RcVehicleProScreen` already calls `onControlPanelEntered()` → `startSendingRcData()`
- Confirm this runs **after** SoftAP handshake completes (session established)
- If handshake finishes after screen open, restart sending on `SessionEstablished` / `isConnected`

### 3. Telemetry → HUD

ESP32 sends e.g.:

```text
RC:DATA,left,100,right,0,lo,1,ro,0,lg,1,rg,0,analog,90,batt,76,led,01
```

- `AndroidBluetoothController.parseIncomingLine` already maps `RC:DATA` via
  `SimpleProtocolTelemetryMapper.applyRcData`
- Confirm SoftAP `WifiSoftApDataTransferService.listenForIncoming()` emits `TextLine`s
  into the same path (already wired in `connectToDevice` WIFI branch)
- `RcVehicleProViewModel` already combines `remoteController.telemetryState` → speed/batt/temp
- Verify HUD updates when DATA arrives (speed = left/10, batt %, motor temp from analog)

### 4. Debug Logcat (optional but requested parity)

Add debug logs (tag e.g. `RcWifiSoftAp`):

- TX: each `RC:CTRL` / `RC:BTN` / `RC:CONNECT` (throttle CTRL if noisy)
- RX: each `RC:DATA` / `RC:ACK` / `RC:NAK`

So Logcat mirrors ESP32 Serial `[PANEL RX]` / `[TELEM TX]`.

### 5. UX copy if TCP up but no CTRL

If connected + camera works but sticks do nothing, surface a hint that SoftAP
requires SIMPLE text (should not happen once force-SIMPLE is fixed).

### 6. Strings en + es

Only if new user-visible errors/hints are added.

---

## Manual test

1. Flash WiFi CAM firmware; Serial @ 115200  
2. Phone joins `TeleCon-RC-CAM` / `telecon1234`  
3. App: RC Vehicle settings → **Wi‑Fi SoftAP** → open HUD → Connect  
4. Serial must show:
   - `[TCP] client connected`
   - `[RX] RC:CONNECT,proto,wifi`
   - `[TX] RC:ACK,app,RC`
   - `[PANEL RX] ...` when moving sticks
   - `[TELEM TX] ...` / `[TX] RC:DATA,...` ~2 Hz  
5. Android HUD battery/speed update from telemetry  
6. Logcat shows matching TX/RX if debug added  

## Do not

- Send binary `AA 55` / `BB 66` on SoftAP TCP  
- Put JPEG on port 3333  
- Gate camera stream on Bluetooth  

## Acceptance

- [ ] SoftAP session always sends SIMPLE `RC:CTRL`  
- [ ] ESP32 Serial shows `[PANEL RX]` when using the HUD  
- [ ] ESP32 Serial shows `[TELEM TX]` / `RC:DATA` periodically  
- [ ] App HUD reflects `batt` / speed / analog from `RC:DATA`  
- [ ] en+es only if new UI strings  
