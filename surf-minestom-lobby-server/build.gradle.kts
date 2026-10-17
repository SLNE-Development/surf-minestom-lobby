plugins {
    id("dev.slne.surf.api.gradle.minestom-server") version "+"
}

val internalPlugins = providers.gradleProperty("internalPlugins")
    .map(String::toBoolean)
    .getOrElse(false)

repositories {
    maven("https://reposilite.slne.dev/public") { name = "slne-repository-public" }
    maven("https://reposilite.slne.dev/releases") { name = "slne-repository-releases" }

    if (internalPlugins) {
        val githubUser = providers.gradleProperty("gpr.user")
            .orElse(providers.environmentVariable("GITHUB_ACTOR"))
        val githubToken = providers.gradleProperty("gpr.key")
            .orElse(providers.environmentVariable("GITHUB_TOKEN"))
        val reposiliteUser = providers.environmentVariable("REPOSILITE_USER")
        val reposiliteToken = providers.environmentVariable("REPOSILITE_TOKEN")

        if (!githubUser.isPresent || !githubToken.isPresent) {
            logger.warn(
                "internalPlugins is enabled but GitHub Packages credentials are missing. " +
                        "Set gpr.user/gpr.key in ~/.gradle/gradle.properties " +
                        "(or GITHUB_ACTOR/GITHUB_TOKEN in the environment), " +
                        "or build with -PinternalPlugins=false."
            )
        }

        if (!reposiliteUser.isPresent || !reposiliteToken.isPresent) {
            logger.warn(
                "internalPlugins is enabled but private reposilite credentials are missing. " +
                        "Set REPOSILITE_USER/REPOSILITE_TOKEN in the environment, " +
                        "or build with -PinternalPlugins=false."
            )
        }

        exclusiveContent {
            forRepository {
                maven("https://maven.pkg.github.com/SLNE-Development/surf-minestom-anticheat") {
                    name = "github-surf-minestom-anticheat"
                    credentials {
                        username = githubUser.orNull
                        password = githubToken.orNull
                    }
                }
            }
            filter { includeGroup(libs.surf.anticheat.minestom.get().group) }
        }

        exclusiveContent {
            forRepository {
                maven("https://reposilite.slne.dev/private") {
                    name = "slne-repository-private"
                    credentials {
                        username = reposiliteUser.orNull
                        password = reposiliteToken.orNull
                    }
                }
            }
            filter {
                includeGroup(libs.surf.ban.bypass.minestom.get().group)
            }
        }
    }
}

surfMinestomServerApi {
    mainClass("dev.slne.minestom.lobby.MainKt")

    // surf-api and the plugins are downloaded on the first start like every other library, which
    // keeps the server jar small
    downloadSurfModules = true

    withSignedChat()
    withLuckPerms()
    withSpark()
    withNpcLib()
    withPlayerVisibility()
    withConsole()
    withPlugins()
}

dependencies {
    // Also added by the Gradle plugin; declared here so that the plugin update check sees surf-api releases
    implementation(libs.surf.api.minestom.server)
    implementation(libs.guice)
    implementation(libs.polar)
    implementation(libs.mini.placeholders.minestom) {
        isTransitive = false
    }

    implementation(libs.komapper.annotation)
    implementation(libs.komapper.jdbc)
    implementation(libs.komapper.slf4j)
    ksp(libs.komapper.processor)
    implementation(libs.komapper.dialect.mariadb.jdbc)
    implementation(libs.komapper.dialect.postgresql.jdbc)
    implementation(libs.hikari)
    implementation(libs.mariadb.jdbc)
    implementation(libs.postgresql.jdbc)

    runtimeOnly(libs.surf.redis.minestom) {
        artifact { classifier = "all" }
    }
    runtimeOnly(libs.surf.rabbitmq.minestom)
    runtimeOnly(libs.surf.core.minestom)
    runtimeOnly(libs.surf.settings.minestom)
    runtimeOnly(libs.surf.punish.minestom)
    runtimeOnly(libs.surf.chat.minestom)
    runtimeOnly(libs.surf.bitmap.provider.minestom)
    runtimeOnly(libs.surf.clan.minestom)
    runtimeOnly(libs.surf.transaction.minestom)
    runtimeOnly(libs.surf.queue.minestom)
    runtimeOnly(libs.surf.playtime.minestom)
    runtimeOnly(libs.surf.jumppad.minestom)
    runtimeOnly(libs.surf.parkour.minestom)
    runtimeOnly(libs.surf.profile.minestom)
    runtimeOnly(libs.surf.content.creator.minestom)
    runtimeOnly(libs.surf.friends.minestom)
    runtimeOnly(libs.surf.social.minestom)
    runtimeOnly(libs.surf.tab.minestom)
    runtimeOnly(libs.surf.nametag.minestom)
    runtimeOnly(libs.surf.teleporter.minestom)
    runtimeOnly(libs.surf.trophy.minestom)
    runtimeOnly(libs.surf.lobby.minestom)

    // Embedded in the server jar, since the bootstrap downloads without credentials. Only the plugin
    // itself is embedded; the dependencies of ban bypass are downloaded like every other library.
    if (internalPlugins) {
        shaded(libs.surf.anticheat.minestom) { isTransitive = false }
        shaded(libs.surf.ban.bypass.minestom) { isTransitive = false }
        runtimeOnly(libs.surf.ban.bypass.minestom)
    }
}

val buildInfoResource = "surf-minestom-lobby-build.properties"
val hasGitDirectory = rootDir.resolve(".git").exists()

fun gitOutput(vararg arguments: String): Provider<String> = when {
    hasGitDirectory -> providers.exec {
        workingDir = rootDir
        commandLine("git", *arguments)
        isIgnoreExitValue = true
    }.standardOutput.asText.map(String::trim)

    else -> providers.provider { "" }
}

val buildCommit = providers.environmentVariable("GITHUB_SHA")
    .orElse(gitOutput("rev-parse", "HEAD"))
val buildBranch = providers.environmentVariable("GITHUB_REF_NAME")
    .orElse(gitOutput("rev-parse", "--abbrev-ref", "HEAD"))
val buildCommitTime = gitOutput("log", "-1", "--format=%cI")
val buildNumber = providers.environmentVariable("GITHUB_RUN_NUMBER")

val generateBuildInfo = tasks.register<WriteProperties>("generateBuildInfo") {
    description = "Writes the build metadata that /version reports."
    destinationFile = layout.buildDirectory.file("generated/build-info/$buildInfoResource")
    comment = "Generated by :surf-minestom-lobby-server:generateBuildInfo - do not edit."

    property("version", version.toString())
    property("commit", buildCommit.getOrElse(""))
    property("branch", buildBranch.getOrElse(""))
    property("commitTime", buildCommitTime.getOrElse(""))
    property("buildNumber", buildNumber.getOrElse(""))
}

sourceSets.main {
    resources.srcDir(generateBuildInfo.map { it.destinationFile.get().asFile.parentFile })
}

tasks.jar {
    archiveClassifier = "thin"
}

tasks.test {
    // Lets the surf api start without the Mixin agent of the server jar
    systemProperty("minestom.inside-test", "true")
}

// The server jar is the only distribution
tasks.named { it in setOf("distZip", "distTar", "shadowDistZip", "shadowDistTar") }.configureEach {
    enabled = false
}

// java-gradle-plugin, applied by the surf plugin, would publish a second publication under the
// coordinates of the server jar
gradlePlugin {
    isAutomatedPublishing = false
}

tasks.shadowJar {
    archiveClassifier = ""
    isZip64 = true
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

publishing {
    publications {
        create<MavenPublication>("lobbyServer") {
            artifact(tasks.shadowJar)
        }
    }
    repositories {
        maven("https://reposilite.slne.dev/private") {
            name = "slne-repository-private"
            credentials {
                username = providers.environmentVariable("REPOSILITE_USER").orNull
                password = providers.environmentVariable("REPOSILITE_TOKEN").orNull
            }
        }
    }
}
