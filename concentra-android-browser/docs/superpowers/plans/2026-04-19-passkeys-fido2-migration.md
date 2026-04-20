# Passkeys: Switch to GMS FIDO2 Privileged API

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace `androidx.credentials.CredentialManager` with `com.google.android.gms:play-services-fido`'s `Fido2PrivilegedApiClient`, which is designed for browsers and allows setting the origin without system permissions. This fixes "RP ID cannot be validated" and "CREDENTIAL_MANAGER_SET_ORIGIN" errors.

**Why:** `androidx.credentials.CredentialManager` requires `CREDENTIAL_MANAGER_SET_ORIGIN` (system-only permission) to set the origin. Without origin, RP ID validation fails. The GMS FIDO2 Privileged API (`Fido2PrivilegedApiClient`) is specifically designed for third-party browsers — it accepts `BrowserPublicKeyCredentialCreationOptions` with `setOrigin(Uri)` and requires no special permissions.

**Architecture change:** The FIDO2 API uses a `PendingIntent` flow instead of coroutines. This means:
- `PasskeyManager` builds `BrowserPublicKeyCredentialCreationOptions` / `BrowserPublicKeyCredentialRequestOptions` from the JSON and origin, then calls `getRegisterPendingIntent()` / `getSignPendingIntent()` to get a `PendingIntent`
- `BrowserActivity` registers an `ActivityResultLauncher` and launches the `PendingIntent`
- On activity result, we extract `PublicKeyCredential` (via `Fido.FIDO2_KEY_CREDENTIAL_EXTRA`) or `AuthenticatorErrorResponse` (via `Fido.FIDO2_KEY_ERROR_EXTRA`)
- The result routes back through `PasskeyBridge` to resolve/reject the JS promise

**Key API points:**
- `PublicKeyCredentialCreationOptions(String)` — parses JSON string directly
- `PublicKeyCredentialRequestOptions` — no JSON constructor, must be built from parsed JSON fields
- `PublicKeyCredential.toJson()` — returns response JSON ready for the web
- `Fido.FIDO2_KEY_CREDENTIAL_EXTRA` — intent extra key for success
- `Fido.FIDO2_KEY_ERROR_EXTRA` — intent extra key for error
- Request codes: `REGISTER_REQUEST_CODE = 1001`, `SIGN_REQUEST_CODE = 1002`

**Tech Stack:** Kotlin, `com.google.android.gms:play-services-fido:21.0.0`, `androidx.activity.result.ActivityResultLauncher`

---

### Task 1: Replace `androidx.credentials` with `play-services-fido` dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Update version catalog**

Remove the `androidx-credentials` and `androidx-credentials-play-services-auth` entries from `gradle/libs.versions.toml`. Ensure `play-services-fido` is present (it was added during investigation):

```toml
play-services-fido = { module = "com.google.android.gms:play-services-fido", version = "21.0.0" }
```

- [ ] **Step 2: Update build.gradle.kts**

Remove `implementation(libs.androidx.credentials)` and `implementation(libs.androidx.credentials.play.services.auth)`. Keep `implementation(libs.play.services.fido)`.

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD FAILED (because PasskeyManager still imports old APIs) — this is expected, will fix in Task 2.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "chore: replace androidx.credentials with play-services-fido dependency"
```

---

### Task 2: Rewrite `PasskeyManager` to use GMS FIDO2 API

**Files:**
- Modify: `app/src/main/java/com/franklinharper/concentra/browser/web/PasskeyManager.kt`
- Modify: `app/src/test/java/com/franklinharper/concentra/browser/web/PasskeyManagerTest.kt`

**Key changes:**
- Replace `CredentialManager` with `Fido2PrivilegedApiClient`
- `create()` now builds `BrowserPublicKeyCredentialCreationOptions` with the origin and returns a `PendingIntent` wrapped in `PasskeyResult`
- `get()` now builds `BrowserPublicKeyCredentialRequestOptions` with the origin and returns a `PendingIntent` wrapped in `PasskeyResult`
- The actual credential creation/authentication happens via the PendingIntent flow in the Activity
- Add a static method `handleActivityResult()` that extracts the result from the Intent and returns the appropriate `PasskeyResult`

- [ ] **Step 1: Update `PasskeyResult` to carry PendingIntent**

Add new variants to `PasskeyResult`:

```kotlin
sealed class PasskeyResult {
    data class Success(val responseJson: String) : PasskeyResult()
    data class Failure(val errorType: String, val message: String) : PasskeyResult()
    object NotSupported : PasskeyResult()
    object Cancelled : PasskeyResult()
    data class LaunchIntent(val pendingIntent: android.app.PendingIntent, val requestCode: Int) : PasskeyResult()
}
```

- [ ] **Step 2: Update tests**

The `create` and `get` methods now return `LaunchIntent` on success (containing a PendingIntent) instead of `Success`. The `Success` result now only comes from `handleActivityResult`. Update tests to reflect this. The `isSupported` tests remain the same.

- [ ] **Step 3: Rewrite `PasskeyManager`**

```kotlin
package com.franklinharper.concentra.browser.web

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.Attachment
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.google.android.gms.fido.fido2.api.common.ResidentKeyRequirement
import com.google.android.gms.fido.fido2.api.common.UserVerificationRequirement
import com.google.android.gms.tasks.Tasks
import org.json.JSONArray
import org.json.JSONObject

class PasskeyManager(
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) {
    fun isSupported(): Boolean = sdkInt >= 23

    companion object {
        const val REGISTER_REQUEST_CODE = 1001
        const val SIGN_REQUEST_CODE = 1002

        fun handleResult(data: android.content.Intent): PasskeyResult {
            val errorBytes = data.getByteArrayExtra(Fido.FIDO2_KEY_ERROR_EXTRA)
            if (errorBytes != null) {
                val error = AuthenticatorErrorResponse.deserializeFromBytes(errorBytes)
                return when (error.errorCode) {
                    ErrorCode.NOT_ALLOWED_ERR -> PasskeyResult.Cancelled
                    else -> PasskeyResult.Failure(
                        errorType = error.errorCode.name,
                        message = error.errorMessage ?: error.errorCode.name,
                    )
                }
            }

            val credBytes = data.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
                ?: return PasskeyResult.Failure("error", "No credential data in result")

            val credential = PublicKeyCredential.deserializeFromBytes(credBytes)
            val response = credential.response
            return when (response) {
                is AuthenticatorAttestationResponse -> {
                    // Registration response — build the WebAuthn JSON
                    val json = buildRegistrationJson(credential, response)
                    PasskeyResult.Success(json)
                }
                is AuthenticatorAssertionResponse -> {
                    // Authentication response — use toJson()
                    PasskeyResult.Success(credential.toJson())
                }
                else -> PasskeyResult.Failure("error", "Unexpected response type")
            }
        }

        private fun buildRegistrationJson(
            credential: PublicKeyCredential,
            response: AuthenticatorAttestationResponse,
        ): String {
            val clientDataJSON = android.util.Base64.encodeToString(
                response.clientDataJSON, android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
            )
            val attestationObject = android.util.Base64.encodeToString(
                response.attestationObject, android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
            )
            val rawId = android.util.Base64.encodeToString(
                credential.rawId, android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
            )
            val transports = response.transports?.toList() ?: emptyList()

            return JSONObject().apply {
                put("id", credential.id)
                put("rawId", rawId)
                put("type", credential.type)
                put("response", JSONObject().apply {
                    put("clientDataJSON", clientDataJSON)
                    put("attestationObject", attestationObject)
                    put("transports", JSONArray(transports))
                })
                put("getClientExtensionResults", JSONObject())
            }.toString()
        }
    }

    fun create(context: Context, requestJson: String, origin: String): PasskeyResult {
        if (!isSupported()) return PasskeyResult.NotSupported
        return try {
            val options = PublicKeyCredentialCreationOptions(requestJson)
            val browserOptions = BrowserPublicKeyCredentialCreationOptions.Builder()
                .setPublicKeyCredentialCreationOptions(options)
                .setOrigin(Uri.parse(origin))
                .build()

            val client = Fido.getFido2PrivilegedApiClient(context)
            val pendingIntent = Tasks.await(client.getRegisterPendingIntent(browserOptions))
            PasskeyResult.LaunchIntent(pendingIntent, REGISTER_REQUEST_CODE)
        } catch (e: Exception) {
            Log.e("PasskeyManager", "create failed", e)
            PasskeyResult.Failure("error", e.message ?: "Unknown error")
        }
    }

    fun get(context: Context, requestJson: String, origin: String): PasskeyResult {
        if (!isSupported()) return PasskeyResult.NotSupported
        return try {
            val json = JSONObject(requestJson)
            val allowList = json.optJSONArray("allowCredentials")?.let { arr ->
                (0 until arr.length()).map { i ->
                    val c = arr.getJSONObject(i)
                    PublicKeyCredentialDescriptor(
                        c.optString("type", PublicKeyCredentialType.PUBLIC_KEY.toString()),
                        android.util.Base64.decode(c.getString("id"), android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP),
                        emptyList(),
                    )
                }
            }

            val requestOptions = PublicKeyCredentialRequestOptions.Builder()
                .setChallenge(android.util.Base64.decode(json.getString("challenge"), android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP))
                .setRpId(json.optString("rpId", null))
                .setAllowList(allowList ?: emptyList())
                .setTimeoutSeconds(json.optDouble("timeout", 60.0))
                .build()

            val browserOptions = BrowserPublicKeyCredentialRequestOptions.Builder()
                .setPublicKeyCredentialRequestOptions(requestOptions)
                .setOrigin(Uri.parse(origin))
                .build()

            val client = Fido.getFido2PrivilegedApiClient(context)
            val pendingIntent = Tasks.await(client.getSignPendingIntent(browserOptions))
            PasskeyResult.LaunchIntent(pendingIntent, SIGN_REQUEST_CODE)
        } catch (e: Exception) {
            Log.e("PasskeyManager", "get failed", e)
            PasskeyResult.Failure("error", e.message ?: "Unknown error")
        }
    }
}
```

- [ ] **Step 4: Run tests**

Run: `./gradlew :app:testDebugUnitTest --tests "*.PasskeyManagerTest"`
Expected: Tests pass (isSupported tests unchanged, create/get tests updated)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/web/PasskeyManager.kt \
        app/src/main/java/com/franklinharper/concentra/browser/web/PasskeyResult.kt \
        app/src/test/java/com/franklinharper/concentra/browser/web/PasskeyManagerTest.kt
git commit -m "feat: rewrite PasskeyManager to use GMS FIDO2 Privileged API"
```

---

### Task 3: Update `PasskeyBridge` for PendingIntent flow

**Files:**
- Modify: `app/src/main/java/com/franklinharper/concentra/browser/web/PasskeyBridge.kt`

**Key changes:**
- `passkeyCreate` and `passkeyGet` now call the synchronous `PasskeyManager.create/get` (no coroutines needed)
- When result is `LaunchIntent`, store the pending request ID and call a callback to launch the PendingIntent from the Activity
- Add a `resolveWithResult(intent)` method that the Activity calls back with the result

```kotlin
package com.franklinharper.concentra.browser.web

import android.app.Activity
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.util.Log

class PasskeyBridge(
    private val context: Context,
    private val webView: WebView,
) {
    private val tag = "PasskeyBridge"
    private val passkeyManager = PasskeyManager()
    private var pendingCreateRequestId: String? = null
    private var pendingGetRequestId: String? = null

    var launchIntentCallback: ((android.app.PendingIntent, Int) -> Unit)? = null

    @JavascriptInterface
    fun passkeyCreate(requestId: String, json: String, origin: String) {
        Log.d(tag, "passkeyCreate: requestId=$requestId origin=$origin json=${json.take(200)}")
        val result = passkeyManager.create(context, json, origin)
        Log.d(tag, "passkeyCreate result: $result")
        when (result) {
            is PasskeyResult.LaunchIntent -> {
                pendingCreateRequestId = requestId
                launchIntentCallback?.invoke(result.pendingIntent, result.requestCode)
            }
            else -> sendResult(requestId, result)
        }
    }

    @JavascriptInterface
    fun passkeyGet(requestId: String, json: String, origin: String) {
        Log.d(tag, "passkeyGet: requestId=$requestId origin=$origin json=${json.take(200)}")
        val result = passkeyManager.get(context, json, origin)
        Log.d(tag, "passkeyGet result: $result")
        when (result) {
            is PasskeyResult.LaunchIntent -> {
                pendingGetRequestId = requestId
                launchIntentCallback?.invoke(result.pendingIntent, result.requestCode)
            }
            else -> sendResult(requestId, result)
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        Log.d(tag, "onActivityResult: requestCode=$requestCode resultCode=$resultCode")
        if (data == null) {
            val requestId = when (requestCode) {
                PasskeyManager.REGISTER_REQUEST_CODE -> pendingCreateRequestId
                PasskeyManager.SIGN_REQUEST_CODE -> pendingGetRequestId
                else -> null
            }
            if (requestId != null) {
                sendResult(requestId, PasskeyResult.Cancelled)
            }
            return
        }

        val result = PasskeyManager.handleResult(data)
        val requestId = when (requestCode) {
            PasskeyManager.REGISTER_REQUEST_CODE -> pendingCreateRequestId.also { pendingCreateRequestId = null }
            PasskeyManager.SIGN_REQUEST_CODE -> pendingGetRequestId.also { pendingGetRequestId = null }
            else -> null
        }
        if (requestId != null) {
            sendResult(requestId, result)
        }
    }

    private fun sendResult(requestId: String, result: PasskeyResult) {
        Log.d(tag, "sendResult: requestId=$requestId result=$result")
        when (result) {
            is PasskeyResult.Success -> resolve(requestId, result.responseJson)
            is PasskeyResult.Cancelled -> reject(requestId, "cancelled", "User cancelled")
            is PasskeyResult.NotSupported -> reject(requestId, "not_supported", "Passkeys require API 23+")
            is PasskeyResult.Failure -> reject(requestId, result.errorType, result.message)
            is PasskeyResult.LaunchIntent -> {} // handled by caller
        }
    }

    private fun resolve(requestId: String, responseJson: String) {
        val escaped = responseJson
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029")
        eval("window.__passkeyResolve('$requestId', '$escaped')")
    }

    private fun reject(requestId: String, errorType: String, message: String) {
        val escaped = message.replace("'", "\\'")
        eval("window.__passkeyReject('$requestId', '$errorType', '$escaped')")
    }

    private fun eval(js: String) {
        webView.post { webView.evaluateJavascript(js, null) }
    }
}
```

- [ ] **Step 1: Replace PasskeyBridge with the new implementation**

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD FAILED (WebViewHost not yet updated) — expected, will fix in Task 4

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/web/PasskeyBridge.kt
git commit -m "feat: rewrite PasskeyBridge for GMS FIDO2 PendingIntent flow"
```

---

### Task 4: Update `WebViewHost` for new bridge API

**Files:**
- Modify: `app/src/main/java/com/franklinharper/concentra/browser/web/WebViewHost.kt`

**Key changes:**
- `PasskeyBridge` no longer takes `activity` or `PasskeyManager` — it takes `context` and creates its own manager
- Expose the `bridge` via a return or callback so `BrowserActivity` can set `launchIntentCallback` and call `onActivityResult`
- The bridge is created alongside the WebView and exposed to the caller

Add a `WebViewWithBridge` data class or a callback parameter to `WebViewHost`.

**Approach:** Add an `onBridgeCreated: (PasskeyBridge) -> Unit` parameter to `WebViewHost`. The caller (BrowserActivity/BrowserRoute) receives the bridge and wires up the PendingIntent launch and activity result.

```kotlin
// WebViewHost signature change:
@Composable
fun WebViewHost(
    settings: BrowserSettings,
    downloadHandler: BrowserDownloadHandler,
    command: WebViewCommand?,
    effect: BrowserViewModel.Effect?,
    onCommandConsumed: () -> Unit,
    onEffectConsumed: () -> Unit,
    onEvent: (WebViewEvent) -> Unit,
    onBridgeCreated: (PasskeyBridge) -> Unit,
    modifier: Modifier = Modifier,
)
```

- [ ] **Step 1: Update WebViewHost to create bridge with new API and expose it via callback**

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD FAILED (BrowserRoute not yet updated) — expected, will fix in Task 5

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/web/WebViewHost.kt
git commit -m "feat: expose PasskeyBridge via onBridgeCreated callback in WebViewHost"
```

---

### Task 5: Wire PendingIntent flow in `BrowserActivity`

**Files:**
- Modify: `app/src/main/java/com/franklinharper/concentra/browser/BrowserActivity.kt`
- Modify: `app/src/main/java/com/franklinharper/concentra/browser/BrowserRoute.kt` (or wherever WebViewHost is called)

**Key changes:**
- `BrowserActivity` registers an `ActivityResultLauncher` using `registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult())`
- The launcher callback calls `bridge.onActivityResult(requestCode, resultCode, data)`
- The bridge reference is threaded through from `WebViewHost`'s `onBridgeCreated` callback
- `BrowserRoute` (or wherever `WebViewHost` is invoked) passes the `onBridgeCreated` parameter through

- [ ] **Step 1: Add ActivityResultLauncher to BrowserActivity**

```kotlin
class BrowserActivity : ComponentActivity() {
    private var passkeyBridge: PasskeyBridge? = null

    private val fidoLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        passkeyBridge?.onActivityResult(
            requestCode = result.data?.getIntExtra("requestCode", -1) ?: -1,
            resultCode = result.resultCode,
            data = result.data,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrowserApp(activity = this)
        }
    }
}
```

Wait — `ActivityResultContracts.StartIntentSenderForResult` doesn't carry `requestCode`. We need to track the request code ourselves. Let me use the approach of storing the current request code in the bridge or launcher.

Actually, the simpler approach: since we only have two request codes, and they're mutually exclusive (only one pending at a time), the bridge already tracks `pendingCreateRequestId` / `pendingGetRequestId`. We can determine the request code from which one is pending.

Better approach: Use `launchIntentSenderForResult` directly via the `ActivityResultLauncher`. The bridge stores the request code along with the pending request:

```kotlin
class BrowserActivity : ComponentActivity() {
    private var passkeyBridge: PasskeyBridge? = null

    private val fidoLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        passkeyBridge?.onActivityResult(result.resultCode, result.data)
    }

    // Called by bridge via launchIntentCallback
    fun launchFidoIntent(pendingIntent: android.app.PendingIntent, requestCode: Int) {
        fidoLauncher.launch(
            androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.intentSender)
                .setFillInIntent(android.content.Intent().putExtra("requestCode", requestCode))
                .build()
        )
    }
}
```

The bridge's `onActivityResult` can infer which request was pending from its own state (pendingCreateRequestId vs pendingGetRequestId).

- [ ] **Step 2: Thread bridge through BrowserRoute to WebViewHost**

Update `BrowserRoute` (or wherever `WebViewHost` is called) to pass `onBridgeCreated` parameter.

- [ ] **Step 3: Verify full build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/BrowserActivity.kt \
        app/src/main/java/com/franklinharper/concentra/browser/BrowserRoute.kt
git commit -m "feat: wire FIDO2 PendingIntent flow through BrowserActivity"
```

---

### Task 6: Update JS polyfill `toBase64Url` to match GMS response format

**Files:**
- Modify: `app/src/main/assets/passkey-polyfill.js`

**Key changes:**
- The GMS FIDO2 API returns responses as JSON strings with base64url-encoded values. The `buildRegistrationJson` method in `PasskeyManager` produces the same format.
- The `PublicKeyCredential.toJson()` for assertion responses also uses base64url.
- The polyfill's `deserializeCreateResponse` and `deserializeGetResponse` should work as-is since they expect base64url strings.
- No changes expected, but verify.

- [ ] **Step 1: Review polyfill compatibility with GMS response format**

Verify that the JSON structure from `buildRegistrationJson` matches what `deserializeCreateResponse` expects:
- `id` (string) ✓
- `rawId` (base64url string) ✓
- `type` (string) ✓
- `response.clientDataJSON` (base64url string) ✓
- `response.attestationObject` (base64url string) ✓

Verify `credential.toJson()` format matches `deserializeGetResponse`:
- `id`, `rawId`, `type` ✓
- `response.clientDataJSON`, `response.authenticatorData`, `response.signature` ✓
- `response.userHandle` (base64url string or null) ✓

- [ ] **Step 2: Commit if changes needed, otherwise note as verified**

---

### Task 7: Clean up debug logging and unused code

**Files:**
- Modify: `app/src/main/java/com/franklinharper/concentra/browser/web/PasskeyBridge.kt`
- Modify: `app/src/main/java/com/franklinharper/concentra/browser/web/PasskeyManager.kt`
- Modify: `app/src/main/java/com/franklinharper/concentra/browser/web/WebViewHost.kt`
- Modify: `app/src/main/assets/passkey-polyfill.js`

- [ ] **Step 1: Remove all `Log.d`, `Log.e`, and `console.log` debug statements added during debugging**

- [ ] **Step 2: Remove unused imports**

- [ ] **Step 3: Verify build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run all tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "chore: clean up debug logging from passkey implementation"
```

---

### Task 8: End-to-end verification on device

- [ ] **Step 1: Install on Pixel 6**

```bash
./gradlew :app:installDebug
```

- [ ] **Step 2: Test on webauthn.io**

1. Navigate to webauthn.io
2. Enter a username, tap Register
3. Verify biometric prompt appears
4. Complete registration — verify success
5. Tap Authenticate
6. Verify biometric prompt appears
7. Complete authentication — verify success

- [ ] **Step 3: Commit any fixes needed**
