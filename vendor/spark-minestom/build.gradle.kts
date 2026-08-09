plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

group = "me.lucko"
version = libs.versions.spark.get()

dependencies {
    api(libs.spark.common)

    compileOnly(libs.minestom)
    compileOnly(libs.slf4j.api)
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        javaParameters = true
    }
}

tasks.processResources {
    val pluginVersion = project.version.toString()
    inputs.property("pluginVersion", pluginVersion)

    filesMatching("me/lucko/spark/minestom/spark-minestom.properties") {
        expand("version" to pluginVersion)
    }
}
