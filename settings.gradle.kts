enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://reposilite.slne.dev/releases")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven("https://reposilite.slne.dev/public") { name = "slne-repository-public" }
        maven("https://reposilite.slne.dev/releases") { name = "slne-repository-releases" }
    }
}

rootProject.name = "surf-minestom-lobby"

include(":surf-minestom-lobby-server")
