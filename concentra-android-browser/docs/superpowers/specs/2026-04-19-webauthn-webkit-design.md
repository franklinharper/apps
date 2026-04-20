# WebAuthn via WebViewFeature.WEB_AUTHENTICATION

**Date:** 2026-04-19
**Status:** Approved

## Background

Three approaches were attempted before this design:

1. **CredentialManager with `origin`** — requires `CREDENTIAL_MANAGER_SET_ORIGIN`, a system-only permission. Blocked.
2. **JS polyfill + CredentialManager without origin** — RP ID validation fails because the passkey's `clientDataJSON` carries `android:apk-key-hash:…` instead of `https://site.com`.
3. **GMS FIDO2 Privileged API** — requires Google allowlisting. Only available to first-party browsers (Chrome, Firefox). Blocked.

## Solution

Use `WebViewFeature.WEB_AUTHENTICATION` from AndroidX WebKit. When enabled, the WebView layer intercepts `navigator.credentials.create()` and `navigator.credentials.get()` calls natively and delegates to CredentialManager internally. Because the WebView owns the origin context, it passes the correct web origin (`https://site.com`) to CredentialManager without the app needing `CREDENTIAL_MANAGER_SET_ORIGIN` or any Google allowlisting.

This eliminates the need for a JS polyfill, a JavascriptInterface bridge, and any custom passkey manager code.

## Architecture

```
WebViewHost
  └─ WebView settings
       └─ WebSettingsCompat.setWebAuthenticationSupport(WEB_AUTHENTICATION_SUPPORT_FOR_APP)
            └─ [platform-owned] CredentialManager → system passkey UI
```

No callbacks, no bridges, no polyfill — one settings call gates the entire feature.

## Scope

### Files changed

| File | Change |
|---|---|
| `gradle/libs.versions.toml` | Add `androidx-webkit = "1.14.0"` version + library alias |
| `app/build.gradle.kts` | Add `implementation(libs.androidx.webkit)` |
| `WebViewHost.kt` | Add feature-guarded `setWebAuthenticationSupport` call; remove `onBridgeCreated` param and `addJavascriptInterface` call |
| `BrowserWebViewClient.kt` | Remove polyfill injection from `onPageStarted` |
| `BrowserActivity.kt` | Remove `onBridgeCreated = {}` |
| `BrowserRoute.kt` | Remove any passkey bridge wiring |

### Files deleted

- `PasskeyBridge.kt`
- `PasskeyManager.kt`
- `PasskeyResult.kt`
- `passkey-polyfill.js`
- `PasskeyManagerTest.kt`

### Dependencies kept

- `androidx.credentials:credentials:1.5.0`
- `androidx.credentials:credentials-play-services-auth:1.5.0`

## Key API

```kotlin
if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_AUTHENTICATION)) {
    WebSettingsCompat.setWebAuthenticationSupport(
        webView.settings,
        WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP,
    )
}
```

Requires `androidx.webkit:webkit:1.14.0`.

## Error handling and degradation

`isFeatureSupported` gates the call. On devices with an older WebView, the flag is simply not set and WebAuthn calls from JS fail the same way they do today — no crash, no error UI required at the app layer. The site itself surfaces an appropriate error to the user.

Conditional mediation (`mediation: "conditional"`) is not supported by the WebKit library and is out of scope.

## Digital Asset Links

DAL is **not required** for this use case. DAL links a native app to its own website for credential sharing. In the WebView browser case, the WebView layer resolves origin on behalf of the visited site — Concentra does not claim ownership of external sites.

## Testing

Manual verification on webauthn.io:
1. Register a passkey — biometric prompt appears, registration succeeds.
2. Authenticate — biometric prompt appears, authentication succeeds.

No unit tests remain for passkey logic; it lives entirely in the platform layer.
