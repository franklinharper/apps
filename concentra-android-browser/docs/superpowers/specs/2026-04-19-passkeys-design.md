# Passkeys (WebAuthn) Support

**Date:** 2026-04-19  
**Status:** Approved

## Overview

Add WebAuthn passkey support (registration and authentication) to the Concentra Android browser. Because Android WebView does not natively bridge `navigator.credentials` to the platform Credential Manager, this is implemented via a JavaScript polyfill + native Android bridge. Gracefully degrades on API < 26. A debug screen in Settings shows the feature's runtime status.

## Architecture

Five components:

| Component | Type | Responsibility |
|---|---|---|
| `passkey-polyfill.js` | JS asset | Overrides `navigator.credentials` in the page |
| `PasskeyBridge` | `@JavascriptInterface` | Receives calls from polyfill, dispatches to `PasskeyManager`, returns results |
| `PasskeyManager` | Android class | Wraps `androidx.credentials.CredentialManager`, handles API level check |
| Integration changes | `WebViewHost`, `BrowserAppContainer` | Wire the polyfill and bridge into the existing WebView |
| Debug screen | `DebugActivity` / `DebugScreen` / `DebugViewModel` | Shows passkey feature status in Settings |

## JavaScript Polyfill

**File:** `app/src/main/assets/passkey-polyfill.js`

Injected via `WebView.evaluateJavascript()` at `onPageStarted`. Overrides `navigator.credentials.create()` and `navigator.credentials.get()` with Promise-based wrappers.

**Request/response correlation:**
- Each call is assigned a `crypto.randomUUID()` request ID.
- Resolvers are stored in a `Map` keyed by request ID.
- Android calls back via `window.__passkeyResolve(requestId, resultJson)` or `window.__passkeyReject(requestId, errorType, message)`.

**Binary encoding:**  
`ArrayBuffer` fields (`challenge`, `userId`, `clientDataJSON`, `authenticatorData`, `signature`, `attestationObject`) are Base64url-encoded for JSON transport in both directions. The polyfill encodes outgoing fields and decodes incoming fields transparently, so the web page always sees native `ArrayBuffer` values.

**Calls into Android:**
```
Android.passkeyCreate(requestId: String, optionsJson: String, origin: String)
Android.passkeyGet(requestId: String, optionsJson: String, origin: String)
```

The `origin` is `window.location.origin` at the time of the call.

## PasskeyBridge

**File:** `web/PasskeyBridge.kt`

`@JavascriptInterface` attached to the WebView as `"Android"`. No existing `addJavascriptInterface` calls in the codebase, so no naming conflict.

Methods:
- `passkeyCreate(requestId, json, origin)` — posts to coroutine scope, calls `PasskeyManager.create()`, evaluates result callback on main thread
- `passkeyGet(requestId, json, origin)` — same pattern for authentication

Result callbacks evaluated via:
```kotlin
webView.post {
    webView.evaluateJavascript("window.__passkeyResolve('$requestId', $resultJson)", null)
}
```

## PasskeyManager

**File:** `web/PasskeyManager.kt`

Constructed with an `Activity` reference. API level checked at construction; `NotSupported` returned immediately on API < 26.

**Registration:**
- Builds `CreatePublicKeyCredentialRequest(requestJson, origin = pageOrigin)`
- Calls `CredentialManager.createCredential(activity, request)`
- Returns `PasskeyResult.Success(responseJson)` on success

**Authentication:**
- Builds `GetCredentialRequest` with `GetPublicKeyCredentialOption(requestJson, origin = pageOrigin)`
- Calls `CredentialManager.getCredential(activity, request)`
- Extracts `PublicKeyCredential` from response, returns `authenticationResponseJson`

**Error mapping:**

| Exception | Result |
|---|---|
| `CreateCredentialCancellationException` | `Cancelled` |
| `GetCredentialCancellationException` | `Cancelled` |
| `CreateCredentialInterruptedException` | `Failure("interrupted", ...)` |
| `CreateCredentialProviderConfigurationException` | `Failure("no_provider", ...)` |
| All others | `Failure("error", message)` |

**Result type:**
```kotlin
sealed class PasskeyResult {
    data class Success(val responseJson: String) : PasskeyResult()
    data class Failure(val errorType: String, val message: String) : PasskeyResult()
    object NotSupported : PasskeyResult()
    object Cancelled : PasskeyResult()
}
```

**Dependencies to add:**
```
androidx.credentials:credentials:1.5.0
androidx.credentials:credentials-play-services-auth:1.5.0
```

## Integration Changes

**`WebViewHost.kt`:**
- Constructor receives `PasskeyBridge`
- Calls `webView.addJavascriptInterface(passkeyBridge, "Android")`
- In `onPageStarted`: injects polyfill via `webView.evaluateJavascript(polyfillJs, null)`
- Polyfill JS string loaded once at construction from `assets/passkey-polyfill.js`

**`BrowserAppContainer.kt`:**
- Constructs `PasskeyManager(activity)`, `PasskeyBridge(passkeyManager, webView)`
- Passes `PasskeyBridge` to `WebViewHost`

**`BrowserActivity.kt`:** No changes.

**`WebViewConfigurator.kt`:** No changes (JS already enabled).

## Debug Screen

Follows the exact pattern of `SettingsActivity` / `SettingsScreen` / `SettingsViewModel`.

**New files:**
- `settings/DebugUiState.kt`
- `settings/DebugViewModel.kt`
- `settings/DebugScreen.kt`
- `settings/DebugActivity.kt`

**`DebugUiState`:**
```kotlin
data class DebugUiState(
    val apiLevel: Int,
    val passkeysSupported: Boolean,
    val passkeysUnsupportedReason: String?,
)
```

**`DebugViewModel`:** Populates `DebugUiState` from `Build.VERSION.SDK_INT` at init. No repository needed.

**`DebugScreen`:** `Scaffold` with back-arrow `TopAppBar`. Read-only info rows:
- "Android API level: {n}"
- "Passkeys supported: Yes" or "Passkeys supported: No — {reason}"

**Entry point:** `SettingsScreen` gains an `onDebugClick` callback. A tappable "Debug" row with a chevron appears above the version number. `SettingsActivity` starts `DebugActivity` on click.

## Graceful Degradation

On API < 26:
- `PasskeyManager` returns `NotSupported` immediately
- `PasskeyBridge` calls `window.__passkeyReject(requestId, "not_supported", "...")` 
- The polyfill rejects the Promise; the web page's own error handling takes over
- The debug screen shows "Passkeys supported: No — requires API 26, running API {n}"

## Out of Scope

- Custom passkey provider selection UI
- Passkey management (listing/deleting stored passkeys)
- Non-passkey credential types (passwords, federated login via Credential Manager)
