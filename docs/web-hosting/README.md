# Web-hosted documents (GitHub Pages)

Published from [MICSBOL/BT_ESP32_USER_DOCUMENTATION](https://github.com/MICSBOL/BT_ESP32_USER_DOCUMENTATION).

**Base URL:** `https://micsbol.github.io/BT_ESP32_USER_DOCUMENTATION/`

| Page | URL |
|------|-----|
| Home | https://micsbol.github.io/BT_ESP32_USER_DOCUMENTATION/ |
| Privacy policy (EN) | https://micsbol.github.io/BT_ESP32_USER_DOCUMENTATION/privacy-policy.html |
| Privacy policy (ES) | https://micsbol.github.io/BT_ESP32_USER_DOCUMENTATION/privacy-policy-es.html |
| User docs PDF (EN) | https://micsbol.github.io/BT_ESP32_USER_DOCUMENTATION/main_en.pdf |
| User docs PDF (ES) | https://micsbol.github.io/BT_ESP32_USER_DOCUMENTATION/main.pdf |

## Enable GitHub Pages (one-time)

1. Open the repo **Settings → Pages**.
2. **Build and deployment:** Deploy from branch `main`, folder `/ (root)`.
3. Wait a few minutes, then open the base URL above.

## Sync privacy HTML from the app

When you change in-app policy text, copy assets to the documentation repo:

```bash
cp app/src/main/assets/privacy_policy_en.html /path/to/BT_ESP32_USER_DOCUMENTATION/privacy-policy.html
cp app/src/main/assets/privacy_policy_es.html /path/to/BT_ESP32_USER_DOCUMENTATION/privacy-policy-es.html
```

Commit and push the documentation repository.
