import java.security.MessageDigest

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
    testImplementation(kotlin("test-junit"))
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    main {
        kotlin {
            srcDir("kmp-web-wizard/src/commonMain/kotlin")
        }
        resources {
            srcDir("kmp-web-wizard/src/commonMain/resources")
        }
    }
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
        val libsDir = layout.buildDirectory.dir("libs").get().asFile
        val scriptFile = File(libsDir, "kmp-app-generator")
        scriptFile.writeText(
            "#!/bin/bash\nexec java -jar \"\$(dirname \"\$0\")/kmp-app-generator-all.jar\" \"\$@\"\n"
        )
        scriptFile.setExecutable(true)
        println("Wrapper script: ${scriptFile.absolutePath}")

        // Keep dist/ in sync so the session-start hook can skip Gradle on unchanged source.
        val projectDir = layout.projectDirectory.asFile
        val distDir = File(projectDir, "dist").also { it.mkdirs() }

        // Copy the fat JAR
        val jar = File(libsDir, "kmp-app-generator-all.jar")
        jar.copyTo(File(distDir, "kmp-app-generator-all.jar"), overwrite = true)

        // Compute hash: all src/ files + build.gradle.kts + kmp-web-wizard submodule commit
        val hashInput = buildString {
            File(projectDir, "src").walkTopDown()
                .filter { it.isFile }
                .sortedBy { it.path }
                .forEach { f ->
                    val digest = MessageDigest.getInstance("SHA-256")
                        .digest(f.readBytes())
                    append(digest.joinToString("") { b -> "%02x".format(b) })
                    append("  ${f.relativeTo(projectDir).path}\n")
                }
            val buildFile = File(projectDir, "build.gradle.kts")
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(buildFile.readBytes())
            append(digest.joinToString("") { b -> "%02x".format(b) })
            append("  build.gradle.kts\n")
            val submoduleHead = ProcessBuilder("git", "rev-parse", "HEAD")
                .directory(File(projectDir, "kmp-web-wizard"))
                .start()
                .inputStream.bufferedReader().readText().trim()
            append("$submoduleHead  kmp-web-wizard\n")
        }
        val finalHash = MessageDigest.getInstance("SHA-256")
            .digest(hashInput.toByteArray())
            .joinToString("") { b -> "%02x".format(b) }
        File(distDir, "source.hash").writeText(finalHash)

        println("dist/ updated (hash: ${finalHash.take(16)}…)")
    }
}
