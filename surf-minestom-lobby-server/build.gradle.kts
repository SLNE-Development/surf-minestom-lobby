import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
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
    implementation(libs.guice.assistedinject)
    implementation(libs.coroutines.core)
    implementation(libs.bundles.log4j)
    implementation(libs.terminal.console.appender)
    implementation(libs.fastutil)

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
    compilerOptions {
        javaParameters = true
    }
}

application {
    mainClass = "dev.slne.minestom.lobby.MainKt"
}

tasks.shadowJar {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
    transform<Log4j2PluginsCacheFileTransformer>()

    manifest {
        attributes["Main-Class"] = "dev.slne.minestom.lobby.MainKt"
        attributes["Launcher-Agent-Class"] = "me.lucko.luckperms.minestom.dependencies.LuckPermsAgent"
        attributes["Multi-Release"] = "true"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}