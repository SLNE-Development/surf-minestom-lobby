package dev.slne.minestom.lobby.server.player.config

import com.google.inject.Singleton
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.MinecraftServer
import net.minestom.server.ServerFlag
import net.minestom.server.network.packet.server.configuration.SelectKnownPacksPacket
import net.minestom.server.registry.Registries
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Negotiates the known packs and sends the registry data, the same way Minestom does it inside
 * `ConnectionManager#doConfiguration`.
 */
@Singleton
class SynchronizeRegistriesTask : ConfigurationTask {

    private companion object {
        val LOGGER = ComponentLogger.logger()
    }

    @Suppress("UnstableApiUsage")
    override fun run(context: ConfigurationContext) {
        if (!context.event.willSendRegistryData()) return

        val player = context.player
        val response = player.playerConnection
            .requestKnownPacks(listOf(SelectKnownPacksPacket.MINECRAFT_CORE))

        val knownPacks = try {
            response.get(ServerFlag.KNOWN_PACKS_RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            LOGGER.warn("Player {} failed to respond to known packs query", player.username)
            player.playerConnection.disconnect()
            return
        }

        val excludeVanilla = knownPacks.contains(SelectKnownPacksPacket.MINECRAFT_CORE)

        player.sendPackets(
            Registries.registryDataPackets(MinecraftServer.getRegistries(), excludeVanilla)
        )
        MinecraftServer.getConnectionManager().sendRegistryTags(player)
    }
}
