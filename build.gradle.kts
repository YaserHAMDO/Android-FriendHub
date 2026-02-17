plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    alias(libs.plugins.daggerHilt) apply false
    alias(libs.plugins.org.jetbrains.kotlin.kapt) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
}

tasks.register("clean", Delete::class) {
    delete(project.layout.buildDirectory)
}