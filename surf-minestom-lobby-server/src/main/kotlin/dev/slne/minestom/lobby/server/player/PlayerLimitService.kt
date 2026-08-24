package dev.slne.minestom.lobby.server.player

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.ConnectionManager
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.api.player.PlayerLimit
import dev.slne.minestom.lobby.server.config.ServerConfig
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.server.ServerListPingEvent
import net.minestom.server.ping.Status

/**
 * Holds the server's player limit and reports it on the server list.
 */
@Singleton
class PlayerLimitService @Inject constructor(config: ServerConfig) : PlayerLimit, EventRegistrar {

    @Volatile
    private var limit = requirePositive(config.maxPlayers)

    override var maxPlayers: Int
        get() = limit
        set(value) {
            limit = requirePositive(value)
        }

    override val playerCount: Int
        get() = ConnectionManager.onlinePlayerCount + ConnectionManager.configPlayers.size

    override fun register(node: EventNode<Event>) {
        node.addListener(::reportPlayerCounts)
    }

    private fun reportPlayerCounts(event: ServerListPingEvent) {
        val playerInfo = event.status.playerInfo() ?: return

        event.status = Status.builder(event.status)
            .playerInfo(
                Status.PlayerInfo.builder(playerInfo)
                    .onlinePlayers(playerCount)
                    .maxPlayers(maxPlayers)
                    .build()
            )
            .build()
    }

    private fun requirePositive(value: Int): Int {
        require(value > 0) { "max-players must be greater than zero, was $value" }
        return value
    }
}
