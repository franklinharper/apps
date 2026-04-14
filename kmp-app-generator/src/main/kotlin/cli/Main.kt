package cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.main as cliktMain
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

class KmpAppGenerator : CliktCommand("kmp-app-generator") {
    override fun help(context: Context) =
        "Generate a Kotlin Multiplatform Compose App project on disk."

    private val outputFolder by argument(help = "Directory to create (must not already exist)")
    private val projectName by option("--name", help = "Project name").default("Multiplatform App")
    private val packageId by option("--id", help = "Package ID (e.g. com.example.myapp)").default("org.company.app")
    private val android by option("--android", help = "Include Android target").flag("--no-android", default = true)
    private val ios by option("--ios", help = "Include iOS target (shared Compose UI)").flag("--no-ios", default = true)
    private val desktop by option("--desktop", help = "Include Desktop/JVM target").flag("--no-desktop", default = true)
    private val web by option("--web", help = "Include Web/Wasm target").flag("--no-web", default = true)
    private val tests by option("--tests", help = "Include sample tests").flag("--no-tests", default = true)
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

fun main(args: Array<String>) = KmpAppGenerator().cliktMain(args)
