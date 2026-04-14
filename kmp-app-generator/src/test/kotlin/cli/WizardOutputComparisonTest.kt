package cli

import com.github.ajalt.clikt.core.parse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import wizard.BinaryFile
import wizard.DefaultComposeAppInfo
import wizard.generateComposeAppFiles
import java.io.File
import kotlin.test.assertTrue

/**
 * Compares CLI output against the reference output produced by the web wizard's
 * DefaultComposeAppInfo() generator — the canonical "what kmp.jetbrains.com generates".
 */
class WizardOutputComparisonTest {

    @Rule
    @JvmField
    val tmp = TemporaryFolder()

    @Test
    fun `cli output matches web wizard DefaultComposeAppInfo reference`() {
        // ── Reference: what the web wizard generates for its default ComposeApp ──
        val refDir = tmp.newFolder("wizard-reference")
        for (projectFile in DefaultComposeAppInfo().generateComposeAppFiles()) {
            val file = File(refDir, projectFile.path)
            file.parentFile?.mkdirs()
            if (projectFile is BinaryFile) {
                val resource = javaClass.getResourceAsStream("/binaries/${projectFile.resourcePath}")
                    ?: error("Binary resource not found: /binaries/${projectFile.resourcePath}")
                resource.use { it.copyTo(file.outputStream()) }
            } else {
                file.writeText(projectFile.content)
            }
        }

        // ── CLI: invoke with parameters that match DefaultComposeAppInfo defaults ──
        //   packageId = "org.company.app", name = "Multiplatform App", all platforms on
        val cliDir = tmp.root.resolve("cli-output") // must not exist yet
        KmpAppGenerator().parse(listOf(
            cliDir.absolutePath,
            "--name", "Multiplatform App",
            "--id",   "org.company.app"
        ))

        // ── Diff ──
        val report = diffDirectories(refDir, cliDir)
        assertTrue(report.isEmpty(), "CLI output differs from web wizard reference:\n\n$report")
    }

    // ── helpers ──────────────────────────────────────────────────────────────────

    private fun diffDirectories(ref: File, actual: File): String {
        val refFiles    = ref.walkTopDown().filter(File::isFile)
            .map { it.relativeTo(ref).path }.toSortedSet()
        val actualFiles = actual.walkTopDown().filter(File::isFile)
            .map { it.relativeTo(actual).path }.toSortedSet()

        return buildString {
            val onlyInRef    = refFiles - actualFiles
            val onlyInActual = actualFiles - refFiles
            val common       = refFiles.intersect(actualFiles)

            if (onlyInRef.isNotEmpty()) {
                appendLine("Missing from CLI output (present in web wizard):")
                onlyInRef.forEach { appendLine("  - $it") }
            }
            if (onlyInActual.isNotEmpty()) {
                appendLine("Extra in CLI output (not in web wizard):")
                onlyInActual.forEach { appendLine("  + $it") }
            }
            for (path in common) {
                val refContent    = File(ref,    path).readText()
                val actualContent = File(actual, path).readText()
                if (refContent != actualContent) {
                    appendLine("Content differs: $path")
                    // Show first differing line for quick diagnosis
                    val refLines    = refContent.lines()
                    val actualLines = actualContent.lines()
                    val diffLine = refLines.zip(actualLines).indexOfFirst { (a, b) -> a != b }
                    if (diffLine >= 0) {
                        appendLine("  ref    line ${diffLine + 1}: ${refLines[diffLine]}")
                        appendLine("  actual line ${diffLine + 1}: ${actualLines[diffLine]}")
                    }
                }
            }
        }.trim()
    }
}
