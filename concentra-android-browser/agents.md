# Concentra Android Browser — Agent Guide

## Overview

Concentra is a minimal Android web browser built with Kotlin, Jetpack Compose, and Android WebView. It focuses on distraction-free browsing with passkey (WebAuthn) support via a JavaScript polyfill bridge to the Android Credential Manager API.

- **Package:** `com.franklinharper.concentra.browser`
- **Application ID:** `com.franklinharper.concentra.browser`
- **Min SDK:** 23 / **Target SDK:** 36 / **Compile SDK:** 36
- **Kotlin:** 2.3.20 / **AGP:** 8.12.3

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Run all unit tests (Robolectric on JVM)
./gradlew test

# Run instrumented UI tests (requires device/emulator)
./gradlew connectedAndroidTest
```

## Project Structure

Single-module Gradle project (`:app`):

```
app/src/main/java/com/franklinharper/concentra/browser/
├── BrowserActivity.kt          # Main activity, entry point
├── BrowserAppContainer.kt      # Manual DI container
├── BrowserViewModel.kt         # Core browser state & navigation logic
├── intent/
│   └── IntentParser.kt         # Parses incoming intents (VIEW, SEND) into LaunchRequest
├── model/
│   ├── BrowserAction.kt        # Sealed interface of user actions
│   ├── BrowserUiState.kt       # UI state data class
│   └── LaunchRequest.kt        # Sealed interface: Empty | OpenUrl
├── settings/
│   ├── SettingsActivity.kt     # Settings screen activity
│   ├── SettingsScreen.kt       # Settings Compose UI
│   ├── SettingsViewModel.kt    # Settings state management
│   ├── SettingsRepository.kt   # Settings persistence interface
│   ├── PreferencesSettingsRepository.kt  # SharedPreferences impl
│   ├── DebugActivity.kt        # Debug info screen
│   ├── DebugScreen.kt          # Debug Compose UI
│   └── DebugViewModel.kt       # Debug state (API level, passkey support)
├── ui/
│   ├── BrowserRoute.kt         # Navigation wiring between ViewModel & Screen
│   ├── BrowserScreen.kt        # Main browser Compose screen
│   ├── BrowserChromeSheet.kt   # Bottom sheet with URL bar & action buttons
│   └── HotspotOverlay.kt       # Bottom-right swipe-up hotspot to reveal chrome
└── web/
    ├── WebViewHost.kt          # Composable wrapping Android WebView
    ├── WebViewConfigurator.kt  # WebView settings application
    ├── WebViewCommand.kt       # Sealed interface: LoadUrl
    ├── WebViewEvent.kt         # Sealed interface: PageLoadStarted/Finished, NavigationStateChanged
    ├── BrowserWebViewClient.kt # WebViewClient forwarding events
    ├── BrowserWebChromeClient.kt  # WebChromeClient (minimal)
    ├── BrowserDownloadHandler.kt  # DownloadManager integration
    ├── PasskeyBridge.kt        # @JavascriptInterface bridge for WebAuthn
    ├── PasskeyManager.kt       # Android Credential Manager integration
    └── PasskeyResult.kt        # Sealed class: Success | Failure | NotSupported | Cancelled

app/src/main/assets/
└── passkey-polyfill.js         # JS polyfill overriding navigator.credentials
```

## Architecture

**Unidirectional data flow (UDF):**

```
Intent → BrowserAppContainer → BrowserViewModel → BrowserUiState (StateFlow)
                ↓                                       ↓
         WebViewHost ←—— WebViewCommand ←—— BrowserRoute → BrowserScreen
                ↓                                       ↓
         WebViewEvent → BrowserViewModel            User Action → BrowserViewModel
```

- **BrowserAppContainer** provides dependencies (settings repo, intent parser, download handler) — manual DI, no Hilt/Dagger.
- **BrowserViewModel** holds all browser state as `MutableStateFlow<BrowserUiState>`, processes `BrowserAction`s, and emits `WebViewCommand`s and side `Effect`s.
- **BrowserRoute** is the Compose wiring layer connecting ViewModel state to `BrowserScreen` and `WebViewHost`, handling effects (share intent, navigation, settings launch).
- **WebViewHost** wraps a `WebView` in `AndroidView`, applies `BrowserSettings` via `WebViewConfigurator`, and forwards lifecycle events as `WebViewEvent`s.

### Chrome UX Pattern

The browser chrome (URL bar, action buttons) is a bottom sheet overlay. When hidden, a small `HotspotOverlay` in the bottom-right corner accepts swipe-up gestures to reveal it. Tapping the scrim hides it again.

### Passkey / WebAuthn System

WebView does not natively support WebAuthn. Concentra works around this with:

1. **`passkey-polyfill.js`** — Injected on every page start. Defines `window.PublicKeyCredential` and overrides `navigator.credentials.create/get` to call `window.Android.passkeyCreate/passkeyGet`.
2. **`PasskeyBridge`** — A `@JavascriptInterface` exposed as `window.Android`. Receives calls from JS, delegates to `PasskeyManager`, then resolves/rejects the JS promise via `evaluateJavascript`.
3. **`PasskeyManager`** — Uses AndroidX Credential Manager (`CredentialManager.createCredential` / `getCredential`) to invoke the system passkey UI. Requires API 26+.

## Key Conventions

- **UI:** Jetpack Compose + Material 3. No XML layouts.
- **State:** `StateFlow` in ViewModels. No LiveData.
- **Side effects:** `BrowserViewModel.Effect` sealed interface consumed in `BrowserRoute` via `LaunchedEffect`.
- **Navigation:** Three activities (`BrowserActivity`, `SettingsActivity`, `DebugActivity`). No Navigation Compose.
- **Settings persistence:** `SharedPreferences` via `SettingsRepository` interface.
- **Testing:** Unit tests use JUnit 4 + Robolectric. Compose UI tests use `createComposeRule` in `androidTest`.
- **Version catalog:** All dependency versions in `gradle/libs.versions.toml`.

## Testing

| Location | Type | Runner |
|---|---|---|
| `app/src/test/` | Unit tests | JUnit 4 + Robolectric |
| `app/src/androidTest/` | Compose UI tests | Android instrumented |

Test classes mirror the source package structure. Each ViewModel, utility, and integration class has a corresponding `*Test.kt`.

## Common Tasks

- **Add a new browser action:** Define it in `BrowserAction`, handle it in `BrowserViewModel.onAction()`, wire UI callbacks in `BrowserRoute` → `BrowserScreen`.
- **Add a new WebView setting:** Add field to `BrowserSettings`, persist in `PreferencesSettingsRepository`, add UI toggle in `SettingsScreen`, apply in `WebViewConfigurator.configure()`.
- **Modify passkey flow:** Edit `passkey-polyfill.js` (asset), `PasskeyBridge` (JS bridge), and/or `PasskeyManager` (Credential Manager).
- **Add a new activity:** Add to `AndroidManifest.xml`, follow the pattern of `SettingsActivity` (standalone activity with its own Compose entry point).
