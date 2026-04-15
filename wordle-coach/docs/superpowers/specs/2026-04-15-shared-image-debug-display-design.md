# Shared Image Debug Display — Design Spec

**Date:** 2026-04-15
**Status:** Approved

## Overview

When a Wordle screenshot is shared into the app, show the original image alongside the decoded tile grid on the first coaching step. This is a debugging aid to verify that the image parser correctly identified the letters and colors.

## Scope

- Only visible on Step 1 (BeforeFirstGuess), only when an image was shared
- Shows all decoded guesses, not just the first
- No change to steps 2–N or to flows where no image was shared

## Design

### Layout

A labeled debug card sits above the existing "Step 1 — Before your first guess" header inside `BeforeFirstGuessStep`. The card contains two equal columns side-by-side:

- **Left:** The original shared screenshot, scaled to fill its half of the card
- **Right:** The decoded tile grid — one `GuessTileRow` per guess, using existing `GuessTile` composables with their `LetterResult`-based colors (green / yellow / gray)

Card styling matches the app's dark theme: dark background, subtle border, small uppercase label at the top ("Shared image & decoded guesses").

When no image was shared, the card is not rendered. No change to existing behavior.

### Data model

`CoachingStep.BeforeFirstGuess` changes from an `object` to a `data class`:

```kotlin
// Before
object BeforeFirstGuess : CoachingStep()

// After
data class BeforeFirstGuess(val guesses: List<CompletedGuess>) : CoachingStep()
```

`CoachingState.fromPuzzle()` passes `puzzle.guesses` when constructing the step.

### Image passing

`AppActivity` loads the bitmap a second time after parsing (simple `BitmapFactory.decodeStream` via `ContentResolver`) and converts it to `ImageBitmap` via `.asImageBitmap()`. This `ImageBitmap?` is threaded through:

```
AppActivity → App(imageBitmap) → CoachingScreen(imageBitmap) → BeforeFirstGuessStep(imageBitmap)
```

`ImageBitmap` is a Compose Multiplatform type — safe to use in `sharedUI/commonMain` with no expect/actual and no new dependencies.

### New composable

`SharedImageDebugCard(imageBitmap: ImageBitmap, guesses: List<CompletedGuess>)` added to `CoachingScreen.kt`.

`BeforeFirstGuessStep` receives both `step: CoachingStep.BeforeFirstGuess` (which carries `guesses`) and `imageBitmap: ImageBitmap?`. It renders `SharedImageDebugCard` at the top when `imageBitmap != null`, then the existing coaching content below.

## Files Changed

| File | Change |
|------|--------|
| `domain/src/commonMain/.../CoachingStep.kt` | `BeforeFirstGuess` → `data class` with `guesses` |
| `domain/src/commonMain/.../CoachingState.kt` | Pass `puzzle.guesses` to `BeforeFirstGuess(...)` |
| `androidApp/src/main/.../AppActivity.kt` | Load bitmap after parsing, convert to `ImageBitmap`, pass to `App` |
| `sharedUI/src/commonMain/.../App.kt` | Add `imageBitmap: ImageBitmap?` parameter, pass to `CoachingScreen` |
| `sharedUI/src/commonMain/.../CoachingScreen.kt` | Add `imageBitmap` param; add `SharedImageDebugCard`; render in `BeforeFirstGuessStep` |

## Out of Scope

- Persisting the image across app restarts
- Making the debug card collapsible
- Showing the image on steps 2–N
