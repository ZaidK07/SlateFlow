// PDFium 2.0.3 uses Kotlin 2.4; align the built-in compiler and Compose plugin.
buildscript {
    repositories { mavenCentral(); google() }
    dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10") }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}