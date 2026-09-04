# TeleCon binary protocol (Control Panel / RC Vehicle)

Compact binary frames for Classic SPP, BLE (Nordic UART), and **DevKit SoftAP TCP**.
Used when the Android app selects **Classic + Binary**, **BLE + Binary**, or
**Wi‑Fi SoftAP + Binary**. (**Classic + Simple** / **Wi‑Fi SoftAP + Simple** use the
text-line protocol in [SIMPLE_PROTOCOL_ESP32.md](SIMPLE_PROTOCOL_ESP32.md) instead;
BLE is binary-only. Kit A CAM SoftAP uses SIMPLE text with `proto=wifi`, not these frames.)

Control Panel and RC Vehicle Pro share the same RC wire protocol.

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
See [SIMPLE_PROTOCOL_ESP32.md](SIMPLE_PROTOCOL_ESP32.md).

**Bench debug (RC Control Panel):** default firmware simulate mode should echo phone
sticks/knobs/switches into `CC 11` / `CC 22` / `CC 33` (same UX as Classic Simple text
echo). Optional Serial inject forwards typed lines/frames to the phone.

---

## Android files

| Area | Path |
|------|------|
| Inbound routing | `data/bluetooth/AndroidBluetoothController.kt` |
| Frame assembly | `data/bluetooth/BluetoothFrameAssembler.kt` |
