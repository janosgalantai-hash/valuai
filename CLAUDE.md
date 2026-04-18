# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ValuAI** is an Android app for AI-powered item valuation. Users photograph items (up to 4 photos), provide a description, and receive an AI-generated market price estimate. The app connects to a FastAPI backend on Azure that uses Claude claude-opus-4-5 with web search to estimate prices.

## Build Commands

Use Android Studio or Gradle directly:

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Output APK location
app/build/outputs/apk/debug/app-debug.apk
```

The project targets SDK 36, minSdk 26. There are no custom lint or test commands configured.

## Architecture

### Android App (Kotlin + Jetpack Compose)

**Entry point:** `SplashActivity` (2s delay) → `MainActivity` → `NavHost`

**Navigation flow:**
- Unauthenticated: `Login` → `Register` (optional)
- Authenticated: Bottom nav with `Estimation` / `History` / `Profile`
- `Estimation` → `Result` (after appraisal completes)

**Key architectural decisions:**
- `EstimationViewModel` holds `selectedImages` and `description` as `StateFlow` so they survive orientation changes — do NOT move these back to `remember {}` in the composable
- Camera uses `ActivityResultContracts.TakePicture()` (system camera), NOT CameraX — CameraX caused black screen on Samsung devices
- When launching the camera, the file must be physically created with `createNewFile()` before getting the FileProvider URI — Samsung requires the file to exist
- Camera permission is checked/requested at the moment the user taps the camera button in `EstimationScreen`, not upfront
- `resetState()` resets only the estimation state (use before camera launch); `reset()` clears everything including images (use for "New Appraisal")

**Localization:** All UI strings are in `com/valuai/i18n/AppStrings.kt` as a `data class` with instances for English, Magyar, Deutsch, Français, 中文. `LocalStrings` is a `CompositionLocal` provided in `MainActivity`. To add a new language: add an instance in `AppStrings.kt` and a case in `stringsForLanguage()`, then add it to the `languages` list in `ProfileScreen`.

**Persistence:** `TokenManager` uses DataStore to store JWT token, selected currency, and selected language.

**Theme:** Dark only. Primary colors defined in `ui/theme/Color.kt`:
- Background: `#0D0D14`
- Gold accent: `#C8A96A`
- Surface: `#1A1A25`, Card: `#141420`

### Backend (FastAPI on Azure)

- **URL:** `https://api.akarmilofasz.com/api/`
- **Server:** Azure Ubuntu VM at `104.209.94.60` (hardcoded in `ApiService.kt` DNS override due to Cloudflare DNS issues)
- **Backend path:** `/opt/valuai/backend/`
- **Restart:** `sudo systemctl restart valuai`
- **AI model:** Claude claude-opus-4-5 with `web_search` tool
- **Admin dashboard:** `GET /dashboard?password=valuai_admin_2024`

**API endpoints used by the app:**
- `POST /auth/login` — returns JWT
- `POST /auth/register` — creates user (backend endpoint needs to be implemented)
- `POST /estimations/` — multipart: `images[]`, `description`, `currency`
- `GET /estimations/` — history list

**JWT tokens expire** — if the app returns 401, the user must log in again. Token lifetime can be extended in the backend auth router.

### FileProvider

Authority: `com.valuai.provider`  
Config: `app/src/main/res/xml/file_paths.xml` — allows cache-path, files-path, external-files-path, external-cache-path.

## Known Issues & Workarounds

- **Samsung camera crash:** File must exist on disk before passing URI to camera launcher (`file.createNewFile()`)
- **Gallery resets images:** Fixed by storing images in ViewModel instead of `remember {}`
- **DNS:** Cloudflare doesn't resolve `api.akarmilofasz.com` correctly from some Android devices — hardcoded IP in `ApiService.kt` via custom OkHttp `Dns` object
- **OkHttp timeouts:** connect=30s, read=120s, write=60s — AI estimation can take up to 60s
- **`SplashActivity.kt.kt`** — the file has a double `.kt.kt` extension due to a rename error; it still compiles fine but should be renamed if refactoring

## Pending Features (not yet implemented)

- Backend `/auth/register` endpoint (Android side is ready)
- JWT auto-refresh / longer token lifetime
- Stripe subscription integration
- Geolocation-based pricing
