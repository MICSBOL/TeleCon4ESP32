# Cursor prompt: ESP32-CAM `/camconfig` Serial debug (`TELECON_DEBUG` only)

Copy everything below into a **new Cursor chat** (Agent mode) on the ESP32 firmware tree
(`/home/miguel/Documents/ESP32_Projects/`).

Android (`TeleCon4ESP32`) already sends stream quality over Wi‑Fi:

```
GET http://192.168.4.1/camconfig?framesize=qvga&quality=22&fps=10
```

when the user taps **Smooth / Balanced / High** on the live camera screen (RC Vehicle Pro or
Control Panel). **Done** only closes the dialog — it does **not** send HTTP. HUD rate is
Android-only and must never appear on Serial.

HTTP contract (do not change):  
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/docs/prompts/ESP32_CAM_CAMCONFIG_RUNTIME_ESP32_PROMPT.md`

---

## Goal

1. Every ESP32-CAM sketch that serves `/stream` or `/capture` must handle **`GET /camconfig`**.
2. When a request arrives, print **one clear line on Arduino Serial Monitor (115200)** so you
   can see the params Android sent.
3. **All Serial prints for this feature must be inside `#if TELECON_DEBUG` only.**
   Release builds (`TELECON_DEBUG 0`) compile to no-ops — no extra Serial traffic, no spam.

Do **not** log every MJPEG frame. One line per `/camconfig` request (plus one boot hint) is enough.

---

## Debug gating (required pattern)

Reuse each sketch’s existing `TeleConConfig.h` / `TeleConDebug.h` (`DBG_PRINTF` / `DBG_PRINTLN`).

If a sketch has no macros yet, add:

```c
#ifndef TELECON_DEBUG
#define TELECON_DEBUG 1   // 0 = production: no Serial camconfig logs
#endif

#if TELECON_DEBUG
  #define DBG_PRINTF(...) Serial.printf(__VA_ARGS__)
  #define DBG_PRINTLN(x)  Serial.println(x)
#else
  #define DBG_PRINTF(...) ((void)0)
  #define DBG_PRINTLN(x)  ((void)0)
#endif
```

**Every camconfig log must be wrapped**, including error paths:

```cpp
#if TELECON_DEBUG
  DBG_PRINTF("[CAM] GET /camconfig %s\n", query);
#endif

// after apply:
#if TELECON_DEBUG
  DBG_PRINTF("[CAM] camconfig applied framesize=%s quality=%d fps=%d\n",
             token, quality, fps);
#endif

#if TELECON_DEBUG
  DBG_PRINTF("[CAM] camconfig FAIL %d %s\n", status, err);
#endif
```

Prefer `#if TELECON_DEBUG` (preprocessor) over runtime `if (TELECON_DEBUG)` so dead code is
stripped when debug is off. `DBG_PRINTF` already no-ops when `TELECON_DEBUG` is 0; still keep
the `#if` around camconfig logs so the intent is obvious.

**Do not** print camconfig lines when `TELECON_DEBUG` is 0.

---

## Required Serial lines (`TELECON_DEBUG == 1`)

Arduino IDE Serial Monitor @ **115200**.

| When | Line |
|------|------|
| Boot (once) | `[CAM] HTTP GET /camconfig  (Smooth/Balanced/HQ from phone)` |
| Incoming GET | `[CAM] GET /camconfig framesize=qvga&quality=22&fps=10` |
| Applied 200 | `[CAM] camconfig applied framesize=qvga quality=22 fps=10` |
| Bad query 400 | `[CAM] camconfig FAIL 400 invalid framesize` |
| Camera down 503 | `[CAM] camconfig FAIL 503 camera not ready` |

Examples Android will send:

| Phone preset | Query |
|--------------|--------|
| Smooth | `framesize=qvga&quality=22&fps=10` |
| Balanced | `framesize=vga&quality=15&fps=0` |
| High quality | `framesize=vga&quality=12&fps=0` |

Log **every** GET, even if params are unchanged (Android may skip duplicates; if firmware
receives it, print it). That is how you confirm the phone reached the CAM.

---

## How to test (Arduino IDE)

1. Set `#define TELECON_DEBUG 1` in that sketch’s `TeleConConfig.h`.
2. Flash. Open Serial Monitor **115200**.
3. Phone joins SoftAP (`TeleCon-RC-CAM` / `telecon1234`).
4. Open RC Vehicle Pro (or Control Panel camera pane) → **Stream quality**.
5. Tap **Smooth** (not Done) → Serial must show `GET /camconfig` then `applied framesize=qvga…`.
6. Set `TELECON_DEBUG 0`, rebuild — those lines must disappear.

---

## Sketches to update

Work in **canonical shared files first**, then copy into each sketch folder (Arduino IDE
does not compile `common/` automatically).

### Already have `/camconfig` — add/unify debug logs only

Canonical module:

`/home/miguel/Documents/ESP32_Projects/TeleCon_RcVehiclePro/common/CameraStreamCamConfig.{h,cpp}`

Then copy into:

| Sketch | Path |
|--------|------|
| Kit A Advanced | `TeleCon_RcVehiclePro/ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi_Binary/` |
| Kit A Normal | `TeleCon_RcVehiclePro/ESP32_cam/WiFi/TeleCon_RcVehiclePro_CAM_WiFi_Simple/` |
| Kit B video | `TeleCon_RcVehiclePro/ESP32_cam/lab/TeleCon_RcVehiclePro_CAM_SoftAP_Video/` |
| BLE CAM lab | `TeleCon_RcVehiclePro/ESP32_cam/lab/TeleCon_RcVehiclePro_CAM_BLE_binary/` |

Today `applyParsed()` already prints `camconfig qvga q=22 fps=10` under `#if TELECON_DEBUG`.
**Upgrade** to the `[CAM] GET …` / `[CAM] camconfig applied …` / `[CAM] camconfig FAIL …`
lines above. Log the **incoming query** before apply (including drain-pending `/stream` path).

Leave `legacy_CAM_WiFi_text_proto_wifi` alone unless you are maintaining it.

### Missing `/camconfig` — add handler + debug logs

| Sketch | Path | Notes |
|--------|------|--------|
| Greenhouse CAM | `TeleCon_Greenhouse/ESP32_cam/Classic_Binary/TeleCon_Greenhouse_CAM/` | Has `/capture` only. Copy `CameraStreamCamConfig.*`, attach on the existing `WebServer`, honour params on next `/capture`. Serial logs `#if TELECON_DEBUG` only. |
| Smart Door Lock CAM | create/update when the sketch exists | Same shared module. |

Control Panel has **no** extra firmware — Kit A / Kit B CAM covers it.

---

## Implementation notes

- Keep applying sensor params in **all** builds (`TELECON_DEBUG` 0 or 1). Debug flags **logs only**.
- Do not wrap `sensor->set_framesize` / HTTP 200 inside `#if TELECON_DEBUG`.
- Do not log `/stream` JPEG boundaries.
- If `DBG_BEGIN` is gated on `TELECON_DEBUG || TELECON_SERIAL_INJECT`, leave that as-is;
  camconfig prints still require `TELECON_DEBUG`.
- Match existing style: `#if TELECON_DEBUG` around `DBG_*` like `RcVehicleControl.cpp` /
  Greenhouse `CameraStream.cpp`.

---

## Do not

- Change Android.
- Print HUD rate (it never reaches firmware).
- Expect Serial on **Done** — only on preset chip → `GET /camconfig`.
- Unconditional `Serial.printf` for camconfig (always `#if TELECON_DEBUG`).
- Separate firmware SKUs per phone.

---

## Deliverables

1. Shared `CameraStreamCamConfig.cpp` logs GET + apply + fail under `#if TELECON_DEBUG`.
2. Same file copied into all RC CAM sketches that already use it.
3. Greenhouse CAM: `/camconfig` + the same debug logs.
4. Confirm `TELECON_DEBUG 0` compiles with zero camconfig Serial output.

When done, note in `TeleCon_RcVehiclePro/common/README.md` (or sketch README):

> Arduino Serial Monitor 115200 + `TELECON_DEBUG 1`: `[CAM] GET /camconfig …` when the
> phone changes Stream quality (Smooth/Balanced/High). `TELECON_DEBUG 0` silences it.
