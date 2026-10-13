import org.gradle.api.tasks.testing.logging.TestExceptionFormat

buildscript {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://reposilite.slne.dev/releases")
    }
    dependencies {
        classpath("dev.slne.surf.api:surf-api-gradle-plugin:+")
    }
}

val projectVersion = providers.gradleProperty("projectVersion").get()

allprojects {
    group = "dev.slne.minestom.lobby"
    version = projectVersion

    repositories {
        mavenLocal()
    }
}

subprojects {
    tasks.withType<Test>().configureEach {
        systemProperty("projectVersion", project.version.toString())

        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            showStackTraces = true
            showCauses = true
        }
    }
}
