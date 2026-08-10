package dev.slne.minestom.lobby.server.player

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.api.player.lobbyPlayer
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import dev.slne.minestom.lobby.server.world.LobbyWorldService
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerGameModeRequestEvent

@Singleton
class LobbyPlayerListener @Inject constructor(
    private val world: LobbyWorldService,
    private val config: ServerConfig,
) : EventRegistrar {

    override fun register(node: EventNode<Event>) {
        with(node) {
            addListener(::handlePlayerConfiguration)
            addListener(::handleGameModeRequest)
        }
    }

    private fun handlePlayerConfiguration(event: AsyncPlayerConfigurationEvent) {
        event.spawningInstance = world.instance
        event.player.respawnPoint = config.spawn.toPos()
        event.player.gameMode = config.defaultGameMode
    }

    private fun handleGameModeRequest(event: PlayerGameModeRequestEvent) {
        val permission = LobbyPermissions.gamemodeSwitcher(event.requestedGameMode)
        val player = event.lobbyPlayer

        if (player.hasPermission(permission)) {
            player.gameMode = event.requestedGameMode
        }
    }
}