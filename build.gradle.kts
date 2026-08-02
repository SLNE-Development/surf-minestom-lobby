plugins {
    application
    id("com.gradleup.shadow") version "9.6.1"
}

group = "dev.slne.minestom.lobby"

dependencies {
    implementation("net.minestom:minestom:2026.07.22-26.2")

    testImplementation("net.minestom:testing:2026.07.22-26.2")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.2")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "dev.slne.minestom.lobby.Main"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "dev.slne.minestom.lobby.Main"
    }
}
tasks.test {
    useJUnitPlatform()
}
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
    mergeServiceFiles()
    archiveClassifier.set("")
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}
