package cli

import com.github.ajalt.clikt.core.parse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
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
import kotlin.test.assertTrue

/**
 * For every meaningful combination of CLI flags, generates:
 *   - a "reference" directory by calling generateComposeAppFiles() directly
 *     (equivalent to what the web wizard produces)
 *   - a "cli" directory by running KmpAppGenerator with the matching flags
 *
 * Asserts that both directories are byte-for-byte identical.
 */
class WizardOutputComparisonTest {

    @Rule
    @JvmField
    val tmp = TemporaryFolder()

    // ── test cases ─────────────────────────────────────────────────────────────

    data class Case(
        val label: String,
        val android: Boolean = true,
        val ios: Boolean = true,
        val desktop: Boolean = true,
        val web: Boolean = true,
        val tests: Boolean = true,
    )

    private val cases = listOf(
        // all platforms
        Case("all platforms"),
        Case("all platforms, no tests",         tests   = false),
        // single platform
        Case("android only",   ios=false, desktop=false, web=false),
        Case("ios only",       android=false, desktop=false, web=false),
        Case("desktop only",   android=false, ios=false,     web=false),
        Case("web only",       android=false, ios=false, desktop=false),
        // two platforms
        Case("android + ios",  desktop=false, web=false),
        Case("android + desktop", ios=false, web=false),
        Case("android + web",  ios=false, desktop=false),
        Case("ios + desktop",  android=false, web=false),
        Case("ios + web",      android=false, desktop=false),
        Case("desktop + web",  android=false, ios=false),
        // three platforms
        Case("android + ios + desktop",   web=false),
        Case("android + ios + web",       desktop=false),
        Case("android + desktop + web",   ios=false),
        Case("ios + desktop + web",       android=false),
    )

    // ── single test that runs all cases ────────────────────────────────────────

    @Test
    fun `cli output matches direct generateComposeAppFiles for all flag combinations`() {
        val failures = mutableListOf<String>()

        for (case in cases) {
            val refDir = tmp.newFolder("ref-${case.label.replace(' ', '-').replace('+', '_')}")
            val cliDir = tmp.root.resolve("cli-${case.label.replace(' ', '-').replace('+', '_')}")

            // Reference: call generateComposeAppFiles() directly with the same
            // ProjectInfo that the CLI would build for this combination of flags
            writeProjectFiles(buildInfo(case), refDir)

            // CLI
            KmpAppGenerator().parse(cliFlags(case, cliDir))

            // Diff
            val diff = diffDirectories(refDir, cliDir)
            if (diff.isNotEmpty()) {
                failures += "── [${case.label}] ──\n$diff"
            }
        }

        assertTrue(failures.isEmpty(),
            "Differences found in ${failures.size} case(s):\n\n${failures.joinToString("\n\n")}")
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    /** Mirrors the ProjectInfo construction in Main.kt exactly. */
    private fun buildInfo(c: Case): ProjectInfo {
        val platforms = buildSet {
            if (c.android)  add(ProjectPlatform.Android)
            if (c.ios)      add(ProjectPlatform.Ios)
            if (c.desktop)  add(ProjectPlatform.Jvm)
            if (c.web)      add(ProjectPlatform.Wasm)
        }
        val dependencies = buildSet {
            add(KotlinMultiplatformPlugin)
            if (c.android) {
                add(KotlinAndroidPlugin)
                add(AndroidKmpLibraryPlugin)
            }
            if (c.desktop) add(KotlinJvmPlugin)
            add(ComposeCompilerPlugin)
            add(ComposeMultiplatformPlugin)
            addAll(DefaultComposeLibraries)
            if (c.android) {
                add(AndroidApplicationPlugin)
                add(AndroidxActivityCompose)
            }
        }
        return ProjectInfo(
            packageId    = "org.company.app",
            name         = "Multiplatform App",
            moduleName   = "sharedUI",
            platforms    = platforms,
            dependencies = dependencies,
            addTests     = c.tests,
            type         = WizardType.ComposeApp
        )
    }

    private fun cliFlags(c: Case, outDir: File): List<String> = buildList {
        add(outDir.absolutePath)
        add("--name"); add("Multiplatform App")
        add("--id");   add("org.company.app")
        add(if (c.android) "--android"  else "--no-android")
        add(if (c.ios)     "--ios"      else "--no-ios")
        add(if (c.desktop) "--desktop"  else "--no-desktop")
        add(if (c.web)     "--web"      else "--no-web")
        add(if (c.tests)   "--tests"    else "--no-tests")
    }

    private fun writeProjectFiles(info: ProjectInfo, dir: File) {
        for (projectFile in info.generateComposeAppFiles()) {
            val file = File(dir, projectFile.path)
            file.parentFile?.mkdirs()
            if (projectFile is BinaryFile) {
                val res = javaClass.getResourceAsStream("/binaries/${projectFile.resourcePath}")
                    ?: error("Resource not found: /binaries/${projectFile.resourcePath}")
                res.use { it.copyTo(file.outputStream()) }
            } else {
                file.writeText(projectFile.content)
            }
        }
    }

    private fun diffDirectories(ref: File, actual: File): String {
        val refFiles    = ref.walkTopDown().filter(File::isFile)
            .map { it.relativeTo(ref).path }.toSortedSet()
        val actualFiles = actual.walkTopDown().filter(File::isFile)
            .map { it.relativeTo(actual).path }.toSortedSet()

        return buildString {
            (refFiles - actualFiles).forEach    { appendLine("  missing : $it") }
            (actualFiles - refFiles).forEach    { appendLine("  extra   : $it") }
            refFiles.intersect(actualFiles).forEach { path ->
                val r = File(ref,    path).readText()
                val a = File(actual, path).readText()
                if (r != a) {
                    appendLine("  differs : $path")
                    val rl = r.lines(); val al = a.lines()
                    val i = rl.zip(al).indexOfFirst { (x, y) -> x != y }
                    if (i >= 0) {
                        appendLine("    ref    L${i+1}: ${rl[i]}")
                        appendLine("    actual L${i+1}: ${al[i]}")
                    }
                }
            }
        }.trim()
    }
}
