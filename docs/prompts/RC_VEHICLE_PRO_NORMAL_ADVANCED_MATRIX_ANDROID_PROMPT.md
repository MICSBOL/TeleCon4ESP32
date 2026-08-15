# Cursor prompt: RC Vehicle Pro — Normal / Advanced matrix (Android)

**Status: implemented (SoftAP Binary portable base).**  
Chosen approach: shared `WIFI_SIMPLE` / `WIFI_BINARY` SoftAP TCP modes (board selects SSID + camera), not RC-only `proto=wifi`.

Firmware: `/home/miguel/Documents/ESP32_Projects/TeleCon_RcVehiclePro/README.md`

---

## Approach (portable SoftAP base)

Use the **same SoftAP connection modes** as Control Panel / future Greenhouse:

| Mode | Wire | DevKit SSID | CAM SSID (camera apps) |
|------|------|-------------|-------------------------|
| `WIFI_CAM_STARTER` | `proto=simple` | — | `TeleCon-RC-CAM-Starter` |
| `WIFI_SIMPLE` | `proto=simple` | `ESP32-TC-{PREFIX}-WiFi-Simple` | main CAM SoftAP (if offered) |
| `WIFI_BINARY` | `proto=binary` | `ESP32-TC-{PREFIX}-WiFi-Binary` | `TeleCon-RC-CAM` |

- `CameraLinkProfile.WIFI_SOFTAP` = SoftAP **HTTP video + TCP control** (protocol from mode).
- Legacy `WIFI_SOFTAP` enum (`proto=wifi`) kept only for DataStore migration → coerce to `WIFI_BINARY`.
- Kit B (`BLE_BINARY` on CAM board) removed from product CAM settings (lab only).

### RC Vehicle product settings

| Board | Normal | Advanced |
|-------|--------|----------|
| DevKit | Classic Simple, Wi‑Fi Simple | Classic Binary, BLE Binary, Wi‑Fi Binary |
| CAM | CAM SoftAP Starter | CAM SoftAP + Binary (`WIFI_BINARY`) |

Greenhouse / other camera apps reuse the same CAM list (`WIFI_CAM_STARTER` + `WIFI_BINARY`).

---

## Done in app

- `availableConnectionModes(CAM)` → starter + `WIFI_BINARY`
- `resolveCameraLinkProfile`: SoftAP TCP modes → SoftAP video+TCP; Kit B profile retained for lab coerce
- SoftAP CTRL/handshake: `WIFI_BINARY` uses ADVANCED binary (no force-SIMPLE on CAM SoftAP)
- `Esp32SoftApDevice.forConnectionMode(..., board)` picks CAM vs DevKit SSID
- Strings en/es for CAM SoftAP Binary; fail copy updated
- Unit tests updated for handshake / modes / camera profile / ViewModel SoftAP Binary

---

## Manual verify

1. CAM Advanced → SoftAP Binary → join `TeleCon-RC-CAM` → `proto,binary` + sticks as `AA 55`
2. CAM Normal → Starter → `TeleCon-RC-CAM-Starter` → `proto,simple`
3. DevKit Wi‑Fi Binary unchanged (`ESP32-TC-RC-WiFi-Binary`)
4. Settings CAM: no Kit B, no legacy `proto=wifi` Advanced option
