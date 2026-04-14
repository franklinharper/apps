# kmp-app-generator

A command-line tool that generates a [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) Compose project on disk. It produces the same output as the [KMP Web Wizard](https://kmp.jetbrains.com/) but runs locally with a single command.

## Requirements

- Java 17+

## Build

```bash
cd kmp-app-generator
gradle generateWrapper
```

Outputs two files in `build/libs/`:

| File | Description |
|------|-------------|
| `kmp-app-generator` | Shell script wrapper |
| `kmp-app-generator-all.jar` | Self-contained fat JAR |

## Installation

Copy (or symlink) both files to any directory on your `PATH`. `~/.local/bin` works on most Linux/macOS systems:

```bash
# copy
cp build/libs/kmp-app-generator-all.jar ~/.local/bin/
cp build/libs/kmp-app-generator          ~/.local/bin/

# or symlink (picks up rebuilds automatically)
ln -sf "$PWD/build/libs/kmp-app-generator-all.jar" ~/.local/bin/kmp-app-generator-all.jar
ln -sf "$PWD/build/libs/kmp-app-generator"          ~/.local/bin/kmp-app-generator
```

## Usage

```
kmp-app-generator [<options>] <outputfolder>
```

`<outputfolder>` must not already exist — the tool creates it.

### Options

| Option | Default | Description |
|--------|---------|-------------|
| `--name=<text>` | `Multiplatform App` | Project name |
| `--id=<text>` | `org.company.app` | Package ID |
| `--android / --no-android` | `true` | Include Android target |
| `--ios / --no-ios` | `true` | Include iOS target |
| `--desktop / --no-desktop` | `true` | Include Desktop/JVM target |
| `--web / --no-web` | `true` | Include Web/Wasm target |
| `--tests / --no-tests` | `true` | Include sample tests |
| `--debug` | `false` | Keep partial output folder on failure |

All four platforms are enabled by default. At least one platform must be selected.

### Examples

```bash
# All platforms (default)
kmp-app-generator ~/projects/my-app --name "My App" --id "com.example.myapp"

# Android + iOS only
kmp-app-generator ~/projects/my-app --name "My App" --id "com.example.myapp" \
  --no-desktop --no-web

# Desktop only, no tests
kmp-app-generator ~/projects/my-app --no-android --no-ios --no-web --no-tests
```

## Project structure

```
kmp-app-generator/
├── build.gradle.kts          # Gradle build: shadow JAR + wrapper task
├── settings.gradle.kts
├── dist/
│   ├── kmp-app-generator-all.jar  # Pre-built fat JAR (committed)
│   └── source.hash                # Hash of sources when JAR was built
├── kmp-web-wizard/           # git submodule — upstream generation logic
│   └── src/commonMain/       # wired into this project's main source set
└── src/
    ├── main/kotlin/cli/
    │   └── Main.kt           # CLI entry point (KmpAppGenerator command)
    └── test/kotlin/cli/
        ├── CliTest.kt                    # Basic CLI behaviour tests
        └── WizardOutputComparisonTest.kt # Output matches web wizard for all flag combos
```

The generation logic lives entirely in the `kmp-web-wizard` submodule. `Main.kt` translates CLI flags into a `ProjectInfo` and calls `generateComposeAppFiles()`.

## Tests

```bash
gradle test
```

`WizardOutputComparisonTest` runs 16 cases (all non-empty subsets of the four platforms, plus `--no-tests`) and verifies byte-for-byte that the CLI output matches calling `generateComposeAppFiles()` directly — the same code path the web wizard uses.

## Updating the wizard

The `kmp-web-wizard` submodule tracks [terrakok/kmp-web-wizard](https://github.com/terrakok/kmp-web-wizard). To pull in upstream changes:

```bash
cd kmp-web-wizard && git pull origin master && cd ..
git add kmp-web-wizard
git commit -m "chore: update kmp-web-wizard submodule"
```

Then rebuild:

```bash
gradle generateWrapper
```

`generateWrapper` automatically copies the new JAR to `dist/` and rewrites `dist/source.hash`, so just commit the result:

```bash
git add dist/
git commit -m "chore: update dist/ after wizard bump"
```

## Claude Code session hook

`.claude/settings.json` registers a `SessionStart` hook (`.claude/hooks/session-start.sh`) that runs automatically when you open this repo in a remote Claude Code session ([claude.ai/code](https://claude.ai/code)).

**What it does:**

1. Initialises the `kmp-web-wizard` submodule
2. Computes a hash of `src/`, `build.gradle.kts`, and the submodule commit
3. Compares it against `dist/source.hash`
   - **Hash matches** → copies `dist/kmp-app-generator-all.jar` directly (no Gradle, fast)
   - **Hash differs** → runs `gradle generateWrapper`, updates `dist/` for next time
4. Writes a wrapper script and installs both to `~/.local/bin`
5. Adds `~/.local/bin` to `PATH` for the session

After the hook completes, `kmp-app-generator` is available in every terminal tab for that session.
