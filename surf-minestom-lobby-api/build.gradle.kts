plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnlyApi(libs.minestom)
    compileOnlyApi(libs.guice)
}

kotlin {
    jvmToolchain(25)
}