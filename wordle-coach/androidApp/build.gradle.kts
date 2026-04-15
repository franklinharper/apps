import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.franklinharper.wordlecoach.androidApp"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        targetSdk = 36

        applicationId = "com.franklinharper.wordlecoach.androidApp"
        versionCode = 1
        versionName = "1.0.0"

        // Read the Anthropic API key from local.properties.
        // Add the following line to local.properties (never commit it):
        //   anthropic.api.key=sk-ant-...
        val localProps = java.util.Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) localPropsFile.inputStream().use { localProps.load(it) }
        buildConfigField("String", "ANTHROPIC_API_KEY",
            "\"${localProps.getProperty("anthropic.api.key", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":sharedUI"))
    implementation(libs.androidx.activityCompose)
    implementation(libs.anthropic.java)
}
