import org.gradle.api.tasks.testing.logging.TestExceptionFormat

val projectVersion = providers.gradleProperty("projectVersion").get()

allprojects {
    group = "dev.slne.minestom.lobby"
    version = projectVersion
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
