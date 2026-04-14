# kmp-app-generator CLI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a fat-JAR JVM CLI (`kmp-app-generator`) that generates Kotlin Multiplatform Compose App projects on disk by vendoring the generation logic from `terrakok/kmp-web-wizard`.

**Architecture:** The `kmp-web-wizard` generator logic (pure Kotlin, no KMP-specific APIs) is copied into `src/main/kotlin/wizard/` and its binary resources into `src/main/resources/binaries/`. A single CLI entrypoint (`cli/Main.kt`) uses clikt to parse flags, builds a `ProjectInfo`, calls `generateComposeAppFiles()`, and writes files to disk. A `shadowJar` Gradle task produces `kmp-app-generator-all.jar`; a `generateWrapper` task writes the `kmp-app-generator` shell script alongside it.

**Tech Stack:** Kotlin 2.2.20, Gradle, clikt 5.0.3, shadowJar plugin 8.1.1, JUnit 4 (already used by the vendored test helpers)

---

## File Structure

| Action | Path | Responsibility |
|--------|------|----------------|
| Modify | `build.gradle.kts` | Add shadowJar plugin, clikt dep, wrapper script task |
| Create | `src/main/kotlin/wizard/` | Vendored generator source from kmp-web-wizard |
| Create | `src/main/resources/binaries/` | Vendored binary resources (icons, gradle wrapper, etc.) |
| Create | `src/main/kotlin/cli/Main.kt` | CLI entrypoint — parse flags, build ProjectInfo, write files |
| Create | `src/test/kotlin/cli/CliTest.kt` | Tests for the CLI command |
| Delete | `src/main/kotlin/Main.kt` | Replaced by cli/Main.kt |

---

## Task 1: Configure the Build

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Replace `build.gradle.kts` with the following**

```kotlin
plugins {
    kotlin("jvm") version "2.2.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.franklinharper.kmp-app-generator"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "cli.MainKt"
    }
    archiveFileName.set("kmp-app-generator-all.jar")
}

tasks.register("generateWrapper") {
    dependsOn("shadowJar")
    doLast {
        val jarFile = tasks.named("shadowJar", com.github.johnrengelman.shadow.tasks.ShadowJar::class).get()
            .archiveFile.get().asFile
        val scriptFile = File(jarFile.parentFile, "kmp-app-generator")
        scriptFile.writeText(
            "#!/bin/bash\nexec java -jar \"\$(dirname \"\$0\")/kmp-app-generator-all.jar\" \"\$@\"\n"
        )
        scriptFile.setExecutable(true)
        println("Wrapper script: ${scriptFile.absolutePath}")
    }
}
```

- [ ] **Step 2: Verify the build configuration resolves**

```bash
./gradlew dependencies --configuration runtimeClasspath 2>&1 | grep -E "clikt|shadow|BUILD"
```

Expected: lines referencing `clikt-jvm-5.0.3` and `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "build: add shadowJar plugin, clikt dependency, wrapper script task"
```

---

## Task 2: Vendor the kmp-web-wizard Source

**Files:**
- Create: `src/main/kotlin/wizard/` (entire tree)
- Create: `src/main/resources/binaries/` (entire tree)
- Delete: `src/main/kotlin/Main.kt`

- [ ] **Step 1: Clone kmp-web-wizard into a temp directory**

```bash
git clone --depth 1 https://github.com/terrakok/kmp-web-wizard.git /tmp/kmp-web-wizard
```

Expected: `Cloning into '/tmp/kmp-web-wizard'...` then `done.`

- [ ] **Step 2: Copy the generator source into this project**

```bash
cp -r /tmp/kmp-web-wizard/src/commonMain/kotlin/wizard src/main/kotlin/wizard
cp -r /tmp/kmp-web-wizard/src/commonMain/resources/binaries src/main/resources/binaries
```

- [ ] **Step 3: Delete the placeholder Main.kt**

```bash
rm src/main/kotlin/Main.kt
```

- [ ] **Step 4: Verify the vendored source compiles**

```bash
./gradlew compileKotlin 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

If you see errors mentioning `BuildConfig`, it means the wizard source references the generated `BuildConfig` class. Fix by creating `src/main/kotlin/wizard/BuildConfig.kt`:

```kotlin
package wizard

object BuildConfig {
    val wizardType = WizardType.ComposeApp
}
```

- [ ] **Step 5: Clean up the temp clone**

```bash
rm -rf /tmp/kmp-web-wizard
```

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/wizard src/main/resources/binaries
git rm src/main/kotlin/Main.kt
git commit -m "vendor: add kmp-web-wizard generator source and binary resources (Apache 2.0)"
```

---

## Task 3: Write a Failing CLI Test

**Files:**
- Create: `src/test/kotlin/cli/CliTest.kt`

- [ ] **Step 1: Create `src/test/kotlin/cli/CliTest.kt`**

```kotlin
package cli

import com.github.ajalt.clikt.core.UsageError
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CliTest {

    @Rule
    @JvmField
    val tmp = TemporaryFolder()

    @Test
    fun `generates all-platform project by default`() {
        val outDir = tmp.root.resolve("my-app")
        KmpAppGenerator().parse(
            listOf(outDir.absolutePath, "--name", "My App", "--id", "com.example.myapp")
        )
        assertTrue(outDir.exists(), "Output folder should be created")
        assertTrue(outDir.resolve("gradlew").exists(), "gradlew should exist")
        assertTrue(outDir.resolve("gradlew.bat").exists(), "gradlew.bat should exist")
        assertTrue(outDir.resolve("settings.gradle.kts").exists(), "settings.gradle.kts should exist")
        assertTrue(outDir.resolve("build.gradle.kts").exists(), "root build.gradle.kts should exist")
        assertTrue(outDir.resolve("androidApp").isDirectory, "androidApp directory should exist")
        assertTrue(outDir.resolve("iosApp").isDirectory, "iosApp directory should exist")
    }

    @Test
    fun `fails with UsageError if output folder already exists`() {
        val existing = tmp.newFolder("existing")
        assertFailsWith<UsageError> {
            KmpAppGenerator().parse(listOf(existing.absolutePath))
        }
    }

    @Test
    fun `generates android-only project when other platforms disabled`() {
        val outDir = tmp.root.resolve("android-only")
        KmpAppGenerator().parse(
            listOf(outDir.absolutePath, "--no-ios", "--no-desktop", "--no-web")
        )
        assertTrue(outDir.resolve("androidApp").isDirectory, "androidApp should exist")
        assertFalse(outDir.resolve("iosApp").exists(), "iosApp should not exist")
    }

    @Test
    fun `fails with UsageError when all platforms disabled`() {
        val outDir = tmp.root.resolve("no-platforms")
        assertFailsWith<UsageError> {
            KmpAppGenerator().parse(
                listOf(outDir.absolutePath, "--no-android", "--no-ios", "--no-desktop", "--no-web")
            )
        }
        assertFalse(outDir.exists(), "Output folder should not be created")
    }

    @Test
    fun `does not include iosApp when ios disabled`() {
        val outDir = tmp.root.resolve("no-ios")
        KmpAppGenerator().parse(listOf(outDir.absolutePath, "--no-ios"))
        assertFalse(outDir.resolve("iosApp").exists(), "iosApp should not exist")
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails (class not found)**

```bash
./gradlew test --tests "cli.CliTest" 2>&1 | tail -15
```

Expected: `BUILD FAILED` with `ClassNotFoundException: cli.KmpAppGenerator` or similar

- [ ] **Step 3: Commit the failing test**

```bash
git add src/test/kotlin/cli/CliTest.kt
git commit -m "test: add failing CLI tests for kmp-app-generator"
```

---

## Task 4: Implement the CLI

**Files:**
- Create: `src/main/kotlin/cli/Main.kt`

- [ ] **Step 1: Create `src/main/kotlin/cli/Main.kt`**

```kotlin
package cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import wizard.BinaryFile
import wizard.ProjectInfo
import wizard.ProjectPlatform
import wizard.WizardType
import wizard.dependencies.AndroidApplicationPlugin
import wizard.dependencies.AndroidKmpLibraryPlugin
import wizard.dependencies.AndroidxActivityCompose
import wizard.dependencies.ComposeCompilerPlugin
import wizard.dependencies.ComposeMultiplatformPlugin
import wizard.dependencies.DefaultComposeLibraries
import wizard.dependencies.KotlinAndroidPlugin
import wizard.dependencies.KotlinJvmPlugin
import wizard.dependencies.KotlinMultiplatformPlugin
import wizard.generateComposeAppFiles
import java.io.File

class KmpAppGenerator : CliktCommand(
    name = "kmp-app-generator",
    help = "Generate a Kotlin Multiplatform Compose App project on disk."
) {
    private val outputFolder by argument(help = "Directory to create (must not already exist)")
    private val projectName by option("--name", help = "Project name").default("Multiplatform App")
    private val packageId by option("--id", help = "Package ID (e.g. com.example.myapp)").default("org.company.app")
    private val android by option("--android/--no-android", help = "Include Android target").flag(default = true)
    private val ios by option("--ios/--no-ios", help = "Include iOS target (shared Compose UI)").flag(default = true)
    private val desktop by option("--desktop/--no-desktop", help = "Include Desktop/JVM target").flag(default = true)
    private val web by option("--web/--no-web", help = "Include Web/Wasm target").flag(default = true)
    private val tests by option("--tests/--no-tests", help = "Include sample tests").flag(default = true)
    private val debug by option("--debug", help = "Keep partially written folder on failure").flag(default = false)

    override fun run() {
        val out = File(outputFolder)
        if (out.exists()) {
            throw UsageError("Output folder already exists: ${out.absolutePath}")
        }

        val platforms = buildSet {
            if (android) add(ProjectPlatform.Android)
            if (ios) add(ProjectPlatform.Ios)
            if (desktop) add(ProjectPlatform.Jvm)
            if (web) {
                add(ProjectPlatform.Wasm)
                add(ProjectPlatform.Js)
            }
        }
        if (platforms.isEmpty()) {
            throw UsageError("At least one platform must be selected (see --help)")
        }

        val dependencies = buildSet {
            add(KotlinMultiplatformPlugin)
            add(ComposeCompilerPlugin)
            add(ComposeMultiplatformPlugin)
            addAll(DefaultComposeLibraries)
            if (android) {
                add(KotlinAndroidPlugin)
                add(AndroidApplicationPlugin)
                add(AndroidKmpLibraryPlugin)
                add(AndroidxActivityCompose)
            }
            if (desktop) {
                add(KotlinJvmPlugin)
            }
        }

        val info = ProjectInfo(
            packageId = packageId,
            name = projectName,
            moduleName = "sharedUI",
            platforms = platforms,
            dependencies = dependencies,
            addTests = tests,
            type = WizardType.ComposeApp
        )

        out.mkdirs()
        try {
            val files = info.generateComposeAppFiles()
            for (projectFile in files) {
                val file = File(out, projectFile.path)
                file.parentFile?.mkdirs()
                if (projectFile is BinaryFile) {
                    val resource = javaClass.getResourceAsStream("/binaries/${projectFile.resourcePath}")
                        ?: error("Binary resource not found on classpath: /binaries/${projectFile.resourcePath}")
                    resource.use { input -> file.outputStream().use { input.copyTo(it) } }
                } else {
                    file.writeText(projectFile.content)
                }
            }
            echo("Generated ${files.size} files in: ${out.absolutePath}")
        } catch (e: Exception) {
            echo("Error: ${e.message}", err = true)
            if (!debug) {
                out.deleteRecursively()
            }
            throw e
        }
    }
}

fun main(args: Array<String>) = KmpAppGenerator().main(args)
```

- [ ] **Step 2: Run the tests**

```bash
./gradlew test --tests "cli.CliTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL` with 5 tests passing. If any test fails, read the error message carefully:
- `ClassCastException` or `NoSuchMethodError` → check clikt API — replace `flag(default = true)` with `flag("--android/--no-android", default = true)` if needed
- `NullPointerException` in resource loading → the binary resources may not be on the test classpath; see Task 2 Step 4 for the `BuildConfig` fix pattern

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/cli/Main.kt
git commit -m "feat: implement kmp-app-generator CLI with clikt"
```

---

## Task 5: Build and Verify End-to-End

**Files:** no new files — verifying build artifacts

- [ ] **Step 1: Build the fat JAR and wrapper script**

```bash
./gradlew generateWrapper 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL` and a line like `Wrapper script: .../build/libs/kmp-app-generator`

- [ ] **Step 2: Confirm the artifacts exist**

```bash
ls -lh build/libs/
```

Expected: both `kmp-app-generator-all.jar` and `kmp-app-generator` (executable) are present.

- [ ] **Step 3: Run `--help` via the wrapper**

```bash
build/libs/kmp-app-generator --help
```

Expected output:
```
Usage: kmp-app-generator [<options>] <output-folder>

  Generate a Kotlin Multiplatform Compose App project on disk.

Options:
  --name TEXT           Project name
  --id TEXT             Package ID (e.g. com.example.myapp)
  --android/--no-android  Include Android target
  --ios/--no-ios        Include iOS target (shared Compose UI)
  --desktop/--no-desktop  Include Desktop/JVM target
  --web/--no-web        Include Web/Wasm target
  --tests/--no-tests    Include sample tests
  --debug               Keep partially written folder on failure
  -h, --help            Show this message and exit
```

- [ ] **Step 4: Generate a test project with all defaults**

```bash
build/libs/kmp-app-generator /tmp/test-kmp-app --name "Test App" --id "com.example.testapp"
```

Expected: `Generated N files in: /tmp/test-kmp-app`

- [ ] **Step 5: Verify the generated project structure**

```bash
find /tmp/test-kmp-app -maxdepth 2 -type f | sort | head -30
```

Expected to see: `gradlew`, `gradlew.bat`, `settings.gradle.kts`, `build.gradle.kts`, files under `androidApp/`, `iosApp/`, `desktopApp/`, `webApp/`, `sharedUI/`

- [ ] **Step 6: Verify the output matches the web wizard**

Visit https://kmp.jetbrains.com/ in a browser, configure the same platforms (all on), download the zip, unzip it, and compare the directory structure with `/tmp/test-kmp-app`. The file lists should be identical.

- [ ] **Step 7: Clean up the test output**

```bash
rm -rf /tmp/test-kmp-app
```

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "docs: verify end-to-end generation matches web wizard output"
```
