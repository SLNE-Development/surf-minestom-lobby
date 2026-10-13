package dev.slne.minestom.lobby.server.lifecycle

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.codeofconduct.CodeOfConductService
import dev.slne.minestom.lobby.server.database.LobbyDatabase
import dev.slne.minestom.lobby.server.integration.miniplaceholders.MiniPlaceholdersService
import dev.slne.minestom.lobby.server.permission.PermissionLevelService
import dev.slne.minestom.lobby.server.player.LobbyPlayerService
import dev.slne.minestom.lobby.server.upload.UploadService
import dev.slne.minestom.lobby.server.world.LobbyWorldService
import net.minestom.server.MinecraftServer.LOGGER
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Starts the lobby's own services in order and stops them in reverse.
 *
 * Runs as [LobbyFeature] inside the surf api, which provides everything else (commands, chat,
 * LuckPerms, spark, plugins, ...).
 */
@Singleton
class ServerLifecycle @Inject constructor(
    miniPlaceholders: MiniPlaceholdersService,
    permissionLevel: PermissionLevelService,
    database: LobbyDatabase,
    uploads: UploadService,
    codeOfConduct: CodeOfConductService,
    world: LobbyWorldService,
    players: LobbyPlayerService,
) {
    private val services = listOf(
        miniPlaceholders,
        permissionLevel,
        database,
        uploads,
        codeOfConduct,
        world,
        players,
    )

    private val started = ArrayDeque<LobbyService>()
    private val running = AtomicBoolean()

    suspend fun start() {
        check(running.compareAndSet(false, true)) {
            "Lobby services have already been started"
        }

        for (service in services) {
            LOGGER.debug("Starting {}.", service.serviceName)

            try {
                service.start()
            } catch (startupFailure: Throwable) {
                runCatching { stop() }.onFailure(startupFailure::addSuppressed)
                throw startupFailure
            }

            started.addLast(service)
        }
    }

    suspend fun stop() {
        if (!running.compareAndSet(true, false)) return

        var failure: Throwable? = null

        while (started.isNotEmpty()) {
            val service = started.removeLast()

            LOGGER.debug("Stopping {}.", service.serviceName)

            try {
                service.stop()
            } catch (currentFailure: Throwable) {
                LOGGER.error("Failed to stop {}.", service.serviceName, currentFailure)

                if (failure == null) {
                    failure = currentFailure
                } else {
                    failure.addSuppressed(currentFailure)
                }
            }
        }

        failure?.let { throw it }
    }
}
