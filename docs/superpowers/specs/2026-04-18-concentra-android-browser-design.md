# Concentra Android Browser Design

## Overview

Concentra is an Android browser app focused on distraction-free, edge-to-edge web viewing with browser controls hidden by default while the Android status bar remains visible. The browser chrome is presented as a bottom sheet that the user reveals with a swipe-up gesture from a small bottom-right hotspot. When launched without a URL, the browser opens into an empty start state with the chrome shown immediately and the URL field focused. When launched with a URL, the page loads full-screen and the chrome remains hidden until summoned.

The first version is a single-session, single-WebView browser implemented in Kotlin using Jetpack Compose, MVVM, manual dependency injection, Material Design, and red-green TDD. It supports direct URL launches, share-target URL ingestion, downloads via Android `DownloadManager`, JavaScript dialogs, pinch-to-zoom, and a minimal settings surface with a third-party cookie toggle.

## Goals

- Provide a full-screen Android browsing experience with no persistent browser chrome.
- Reveal browser controls only through a deliberate gesture from a constrained hotspot that minimally interferes with web content.
- Support direct launch URLs and shared URLs while keeping behavior deterministic and single-session.
- Support common browser essentials: navigation, URL entry, downloads, sharing, find-in-page, JavaScript dialogs, cookies, and storage.
- Keep the architecture small, testable, and aligned with the requested stack.

## Non-Goals

- Tabs or multi-session browsing
- Popup windows or multiple WebView windows
- File share-target ingestion
- Rich settings beyond the minimum needed for v1
- Browser sync, bookmarks, history UI, or saved sessions

## Product Behavior

### Launch Behavior

- If the app is launched without a URL:
  - Show an empty start state.
  - Open the browser chrome immediately.
  - Focus the URL field.
- If the app is launched with a URL:
  - Load the URL immediately.
  - Show the web page full-screen.
  - Keep browser chrome hidden initially.
- If the app is already open and receives a new launch or share URL:
  - Replace the current page in the existing session.

### Chrome Reveal And Dismissal

- The browser runs edge-to-edge by default while keeping the Android status bar visible.
- A small semi-transparent hotspot overlays the bottom-right corner of the screen.
- Swiping up from that hotspot reveals the browser chrome bottom sheet.
- Taps on the hotspot do nothing.
- Tapping outside the chrome dismisses it.
- Android system Back behavior:
  1. If chrome is visible, close the chrome.
  2. Else if the WebView can navigate back, navigate back.
  3. Else close the app.

### Browser Chrome Layout

The bottom sheet contains:

- URL bar
- Settings icon in the upper right
- Actions:
  - Google
  - Archive Today
  - Share Link
  - Exit
  - Find In Page

### Action Semantics

- `Google`: immediately loads `https://google.com`
- `Archive Today`: immediately loads `https://archive.ph/<current-url>` when a current URL exists; otherwise it is visible but disabled
- `Share Link`: immediately opens the Android system share UI with the current URL when a current URL exists; otherwise it is visible but disabled
- `Find In Page`: immediately opens in-page find UI and drives WebView find APIs when a current page exists; otherwise it is visible but disabled
- `Exit`: immediately closes the app
- `Settings`: opens a separate settings screen in v1

### Share Target Behavior

The app is a share target for:

- Shared text that is itself a URL
- Shared text containing a URL

The app does not accept shared files in v1.

## Technical Architecture

### High-Level Structure

The app will use a single activity with a Compose shell around one managed `WebView`.

Main components:

- `BrowserActivity`
  - Android entry point
  - Receives intents
  - Creates and wires dependencies manually
- `BrowserViewModel`
  - Owns screen state and action handling
  - Translates user intents into browser commands
- `BrowserScreen`
  - Full-screen Compose UI
  - Hosts the `WebView`
  - Renders bottom hotspot, chrome bottom sheet, find UI, dialogs, and minimal settings UI
- `WebViewHost`
  - Thin adapter that configures and owns the platform `WebView`
  - Bridges `WebViewClient`, `WebChromeClient`, download callbacks, and JS dialog callbacks
- `IntentParser`
  - Normalizes launch intents and share intents into a small internal command model
- `SettingsRepository`
  - Persists browser settings, starting with third-party cookie preference
- `BrowserCoordinator` or equivalent action service
  - Keeps Android framework details and one-off command execution out of Compose UI code

### MVVM State Model

Primary screen state should include:

- `currentUrl`
- `pendingUrlInput`
- `pageTitle`
- `isChromeVisible`
- `isInitialEmptyLaunch`
- `canGoBack`
- `isLoading`
- `findInPageQuery`
- `settings.thirdPartyCookiesEnabled`
- `jsDialogState`

The `ViewModel` owns state transitions. The `WebView` is treated as an imperative engine behind a narrow interface rather than as the source of truth for presentation logic.

### Manual Dependency Injection

Use explicit constructor injection and an app container assembled from the activity. The app is small enough that no DI framework is warranted. Initial dependency graph:

- `BrowserViewModel`
- `IntentParser`
- `SettingsRepository`
- `BrowserCoordinator`
- `UrlNormalizer` / `UrlBuilder`

This keeps boundaries testable and easy to change.

## WebView Policy

### Core Runtime Configuration

The browser should behave like a normal mobile browser within the constraints of a single session:

- JavaScript enabled
- DOM storage enabled
- First-party cookies enabled
- Third-party cookies disabled by default
- Third-party cookies toggle exposed in settings
- Pinch-to-zoom enabled
- Natural horizontal and vertical scrolling allowed
- On-screen zoom controls hidden
- Respect page viewport behavior
- Enable compatibility-oriented viewport settings conservatively where needed

### JavaScript Dialogs

Support JavaScript dialog APIs:

- `alert(...)`
- `confirm(...)`
- `prompt(...)`

These are surfaced as native Material dialogs over the browser UI.

### Popups And Multiple Windows

Do not support popup windows or multiple browser windows in v1. JavaScript dialog support is included, but `window.open` style multi-window behavior remains disabled.

### Downloads

Downloads are delegated to Android `DownloadManager`.

Implementation responsibilities:

- Receive file download requests from `WebView`
- Hand them to `DownloadManager`
- Let the system own persistence, notifications, and file access
- Show lightweight app feedback such as a toast or snackbar if useful

## UI Design

### Full-Screen Surface

The web page occupies the full available screen in an edge-to-edge layout. The browser does not reserve space for persistent controls during normal browsing, and the Android status bar remains visible at the top.

### Bottom Hotspot

The chrome reveal hotspot must be:

- Anchored to the bottom-right corner
- `64dp x 64dp`
- Semi-transparent so it is visible without dominating the page
- Narrow enough to minimize interference with page interaction
- Large enough to remain usable

Only the hotspot intercepts the upward reveal gesture. Taps on the hotspot are ignored. The rest of the page should behave like a normal browser surface.

### Start State

When launched without a URL, show an empty start state with:

- Browser chrome visible
- URL field focused
- No page loaded yet

This is not a home page; it is a blank entry state.

### Settings Surface

v1 requires a real settings screen rather than a placeholder action.

Initial content:

- `Enable third-party cookies` switch

The settings surface is a separate screen reached from the browser chrome. The implementation plan should keep this screen minimal and consistent with the single-activity architecture.

## Intent Handling

Intent handling must normalize multiple entry paths:

- Direct launches without a URL
- Direct launches with a URL
- Shared text containing a URL
- New intents delivered while the activity is already running

Rules:

- Incoming URL replaces the current page in the active session
- Empty launches show chrome and focus URL input
- Invalid or missing URLs from shared text should fail gracefully and keep the user in a recoverable state

## Error Handling

The app should explicitly handle:

- Invalid URLs entered manually
- Share payloads that contain no usable URL
- Page load failures
- Download handoff failures
- JavaScript dialog lifecycle interruptions
- WebView state changes during configuration changes or activity recreation

The UI should avoid crashes and expose a recoverable state whenever possible.

## Testing Strategy

The implementation should follow red-green TDD with tests written first for each unit of behavior.

### Unit Tests

- `IntentParser`:
  - Empty launch
  - Direct URL launch
  - Shared text with URL
  - Shared text without URL
- URL normalization and the `archive.ph` URL builder
- `BrowserViewModel`:
  - Chrome visibility transitions
  - Back behavior decision logic
  - Action dispatch
  - Settings toggling
  - Initial state for empty and URL launches

### UI Tests

- Empty launch shows chrome and focuses URL entry
- URL launch loads content with chrome hidden
- Outside tap closes chrome
- Settings toggle is present and updates UI state
- Find-in-page UI opens when requested

### Focused Integration Tests

- WebView callback bridging into the `ViewModel`
- Download delegation to `DownloadManager`
- JavaScript dialog events bridged to native UI state

The design should keep as much logic as possible out of raw `WebView` code so the majority of behavior remains testable without heavy instrumentation.

## Delivery Constraints

- Language: Kotlin
- UI: Jetpack Compose with Material 3
- Architecture: MVVM
- DI: manual dependency injection
- Process: red-green TDD
- App creation: Android CLI
- Project directory: `concentra-android-browser`

## Risks And Mitigations

### Gesture Interference

Risk:
- A reveal gesture can conflict with web content interaction.

Mitigation:
- Restrict interception to a `64dp x 64dp` bottom-right hotspot rather than global gesture capture or a full-width bottom strip.

### WebView Complexity

Risk:
- `WebView` lifecycle and callback behavior can leak framework concerns into the app.

Mitigation:
- Isolate `WebView` handling in a narrow adapter and push logic into plain Kotlin state management.

### Cookie Setting Changes

Risk:
- Toggling third-party cookie behavior may not fully affect already loaded content until navigation changes.

Mitigation:
- Define a clear reload policy during implementation and communicate state transitions explicitly.

### JavaScript Compatibility

Risk:
- Some sites require behavior beyond the v1 browser scope, especially popup windows.

Mitigation:
- Support JS dialogs fully, but keep multi-window disabled and treat unsupported behaviors as out of scope for v1.
