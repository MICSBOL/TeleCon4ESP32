# Cursor prompt: RC Vehicle Pro — HUD image-processing frequency (weak devices)

Copy into a **new Cursor chat** (Agent mode) on
`/home/miguel/AndroidStudioProjects/SeriousApp/TeleCon4ESP32/`.

**Scope:** SoftAP camera HUD for RC Vehicle Pro (Kit A SoftAP and Kit B SoftAP camera — same visibility as `showSoftApPerformanceSettings`).

Related (already done — extend, do not redo):

- SoftAP presets: [`RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_SETTINGS_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_SETTINGS_ANDROID_PROMPT.md)
- Stream + FPS cap drain: [`RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_WIFI_SOFTAP_PERF_ANDROID_PROMPT.md)
- Smooth SoftAP radio/locks: [`RC_VEHICLE_PRO_WIFI_SMOOTH_STREAM_CONTROL_ANDROID_PROMPT.md`](./RC_VEHICLE_PRO_WIFI_SMOOTH_STREAM_CONTROL_ANDROID_PROMPT.md)

Domain already present:

- `SoftApPerformancePreset` → fixed `maxHudFps` (Smooth=10, Balanced=15, High=null)
- `HudPreviewOptions.maxHudFps` + `minPublishGapMs()` in `Esp32CameraStreamRepository`
- Settings UI: `SoftApPerformanceSettingsSection`

---

## Problem

Coarse presets help, but the **weakest phones** (e.g. older Xiaomi / Poco mid-range under SoftAP load) may still need a **lower HUD decode/publish rate** than Smooth’s fixed 10 FPS — without forcing the user onto a different overall preset or changing SoftAP CTRL / `/camconfig` every time.

Users need an explicit control: **how often the app decodes and paints SoftAP frames**.

---

## Goal

Add a settings control **Image processing rate** (HUD refresh frequency) with options tuned for weak devices. Persist it. Apply it as the effective `HudPreviewOptions.maxHudFps` while SoftAP HUD presets are active.

**Critical rule (unchanged):** when skipping a frame for the FPS cap, **still consume/drain** the JPEG from MJPEG (or `/capture` poll). Never stall SoftAP by stopping reads.

This control is **Android HUD only**. It does **not** replace firmware `/camconfig` stream FPS (airtime). Optional later: when rate ≤ 10 and Smooth is selected, keep existing `/camconfig` behaviour.

---

## UX

Show under the existing SoftAP performance section (`SoftApPerformanceSettingsSection`), **after** the Smooth / Balanced / High quality rows.

### Control label

- Title: **Image processing rate** (en) / matching es
- Short body: explains this limits how often SoftAP frames are decoded for the HUD — lower = smoother on weak phones, may look more stuttery.

### Options (selectable; same `ConnectionModeOption` / radio style as presets)

| Option id | Label (en) | HUD max FPS | Intended for |
|-----------|------------|-------------|--------------|
| `AUTO` | Auto (from preset) | Use preset’s `maxHudFps` (10 / 15 / uncapped) | Default — no behaviour change |
| `FPS_5` | 5 FPS — weakest devices | 5 | Very weak / hot SoftAP |
| `FPS_8` | 8 FPS | 8 | Weak mid-range |
| `FPS_10` | 10 FPS | 10 | Typical Smooth / Poco |
| `FPS_12` | 12 FPS | 12 | Mild throttle |
| `FPS_15` | 15 FPS | 15 | Balanced-like |
| `UNCAPPED` | Unlimited | `null` | Strong phones only |

Default: **`AUTO`**.

When preset is **High quality** and user picks a capped rate, honour the explicit rate (user override). When preset is Smooth and user picks Auto, keep 10.

Do **not** add free-form sliders in v1 — discrete options only.

---

## Domain model

Add something like:

```kotlin
enum class SoftApHudProcessingRate {
    AUTO,      // follow SoftApPerformancePreset.maxHudFps
    FPS_5,
    FPS_8,
    FPS_10,
    FPS_12,
    FPS_15,
    UNCAPPED;

    /** Null = no hard cap. */
    fun resolveMaxHudFps(preset: SoftApPerformancePreset): Int? = when (this) {
        AUTO -> preset.maxHudFps
        FPS_5 -> 5
        FPS_8 -> 8
        FPS_10 -> 10
        FPS_12 -> 12
        FPS_15 -> 15
        UNCAPPED -> null
    }

    companion object {
        val DEFAULT = AUTO
        fun fromStored(value: String?): SoftApHudProcessingRate =
            value?.let { runCatching { valueOf(it) }.getOrNull() } ?: DEFAULT
    }
}
```

Effective HUD options when SoftAP performance applies:

```kotlin
preset.toHudPreviewOptions().copy(
    maxHudFps = rate.resolveMaxHudFps(preset),
)
```

Keep `inSampleSize` / `useRgb565` from the **preset** (this prompt only changes frequency). Optional v1.1: when rate ≤ 5, force `inSampleSize = max(preset, 4)` — only if 5 FPS alone is not enough on device; prefer not bundling that unless needed.

---

## Persistence

- DataStore key scoped like the SoftAP preset (app / RC Vehicle Pro SoftAP).
- `ISettingsRepository` + `SettingsRepository`: `Flow` + save method mirroring `softApPerformancePreset`.
- `ApplicationSettingsViewModel`: expose state + `onSoftApHudProcessingRateChanged`.
- Wire `RcVehicleProViewModel` (and any other place that calls `setHudPreviewOptions` from the preset) to **combine preset + rate** before applying.

Default stored value = `AUTO` so existing users see no change until they pick a lower rate.

---

## Stream wiring

- `Esp32CameraStreamRepository` already respects `HudPreviewOptions.maxHudFps` via `minPublishGapMs` — **reuse**; do not add a second FPS gate.
- On rate change while streaming: update options immediately (same as preset change); no app restart.
- Photo path: unchanged (full JPEG / `/capture`).

SoftAP CTRL period: **unchanged** by this control (still from preset). Frequency here is HUD decode/publish only.

---

## Settings UI

Extend `SoftApPerformanceSettingsSection` (or sibling composable in the same screen):

```kotlin
SoftApPerformanceSettingsSection(
    selectedPreset = ...,
    onPresetSelected = ...,
    selectedHudRate = ...,
    onHudRateSelected = ...,
)
```

- Section or subsection title + body (en + es).
- Selectable group of rate options.
- Show under SoftAP performance visibility gate (`showSoftApPerformanceSettings`) — same as presets.

---

## Strings

Add matching keys in `values/strings.xml` and `values-es/strings.xml`, e.g.:

- `rc_vehicle_softap_hud_rate_section_title`
- `rc_vehicle_softap_hud_rate_section_body`
- `rc_vehicle_softap_hud_rate_auto` (+ `_description`)
- `rc_vehicle_softap_hud_rate_5` / `_8` / `_10` / `_12` / `_15` / `_unlimited` (+ short descriptions mentioning weak devices where relevant)

Keep copy concise; emphasize **weaker phones → lower FPS**.

---

## Tests

- Unit: `resolveMaxHudFps` for each rate × Smooth/Balanced/High.
- `fromStored` null / invalid → `AUTO`.
- Fake settings repo flow + save if other SoftAP preset tests do the same.
- Existing `HudPreviewOptions.minPublishGapMs` already covers gap math (5 FPS → 200 ms).

---

## Do not

- Stop draining MJPEG when skipping HUD publish
- Change SoftAP CTRL period from this control
- Require firmware `/camconfig` for acceptance
- Process-wide SoftAP bind / ExoPlayer / WebView preview
- Auto-detect Poco model names — user picks the rate
- Greenhouse full-quality default unless it already opts into SoftAP presets

---

## Manual test plan

1. RC settings → SoftAP performance visible → see presets **and** image processing rate; default Auto.
2. On a mid-range / weak phone: Smooth + **5 FPS** → HUD updates slowly but UI/sticks stay responsive; stream does not freeze.
3. Auto + Smooth → still ~10 FPS (same as today).
4. Unlimited + High quality → uncapped (or floor only).
5. Change rate while streaming → applies without crash; photo still works.
6. en + es strings present.
7. Leave SoftAP / stop stream → no lock leaks from this change (WifiLock owned elsewhere).

---

## Acceptance

- [ ] Discrete HUD processing-rate options in SoftAP settings (including **5 / 8 FPS** for weakest devices)
- [ ] Default **Auto** preserves current preset FPS behaviour
- [ ] Persisted; applied via existing `setHudPreviewOptions` / `maxHudFps`
- [ ] Skip-publish still drains SoftAP JPEG
- [ ] CTRL / `/camconfig` / presets otherwise unchanged
- [ ] en + es strings
- [ ] Unit tests for resolve + storage parsing

---

## Suggested order

1. Domain enum + resolve helper + tests  
2. DataStore + repository + ViewModel  
3. Combine preset + rate → `setHudPreviewOptions` in RC session  
4. Settings UI + strings en/es  
5. Manual check on weakest available SoftAP phone (5 vs Auto)
