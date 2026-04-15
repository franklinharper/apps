plugins {
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    // AGP plugins omitted: Google Maven is not accessible in this build environment.
    // alias(libs.plugins.kotlin.android).apply(false)
    // alias(libs.plugins.android.kmp.library).apply(false)
    // alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.compose.multiplatform).apply(false)
}
