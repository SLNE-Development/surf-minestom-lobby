package dev.slne.minestom.lobby.server.player.handler

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.api.instance.LobbyInstance
import dev.slne.minestom.lobby.api.player.lobbyPlayer
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerGameModeRequestEvent
import net.minestom.server.instance.InstanceContainer

@Singleton
class LobbyPlayerHandler @Inject constructor(
    @LobbyInstance
    private val lobbyInstance: InstanceContainer,
    private val config: ServerConfig,
) {

    fun initialize(eventNode: EventNode<Event>) {
        with(eventNode) {
            addListener(::handlePlayerGameModeRequest)
            addListener(::handlePlayerConfigurationEvent)
        }
    }

    private fun handlePlayerConfigurationEvent(event: AsyncPlayerConfigurationEvent) {
        event.spawningInstance = lobbyInstance
        event.player.respawnPoint = config.spawn.toPos()
    }

    private fun handlePlayerGameModeRequest(event: PlayerGameModeRequestEvent) {
        val permission = LobbyPermissions.gamemodeSwitcher(event.requestedGameMode)
        val player = event.lobbyPlayer
        if (player.hasPermission(permission)) {
            player.gameMode = event.requestedGameMode
        }
    }
}