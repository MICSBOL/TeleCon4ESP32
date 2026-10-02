# Web-hosted documents (GitHub Pages)

Published from [MICSBOL/TeleCon_ESP32](https://github.com/MICSBOL/TeleCon_ESP32).

**Base URL:** `https://micsbol.github.io/TeleCon_ESP32/`

| Page | URL |
|------|-----|
| Home | https://micsbol.github.io/TeleCon_ESP32/ |
| Privacy policy (EN) | https://micsbol.github.io/TeleCon_ESP32/privacy-policy.html |
| Privacy policy (ES) | https://micsbol.github.io/TeleCon_ESP32/privacy-policy-es.html |
| User docs PDF (EN) | https://micsbol.github.io/TeleCon_ESP32/General_Documentation/en/build/telecon_user_guide.pdf |
| User docs PDF (ES) | https://micsbol.github.io/TeleCon_ESP32/General_Documentation/es/build/telecon_user_guide.pdf |

The user-guide PDFs are the files already in `General_Documentation`. Privacy HTML is committed at the root of **TeleCon_ESP32** so GitHub Pages can serve it from `/`.

## Enable GitHub Pages (one-time)

1. Open **MICSBOL/TeleCon_ESP32** → **Settings → Pages**.
2. **Build and deployment:** Deploy from branch `main`, folder `/ (root)`.
3. Wait a few minutes, then open the base URL above.

## Sync privacy HTML

When the policy text changes, copy the files from this folder to the firmware repository root and push `main`:

```bash
cp docs/web-hosting/privacy-policy.html /path/to/TeleCon_ESP32/privacy-policy.html
cp docs/web-hosting/privacy-policy-es.html /path/to/TeleCon_ESP32/privacy-policy-es.html
```

## User guide PDFs

About opens the hosted user-guide PDFs listed above. Those files live in `TeleCon_ESP32` under `General_Documentation`. After replacing a PDF there, push and wait for GitHub Pages. A PDF viewer may cache the previous file.
