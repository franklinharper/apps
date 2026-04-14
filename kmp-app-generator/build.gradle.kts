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
        val libsDir = layout.buildDirectory.dir("libs").get().asFile
        val scriptFile = File(libsDir, "kmp-app-generator")
        scriptFile.writeText(
            "#!/bin/bash\nexec java -jar \"\$(dirname \"\$0\")/kmp-app-generator-all.jar\" \"\$@\"\n"
        )
        scriptFile.setExecutable(true)
        println("Wrapper script: ${scriptFile.absolutePath}")
    }
}
