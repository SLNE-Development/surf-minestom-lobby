enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.lucko.me/")
    }
}

rootProject.name = "surf-minestom-lobby"

include(":surf-minestom-lobby-api")
include(":surf-minestom-lobby-server")

val luckPermsDir = file("vendor/LuckPerms")
if (!luckPermsDir.resolve("settings.gradle").isFile) {
    error(
        """
        vendor/LuckPerms is missing - it is generated, not checked in. Create it with:
          git submodule update --init --recursive
          cd vendor && ./gradlew applyPatches
        """.trimIndent()
    )
}

includeBuild(luckPermsDir) {
    dependencySubstitution {
        substitute(module("club.tesseract:luckperms-minestom")).using(project(":minestom"))
    }
}

includeBuild("vendor/spark-minestom") {
    dependencySubstitution {
        substitute(module("me.lucko:spark-minestom")).using(project(":"))
    }
}
