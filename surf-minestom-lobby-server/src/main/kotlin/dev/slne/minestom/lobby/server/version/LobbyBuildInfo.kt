package dev.slne.minestom.lobby.server.version

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.Properties

private const val BUILD_INFO_RESOURCE = "surf-minestom-lobby-build.properties"
private const val SHORT_COMMIT_LENGTH = 7

data class LobbyBuildInfo(
    val version: String,
    val commit: String?,
    val branch: String?,
    val commitTime: Instant?,
    val buildNumber: Int?,
) {
    val shortCommit: String? = commit?.take(SHORT_COMMIT_LENGTH)

    val displayVersion: String = buildString {
        append(version)
        buildNumber?.let { append('-').append(it) }
        branch?.let { append('-').append(it) }
        shortCommit?.let { append('@').append(it) }
    }

    companion object {
        val current: LobbyBuildInfo by lazy(::readBuildInfo)

        private fun readBuildInfo(): LobbyBuildInfo {
            val properties = readProperties()

            return LobbyBuildInfo(
                version = properties.value("version") ?: "unknown",
                commit = properties.value("commit"),
                branch = properties.value("branch"),
                commitTime = properties.value("commitTime")?.toInstantOrNull(),
                buildNumber = properties.value("buildNumber")?.toIntOrNull(),
            )
        }

        private fun readProperties(): Properties = Properties().apply {
            val stream = LobbyBuildInfo::class.java.classLoader
                .getResourceAsStream(BUILD_INFO_RESOURCE)
                ?: return@apply

            stream.use { runCatching { load(it) } }
        }

        private fun Properties.value(key: String): String? =
            getProperty(key)?.trim()?.takeIf(String::isNotEmpty)

        private fun String.toInstantOrNull(): Instant? =
            try {
                OffsetDateTime.parse(this).toInstant()
            } catch (_: DateTimeParseException) {
                null
            }
    }
}
