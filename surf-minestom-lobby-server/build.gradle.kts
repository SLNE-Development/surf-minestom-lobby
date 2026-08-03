import sun.jvmstat.monitor.MonitoredVmUtil.mainClass

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
//    maven("https://repo.hypera.dev/snapshots/")
}

dependencies {
    implementation(projects.surfMinestomLobbyApi)

    implementation(libs.minestom)
    implementation(libs.guice)
    implementation(libs.coroutines.core)
    implementation(libs.bundles.log4j)

    implementation(libs.configurate.yaml)
    implementation(libs.configurate.kotlin)
    implementation(libs.luckperms.minestom)


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
        attributes["Launcher-Agent-Class"] = "me.lucko.luckperms.minestom.dependencies.LuckPermsAgent"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}