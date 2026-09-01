package dev.slne.minestom.lobby.server.player

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.PacketListenerManager
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.api.player.event.PlayerToggleFlightEvent
import dev.slne.minestom.lobby.api.player.lobbyPlayer
import dev.slne.minestom.lobby.server.config.ServerConfig
import dev.slne.minestom.lobby.server.permission.LobbyPermissions
import dev.slne.minestom.lobby.server.player.config.AwaitSettingsTask
import dev.slne.minestom.lobby.server.player.config.CodeOfConductConfigurationTask
import dev.slne.minestom.lobby.server.util.setConfigurationListener
import dev.slne.minestom.lobby.server.util.setPlayListener
import dev.slne.minestom.lobby.server.world.LobbyWorldService
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerGameModeRequestEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.listener.AbilitiesListener
import net.minestom.server.network.packet.client.play.ClientPlayerAbilitiesPacket
import net.minestom.server.network.packet.client.play.ClientQueryEntityNbtPacket
import net.minestom.server.network.packet.server.play.PlayerAbilitiesPacket
import sun.security.krb5.internal.KDCOptions.with
import kotlin.experimental.and

@Singleton
class LobbyPlayerListener @Inject constructor(
    private val world: LobbyWorldService,
    private val config: ServerConfig,
    private val codeOfConduct: CodeOfConductConfigurationTask,
    private val awaitSettings: AwaitSettingsTask,
) : EventRegistrar {

    override fun register(node: EventNode<Event>) {
        with(node) {
            addListener(::handleJoin)
            addListener(::handleDisconnect)
            addListener(awaitSettings::handleSettingsChange)
            addListener(::handlePlayerConfiguration)
            addListener(::handleGameModeRequest)
        }

        with(PacketListenerManager) {
            setPlayListener(::handlePlayerAbilities)
            setPlayListener(::handleQueryEntityNbtPacket)
            setConfigurationListener(codeOfConduct::handleAcceptPacket)
        }
    }

    private fun handleDisconnect(event: PlayerDisconnectEvent) {
        awaitSettings.handleDisconnect(event)
        codeOfConduct.handleDisconnect(event)

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
        val player = event.lobbyPlayer

        if (player.hasPermission(permission)) {
            player.gameMode = event.requestedGameMode
        }
    }

    private fun handlePlayerAbilities(packet: ClientPlayerAbilitiesPacket, player: Player) {
        val isFlying = packet.flags and PlayerAbilitiesPacket.FLAG_FLYING != 0.toByte()
        if (player.isAllowFlying && player.isFlying != isFlying) {
            val event = PlayerToggleFlightEvent(player.requireLobbyPlayerImpl(), isFlying)
            EventDispatcher.call(event)
            if (event.isCancelled) {
                player.refreshAbilities()
                return
            }
        }

        AbilitiesListener.listener(packet, player)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleQueryEntityNbtPacket(packet: ClientQueryEntityNbtPacket, player: Player) {
        // No op
    }
}