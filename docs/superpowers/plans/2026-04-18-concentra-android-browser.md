# Concentra Android Browser Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a single-session Android browser app with a full-screen `WebView`, swipe-up bottom-right hotspot, bottom-sheet browser chrome, share-target URL handling, downloads via `DownloadManager`, JavaScript dialog support, and a minimal settings screen for third-party cookies.

**Architecture:** Create a standard Android app in `concentra-android-browser` using the Android CLI, then layer a single-activity Compose shell around one managed `WebView`. Keep browser behavior in focused units: intent parsing, URL building, settings persistence, `ViewModel` state, and a thin `WebView` bridge so most logic remains plain Kotlin and testable.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Android `WebView`, MVVM, manual dependency injection, JUnit, AndroidX test/Compose UI test, red-green TDD.

---

## Planned File Structure

Expected scaffold root:

- `concentra-android-browser/settings.gradle.kts`
- `concentra-android-browser/build.gradle.kts`
- `concentra-android-browser/gradle.properties`
- `concentra-android-browser/app/build.gradle.kts`
- `concentra-android-browser/app/src/main/AndroidManifest.xml`
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/BrowserActivity.kt`

Files to create or modify during implementation:

- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/BrowserAppContainer.kt`
  - Manual DI assembly
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/intent/IntentParser.kt`
  - Parse launch and share intents into internal commands
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/model/LaunchRequest.kt`
  - Internal launch/share request models
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/model/BrowserAction.kt`
  - User action models and browser commands
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/model/BrowserUiState.kt`
  - ViewModel state definitions
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/navigation/BrowserNavigator.kt`
  - Screen routing for browser and settings
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/settings/SettingsRepository.kt`
  - Interface for persisted settings
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/settings/PreferencesSettingsRepository.kt`
  - SharedPreferences-backed repository
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/url/UrlNormalizer.kt`
  - Normalize raw input to browsable URLs
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/url/ArchiveTodayUrlBuilder.kt`
  - Build `https://archive.ph/<current-url>`
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/WebViewCommand.kt`
  - Commands sent from ViewModel to WebView layer
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/WebViewEvent.kt`
  - Events sent from WebView layer to ViewModel
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/BrowserWebViewClient.kt`
  - Navigation and load callbacks
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/BrowserWebChromeClient.kt`
  - Title, progress, JS dialogs, download hooks
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/WebViewConfigurator.kt`
  - Runtime `WebView` settings application
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/WebViewHost.kt`
  - Thin adapter for actual `WebView`
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/download/DownloadStarter.kt`
  - Start downloads through `DownloadManager`
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/share/ShareLauncher.kt`
  - Open Android share UI
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserRoute.kt`
  - Compose route wiring for browser screen
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserScreen.kt`
  - Main browser Compose screen
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserChromeSheet.kt`
  - Bottom-sheet chrome content
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/HotspotOverlay.kt`
  - Bottom-right swipe hotspot
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/FindInPageBar.kt`
  - Find-in-page UI
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/JsDialogHost.kt`
  - Compose Material dialog renderer for JS dialogs
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/SettingsScreen.kt`
  - Separate settings screen
- `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt`
  - Main ViewModel
- `concentra-android-browser/app/src/androidTest/java/com/franklinharper/concentra/browser/ui/BrowserScreenTest.kt`
  - Compose UI tests
- `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/intent/IntentParserTest.kt`
- `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/url/UrlNormalizerTest.kt`
- `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/url/ArchiveTodayUrlBuilderTest.kt`
- `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/settings/PreferencesSettingsRepositoryTest.kt`
- `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt`
- `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/web/WebViewConfiguratorTest.kt`

## Task 1: Scaffold The Android Project

**Files:**
- Create: `concentra-android-browser/`
- Modify: `concentra-android-browser/app/build.gradle.kts`
- Modify: `concentra-android-browser/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Install the Android CLI if it is not already available**

Run:

```bash
curl -fsSL https://dl.google.com/android/cli/latest/darwin_arm64/install.sh | bash
```

Expected: installer completes and the CLI binary becomes available in the shell profile or install directory.

- [ ] **Step 2: Create the new app project in `concentra-android-browser`**

Run the Android CLI command that generates a Kotlin + Compose app project in:

```text
/Users/frank/proj/apps/concentra-android-browser
```

Expected: standard Android project structure with an `app` module and Gradle wrapper.

- [ ] **Step 3: Verify the scaffold builds before changing behavior**

Run:

```bash
cd /Users/frank/proj/apps/concentra-android-browser
./gradlew build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Configure the app module for the browser feature set**

Update `app/build.gradle.kts` to ensure:

```kotlin
android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:<existing-version>")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:<existing-version>")
    implementation("androidx.compose.material3:material3:<existing-version>")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:<existing-version>")
    debugImplementation("androidx.compose.ui:ui-test-manifest:<existing-version>")
}
```

- [ ] **Step 5: Update the manifest for browser intent handling**

Add intent filters for:

- launcher start
- `ACTION_VIEW` for `http` and `https`
- `ACTION_SEND` for `text/plain`

Expected manifest shape:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="http" />
    <data android:scheme="https" />
</intent-filter>
```

- [ ] **Step 6: Run the app build again**

Run:

```bash
./gradlew build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add concentra-android-browser
git commit -m "feat: scaffold Concentra Android browser app"
```

## Task 2: Add URL Parsing And Launch Request Models

**Files:**
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/model/LaunchRequest.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/intent/IntentParser.kt`
- Test: `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/intent/IntentParserTest.kt`

- [ ] **Step 1: Write the failing `IntentParser` tests**

```kotlin
@Test
fun `empty launch returns empty request`() {
    val parser = IntentParser()
    val intent = Intent(Intent.ACTION_MAIN)

    assertEquals(LaunchRequest.Empty, parser.parse(intent))
}

@Test
fun `view intent returns url request`() {
    val parser = IntentParser()
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))

    assertEquals(
        LaunchRequest.OpenUrl("https://example.com"),
        parser.parse(intent)
    )
}

@Test
fun `send intent extracts first url from text`() {
    val parser = IntentParser()
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Read this https://example.com/page")
    }

    assertEquals(
        LaunchRequest.OpenUrl("https://example.com/page"),
        parser.parse(intent)
    )
}
```

- [ ] **Step 2: Run the parser tests to verify they fail**

Run:

```bash
./gradlew testDebugUnitTest --tests '*IntentParserTest'
```

Expected: FAIL because the parser and model do not exist yet.

- [ ] **Step 3: Write the minimal launch request model and parser**

```kotlin
sealed interface LaunchRequest {
    data object Empty : LaunchRequest
    data class OpenUrl(val url: String) : LaunchRequest
}
```

```kotlin
class IntentParser {
    fun parse(intent: Intent?): LaunchRequest {
        // ACTION_VIEW -> data string
        // ACTION_SEND text/plain -> first URL in shared text
        // everything else -> Empty
    }
}
```

- [ ] **Step 4: Run the parser tests to verify they pass**

Run:

```bash
./gradlew testDebugUnitTest --tests '*IntentParserTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/model/LaunchRequest.kt app/src/main/java/com/franklinharper/concentra/browser/intent/IntentParser.kt app/src/test/java/com/franklinharper/concentra/browser/intent/IntentParserTest.kt
git commit -m "feat: parse browser launch intents"
```

## Task 3: Add URL Normalization And Archive Today URL Building

**Files:**
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/url/UrlNormalizer.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/url/ArchiveTodayUrlBuilder.kt`
- Test: `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/url/UrlNormalizerTest.kt`
- Test: `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/url/ArchiveTodayUrlBuilderTest.kt`

- [ ] **Step 1: Write the failing URL tests**

```kotlin
@Test
fun `normalizer adds https scheme to bare host`() {
    assertEquals(
        "https://example.com",
        UrlNormalizer().normalize("example.com")
    )
}

@Test
fun `normalizer keeps full https url`() {
    assertEquals(
        "https://example.com/path",
        UrlNormalizer().normalize("https://example.com/path")
    )
}

@Test
fun `archive builder prefixes current url`() {
    assertEquals(
        "https://archive.ph/https://example.com",
        ArchiveTodayUrlBuilder().build("https://example.com")
    )
}
```

- [ ] **Step 2: Run the URL tests to verify they fail**

Run:

```bash
./gradlew testDebugUnitTest --tests '*UrlNormalizerTest' --tests '*ArchiveTodayUrlBuilderTest'
```

Expected: FAIL because the URL helpers do not exist.

- [ ] **Step 3: Write the minimal URL helper implementations**

```kotlin
class UrlNormalizer {
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
    }
}
```

```kotlin
class ArchiveTodayUrlBuilder {
    fun build(currentUrl: String): String = "https://archive.ph/$currentUrl"
}
```

- [ ] **Step 4: Run the URL tests to verify they pass**

Run:

```bash
./gradlew testDebugUnitTest --tests '*UrlNormalizerTest' --tests '*ArchiveTodayUrlBuilderTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/url app/src/test/java/com/franklinharper/concentra/browser/url
git commit -m "feat: add browser URL helpers"
```

## Task 4: Add Persisted Settings With Third-Party Cookies Defaulting Off

**Files:**
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/settings/SettingsRepository.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/settings/PreferencesSettingsRepository.kt`
- Test: `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/settings/PreferencesSettingsRepositoryTest.kt`

- [ ] **Step 1: Write the failing settings tests**

```kotlin
@Test
fun `third party cookies default to disabled`() {
    val repo = PreferencesSettingsRepository(fakeSharedPreferences())

    assertFalse(repo.load().thirdPartyCookiesEnabled)
}

@Test
fun `saving third party cookies enabled persists value`() {
    val repo = PreferencesSettingsRepository(fakeSharedPreferences())

    repo.saveThirdPartyCookiesEnabled(true)

    assertTrue(repo.load().thirdPartyCookiesEnabled)
}
```

- [ ] **Step 2: Run the settings tests to verify they fail**

Run:

```bash
./gradlew testDebugUnitTest --tests '*PreferencesSettingsRepositoryTest'
```

Expected: FAIL because the repository does not exist.

- [ ] **Step 3: Write the minimal repository implementation**

```kotlin
data class BrowserSettings(
    val thirdPartyCookiesEnabled: Boolean = false
)

interface SettingsRepository {
    fun load(): BrowserSettings
    fun saveThirdPartyCookiesEnabled(enabled: Boolean)
}
```

- [ ] **Step 4: Run the settings tests to verify they pass**

Run:

```bash
./gradlew testDebugUnitTest --tests '*PreferencesSettingsRepositoryTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/settings app/src/test/java/com/franklinharper/concentra/browser/settings
git commit -m "feat: persist browser settings"
```

## Task 5: Create Browser UI State And ViewModel Skeleton

**Files:**
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/model/BrowserUiState.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/model/BrowserAction.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt`
- Test: `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt`

- [ ] **Step 1: Write the failing ViewModel state tests**

```kotlin
@Test
fun `empty launch shows chrome immediately`() {
    val viewModel = buildViewModel(LaunchRequest.Empty)

    assertTrue(viewModel.uiState.value.isChromeVisible)
    assertNull(viewModel.uiState.value.currentUrl)
}

@Test
fun `url launch hides chrome and emits load command`() {
    val viewModel = buildViewModel(LaunchRequest.OpenUrl("https://example.com"))

    assertFalse(viewModel.uiState.value.isChromeVisible)
    assertEquals("https://example.com", viewModel.pendingWebCommand())
}

@Test
fun `archive action is disabled with no current url`() {
    val viewModel = buildViewModel(LaunchRequest.Empty)

    assertFalse(viewModel.uiState.value.isArchiveTodayEnabled)
}
```

- [ ] **Step 2: Run the ViewModel tests to verify they fail**

Run:

```bash
./gradlew testDebugUnitTest --tests '*BrowserViewModelTest'
```

Expected: FAIL because the state and ViewModel do not exist.

- [ ] **Step 3: Write minimal state and ViewModel code**

Include state fields for:

- `currentUrl`
- `pendingUrlInput`
- `isChromeVisible`
- `isLoading`
- `canGoBack`
- `isGoogleEnabled`
- `isArchiveTodayEnabled`
- `isShareEnabled`
- `isFindInPageEnabled`
- `settings`

Include one-off command emission for initial URL loads.

- [ ] **Step 4: Run the ViewModel tests to verify they pass**

Run:

```bash
./gradlew testDebugUnitTest --tests '*BrowserViewModelTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/model app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt
git commit -m "feat: add browser state model"
```

## Task 6: Add Browser Action Handling In The ViewModel

**Files:**
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/model/BrowserAction.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt`
- Test: `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt`

- [ ] **Step 1: Write the failing action tests**

Add tests for:

```kotlin
@Test
fun `google action emits google load command`() { /* ... */ }

@Test
fun `archive action emits archive url when current url exists`() { /* ... */ }

@Test
fun `share action emits share effect when current url exists`() { /* ... */ }

@Test
fun `settings action emits open settings effect`() { /* ... */ }

@Test
fun `back closes chrome before navigating web history`() { /* ... */ }
```

- [ ] **Step 2: Run the ViewModel tests to verify they fail**

Run:

```bash
./gradlew testDebugUnitTest --tests '*BrowserViewModelTest'
```

Expected: FAIL on newly added behavior.

- [ ] **Step 3: Implement minimal action handling**

Add handling for:

- URL submit
- Google
- Archive Today
- Share Link
- Find In Page
- Open Settings
- Exit
- Back press
- Chrome show/hide

- [ ] **Step 4: Run the ViewModel tests to verify they pass**

Run:

```bash
./gradlew testDebugUnitTest --tests '*BrowserViewModelTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/model/BrowserAction.kt app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt
git commit -m "feat: handle browser actions"
```

## Task 7: Add WebView Configuration And Callback Models

**Files:**
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/WebViewCommand.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/WebViewEvent.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/WebViewConfigurator.kt`
- Create: `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/web/WebViewConfiguratorTest.kt`

- [ ] **Step 1: Write the failing configurator tests**

Focus the test on a fake settings sink that records applied values:

```kotlin
@Test
fun `configurator enables javascript dom storage and zoom`() { /* ... */ }

@Test
fun `configurator disables third party cookies by default`() { /* ... */ }
```

- [ ] **Step 2: Run the configurator tests to verify they fail**

Run:

```bash
./gradlew testDebugUnitTest --tests '*WebViewConfiguratorTest'
```

Expected: FAIL because the configurator does not exist.

- [ ] **Step 3: Implement the minimal configurator**

Ensure the configurator applies:

- JavaScript enabled
- DOM storage enabled
- built-in zoom controls enabled
- display zoom controls disabled
- support zoom enabled
- third-party cookies from settings
- multi-window disabled

- [ ] **Step 4: Run the configurator tests to verify they pass**

Run:

```bash
./gradlew testDebugUnitTest --tests '*WebViewConfiguratorTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/web app/src/test/java/com/franklinharper/concentra/browser/web
git commit -m "feat: configure browser webview"
```

## Task 8: Build The Browser Compose Screen Skeleton

**Files:**
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/BrowserActivity.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/BrowserAppContainer.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserRoute.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserScreen.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserChromeSheet.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/HotspotOverlay.kt`
- Test: `concentra-android-browser/app/src/androidTest/java/com/franklinharper/concentra/browser/ui/BrowserScreenTest.kt`

- [ ] **Step 1: Write the failing UI tests for empty launch and disabled actions**

Add tests such as:

```kotlin
@Test
fun empty_launch_shows_chrome() { /* ... */ }

@Test
fun archive_share_find_are_disabled_without_url() { /* ... */ }
```

- [ ] **Step 2: Run the UI tests to verify they fail**

Run:

```bash
./gradlew connectedDebugAndroidTest
```

Expected: FAIL because the UI does not exist yet.

- [ ] **Step 3: Implement the initial Compose browser shell**

Include:

- edge-to-edge layout with visible status bar
- browser chrome bottom sheet
- URL text field
- action buttons
- `64dp x 64dp` semi-transparent bottom-right hotspot
- disabled states for URL-dependent actions

- [ ] **Step 4: Run the UI tests to verify they pass**

Run:

```bash
./gradlew connectedDebugAndroidTest
```

Expected: PASS for the new UI tests.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser app/src/androidTest/java/com/franklinharper/concentra/browser/ui/BrowserScreenTest.kt
git commit -m "feat: add browser compose shell"
```

## Task 9: Add The Managed WebView Host

**Files:**
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/BrowserWebViewClient.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/BrowserWebChromeClient.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/WebViewHost.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserScreen.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt`
- Test: `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt`

- [ ] **Step 1: Write the failing tests for WebView event handling**

Add tests for:

- page commit updates `currentUrl`
- title updates `pageTitle`
- loading state toggles
- `canGoBack` updates from WebView events

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
./gradlew testDebugUnitTest --tests '*BrowserViewModelTest'
```

Expected: FAIL on missing WebView event handling.

- [ ] **Step 3: Implement the `WebView` bridge**

Requirements:

- `AndroidView` hosts one `WebView`
- `BrowserWebViewClient` sends navigation and load events
- `BrowserWebChromeClient` sends title, progress, and JS dialog events
- `WebViewHost` applies `WebViewCommand`s from the ViewModel

- [ ] **Step 4: Run the ViewModel tests to verify they pass**

Run:

```bash
./gradlew testDebugUnitTest --tests '*BrowserViewModelTest'
```

Expected: PASS.

- [ ] **Step 5: Manually verify a URL launch renders a page**

Run:

```bash
./gradlew :app:installDebug
```

Then launch the app with a test URL and confirm a page renders full-screen with the chrome hidden.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/web app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserScreen.kt app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt
git commit -m "feat: embed managed webview"
```

## Task 10: Add JS Dialog Support

**Files:**
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/JsDialogHost.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/BrowserWebChromeClient.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/model/BrowserUiState.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt`
- Test: `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt`

- [ ] **Step 1: Write the failing tests for JS dialog state**

Add tests for:

- `alert` dialog appears in state
- confirming a `confirm` dialog triggers positive callback
- canceling a `prompt` dialog clears dialog state

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
./gradlew testDebugUnitTest --tests '*BrowserViewModelTest'
```

Expected: FAIL on JS dialog handling.

- [ ] **Step 3: Implement the minimal JS dialog bridge**

Requirements:

- `BrowserWebChromeClient` captures `onJsAlert`, `onJsConfirm`, and `onJsPrompt`
- `BrowserViewModel` exposes dialog state
- `JsDialogHost` renders Material dialogs and forwards user decisions

- [ ] **Step 4: Run the tests to verify they pass**

Run:

```bash
./gradlew testDebugUnitTest --tests '*BrowserViewModelTest'
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/web/BrowserWebChromeClient.kt app/src/main/java/com/franklinharper/concentra/browser/ui/JsDialogHost.kt app/src/main/java/com/franklinharper/concentra/browser/model/BrowserUiState.kt app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt
git commit -m "feat: support javascript dialogs"
```

## Task 11: Add Find-In-Page UI And Browser Back Behavior

**Files:**
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/FindInPageBar.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserScreen.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt`
- Test: `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt`
- Test: `concentra-android-browser/app/src/androidTest/java/com/franklinharper/concentra/browser/ui/BrowserScreenTest.kt`

- [ ] **Step 1: Write the failing tests for find-in-page and back behavior**

Add tests for:

- tapping `Find In Page` opens find UI when a page exists
- Back closes chrome before web history
- Back closes find UI if find UI is open

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
./gradlew testDebugUnitTest --tests '*BrowserViewModelTest'
./gradlew connectedDebugAndroidTest
```

Expected: FAIL on missing find UI and back behavior.

- [ ] **Step 3: Implement the minimal find UI flow**

Requirements:

- separate find bar UI
- `WebView.findAllAsync(query)` integration
- close or clear behavior wired into Back handling

- [ ] **Step 4: Run the tests to verify they pass**

Run:

```bash
./gradlew testDebugUnitTest --tests '*BrowserViewModelTest'
./gradlew connectedDebugAndroidTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/ui/FindInPageBar.kt app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserScreen.kt app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt app/src/androidTest/java/com/franklinharper/concentra/browser/ui/BrowserScreenTest.kt
git commit -m "feat: add find in page flow"
```

## Task 12: Add Separate Settings Screen And Cookie Toggle Wiring

**Files:**
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/navigation/BrowserNavigator.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/SettingsScreen.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserRoute.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/WebViewConfigurator.kt`
- Test: `concentra-android-browser/app/src/androidTest/java/com/franklinharper/concentra/browser/ui/BrowserScreenTest.kt`
- Test: `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt`

- [ ] **Step 1: Write the failing tests for settings navigation and toggle persistence**

Add tests for:

- settings action opens settings screen
- third-party cookie toggle reflects persisted default `false`
- toggling setting updates repository and future WebView configuration

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
./gradlew testDebugUnitTest --tests '*BrowserViewModelTest'
./gradlew connectedDebugAndroidTest
```

Expected: FAIL on missing settings navigation and toggle.

- [ ] **Step 3: Implement the separate settings screen**

Requirements:

- navigation between browser and settings in one activity
- single switch for third-party cookies
- Back returns to browser screen
- changed setting is applied on future page loads or explicit reload path

- [ ] **Step 4: Run the tests to verify they pass**

Run:

```bash
./gradlew testDebugUnitTest --tests '*BrowserViewModelTest'
./gradlew connectedDebugAndroidTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/navigation/BrowserNavigator.kt app/src/main/java/com/franklinharper/concentra/browser/ui/SettingsScreen.kt app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserRoute.kt app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt app/src/main/java/com/franklinharper/concentra/browser/web/WebViewConfigurator.kt app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt app/src/androidTest/java/com/franklinharper/concentra/browser/ui/BrowserScreenTest.kt
git commit -m "feat: add browser settings screen"
```

## Task 13: Add Share, Downloads, And New-Intent Replacement Flow

**Files:**
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/share/ShareLauncher.kt`
- Create: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/download/DownloadStarter.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/BrowserActivity.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/web/BrowserWebChromeClient.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt`
- Test: `concentra-android-browser/app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt`

- [ ] **Step 1: Write the failing tests for share, downloads, and new intents**

Add tests for:

- share effect emitted only when current URL exists
- new incoming URL replaces current session
- download request emits download effect with required metadata

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
./gradlew testDebugUnitTest --tests '*BrowserViewModelTest'
```

Expected: FAIL on the new side-effect behavior.

- [ ] **Step 3: Implement the minimal platform integration**

Requirements:

- `ShareLauncher` starts Android share sheet
- `DownloadStarter` enqueues downloads in `DownloadManager`
- `BrowserActivity.onNewIntent` reparses new intents and forwards them to the ViewModel
- download requests are bridged from `WebView`

- [ ] **Step 4: Run the tests to verify they pass**

Run:

```bash
./gradlew testDebugUnitTest --tests '*BrowserViewModelTest'
```

Expected: PASS.

- [ ] **Step 5: Manually verify share target and direct URL launch**

Verify:

- `adb shell am start -a android.intent.action.VIEW -d https://example.com`
- Android share of text containing a URL into the app
- tapping `Share Link` opens the system share UI

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/share/ShareLauncher.kt app/src/main/java/com/franklinharper/concentra/browser/download/DownloadStarter.kt app/src/main/java/com/franklinharper/concentra/browser/BrowserActivity.kt app/src/main/java/com/franklinharper/concentra/browser/web/BrowserWebChromeClient.kt app/src/main/java/com/franklinharper/concentra/browser/BrowserViewModel.kt app/src/test/java/com/franklinharper/concentra/browser/BrowserViewModelTest.kt
git commit -m "feat: add browser platform integrations"
```

## Task 14: Polish Edge-To-Edge Behavior, Hotspot Gesture, And Exit Flow

**Files:**
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/HotspotOverlay.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserScreen.kt`
- Modify: `concentra-android-browser/app/src/main/java/com/franklinharper/concentra/browser/BrowserActivity.kt`
- Test: `concentra-android-browser/app/src/androidTest/java/com/franklinharper/concentra/browser/ui/BrowserScreenTest.kt`

- [ ] **Step 1: Write the failing UI tests for hotspot and chrome dismissal**

Add tests for:

- chrome opens when swipe-up gesture starts in hotspot region
- tapping outside the sheet closes chrome
- hotspot taps alone do not trigger chrome

- [ ] **Step 2: Run the UI tests to verify they fail**

Run:

```bash
./gradlew connectedDebugAndroidTest
```

Expected: FAIL on the new gesture-specific behavior.

- [ ] **Step 3: Implement the hotspot gesture and finish flow**

Requirements:

- `64dp x 64dp` bottom-right hotspot
- semi-transparent overlay
- swipe-up detection only
- no action on tap
- `Exit` finishes the activity immediately

- [ ] **Step 4: Run the UI tests to verify they pass**

Run:

```bash
./gradlew connectedDebugAndroidTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/franklinharper/concentra/browser/ui/HotspotOverlay.kt app/src/main/java/com/franklinharper/concentra/browser/ui/BrowserScreen.kt app/src/main/java/com/franklinharper/concentra/browser/BrowserActivity.kt app/src/androidTest/java/com/franklinharper/concentra/browser/ui/BrowserScreenTest.kt
git commit -m "feat: finalize browser gesture interactions"
```

## Task 15: Final Verification And Documentation

**Files:**
- Modify: `concentra-android-browser/README.md` or create it if absent

- [ ] **Step 1: Add a concise project README**

Document:

- how to build
- how to run tests
- supported launch intents
- known v1 limitations

- [ ] **Step 2: Run unit tests**

Run:

```bash
./gradlew testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run instrumentation and UI tests**

Run:

```bash
./gradlew connectedDebugAndroidTest
```

Expected: all connected tests pass on an available device or emulator.

- [ ] **Step 4: Run a full build**

Run:

```bash
./gradlew build
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Perform manual smoke verification**

Verify these behaviors on device or emulator:

- empty launch shows chrome and focused URL field
- direct URL launch opens the page with chrome hidden
- hotspot swipe-up reveals chrome
- disabled actions stay disabled when no current URL exists
- `Google`, `Archive Today`, `Share Link`, `Find In Page`, `Settings`, and `Exit` work as designed
- status bar remains visible while content renders edge-to-edge
- pinch-to-zoom and natural page scrolling work
- JavaScript dialogs render as native dialogs
- share target replaces the current session URL
- downloads enqueue through `DownloadManager`

- [ ] **Step 6: Commit**

```bash
git add README.md
git commit -m "docs: add Concentra browser usage notes"
```

