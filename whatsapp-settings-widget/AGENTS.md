# AGENTS.md — WhatsApp Settings Widget

Guidance for coding agents working in this subproject.

## Project overview

This is a small Android app + Glance home-screen widget for monitoring WhatsApp's battery optimization state.

The app monitors the regular WhatsApp package:

```text
com.whatsapp
```

It does **not** currently monitor WhatsApp Business (`com.whatsapp.w4b`).

## Important Android API limitation

Do not claim the app can reliably detect Android's exact **Allow background usage** switch for WhatsApp.

The app uses the public API:

```kotlin
PowerManager.isIgnoringBatteryOptimizations("com.whatsapp")
```

This can reliably detect whether WhatsApp is **unrestricted** / ignoring battery optimizations.

It cannot distinguish between:

- background usage enabled + optimized
- background usage disabled

Therefore the app's green/non-red state should be described as:

```text
Background usage: Restricted or optimized
```

not simply "disabled".

## Expected status meanings

Current status states:

- `BackgroundUsageUnrestricted`
  - WhatsApp ignores battery optimizations.
  - Battery-drain risk.
  - Red in app/widget.
- `BackgroundUsageRestrictedOrOptimized`
  - WhatsApp is not unrestricted.
  - Could be optimized or background usage disabled.
  - Green in app/widget.
- `NotInstalled`
  - WhatsApp package is not visible/installed.
  - Gray in widget.

## Architecture notes

Key packages:

```text
app/src/main/java/com/franklinharper/whatsapp/settings/domain
app/src/main/java/com/franklinharper/whatsapp/settings/data
app/src/main/java/com/franklinharper/whatsapp/settings/monitor
app/src/main/java/com/franklinharper/whatsapp/settings/presentation
app/src/main/java/com/franklinharper/whatsapp/settings/ui
app/src/main/java/com/franklinharper/whatsapp/settings/widget
```

Responsibilities:

- `domain/`
  - status models
  - repository interfaces
  - package constants
  - persistence value mapping
- `data/`
  - local SQLite storage for raw status transitions and unrestricted sessions
- `monitor/`
  - detects current status and records status changes
- `presentation/`
  - maps domain state into UI labels
  - formats unrestricted sessions into human-readable UI models
- `ui/`
  - main Compose app screen
  - app colors/theme
- `widget/`
  - Glance widget rendering
  - widget state synchronization
  - WorkManager periodic updates

## Status/session tracking

The app stores two kinds of persisted data:

1. Raw status transition history.
2. Unrestricted sessions.

Only record a new status transition when detected status changes from the previous recorded status.

Session rules:

- entering `BackgroundUsageUnrestricted` opens a new unrestricted session
- leaving `BackgroundUsageUnrestricted` closes any open unrestricted session
- if status is unchanged, do not insert new rows

Detection currently happens when:

- the app resumes
- the periodic widget worker runs
- widget initial render needs state

## Widget behavior

The widget should render from Glance widget state, not independently drift from the app's displayed status.

Use `StatusWidgetUpdater` to synchronize widget state and redraw widgets.

Widget display:

- 1x1 widget
- small `WA` title
- vertical battery icon
- red background for unrestricted
- green background for restricted/optimized
- gray background for not installed
- tapping widget opens the app

## UI behavior

Main screen:

- shows current detected status at top
- has button to open WhatsApp's Android app settings page
- shows a lazy list of detected unrestricted sessions
- newest sessions appear first
- session dates/times must be human-friendly
- sessions can cross midnight or span multiple days
- ongoing sessions end with `Now`

Use wording such as:

```text
Detected unrestricted sessions
Approx. 1 hr 30 min
Approx. 2 days 4 hr so far
```

Avoid implying exact real-time monitoring. Detection is periodic/app-use-based.

## Testing commands

Before committing code changes in this subproject, run:

```bash
./gradlew test assembleDebug assembleDebugAndroidTest
```

For connected device tests on a Pixel/device:

```bash
./gradlew connectedDebugAndroidTest
```

If connected tests fail, inspect:

```text
app/build/reports/androidTests/connected/debug/index.html
app/build/outputs/androidTest-results/connected/debug/
```

and use:

```bash
adb logcat -d
adb shell pm list instrumentation
```

## Known connected-test caveat

The Android test runner must be configured as:

```kotlin
testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
```

The test APK should list instrumentation similar to:

```text
com.franklinharper.whatsapp.settings.test/androidx.test.runner.AndroidJUnitRunner
```

## Coding guidelines

- Prefer one shared path for detecting and recording status changes.
- Do not duplicate status interpretation logic in widget and app UI.
- Use string resources for user-visible app UI text.
- Keep domain models free from direct UI strings.
- Keep session formatting unit-testable.
- Be careful with Glance APIs; compile after widget changes.
- Glance padding should use `dp`, e.g. `6.dp`.
- Do not use hidden Android settings intents unless explicitly asked; prefer public APIs.
- Do not reintroduce `WidgetTrampolineActivity` unless widget behavior changes back to directly opening settings.

## Git notes

This subproject lives inside the larger repo at:

```text
/Users/frank/proj/apps/whatsapp-settings-widget
```

Run Gradle commands from the subproject directory.
