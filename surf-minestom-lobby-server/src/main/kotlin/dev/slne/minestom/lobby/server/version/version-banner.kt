package dev.slne.minestom.lobby.server.version

import dev.slne.minestom.lobby.api.coroutine.minestomBlockingScope
import kotlinx.coroutines.launch
import net.minestom.server.MinecraftServer
import org.slf4j.Logger

/**
 * Logs the running build and, once the check has answered, how far behind it is.
 */
fun logVersionBanner(versionService: LobbyVersionService, logger: Logger) {
    val buildInfo = versionService.buildInfo

    logger.info(
        "This server is running Surf Minestom Lobby version {}{} (MC {}, protocol {}).",
        buildInfo.displayVersion,
        buildInfo.commitTime?.let { " ($it)" } ?: "",
        MinecraftServer.VERSION_NAME,
        MinecraftServer.PROTOCOL_VERSION,
    )

    minestomBlockingScope.launch {
        when (val status = versionService.status()) {
            LobbyVersionStatus.UpToDate ->
                logger.info("This server is running the latest build.")

            LobbyVersionStatus.DevelopmentBuild ->
                logger.info("Development build - skipping the build check.")

            is LobbyVersionStatus.Behind -> logger.warn(
                "This server is {}{} build(s) behind. The latest build is {}: {}",
                if (status.atLeast) "at least " else "",
                status.builds,
                status.latestBuildNumber,
                LOBBY_DOWNLOAD_URL,
            )

            is LobbyVersionStatus.CheckFailed ->
                logger.warn("Could not check for newer builds: {}", status.reason)
        }
    }
}
