package dev.slne.minestom.lobby.server.lifecycle

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.chat.ChatFormatService
import dev.slne.minestom.lobby.server.chat.ChatService
import dev.slne.minestom.lobby.server.command.CommandService
import dev.slne.minestom.lobby.server.command.commandapi.MinestomCommandAPIService
import dev.slne.minestom.lobby.server.event.LobbyEventService
import dev.slne.minestom.lobby.server.integration.luckperms.LuckPermsService
import dev.slne.minestom.lobby.server.integration.spark.SparkService
import dev.slne.minestom.lobby.server.permission.PermissionLevelService
import dev.slne.minestom.lobby.server.player.LobbyPlayerService
import dev.slne.minestom.lobby.server.world.LobbyWorldService
import net.minestom.server.MinecraftServer.LOGGER
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class ServerLifecycle @Inject constructor(
    luckPerms: LuckPermsService,
    spark: SparkService,
    permissionLevel: PermissionLevelService,
    world: LobbyWorldService,
    players: LobbyPlayerService,
    commandApi: MinestomCommandAPIService,
    commands: CommandService,
    chat: ChatService,
    chatFormat: ChatFormatService,
    events: LobbyEventService,
) {
    private val services = orderedLobbyServices(
        events,
        luckPerms,
        spark,
        permissionLevel,
        world,
        players,
        commandApi,
        commands,
        chat,
        chatFormat,
    )

    private val started = ArrayDeque<LobbyService>()
    private val running = AtomicBoolean()

    suspend fun start() {
        check(running.compareAndSet(false, true)) {
            "Core server components have already been started"
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

internal fun orderedLobbyServices(
    events: LobbyService,
    luckPerms: LobbyService,
    spark: LobbyService,
    permissionLevel: LobbyService,
    world: LobbyService,
    players: LobbyService,
    commandApi: LobbyService,
    commands: LobbyService,
    chat: LobbyService,
    chatFormat: LobbyService,
): List<LobbyService> = listOf(
    events,
    luckPerms,
    spark,
    permissionLevel,
    world,
    players,
    commandApi,
    commands,
    chat,
    chatFormat,
)
