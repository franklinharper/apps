# Shared Image Debug Display Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When a Wordle screenshot is shared into the app, show the original image side-by-side with the decoded tile grid at the top of Step 1.

**Architecture:** `CoachingStep.BeforeFirstGuess` becomes a data class carrying the decoded guesses. `AppActivity` loads the shared image as an `ImageBitmap` and passes it through `App` → `CoachingScreen` → `BeforeFirstGuessStep`, which renders a new `SharedImageDebugCard` composable when a bitmap is present.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, `androidx.compose.ui.graphics.ImageBitmap`, `android.graphics.BitmapFactory`

---

## File Map

| File | Change |
|------|--------|
| `domain/src/commonMain/kotlin/com/franklinharper/wordlecoach/domain/CoachingStep.kt` | `BeforeFirstGuess` object → data class with `guesses` |
| `domain/src/commonMain/kotlin/com/franklinharper/wordlecoach/domain/CoachingState.kt` | Pass `puzzle.guesses` / `emptyList()` to `BeforeFirstGuess(...)` |
| `domain/src/commonTest/kotlin/com/franklinharper/wordlecoach/domain/CoachingStateTest.kt` | New — tests for `BeforeFirstGuess` carrying guesses |
| `androidApp/src/main/kotlin/com/franklinharper/wordlecoach/androidApp/AppActivity.kt` | Load bitmap on IO thread, pass `ImageBitmap?` to `App` |
| `sharedUI/src/commonMain/kotlin/com/franklinharper/wordlecoach/App.kt` | Add `imageBitmap: ImageBitmap?` param, pass to `CoachingScreen` |
| `sharedUI/src/commonMain/kotlin/com/franklinharper/wordlecoach/CoachingScreen.kt` | Add `imageBitmap` param; add `SharedImageDebugCard`; update `BeforeFirstGuessStep` |

---

## Task 1: Write failing tests for the domain change

**Files:**
- Create: `domain/src/commonTest/kotlin/com/franklinharper/wordlecoach/domain/CoachingStateTest.kt`

- [ ] **Step 1: Create the test file**

```kotlin
package com.franklinharper.wordlecoach.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CoachingStateTest {

    private val crane = CompletedGuess(
        listOf(
            GuessedTile('C', LetterResult.Absent),
            GuessedTile('R', LetterResult.Present),
            GuessedTile('A', LetterResult.Absent),
            GuessedTile('N', LetterResult.Absent),
            GuessedTile('E', LetterResult.Correct),
        )
    )
    private val snout = CompletedGuess(
        listOf(
            GuessedTile('S', LetterResult.Correct),
            GuessedTile('N', LetterResult.Correct),
            GuessedTile('O', LetterResult.Correct),
            GuessedTile('U', LetterResult.Correct),
            GuessedTile('T', LetterResult.Correct),
        )
    )

    @Test
    fun initial_step_has_empty_guesses() {
        val state = CoachingState.initial()
        val step = assertIs<CoachingStep.BeforeFirstGuess>(state.currentStep)
        assertEquals(emptyList(), step.guesses)
    }

    @Test
    fun fromPuzzle_step_zero_carries_all_guesses() {
        val puzzle = PuzzleResult(listOf(crane, snout))
        val state = CoachingState.fromPuzzle(puzzle)
        val step = assertIs<CoachingStep.BeforeFirstGuess>(state.currentStep)
        assertEquals(listOf(crane, snout), step.guesses)
    }
}
```

- [ ] **Step 2: Run the tests — expect compilation failure**

```bash
cd /Users/frank/proj/apps/wordle-coach
./gradlew :domain:jvmTest 2>&1 | tail -20
```

Expected: compile error — `CoachingStep.BeforeFirstGuess` is an object, not a data class, so `step.guesses` won't resolve.

---

## Task 2: Update `CoachingStep` and `CoachingState`

**Files:**
- Modify: `domain/src/commonMain/kotlin/com/franklinharper/wordlecoach/domain/CoachingStep.kt`
- Modify: `domain/src/commonMain/kotlin/com/franklinharper/wordlecoach/domain/CoachingState.kt`

- [ ] **Step 1: Change `BeforeFirstGuess` from object to data class**

Replace the entire content of `CoachingStep.kt`:

```kotlin
package com.franklinharper.wordlecoach.domain

sealed class CoachingStep {

    /** The state before the player makes their first guess. */
    data class BeforeFirstGuess(val guesses: List<CompletedGuess>) : CoachingStep()

    /**
     * The state immediately after the player has submitted guess [guessNumber] (1-based).
     *
     * @param guess             The tiles and results for that guess.
     * @param remainingAnswers  Answer-list words still consistent with all guesses so far.
     */
    data class AfterGuess(
        val guessNumber: Int,
        val guess: CompletedGuess,
        val remainingAnswers: List<String>,
    ) : CoachingStep()
}
```

- [ ] **Step 2: Update `CoachingState` to pass guesses**

Replace the entire content of `CoachingState.kt`:

```kotlin
package com.franklinharper.wordlecoach.domain

data class CoachingState(
    val steps: List<CoachingStep>,
    val currentStepIndex: Int,
) {
    val currentStep: CoachingStep get() = steps[currentStepIndex]
    val canGoBack: Boolean get() = currentStepIndex > 0
    val canGoForward: Boolean get() = currentStepIndex < steps.size - 1

    fun goBack(): CoachingState =
        if (canGoBack) copy(currentStepIndex = currentStepIndex - 1) else this

    fun goForward(): CoachingState =
        if (canGoForward) copy(currentStepIndex = currentStepIndex + 1) else this

    companion object {
        /** Single-step state used before the user has shared a puzzle. */
        fun initial() = CoachingState(
            steps = listOf(CoachingStep.BeforeFirstGuess(guesses = emptyList())),
            currentStepIndex = 0,
        )

        /**
         * Builds all coaching steps from a completed puzzle.
         *
         * Step 0 is always [CoachingStep.BeforeFirstGuess] carrying all decoded guesses.
         * Steps 1..N correspond to [CoachingStep.AfterGuess] for each guess,
         * where remaining answers are filtered progressively.
         */
        fun fromPuzzle(puzzle: PuzzleResult): CoachingState {
            val steps = mutableListOf<CoachingStep>(
                CoachingStep.BeforeFirstGuess(guesses = puzzle.guesses)
            )
            var remaining = WordLists.answers
            for ((index, guess) in puzzle.guesses.withIndex()) {
                remaining = WordFilter.filter(remaining, guess)
                steps += CoachingStep.AfterGuess(
                    guessNumber = index + 1,
                    guess = guess,
                    remainingAnswers = remaining,
                )
            }
            return CoachingState(steps = steps, currentStepIndex = 0)
        }
    }
}
```

- [ ] **Step 3: Run domain tests — expect PASS**

```bash
cd /Users/frank/proj/apps/wordle-coach
./gradlew :domain:jvmTest 2>&1 | tail -20
```

Expected:
```
CoachingStateTest > initial_step_has_empty_guesses PASSED
CoachingStateTest > fromPuzzle_step_zero_carries_all_guesses PASSED
BUILD SUCCESSFUL
```

- [ ] **Step 4: Commit**

```bash
git add domain/src/commonMain/kotlin/com/franklinharper/wordlecoach/domain/CoachingStep.kt \
        domain/src/commonMain/kotlin/com/franklinharper/wordlecoach/domain/CoachingState.kt \
        domain/src/commonTest/kotlin/com/franklinharper/wordlecoach/domain/CoachingStateTest.kt
git commit -m "feat: BeforeFirstGuess carries decoded guesses"
```

---

## Task 3: Load `ImageBitmap` in `AppActivity`

**Files:**
- Modify: `androidApp/src/main/kotlin/com/franklinharper/wordlecoach/androidApp/AppActivity.kt`

- [ ] **Step 1: Update `AppActivity` to load bitmap and pass it to `App`**

Replace the entire content of `AppActivity.kt`:

```kotlin
package com.franklinharper.wordlecoach.androidApp

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.franklinharper.wordlecoach.App
import com.franklinharper.wordlecoach.domain.PuzzleResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var puzzle by mutableStateOf<PuzzleResult?>(null)
        var imageBitmap by mutableStateOf<ImageBitmap?>(null)

        @Suppress("DEPRECATION")
        val imageUri: Uri? = if (intent?.action == Intent.ACTION_SEND)
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        else
            null

        if (imageUri != null) {
            lifecycleScope.launch {
                val (parsedPuzzle, bitmap) = withContext(Dispatchers.IO) {
                    val parsed = WordleImageParser(this@AppActivity).parse(imageUri)
                    val bmp = contentResolver.openInputStream(imageUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                    parsed to bmp
                }
                puzzle = parsedPuzzle
                imageBitmap = bitmap
            }
        }

        setContent {
            App(
                puzzle = puzzle,
                imageBitmap = imageBitmap,
                onThemeChanged = { ThemeChanged(it) },
            )
        }
    }
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isDark) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isDark
            isAppearanceLightNavigationBars = isDark
        }
    }
}
```

> `AppActivity.kt` now references `App(imageBitmap = ...)` which doesn't compile until Task 4 adds that parameter. Do not commit yet — `AppActivity.kt` will be committed together with the shared-UI changes in Task 5.

---

## Task 4: Thread `ImageBitmap?` through `App` and `CoachingScreen`

**Files:**
- Modify: `sharedUI/src/commonMain/kotlin/com/franklinharper/wordlecoach/App.kt`
- Modify: `sharedUI/src/commonMain/kotlin/com/franklinharper/wordlecoach/CoachingScreen.kt`

- [ ] **Step 1: Add `imageBitmap` parameter to `App`**

Replace the entire content of `App.kt`:

```kotlin
package com.franklinharper.wordlecoach

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import com.franklinharper.wordlecoach.domain.CoachingState
import com.franklinharper.wordlecoach.domain.PuzzleResult
import com.franklinharper.wordlecoach.theme.AppTheme

/**
 * Root composable.
 *
 * @param puzzle       Parsed puzzle received from the share-target entry point.
 *                     `null` means the app was launched directly (not via a share),
 *                     so only the pre-game step 1 coaching is shown.
 * @param imageBitmap  The raw shared screenshot as a bitmap, for debug display on step 1.
 *                     `null` when the app was launched directly.
 */
@Preview
@Composable
fun App(
    puzzle: PuzzleResult? = null,
    imageBitmap: ImageBitmap? = null,
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
) = AppTheme(onThemeChanged) {
    val initialState = remember(puzzle) {
        if (puzzle != null) CoachingState.fromPuzzle(puzzle)
        else CoachingState.initial()
    }
    var state by remember(initialState) { mutableStateOf(initialState) }

    CoachingScreen(
        state = state,
        imageBitmap = imageBitmap,
        onBack    = { state = state.goBack() },
        onForward = { state = state.goForward() },
    )
}
```

- [ ] **Step 2: Add `imageBitmap` parameter to `CoachingScreen` and update `BeforeFirstGuessStep` call**

In `CoachingScreen.kt`, change the `CoachingScreen` function signature and its `when` branch for `BeforeFirstGuess`:

```kotlin
@Composable
fun CoachingScreen(
    state: CoachingState,
    imageBitmap: ImageBitmap? = null,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationRow(state = state, onBack = onBack, onForward = onForward)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val step = state.currentStep) {
                is CoachingStep.BeforeFirstGuess -> BeforeFirstGuessStep(step, imageBitmap)
                is CoachingStep.AfterGuess       -> AfterGuessStep(step)
            }
        }
    }
}
```

Also add `androidx.compose.ui.graphics.ImageBitmap` to the imports at the top of `CoachingScreen.kt`.

- [ ] **Step 3: Update `BeforeFirstGuessStep` signature**

Change the `BeforeFirstGuessStep` function to accept the step and bitmap. Also convert its inner `LazyColumn` to a scrollable `Column` so the debug card at the top doesn't conflict with nested scrolling:

```kotlin
@Composable
private fun BeforeFirstGuessStep(
    step: CoachingStep.BeforeFirstGuess,
    imageBitmap: ImageBitmap?,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        if (imageBitmap != null) {
            SharedImageDebugCard(imageBitmap = imageBitmap, guesses = step.guesses)
        }
        Text(
            text = "Step 1 — Before your first guess",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Text(
            text = "C/V shape distribution across the ${WordShapeStats.stats.sumOf { it.count }} answer words  (Y counts as vowel)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        )
        ShapeTableHeader()
        HorizontalDivider()
        WordShapeStats.stats.forEach { stat ->
            ShapeRow(stat)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}
```

Add `androidx.compose.foundation.rememberScrollState` and `androidx.compose.foundation.verticalScroll` to the imports.

- [ ] **Step 4: Build to check compilation**

```bash
cd /Users/frank/proj/apps/wordle-coach
./gradlew :sharedUI:compileDebugKotlinAndroid 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` (or only errors about the not-yet-added `SharedImageDebugCard`).

---

## Task 5: Add `SharedImageDebugCard` composable

**Files:**
- Modify: `sharedUI/src/commonMain/kotlin/com/franklinharper/wordlecoach/CoachingScreen.kt`

- [ ] **Step 1: Add `SharedImageDebugCard` and update tile composables to support custom size**

First, add a `tileSize` parameter to `GuessTileRow` and `GuessTile` so the debug card can use smaller tiles that fit side-by-side. The existing callers (`AfterGuessStep`) use the default 52.dp:

```kotlin
@Composable
private fun GuessTileRow(
    guess: CompletedGuess,
    modifier: Modifier = Modifier,
    tileSize: Dp = 52.dp,
    tileGap: Dp = 6.dp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(tileGap),
    ) {
        guess.tiles.forEach { tile ->
            GuessTile(tile, tileSize)
        }
    }
}

@Composable
private fun GuessTile(tile: GuessedTile, size: Dp = 52.dp) {
    val bg = when (tile.result) {
        LetterResult.Correct -> ColorCorrect
        LetterResult.Present -> ColorPresent
        LetterResult.Absent  -> ColorAbsent
    }
    Box(
        modifier = Modifier
            .size(size)
            .background(bg, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tile.letter.toString(),
            color = ColorTileText,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}
```

- [ ] **Step 2: Add `SharedImageDebugCard`**

Add this composable in `CoachingScreen.kt`, between `BeforeFirstGuessStep` and `ShapeTableHeader`:

```kotlin
@Composable
private fun SharedImageDebugCard(
    imageBitmap: ImageBitmap,
    guesses: List<CompletedGuess>,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "SHARED IMAGE & DECODED GUESSES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Left column: the original shared screenshot
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Shared Wordle screenshot",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentScale = ContentScale.Fit,
                )
                // Right column: decoded tile grid at reduced tile size
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    guesses.forEach { guess ->
                        GuessTileRow(
                            guess = guess,
                            tileSize = 30.dp,
                            tileGap = 4.dp,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Add missing imports to `CoachingScreen.kt`**

Add these to the import block at the top:

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
```

- [ ] **Step 4: Build to check compilation**

```bash
cd /Users/frank/proj/apps/wordle-coach
./gradlew :sharedUI:compileDebugKotlinAndroid 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` with no errors.

- [ ] **Step 5: Commit**

```bash
git add sharedUI/src/commonMain/kotlin/com/franklinharper/wordlecoach/App.kt \
        sharedUI/src/commonMain/kotlin/com/franklinharper/wordlecoach/CoachingScreen.kt \
        androidApp/src/main/kotlin/com/franklinharper/wordlecoach/androidApp/AppActivity.kt
git commit -m "feat: show shared image and decoded guesses on step 1"
```

---

## Task 6: Install on device and verify

- [ ] **Step 1: Build and install on Pixel 6**

```bash
cd /Users/frank/proj/apps/wordle-coach
./gradlew installDebug -Pandroid.deploy.serialNumber=18201FDF6004K9 2>&1 | tail -20
```

Expected:
```
> Task :androidApp:installDebug
Installing APK 'androidApp-debug.apk' on 'Pixel 6 - 16' for :androidApp:debug
BUILD SUCCESSFUL
```

- [ ] **Step 2: Share a Wordle screenshot to the app and verify**

On the Pixel 6:
1. Open a Wordle screenshot in Photos or Google Photos
2. Tap Share → Wordle Coach
3. On Step 1, confirm:
   - Debug card appears above "Step 1 — Before your first guess"
   - Left side shows the original screenshot
   - Right side shows colored tile rows matching the screenshot (green/yellow/gray)
   - Navigating forward to steps 2+ hides the card (card is step-1 only)
   - Launching the app directly (without sharing) shows no card
