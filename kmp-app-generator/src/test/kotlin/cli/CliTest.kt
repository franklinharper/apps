package cli

import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.parse
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
