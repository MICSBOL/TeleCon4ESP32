# Cursor prompt: RC Vehicle Pro — drive assist (phone-side only)

Copy into a **new Cursor chat** only if firmware work is being planned for
TeleCon RC Vehicle Pro. **Most of this feature needs no ESP32 changes.**

---

## Verdict

| Feature | Where it runs | ESP32 change? |
|---------|---------------|---------------|
| Dual-rate travel (THR/STR 50/75/100%) | Android `RcStickMapping` | **No** |
| Expo (soft center) | Android | **No** |
| Channel reverse | Android | **No** |
| Adjustable deadzone | Android | **No** |
| Spring / Hold stick modes | Android UI | **No** |
| Persist assist prefs | Android DataStore | **No** |
| Live steer trim while nudging | Android adds offset to `rx` | **No** (live values only) |
| **Lock / save steer center** | Android + firmware NVS | **Yes** — see existing prompt |

Android already maps sticks **before** TX on every link:

`raw → deadzone → expo → travel → reverse → steer trim` → protocol encode

ESP32 continues to receive normal stick channels in **−1…1** (Simple) or equivalent
binary units. Do **not** re-apply expo/travel/deadzone/reverse on the MCU for the
same user preference — that would double-map.

---

## Connection matrix (no new commands)

Drive assist does **not** add SET keys, button IDs, or CAM endpoints.

| Kit / mode | Video | Control transport | Protocol | Drive assist impact |
|------------|-------|-------------------|----------|---------------------|
| Classic DevKit, no cam | — | Classic SPP | Simple `RC:CTRL` | Mapped sticks in payload |
| Classic DevKit, no cam | — | Classic SPP | Binary `AA 55` | Mapped sticks in payload |
| BLE DevKit, no cam | — | BLE NUS | Binary `AA 55` | Mapped sticks in payload |
| Kit A SoftAP (CAM) | HTTP `/stream` | TCP `:3333` | Simple or Binary | Mapped sticks on TCP |
| Kit B SoftAP video + BLE control | SoftAP HTTP | BLE NUS | Binary | Mapped sticks on BLE; video unchanged |
| SoftAP + Binary | SoftAP | TCP `:3333` | Binary | Mapped sticks on TCP |

Camera / SoftAP performance / `/camconfig` are **unrelated**. Do not change stream
URLs, SSIDs, or telemetry formats for drive assist.

---

## Only firmware work still required

If **steer center lock** (✓ after trim tickers) is not implemented yet, follow:

[`RC_VEHICLE_PRO_STEER_CENTER_TRIM_ESP32_PROMPT.md`](./RC_VEHICLE_PRO_STEER_CENTER_TRIM_ESP32_PROMPT.md)

Summary of that contract (unchanged by drive assist):

| Mode | Save-center command |
|------|---------------------|
| Simple (Classic SPP or SoftAP TCP) | `RC:SET,steer_center,1,rx,<n>` |
| Binary (Classic / BLE / SoftAP TCP) | `BB 66` button id **`0x10`** (`ButtonEvent.STEER_CENTER_SAVE`) |

After save, firmware should treat stick `rx = 0` as mechanical straight (NVS).

---

## Android source of truth

Repo: `/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`

| Topic | Path |
|-------|------|
| Pipeline | `ui/rc_vehicle_pro/RcStickMapping.kt` |
| Settings model | `domain/model/RcVehicleProControlSettings.kt` |
| Persist | `data/repository/SettingsRepository.kt` (`rc_vehicle_pro_*` keys) |
| UI | `RcVehicleProScreen.kt`, `components/RcDriveAssistDialog.kt`, travel chips in `RcCenterControls` |
| Steer save TX | `BluetoothViewModel.saveSteerCenter()`, `SimpleProtocolEncoder.buildSteerCenterSaveLine` |

---

## Explicit non-goals for firmware

- No `RC:SET` for travel, expo, reverse, or deadzone.
- No NVS for phone drive-assist prefs (phone owns them).
- No change to stick packet layout, channel count, or sign convention.
- No CAM firmware changes for this feature.
- Do not invert channels on ESP32 to “match” Android reverse — Android already
  reverses before TX.

---

## Acceptance (firmware repo)

If only reviewing for drive assist:

1. Confirm sticks still map −1…1 (or binary equivalent) with no extra expo/rate stage.
2. Confirm no new protocol tokens were added for assist.
3. If steer-center save is missing, implement the linked prompt for **all** rows in the
   connection matrix above that carry control.

If those hold, **close with no firmware PR** for dual-rate / expo / reverse / deadzone.
