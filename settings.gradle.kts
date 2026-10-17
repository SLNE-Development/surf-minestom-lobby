enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://reposilite.slne.dev/releases")
    }
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
