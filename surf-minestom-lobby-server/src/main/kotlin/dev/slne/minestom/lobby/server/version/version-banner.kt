package dev.slne.minestom.lobby.server.version

import dev.slne.minestom.lobby.api.coroutine.minestomBlockingScope
import kotlinx.coroutines.launch
import net.minestom.server.MinecraftServer
import org.slf4j.Logger

fun logVersionBanner(
    versionService: LobbyVersionService,
    logger: Logger,
) {
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
                logger.info("This server is running the latest release.")

            LobbyVersionStatus.DevelopmentBuild ->
                logger.info("Development build - skipping the release check.")

            is LobbyVersionStatus.UpdateAvailable ->
                logger.warn(
                    "A newer Surf Minestom Lobby release is available: {} -> {}: {}",
                    buildInfo.version,
                    status.latestVersion,
                    LOBBY_DOWNLOAD_URL,
                )

            is LobbyVersionStatus.CheckFailed ->
                logger.warn(
                    "Could not check for a newer release: {}",
                    status.reason,
                )
        }
    }
}