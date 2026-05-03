# Dicewars Port Progress

Package: `com.franklinharper.dicewarsport`

Progress status values:

- `Not Started`
- `In Progress`
- `Blocked`
- `Done`

## Phase Table

| Phase | Status | Red test evidence | Green implementation evidence | Commands/results | Notes/blockers |
|---|---|---|---|---|---|
| 0A - Bootstrap and plan | Done | N/A | New app generated; plan and progress docs written | `kmp-app-generator --name=dicewars-port --id=com.franklinharper.dicewarsport --android --ios --desktop --web --tests dicewars-port` succeeded; generated 91 files | Created `IMPLEMENTATION_PLAN.md` and `PROGRESS.md` |
| 0B - Baseline verification and tracking setup | Done | Added `DicewarsScreenContractTest.portHasSameNumberOfScreensAsOriginal`; `./gradlew :sharedUI:jvmTest --rerun-tasks` fails at compile with unresolved `DicewarsScreen` | Baseline commands/results recorded; generated source-set paths documented; `gradlew` executable bit restored | `./gradlew test` failed: generated `ComposeTest.simpleCheck` NPE in debug/release unit tests; `./gradlew :androidApp:assembleDebug` succeeded; `./gradlew :desktopApp:run` compiled/launched then timed out because app stayed open; `./gradlew :webApp:wasmJsBrowserDevelopmentRun` compiled and served at localhost:8080 then timed out because dev server stayed open | Source paths: common app code in `sharedUI/src/commonMain/kotlin/com/franklinharper/dicewarsport/`; common tests in `sharedUI/src/commonTest/kotlin/com/franklinharper/dicewarsport/`; platform entry points in `androidApp`, `desktopApp`, `webApp`, and `sharedUI/src/iosMain`. `local.properties` has empty `sdk.dir` warning. No implementation-plan path correction needed. |
| 1 - Screen-count contract | Done | Reused red test from Phase 0B: `DicewarsScreenContractTest.portHasSameNumberOfScreensAsOriginal` failed with unresolved `DicewarsScreen` | Added `DicewarsScreen` enum with exactly 10 entries in commonMain | `./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain` passed | Screen states: Loading, Title, MapPreview, HumanTurn, AiTurn, Battle, Supply, GameOver, Win, History |
| 2 - Pure game model | Done | Added `DicewarsGameModelTest`; first run failed with unresolved `DicewarsGame` | Added common pure model types: `AreaData`, `PlayerData`, `CellNeighbors`, `HistoryData`, `DicewarsGame`, and `RandomSource` | `./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain` passed | Ported JS constants/defaults and `next_cel` as `nextCell`; corrected expected odd-row neighbor values during red/green cycle |
| 3 - Map generation | Done | Added `DicewarsMapGenerationTest`; first run failed with unresolved `makeMap`, `GameMap`, and `toRenderMap` | Ported `make_map`, `percolate`, and `set_area_line`; added renderer-compatible `GameMap`/`Territory` and adapter | `./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain` passed | Adapter locks indexing: `GameMap.cells[cellIndex]` preserves JS area IDs (`1..31`), and `territories[areaId - 1]` stores that area. Used injected `RandomSource`; owner selection uses `nextInt(count)` for deterministic valid selection. |
| 4 - Map renderer adaptation | Done | Added `MapRendererAdapterTest`; first run failed with missing renderer helpers/models (`id`, `HexGrid`, `HexGeometry`, label/click helpers) | Adapted BattleZone `MapRenderer.kt`, `TerritoryDrawer.kt`, `HexGrid`, `HexGeometry`, `GameColors`, and `UiConstants` into dicewars package; adjusted `GameMap`/`Territory` fields | `./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain` passed | Renderer click callbacks still expose zero-based renderer index, while test helper locks Dicewars JS area ID mapping. Renderer remains UI-only; no rules embedded. |
| 5 - Rules | Done | Added `DicewarsRulesTest`; first run failed with unresolved `isLegalAttack`, `BattleRoll`, `rollBattle`, `resolveBattle`, supply, turn, area-count, and history functions | Added pure rules in `DicewarsRules.kt`: legal attack, deterministic battle roll, battle application/history, supply, next player, connected-area count, history append | `./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain` passed | Battle resolution is separate from animation; attacker wins only on `>`; supply uses stock cap 64 and owned areas below 8 dice only. |
| 6 - AI | Done | Added `DicewarsAiTest`; first run failed with unresolved `AiStrategy`, `Move`, `ExampleAi`, `DefaultAi`, and `DefensiveAi` | Added `DicewarsAi.kt` with `AiStrategy`, `Move`, `ExampleAi`, `DefaultAi`, and `DefensiveAi` using injected RNG where randomness is needed | `./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain` passed | AI returns `null` for no move instead of JS `0`; all returned moves are checked against `isLegalAttack`. |
| 7 - UI state machine | Not Started | Pending | Pending | Pending | Reducer tests for screen transitions |
| 8 - Compose UI | Not Started | Pending | Pending | Pending | Thin UI over state/render models |
| 9 - Build validation | Not Started | Pending | Pending | Pending | Android/Desktop/Web/iOS validation |

## Tracking Procedure

For each phase:

1. Mark the phase `In Progress` when work starts.
2. Add or update red tests first.
3. Record the failing test name or expected failure in the `Red test evidence` column.
4. Implement the smallest green change.
5. Record passing test/build commands in `Commands/results`.
6. Mark `Done` only when the acceptance criteria in `IMPLEMENTATION_PLAN.md` are satisfied.
7. If blocked, mark `Blocked` and document the exact blocker and next action.

## Required recurring checks

The following must stay true throughout the implementation:

- The app package remains `com.franklinharper.dicewarsport`.
- The port has exactly 10 screen states.
- Game logic is derived from `../dicewarsjs/`.
- The board rendering uses/adapts the existing map renderer from `../battlezone/`.
- Pure game logic remains in common Kotlin code and is testable without Compose.

## Command Log

### 2026-05-03 - Phase 0A

```bash
kmp-app-generator --name=dicewars-port --id=com.franklinharper.dicewarsport --android --ios --desktop --web --tests dicewars-port
```

Result: succeeded; generated 91 files, then moved into `/Users/frank/proj/apps/dicewars-port-to-kmp/dicewars-port`.

### 2026-05-03 - Phase 0B

```bash
cd dicewars-port-to-kmp/dicewars-port
chmod +x gradlew
./gradlew test
```

Result: failed. `sharedUI` generated `ComposeTest.simpleCheck` throws `NullPointerException` in both debug and release unit test tasks. Also saw warning that `local.properties` has `sdk.dir` set to an empty value.

```bash
./gradlew :androidApp:assembleDebug
```

Result: succeeded. D8/Kotlin metadata warnings were emitted but APK assembly completed.

```bash
./gradlew :desktopApp:run
```

Result: compiled and launched; command timed out after 25 seconds because the desktop app stayed open.

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Result: compiled successfully and started webpack dev server at `http://localhost:8080/`; command timed out after 40 seconds because the dev server kept running. KMP dependency-resolution warnings mentioned unresolved JS platform for `project :sharedUI`, but Wasm build still served.

Generated source-set layout documented:

- Shared common UI/domain package: `sharedUI/src/commonMain/kotlin/com/franklinharper/dicewarsport/`
- Shared common tests: `sharedUI/src/commonTest/kotlin/com/franklinharper/dicewarsport/`
- Android app module: `androidApp/src/main/kotlin/`
- Desktop app entry point: `desktopApp/src/main/kotlin/main.kt`
- Web app entry point: `webApp/src/commonMain/kotlin/main.kt`
- iOS shared entry point: `sharedUI/src/iosMain/kotlin/main.kt`

Added first red test:

```bash
./gradlew :sharedUI:jvmTest --rerun-tasks
```

Result: expected failure at `DicewarsScreenContractTest.kt:10` with `Unresolved reference 'DicewarsScreen'`.

### 2026-05-03 - Phase 1

Added common enum:

```text
sharedUI/src/commonMain/kotlin/com/franklinharper/dicewarsport/DicewarsScreen.kt
```

The enum contains exactly the 10 required screen states: Loading, Title, MapPreview, HumanTurn, AiTurn, Battle, Supply, GameOver, Win, History.

```bash
./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain
```

Result: passed. The screen-count contract test is green for the shared JVM test target.

### 2026-05-03 - Phase 2

Added model tests:

```text
sharedUI/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsGameModelTest.kt
```

Initial red run:

```bash
./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain
```

Result: failed at compile time with unresolved `DicewarsGame`, as expected.

Added pure common model:

```text
sharedUI/src/commonMain/kotlin/com/franklinharper/dicewarsport/DicewarsModel.kt
```

Implemented JS-derived constants/defaults and `next_cel` as `nextCell`. Included `RandomSource`, `AreaData`, `PlayerData`, `CellNeighbors`, `HistoryData`, and `DicewarsGame` storage needed by later phases.

```bash
./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain
```

Result: passed after correcting test expectations for odd-row hex neighbors to match the JS algorithm.

### 2026-05-03 - Phase 3

Added map generation tests:

```text
sharedUI/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsMapGenerationTest.kt
```

Initial red run:

```bash
./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain
```

Result: failed at compile time with unresolved `makeMap`, `GameMap`, `Territory`, and `toRenderMap`, as expected.

Implemented JS-derived map generation in:

```text
sharedUI/src/commonMain/kotlin/com/franklinharper/dicewarsport/DicewarsModel.kt
```

Ported/adapted:

- `make_map()` -> `DicewarsGame.makeMap(random: RandomSource): GameMap`
- `percolate()`
- `set_area_line()`
- renderer-compatible `GameMap` and `Territory`
- `DicewarsGame.toRenderMap()` adapter

Indexing decision: `GameMap.cells[cellIndex]` preserves the JS area ID (`1..31` for active territories, `0` for empty/sea). The renderer territory list is zero-based, so area `N` maps to `territories[N - 1]`. Tests lock this down.

```bash
./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain
```

Result: passed.

### 2026-05-03 - Phase 4

Added renderer adaptation tests:

```text
sharedUI/src/commonTest/kotlin/com/franklinharper/dicewarsport/MapRendererAdapterTest.kt
```

Initial red run:

```bash
./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain
```

Result: failed at compile time with missing renderer-required fields/helpers (`Territory.id`, `HexGrid`, `HexGeometry`, `computeTerritoryLabelPositionsForTest`, `findDicewarsTerritoryAtPositionForTest`, `visibleDiceCountLabelsForTest`).

Adapted BattleZone renderer code and dependencies into the Dicewars package:

```text
sharedUI/src/commonMain/kotlin/com/franklinharper/dicewarsport/presentation/components/MapRenderer.kt
sharedUI/src/commonMain/kotlin/com/franklinharper/dicewarsport/presentation/components/TerritoryDrawer.kt
sharedUI/src/commonMain/kotlin/com/franklinharper/dicewarsport/HexGrid.kt
sharedUI/src/commonMain/kotlin/com/franklinharper/dicewarsport/HexGeometry.kt
sharedUI/src/commonMain/kotlin/com/franklinharper/dicewarsport/Colors.kt
sharedUI/src/commonMain/kotlin/com/franklinharper/dicewarsport/UiConstants.kt
```

Updated `GameMap`/`Territory` to expose renderer-required fields (`gridWidth`, `gridHeight`, `maxTerritories`, `id`) while preserving Phase 3 adapter behavior. Added public test hooks for label positions, Dicewars area-id click mapping, and dice-count label visibility.

```bash
./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain
```

Result: passed.

### 2026-05-03 - Phase 5

Added rules tests:

```text
sharedUI/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsRulesTest.kt
```

Initial red run:

```bash
./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain
```

Result: failed at compile time with unresolved rules API: `isLegalAttack`, `BattleRoll`, `rollBattle`, `resolveBattle`, `startSupply`, `supplyOneDie`, `nextPlayer`, `setAreaTc`, and `setHistory`.

Implemented pure rules:

```text
sharedUI/src/commonMain/kotlin/com/franklinharper/dicewarsport/DicewarsRules.kt
```

Implemented/adapted from JS:

- legal attack checks
- deterministic `BattleRoll`
- attacker wins only when attacker total is greater than defender total
- battle result application and attack history
- supply stock calculation capped at 64
- one-die supply to owned areas below 8 dice and supply history
- next-player skipping eliminated players
- connected-area maximum count (`setAreaTc`)
- `setHistory`

```bash
./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain
```

Result: passed.

### 2026-05-03 - Phase 6

Added AI tests:

```text
sharedUI/src/commonTest/kotlin/com/franklinharper/dicewarsport/DicewarsAiTest.kt
```

Initial red run:

```bash
./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain
```

Result: failed at compile time with unresolved AI API: `AiStrategy`, `Move`, `ExampleAi`, `DefaultAi`, and `DefensiveAi`.

Implemented AI strategies:

```text
sharedUI/src/commonMain/kotlin/com/franklinharper/dicewarsport/DicewarsAi.kt
```

Ported/adapted from JS:

- `ai_example.js` -> `ExampleAi`
- `ai_default.js` -> `DefaultAi`
- `ai_defensive.js` -> `DefensiveAi`

Kotlin behavior uses `Move?`; no move is represented as `null` instead of JS `0`. Random strategies use injected `RandomSource`; `DefensiveAi` is deterministic.

```bash
./gradlew :sharedUI:jvmTest --rerun-tasks --console=plain
```

Result: passed.
