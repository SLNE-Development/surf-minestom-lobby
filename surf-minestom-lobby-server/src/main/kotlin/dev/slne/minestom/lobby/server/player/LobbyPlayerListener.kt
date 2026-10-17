package dev.slne.minestom.lobby.server.player

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import dev.slne.minestom.lobby.server.util.setPlayListener
import dev.slne.minestom.lobby.server.world.LobbyWorldService
import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.api.minestom.extension.PacketListenerManager
import dev.slne.surf.api.minestom.extension.addListener
import dev.slne.surf.api.minestom.permission.hasPermission
import dev.slne.surf.api.minestom.player.event.AsyncPlayerCountEvent
import dev.slne.surf.api.minestom.player.event.PlayerJoinEvent
import dev.slne.surf.api.minestom.player.event.PlayerQuitEvent
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerGameModeRequestEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.network.packet.client.play.ClientQueryEntityNbtPacket

@Singleton
class LobbyPlayerListener @Inject constructor(
    private val world: LobbyWorldService,
    private val config: ServerConfig,
) : EventRegistrar {

    private companion object {
        val SERVER_FULL = text("Der Server ist voll.", NamedTextColor.RED)
    }

    override fun register(node: EventNode<Event>) {
        with(node) {
            addListener(::handleJoin)
            addListener(::handleDisconnect)
            addListener(::handlePlayerConfiguration)
            addListener(::handleGameModeRequest)
            addListener(::handlePlayerCount)

            // The lobby announces neither joins nor quits, unlike the surf api by default
            addListener<PlayerJoinEvent> { event -> event.joinMessage = null }
            addListener<PlayerQuitEvent> { event -> event.quitMessage = null }
        }

        PacketListenerManager.setPlayListener(::handleQueryEntityNbtPacket)
    }

    private fun handleDisconnect(event: PlayerDisconnectEvent) {
        if (config.logConnections) {
            MinecraftServer.LOGGER.info("Player {} disconnected", event.player.username)
        }
    }

    private fun handleJoin(event: PlayerSpawnEvent) {
        if (!event.isFirstSpawn) return

        if (config.logConnections) {
            MinecraftServer.LOGGER.info("Player {} joined", event.player.username)
        }
    }

    private fun handlePlayerConfiguration(event: AsyncPlayerConfigurationEvent) {
        event.spawningInstance = world.instance
        event.player.respawnPoint = config.spawn.toPos()
        event.player.gameMode = config.defaultGameMode
    }

    private fun handleGameModeRequest(event: PlayerGameModeRequestEvent) {
        val permission = LobbyPermissions.gamemodeSwitcher(event.requestedGameMode)
        val player = event.player

        if (player.hasPermission(permission)) {
            player.gameMode = event.requestedGameMode
        }
    }

    private fun handlePlayerCount(event: AsyncPlayerCountEvent) {
        if (event.result == AsyncPlayerCountEvent.Result.KICK_FULL) {
            event.kickMessage = SERVER_FULL
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleQueryEntityNbtPacket(packet: ClientQueryEntityNbtPacket, player: Player) {
        // No op
    }
}
