package dev.slne.minestom.lobby.server.lifecycle

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.codeofconduct.CodeOfConductService
import dev.slne.minestom.lobby.server.command.CommandService
import dev.slne.minestom.lobby.server.database.LobbyDatabase
import dev.slne.minestom.lobby.server.integration.miniplaceholders.MiniPlaceholdersService
import dev.slne.minestom.lobby.server.permission.PermissionLevelService
import dev.slne.minestom.lobby.server.upload.UploadService
import dev.slne.minestom.lobby.server.world.LobbyWorldService
import net.minestom.server.MinecraftServer.LOGGER

/**
 * Starts and stops the lobby's own services.
 *
 * The services start in two phases around the surf api, [startCore] before and
 * [startIntegrations] after it, and [stop] stops every started service in reverse start order.
 */
@Singleton
class ServerLifecycle @Inject constructor(
    database: LobbyDatabase,
    uploads: UploadService,
    codeOfConduct: CodeOfConductService,
    world: LobbyWorldService,
    permissionLevel: PermissionLevelService,
    miniPlaceholders: MiniPlaceholdersService,
    commands: CommandService,
) {
    /**
     * Started before the surf api, which loads the plugins: they read the lobby world while loading.
     */
    private val coreServices = listOf(database, uploads, codeOfConduct, world)

    /**
     * Started once the surf api runs: they use LuckPerms and the CommandAPI.
     */
    private val integrationServices = listOf(permissionLevel, miniPlaceholders, commands)

    private val started = ArrayDeque<LobbyService>()

    suspend fun startCore() = startAll(coreServices)

    suspend fun startIntegrations() = startAll(integrationServices)

    private suspend fun startAll(services: List<LobbyService>) {
        for (service in services) {
            LOGGER.debug("Starting {}.", service.serviceName)

            service.start()
            started.addLast(service)
        }
    }

    suspend fun stop() {
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
