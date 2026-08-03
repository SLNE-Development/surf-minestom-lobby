plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(projects.surfMinestomLobbyApi)

    implementation(libs.minestom)
    implementation(libs.guice)
    implementation(libs.coroutines.core)
    implementation(libs.bundles.log4j)

    implementation(libs.configurate.yaml)
    implementation(libs.configurate.kotlin)


//    runtimeOnly(libs.surf.api.minestom)
//    runtimeOnly(libs.surf.redis.minestom)
//    runtimeOnly(libs.surf.rabbitmq.minestom)
//    runtimeOnly(libs.surf.lobby.minestom)

    testImplementation(libs.minestom.testing)
    testImplementation(libs.junit.jupiter)
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass = "dev.slne.minestom.lobby.MainKt"
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()

    manifest {
        attributes["Main-Class"] = "dev.slne.minestom.lobby.MainKt"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}