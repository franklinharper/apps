# kmp-app-generator CLI — Design Spec

**Date:** 2026-04-14  
**Status:** Approved

## Overview

Build a JVM CLI (`kmp-app-generator`) that reuses the generation logic from `terrakok/kmp-web-wizard` (Apache 2.0) to produce Kotlin Multiplatform project files directly on disk, without requiring a browser or a server. The CLI will be validated internally before any upstream PR is considered.

## Key Decisions

- **No server API exists.** The web wizard is 100% client-side (JSZip + FileSaver.js). The generation logic lives in `commonMain` and already runs on JVM (there are JVM tests).
- **Source:** `terrakok/kmp-web-wizard` generator code (Apache 2.0 license), vendored or included as a source dependency
- **Scope:** Compose App only (KMP Library support is out of scope)
- **Distribution:** Fat JAR (`java -jar kmp-app-generator.jar`). Java 21 is confirmed available in the target environment.
- **Invocation:** Flags only — no interactive mode.
- **Upstream PR:** Deferred — tool will be validated internally first.

## Architecture

A new `jvmMain` source set is added alongside the existing `jsMain`. It contains a single file:

```
src/jvmMain/kotlin/cli/Main.kt   — clikt CLI entrypoint (~80 lines)
```

The entrypoint:
1. Parses CLI arguments via clikt
2. Builds a `ProjectInfo` from the parsed arguments
3. Builds the dependency set using the same logic as the web wizard (`DefaultComposeLibraries` + platform-conditional additions)
4. Calls the existing `generateComposeAppFiles()` from `commonMain`
5. Writes the resulting `ProjectFile` list to disk

No changes to `commonMain` or `jsMain`.

## CLI Interface

```
Usage: kmp-app-generator <output-folder> [OPTIONS]

Arguments:
  output-folder   Directory to create (must not already exist)

Options:
  --name TEXT         Project name (default: "Multiplatform App")
  --id TEXT           Package ID (default: "org.company.app")
  --android/--no-android   Include Android target (default: on)
  --ios/--no-ios           Include iOS target with shared Compose UI (default: on)
  --desktop/--no-desktop   Include Desktop/JVM target (default: on)
  --web/--no-web           Include Web/Wasm target (default: on)
  --tests/--no-tests        Include sample tests (default: on)
  --debug             Keep partially written folder on failure (default: off)
  -h, --help          Show this message and exit
```

Platform flag to `ProjectInfo` mapping:

| Flag | ProjectPlatform |
|------|----------------|
| `--android` | `Android` |
| `--ios` | `Ios` |
| `--desktop` | `Jvm` |
| `--web` | `Wasm` + `Js` (web requires both) |

At least one platform must be selected; if none are selected the CLI exits with a clear error.

## File Writing & Error Handling

1. Fail with a clear error and exit non-zero if `output-folder` already exists
2. Create the output folder
3. Call `generateComposeAppFiles()` with the constructed `ProjectInfo`
4. For each `ProjectFile`: create parent directories and write `content` as UTF-8
5. For each `BinaryFile`: load from classpath at `resourcePath` and write raw bytes
6. On any mid-write failure:
   - Print a clear error message
   - Delete the output folder (unless `--debug` is present)
   - Exit non-zero

## Build Configuration Changes

All changes are in `build.gradle.kts`:

1. Add `jvm()` to the `kotlin { }` multiplatform targets block
2. Add clikt dependency scoped to `jvmMain`:
   ```kotlin
   jvmMainImplementation("com.github.ajalt.clikt:clikt:5.0.3")
   ```
3. Add the `com.github.johnrengelman.shadow` plugin and configure a `shadowJar` task:
   - Main class: `cli.MainKt`
   - Output artifact: `kmp-app-generator-all.jar`
4. Add a Gradle task that generates a `kmp-app-generator` wrapper shell script alongside the JAR:
   ```bash
   #!/bin/bash
   exec java -jar "$(dirname "$0")/kmp-app-generator-all.jar" "$@"
   ```
   The script is made executable as part of the task.

## Out of Scope

- KMP Library wizard mode
- iOS with SwiftUI (native UI) target — current wizard only supports shared Compose UI for iOS
- Interactive/prompt mode
- Native binary (GraalVM) — fat JAR is sufficient given Java 21 availability
- Package manager distribution (Homebrew, etc.)
