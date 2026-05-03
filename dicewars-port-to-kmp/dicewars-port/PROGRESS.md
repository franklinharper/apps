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
| 3 - Map generation | Not Started | Pending | Pending | Pending | Source: `make_map`, `percolate`, `set_area_line` |
| 4 - Map renderer adaptation | Not Started | Pending | Pending | Pending | Reuse battlezone `MapRenderer.kt` and `TerritoryDrawer.kt` |
| 5 - Rules | Not Started | Pending | Pending | Pending | Legal attack, battle, supply, turn, history |
| 6 - AI | Not Started | Pending | Pending | Pending | Source: `ai_example`, `ai_default`, `ai_defensive` |
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
