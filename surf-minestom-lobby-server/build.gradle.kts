import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator
import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import xyz.jpenilla.gremlin.gradle.ShadowGremlin

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.shadow)
    alias(libs.plugins.gremlim)

    id("dev.slne.surf.api.gradle.minestom-relocations") version "+"
    kotlin("kapt")
}

repositories {
    maven("https://reposilite.slne.dev/public") { name = "slne-repository-public" }
    maven("https://reposilite.slne.dev/releases") { name = "slne-repository-releases" }
    maven("https://repo.lucko.me/")
}

configurations.compileOnly {
    extendsFrom(configurations.runtimeDownload.get())
}

configurations.runtimeClasspath {
    exclude(group = "com.google.code.gson", module = "gson")
    exclude(group = "com.google.guava", module = "guava")
    exclude(group = "com.github.ben-manes.caffeine", module = "caffeine")
    exclude(group = "net.bytebuddy", module = "byte-buddy")
    exclude(group = "org.checkerframework", module = "checker-qual")
    exclude(group = "org.jetbrains", module = "annotations")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
}

dependencies {
    implementation(projects.surfMinestomLobbyApi)

    runtimeDownload(libs.minestom)
    runtimeDownload(libs.guice)
    runtimeDownload(libs.guice.assistedinject)
    implementation(libs.coroutines.core)
    implementation(libs.bundles.log4j)
    implementation(libs.slf4j.api)
    implementation(libs.terminal.console.appender)
    runtimeDownload(libs.fastutil)
    runtimeDownload(libs.polar)
    runtimeDownload(libs.npc)
    runtimeDownload(libs.brigadier)

    runtimeDownload(libs.komapper.annotation)
    runtimeDownload(libs.komapper.jdbc)
    runtimeDownload(libs.komapper.slf4j)
    ksp(libs.komapper.processor)
    runtimeDownload(libs.komapper.dialect.mariadb.jdbc)
    runtimeDownload(libs.komapper.dialect.postgresql.jdbc)
    runtimeDownload(libs.hikari)
    runtimeDownload(libs.mariadb.jdbc)
    runtimeDownload(libs.postgresql.jdbc)

    implementation(libs.fabric.mixin)
    annotationProcessor(libs.fabric.mixin)
    implementation(libs.mixin.extra)
    annotationProcessor(libs.mixin.extra)

    runtimeDownload(libs.configurate.yaml)
    runtimeDownload(libs.configurate.kotlin)
    implementation(libs.luckperms.minestom)
    implementation(libs.spark.minestom)

    runtimeDownload(libs.surf.api.minestom)
    runtimeDownload(libs.surf.redis.minestom) {
        artifact { classifier = "all" }
    }
    runtimeDownload(libs.surf.rabbitmq.minestom)
    runtimeDownload(libs.surf.core.minestom)
    runtimeDownload(libs.surf.settings.minestom)
    runtimeDownload(libs.surf.punish.minestom)
    runtimeDownload(libs.surf.chat.minestom)
    runtimeDownload(libs.surf.bitmap.provider.minestom)
    runtimeDownload(libs.surf.clan.minestom)
    runtimeDownload(libs.surf.transaction.minestom)
    runtimeDownload(libs.surf.queue.minestom)
    runtimeDownload(libs.surf.playtime.minestom)
    runtimeDownload(libs.surf.jumppad.minestom)
    runtimeDownload(libs.surf.content.creator.minestom)
    runtimeDownload(libs.surf.friends.minestom)
//    runtimeDownload(libs.surf.lobby.minestom)

    testImplementation(libs.minestom.testing)
    testImplementation(libs.brigadier)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.coroutines.test)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        javaParameters = true
        optIn.add("dev.slne.minestom.lobby.api.util.InternalMinestomLobbyApi")
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

tasks.writeDependencies {
    val patternField = SimpleRelocator::class.java.getDeclaredField("pattern")
        .apply { isAccessible = true }
    val shadedPatternField = SimpleRelocator::class.java.getDeclaredField("shadedPattern")
        .apply { isAccessible = true }

    val relocators = tasks.shadowJar.get().relocators.get()
    val simpleRelocators = relocators.filterIsInstance<SimpleRelocator>()
    require(simpleRelocators.size == relocators.size) {
        "Cannot mirror relocations onto $name: " +
                (relocators - simpleRelocators.toSet()).joinToString { it::class.java.name } +
                " are no SimpleRelocators"
    }

    simpleRelocators.forEach { relocator ->
        ShadowGremlin.relocate(
            this,
            patternField.get(relocator) as String,
            shadedPatternField.get(relocator) as String,
            relocator.includes.toSet(),
            relocator.excludes.toSet()
        )
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
