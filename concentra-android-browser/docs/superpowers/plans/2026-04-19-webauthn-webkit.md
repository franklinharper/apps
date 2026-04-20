# WebAuthn via WebViewFeature.WEB_AUTHENTICATION Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the JS polyfill + JavascriptInterface bridge with a single `WebSettingsCompat.setWebAuthenticationSupport` call that lets the WebView handle `navigator.credentials.*` natively via CredentialManager.

**Architecture:** `WebViewFeature.WEB_AUTHENTICATION` (androidx.webkit ≥ 1.14.0) intercepts WebAuthn JS calls inside the WebView and delegates them to CredentialManager internally. The WebView layer owns origin context, so no `CREDENTIAL_MANAGER_SET_ORIGIN` permission or Google allowlisting is required. `PasskeyBridge`, `PasskeyManager`, `PasskeyResult`, and the JS polyfill are deleted entirely.

**Tech Stack:** Kotlin, `androidx.webkit:webkit:1.14.0`, `androidx.credentials:credentials:1.5.0`, `androidx.credentials:credentials-play-services-auth:1.5.0`

---

### Task 1: Replace play-services-fido with androidx.credentials + androidx.webkit

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

No unit tests for this task — it is purely a dependency change.

**Context:** The worktree currently uses `com.google.android.gms:play-services-fido:21.0.0` (from the abandoned GMS FIDO2 Privileged API approach). This task replaces it with `androidx.credentials` (required at runtime by the WebKit WebAuthn layer on GMS devices) and adds `androidx.webkit:1.14.0` (which provides `WebViewFeature.WEB_AUTHENTICATION`).

- [ ] **Step 1: Replace the version catalog**

Replace the entire contents of `gradle/libs.versions.toml` with:

```toml
[versions]
kotlin = "2.3.20"
agp = "8.12.3"
activityCompose = "1.13.0"
lifecycleViewModelCompose = "2.10.0"
material3 = "1.4.0"
composeUi = "1.10.6"
webkit = "1.14.0"

[libraries]
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycleViewModelCompose" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3", version.ref = "material3" }
androidx-compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended", version = "1.7.8" }
androidx-credentials = { module = "androidx.credentials:credentials", version = "1.5.0" }
androidx-credentials-play-services-auth = { module = "androidx.credentials:credentials-play-services-auth", version = "1.5.0" }
androidx-webkit = { module = "androidx.webkit:webkit", version.ref = "webkit" }
androidx-compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4", version.ref = "composeUi" }
androidx-compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest", version.ref = "composeUi" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

- [ ] **Step 2: Replace the dependencies block in build.gradle.kts**

Replace the entire `dependencies { ... }` block in `app/build.gradle.kts` with:

```kotlin
dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.webkit)
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

- [ ] **Step 3: Verify the build resolves the new dependencies**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD FAILED — the GMS FIDO2 imports in PasskeyManager and PasskeyBridge will now be unresolved. This is expected; those files are deleted in Task 3.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "chore: replace play-services-fido with androidx.credentials + androidx.webkit"
```

---

### Task 2: Enable native WebAuthn in WebViewHost; remove bridge cascade

**Files:**
- Modify: `app/src/main/java/com/franklinharper/concentra/browser/web/WebViewHost.kt`
- Modify: `app/src/main/java/com/franklinharper/concentra/browser/web/BrowserWebViewClient.kt`
- Modify: `app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserScreen.kt`
- Modify: `app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserRoute.kt`
- Modify: `app/src/main/java/com/franklinharper/concentra/browser/BrowserActivity.kt`

No unit tests for this task — the passkey logic now lives entirely in the platform layer. All five files must be updated together because removing the `onBridgeCreated` parameter from `WebViewHost` would break `BrowserScreen`, removing it from `BrowserScreen` would break `BrowserRoute`, and so on down the call chain.

- [ ] **Step 1: Replace WebViewHost.kt**

`WebViewHost.kt` gains the `setWebAuthenticationSupport` call and loses: the polyfill JS loading, the bridge creation, `addJavascriptInterface`, and the `onBridgeCreated` parameter. The `onPageStarted` lambda passed to `BrowserWebViewClient` is also removed (handled in Step 2).

Replace the entire file with:

```kotlin
package com.franklinharper.concentra.browser.web

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.franklinharper.concentra.browser.BrowserViewModel
import com.franklinharper.concentra.browser.settings.BrowserSettings

@Composable
fun WebViewHost(
    settings: BrowserSettings,
    downloadHandler: BrowserDownloadHandler,
    command: WebViewCommand?,
    effect: BrowserViewModel.Effect?,
    onCommandConsumed: () -> Unit,
    onEffectConsumed: () -> Unit,
    onEvent: (WebViewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val configurator = remember { WebViewConfigurator() }
    val currentOnEvent = rememberUpdatedState(onEvent)
    val currentDownloadHandler = rememberUpdatedState(downloadHandler)

    val webView = remember(context) {
        WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            webChromeClient = BrowserWebChromeClient()
            webViewClient = BrowserWebViewClient(
                onEvent = { event -> currentOnEvent.value(event) },
            )
            if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_AUTHENTICATION)) {
                WebSettingsCompat.setWebAuthenticationSupport(
                    this.settings,
                    WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP,
                )
            }
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.destroy()
        }
    }

    LaunchedEffect(webView, command) {
        when (val currentCommand = command) {
            is WebViewCommand.LoadUrl -> {
                webView.loadUrl(currentCommand.url)
                onCommandConsumed()
            }
            null -> Unit
        }
    }

    LaunchedEffect(webView, effect) {
        when (effect) {
            BrowserViewModel.Effect.GoBack -> {
                if (webView.canGoBack()) {
                    webView.goBack()
                }
                onEffectConsumed()
            }
            BrowserViewModel.Effect.OpenFindInPage -> {
                @Suppress("DEPRECATION")
                webView.showFindDialog(null, true)
                onEffectConsumed()
            }
            null -> Unit
            else -> Unit
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier,
        update = { currentWebView ->
            currentWebView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                currentDownloadHandler.value.enqueue(
                    url = url,
                    userAgent = userAgent,
                    contentDisposition = contentDisposition,
                    mimeType = mimeType,
                )
            }
            settings.applyTo(
                webView = currentWebView,
                configurator = configurator,
            )
        },
    )
}

private fun BrowserSettings.applyTo(
    webView: WebView,
    configurator: WebViewConfigurator,
) {
    configurator.configure(
        settings = this,
        sink =
            object : WebViewConfigurator.SettingsSink {
                override var javaScriptEnabled: Boolean
                    get() = webView.settings.javaScriptEnabled
                    set(value) {
                        webView.settings.javaScriptEnabled = value
                    }

                override var domStorageEnabled: Boolean
                    get() = webView.settings.domStorageEnabled
                    set(value) {
                        webView.settings.domStorageEnabled = value
                    }

                override var builtInZoomControlsEnabled: Boolean
                    get() = webView.settings.builtInZoomControls
                    set(value) {
                        webView.settings.builtInZoomControls = value
                    }

                override var displayZoomControlsEnabled: Boolean
                    get() = webView.settings.displayZoomControls
                    set(value) {
                        webView.settings.displayZoomControls = value
                    }

                override var supportZoomEnabled: Boolean
                    get() = webView.settings.supportZoom()
                    set(value) {
                        webView.settings.setSupportZoom(value)
                    }

                override var supportMultipleWindows: Boolean
                    get() = webView.settings.supportMultipleWindows()
                    set(value) {
                        webView.settings.setSupportMultipleWindows(value)
                    }

                override var thirdPartyCookiesEnabled: Boolean
                    get() = CookieManager.getInstance().acceptThirdPartyCookies(webView)
                    set(value) {
                        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, value)
                    }
            },
    )
}
```

- [ ] **Step 2: Replace BrowserWebViewClient.kt**

Remove the `onPageStarted` callback parameter — nothing injects the polyfill anymore.

Replace the entire file with:

```kotlin
package com.franklinharper.concentra.browser.web

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient

class BrowserWebViewClient(
    private val onEvent: (WebViewEvent) -> Unit,
) : WebViewClient() {
    override fun onPageStarted(
        view: WebView?,
        url: String?,
        favicon: Bitmap?,
    ) {
        onEvent(WebViewEvent.PageLoadStarted(url = url))
    }

    override fun doUpdateVisitedHistory(
        view: WebView?,
        url: String?,
        isReload: Boolean,
    ) {
        onEvent(
            WebViewEvent.NavigationStateChanged(
                currentUrl = url,
                canGoBack = view?.canGoBack() == true,
            ),
        )
    }

    override fun onPageFinished(
        view: WebView?,
        url: String?,
    ) {
        onEvent(
            WebViewEvent.PageLoadFinished(
                currentUrl = url,
                canGoBack = view?.canGoBack() == true,
            ),
        )
    }
}
```

- [ ] **Step 3: Replace BrowserScreen.kt**

Remove the `onBridgeCreated` parameter and its forwarding to `WebViewHost`.

Replace the entire file with:

```kotlin
package com.franklinharper.concentra.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.franklinharper.concentra.browser.model.BrowserUiState
import com.franklinharper.concentra.browser.BrowserViewModel
import com.franklinharper.concentra.browser.web.WebViewCommand
import com.franklinharper.concentra.browser.web.WebViewEvent
import com.franklinharper.concentra.browser.web.WebViewHost
import com.franklinharper.concentra.browser.web.BrowserDownloadHandler

@Composable
fun BrowserScreen(
    uiState: BrowserUiState,
    urlInput: String,
    downloadHandler: BrowserDownloadHandler,
    pendingWebCommand: WebViewCommand?,
    pendingWebEffect: BrowserViewModel.Effect?,
    onWebCommandConsumed: () -> Unit,
    onWebEffectConsumed: () -> Unit,
    onWebViewEvent: (WebViewEvent) -> Unit,
    onUrlInputChange: (String) -> Unit,
    onUrlSubmit: () -> Unit,
    onGoogleClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onShareClick: () -> Unit,
    onFindClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    onHotspotSwipeUp: () -> Unit,
    onChromeScrimTap: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFE9E2D6), Color(0xFFF7F2EA)),
                        ),
                )
                .semantics {
                    testTagsAsResourceId = true
                },
    ) {
        WebViewHost(
            settings = uiState.settings,
            downloadHandler = downloadHandler,
            command = pendingWebCommand,
            effect = pendingWebEffect,
            onCommandConsumed = onWebCommandConsumed,
            onEffectConsumed = onWebEffectConsumed,
            onEvent = onWebViewEvent,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
        )

        if (uiState.isChromeVisible) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.08f))
                            .clickable(onClick = onChromeScrimTap),
                )

                BrowserChromeSheet(
                    uiState = uiState,
                    urlInput = urlInput,
                    requestInitialFocus = uiState.currentUrl == null,
                    onUrlInputChange = onUrlInputChange,
                    onUrlSubmit = onUrlSubmit,
                    onGoogleClick = onGoogleClick,
                    onArchiveClick = onArchiveClick,
                    onShareClick = onShareClick,
                    onFindClick = onFindClick,
                    onSettingsClick = onSettingsClick,
                    onExitClick = onExitClick,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                )
            }
        }

        if (!uiState.isChromeVisible) {
            HotspotOverlay(
                onSwipeUp = onHotspotSwipeUp,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding(),
            )
        }
    }
}

object BrowserScreenTags {
    const val ChromeSheet = "browser_chrome_sheet"
    const val UrlField = "browser_url_field"
    const val HotspotOverlay = "browser_hotspot_overlay"
}
```

- [ ] **Step 4: Replace BrowserRoute.kt**

Remove the `onBridgeCreated` parameter and its forwarding to `BrowserScreen`.

Replace the entire file with:

```kotlin
package com.franklinharper.concentra.browser.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.franklinharper.concentra.browser.BrowserAppContainer
import com.franklinharper.concentra.browser.BrowserViewModel
import com.franklinharper.concentra.browser.model.BrowserAction
import com.franklinharper.concentra.browser.settings.SettingsActivity
import com.franklinharper.concentra.browser.web.WebViewCommand

@Composable
fun BrowserRoute(container: BrowserAppContainer) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: BrowserViewModel =
        viewModel(
            factory =
                viewModelFactory {
                    initializer {
                        BrowserViewModel(
                            launchRequest = container.launchRequest,
                            settingsRepository = container.settingsRepository,
                        )
                    }
                },
        )
    val uiState by viewModel.uiState.collectAsState()
    val pendingEffect by viewModel.pendingEffect.collectAsState()
    var urlInput by rememberSaveable { mutableStateOf(uiState.pendingUrlInput) }
    var pendingWebCommand by remember { mutableStateOf<WebViewCommand?>(null) }
    var pendingWebEffect by remember { mutableStateOf<BrowserViewModel.Effect?>(null) }

    LaunchedEffect(uiState.pendingUrlInput) {
        urlInput = uiState.pendingUrlInput
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.reloadSettings()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState) {
        viewModel.consumePendingWebCommand()?.let { pendingWebCommand = it }
    }

    LaunchedEffect(pendingEffect) {
        viewModel.consumePendingEffect()?.let { effect ->
            when (effect) {
                is BrowserViewModel.Effect.ShareUrl -> {
                    val shareIntent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, effect.url)
                        }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                }
                BrowserViewModel.Effect.Exit -> activity?.finish()
                BrowserViewModel.Effect.OpenSettings ->
                    context.startActivity(Intent(context, SettingsActivity::class.java))
                BrowserViewModel.Effect.GoBack,
                BrowserViewModel.Effect.OpenFindInPage -> pendingWebEffect = effect
            }
        }
    }

    BackHandler {
        viewModel.onAction(BrowserAction.BackPressed)
    }

    BrowserScreen(
        uiState = uiState,
        urlInput = urlInput,
        downloadHandler = container.downloadHandler,
        pendingWebCommand = pendingWebCommand,
        pendingWebEffect = pendingWebEffect,
        onWebCommandConsumed = { pendingWebCommand = null },
        onWebEffectConsumed = { pendingWebEffect = null },
        onWebViewEvent = viewModel::onWebViewEvent,
        onUrlInputChange = { urlInput = it },
        onUrlSubmit = { viewModel.onAction(BrowserAction.SubmitUrl(urlInput)) },
        onGoogleClick = { viewModel.onAction(BrowserAction.GoogleClicked) },
        onArchiveClick = { viewModel.onAction(BrowserAction.ArchiveTodayClicked) },
        onShareClick = { viewModel.onAction(BrowserAction.ShareLinkClicked) },
        onFindClick = { viewModel.onAction(BrowserAction.FindInPageClicked) },
        onSettingsClick = { viewModel.onAction(BrowserAction.OpenSettingsClicked) },
        onExitClick = { viewModel.onAction(BrowserAction.ExitClicked) },
        onHotspotSwipeUp = { viewModel.onAction(BrowserAction.ShowChrome) },
        onChromeScrimTap = { viewModel.onAction(BrowserAction.HideChrome) },
    )
}
```

- [ ] **Step 5: Replace BrowserActivity.kt**

Remove the `onBridgeCreated = {}` argument and the now-unused `PasskeyBridge` import.

Replace the entire file with:

```kotlin
package com.franklinharper.concentra.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.franklinharper.concentra.browser.ui.BrowserRoute

class BrowserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrowserApp(activity = this)
        }
    }
}

@Composable
private fun BrowserApp(activity: BrowserActivity) {
    val container = remember(activity) { BrowserAppContainer(activity) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BrowserRoute(container = container)
        }
    }
}
```

- [ ] **Step 6: Verify the full build compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/web/WebViewHost.kt \
        app/src/main/java/com/franklinharper/concentra/browser/web/BrowserWebViewClient.kt \
        app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserScreen.kt \
        app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserRoute.kt \
        app/src/main/java/com/franklinharper/concentra/browser/BrowserActivity.kt
git commit -m "feat: enable native WebAuthn via WebViewFeature.WEB_AUTHENTICATION"
```

---

### Task 3: Delete dead files and remove unused test dependencies

**Files:**
- Delete: `app/src/main/java/com/franklinharper/concentra/browser/web/PasskeyBridge.kt`
- Delete: `app/src/main/java/com/franklinharper/concentra/browser/web/PasskeyManager.kt`
- Delete: `app/src/main/java/com/franklinharper/concentra/browser/web/PasskeyResult.kt`
- Delete: `app/src/main/assets/passkey-polyfill.js`
- Delete: `app/src/test/java/com/franklinharper/concentra/browser/web/PasskeyManagerTest.kt`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Delete the five dead files**

```bash
rm app/src/main/java/com/franklinharper/concentra/browser/web/PasskeyBridge.kt
rm app/src/main/java/com/franklinharper/concentra/browser/web/PasskeyManager.kt
rm app/src/main/java/com/franklinharper/concentra/browser/web/PasskeyResult.kt
rm app/src/main/assets/passkey-polyfill.js
rm app/src/test/java/com/franklinharper/concentra/browser/web/PasskeyManagerTest.kt
```

- [ ] **Step 2: Remove now-unused test dependencies from build.gradle.kts**

`androidx.test:core` and `robolectric` were only used by `PasskeyManagerTest`. Replace the entire `dependencies` block in `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.webkit)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

- [ ] **Step 3: Verify full build and test suite**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, 0 tests run (no test files remain), 0 failures

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: delete PasskeyBridge, PasskeyManager, polyfill and unused test deps"
```

---

### Task 4: End-to-end verification on device

- [ ] **Step 1: Install the debug build**

```bash
./gradlew :app:installDebug
```

Expected: BUILD SUCCESSFUL, app installs on connected device.

- [ ] **Step 2: Test registration on webauthn.io**

1. Open the app and navigate to `https://webauthn.io`
2. Enter any username and tap **Register**
3. Expected: the system passkey bottom sheet appears (biometric / screen lock prompt)
4. Complete the prompt
5. Expected: the page shows a success message confirming passkey registration

- [ ] **Step 3: Test authentication on webauthn.io**

1. On the same page, tap **Authenticate**
2. Expected: the system passkey bottom sheet appears again
3. Complete the prompt
4. Expected: the page shows a success message confirming authentication

- [ ] **Step 4: Commit any fixes found during verification**

If the device's WebView does not support `WebViewFeature.WEB_AUTHENTICATION` (i.e., `isFeatureSupported` returns false), update the WebView APK via the Play Store on the test device and retry. The feature requires WebView 107+ (Android System WebView 107.0.5304.87 or later).
