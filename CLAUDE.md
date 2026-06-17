# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android QR/barcode app (`com.odom.barcodeqr`, app name "barcodeqr"). Single Gradle module `:app`, Kotlin, View-based UI (no Compose). Code comments and many user-facing intents are in Korean; UI strings are localized via `values/strings.xml` (Korean default) and `values-en/strings.xml`.

- `minSdk 26`, `compileSdk`/`targetSdk 36`, JVM target 17.
- `viewBinding` and `dataBinding` are both enabled — UI code accesses views through generated `*Binding` classes, not `findViewById` (except inside the dialog inflated in `ScanFragment`).

## Build & test

```bash
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build + install on connected device/emulator
./gradlew test                   # JVM unit tests (app/src/test)
./gradlew connectedAndroidTest   # instrumented tests (needs device/emulator)
./gradlew lint                   # Android lint

# single unit test
./gradlew test --tests "com.odom.barcodeqr.ExampleUnitTest.addition_isCorrect"
```

There is no app-level CI; release build type has `isMinifyEnabled = false`, so ProGuard rules are effectively unused.

## Architecture

Single-Activity + Navigation Component. `MainActivity` hosts a `NavHostFragment` (`nav_host_fragment_activity_main`) driven by `res/navigation/mobile_navigation.xml`, wired to a `BottomNavigationView`. Three top-level destinations, **start destination is `navigation_generate`**:

- **`ScanFragment`** — live camera scanning via ZXing (`com.journeyapps:zxing-android-embedded`, `DecoratedBarcodeView`). Handles CAMERA permission inline, front/back toggle, torch. On scan it shows a custom AlertDialog (`dialog_scan_result`); saving/opening a result writes to history.
- **`GenerateFragment`** — generates QR codes with ZXing `MultiFormatWriter` + `BarcodeEncoder`. Supports foreground/background/border colors (color picker via `com.github.yukuku:ambilwarna`), an optional centered circular logo (gallery pick or camera capture via `ActivityResultContracts` + `FileProvider`), share (`FileProvider` cache dir), and save-to-gallery (`MediaStore`). Color choices persist in the `qr_customization` SharedPreferences.
- **`HistoryFragment`** — RecyclerView of saved scans/generations using `HistoryModernAdapter`, with client-side search filtering and clear-all. (`HistoryAdapter`/`history_item.xml` are the older, unused variant.)

### Data layer (Room)

`HistoryDatabase` (singleton, DB name `QR_db`, version 1) → `HistoryDao` (Flow-based queries) → single entity `HistoryItem(id, qrString, createdAt)`. No migrations defined — bumping the schema requires adding a migration or it will crash on existing installs.

`HistoryViewModel` is an `AndroidViewModel` exposing `listHistory` as a `StateFlow`. **Each fragment creates its own ViewModel instance** via `ViewModelProvider(this, AndroidViewModelFactory...)` — it is intentionally *not* activity-scoped/shared.

### Ads & review

- `utils/AdManager` owns interstitial ads. Call `incrementGenerateCount()` / `incrementScanCount()` after a successful action; it returns `true` (and shows the interstitial via `showInterstitialAd`) once a threshold is hit — generate every 3, scan every 5 — using the `ad_counter` SharedPreferences. Each fragment constructs its own `AdManager`.
- Banner ads (`AdView`) are loaded directly in `GenerateFragment` and `HistoryFragment`.
- AdMob unit IDs live in `strings.xml` (`REAL_ADMOB_APP_ID`, `REAL_ADMOB_BANNER_ID`, `REAL_ADMOB_FULLSCREEN_ID`) — these are **production** IDs, not test IDs; use AdMob test IDs when developing to avoid policy issues.
- `MainActivity` triggers the Play in-app review flow (`ReviewManagerFactory`) on first back-press at a top-level destination; second back-press within 2s exits.

## Dependencies

Gradle version catalog is `gradle/libs.versions.toml`, but several libraries (ZXing, AmbilWarna, Room, Play review, Play services ads) are declared with **hardcoded coordinates directly in `app/build.gradle.kts`** rather than the catalog. When adding/upgrading those, edit `build.gradle.kts`. JitPack is configured as a repo (needed for AmbilWarna). Room uses `kapt`.
