plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnlyApi(libs.minestom)
    compileOnlyApi(libs.guice)
    compileOnlyApi(libs.guice.assistedinject)
    compileOnlyApi(libs.coroutines.core)
    compileOnlyApi(libs.bundles.log4j)
    compileOnlyApi(libs.fastutil)
    compileOnlyApi(libs.npc)

    compileOnlyApi(libs.brigadier)
    testApi(libs.brigadier)
}

kotlin {
    jvmToolchain(25)
}

publishing {
    repositories {
        maven("https://reposilite.slne.dev/releases/") {
            name = "slne-repository-releases"
            credentials {
                username = System.getenv("SLNE_RELEASES_REPO_USERNAME")
                password = System.getenv("SLNE_RELEASES_REPO_PASSWORD")
            }
        }
    }

    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}