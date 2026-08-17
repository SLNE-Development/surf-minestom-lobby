import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.shadow)

    id("dev.slne.surf.api.gradle.minestom-relocations") version "+"
    kotlin("kapt")
}

repositories {
    mavenCentral()
    maven("https://repo.lucko.me/")
//    maven("https://repo.hypera.dev/snapshots/")
    maven("https://reposilite.slne.dev/public") { name = "slne-repository-public" }
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
    implementation(libs.polar)

    implementation(libs.komapper.annotation)
    implementation(libs.komapper.jdbc)
    runtimeOnly(libs.komapper.slf4j)
    ksp(libs.komapper.processor)
    implementation(libs.komapper.dialect.mariadb.jdbc)
    implementation(libs.komapper.dialect.postgresql.jdbc)
    implementation(libs.hikari)
    runtimeOnly(libs.mariadb.jdbc)
    runtimeOnly(libs.postgresql.jdbc)

    implementation(libs.fabric.mixin)
    annotationProcessor(libs.fabric.mixin)
    implementation(libs.mixin.extra)
    annotationProcessor(libs.mixin.extra)

    implementation(libs.configurate.yaml)
    implementation(libs.configurate.kotlin)
    implementation(libs.luckperms.minestom)
    implementation(libs.spark.minestom)

    runtimeOnly(libs.surf.api.minestom)
    runtimeOnly(libs.surf.redis.minestom)
    runtimeOnly(libs.surf.rabbitmq.minestom)
    runtimeOnly(libs.surf.core.minestom)
//    runtimeOnly(libs.surf.chat.minestom)
//    runtimeOnly(libs.surf.clan.minestom)
//    runtimeOnly(libs.surf.lobby.minestom)

    testImplementation(libs.minestom.testing)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.coroutines.test)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        javaParameters = true
    }
}

val serverMainClass = "dev.slne.minestom.lobby.MainKt"

application {
    mainClass = serverMainClass
}

tasks.jar {
    archiveClassifier = "thin"
}

tasks.shadowJar {
    archiveClassifier = ""
    isZip64 = true
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
    transform<Log4j2PluginsCacheFileTransformer>()

    manifest {
        attributes["Main-Class"] = serverMainClass
        attributes["Launcher-Agent-Class"] =
            "dev.slne.minestom.lobby.server.instrumentation.LobbyAgent"

        attributes["Can-Redefine-Classes"] = "true"
        attributes["Can-Retransform-Classes"] = "true"

        attributes["Multi-Release"] = "true"

        attributes(
            mapOf(
                "Implementation-Version" to libs.versions.asm.get()
            ),
            "org/objectweb/asm/"
        )
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
