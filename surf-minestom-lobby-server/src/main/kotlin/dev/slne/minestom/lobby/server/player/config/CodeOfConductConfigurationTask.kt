package dev.slne.minestom.lobby.server.player.config

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.server.codeofconduct.CodeOfConductService
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.network.packet.client.configuration.ClientAcceptCodeOfConductPacket
import net.minestom.server.network.packet.server.configuration.CodeOfConductPacket
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Sends the code of conduct and holds the configuration until the player accepts it.
 */
@Singleton
class CodeOfConductConfigurationTask @Inject constructor(
    private val codeOfConduct: CodeOfConductService,
) : ConfigurationTask {

    companion object {
        private val LOGGER = ComponentLogger.logger()
    }

    private val pending = ConcurrentHashMap<UUID, CompletableFuture<Unit>>()

    override fun run(context: ConfigurationContext) {
        if (!context.isFirstConfig) return

        if (!codeOfConduct.enabled) return
        val player = context.player
        val codeOfConduct = codeOfConduct.textFor(player.locale) ?: return

        val future = CompletableFuture<Unit>()
        val pendingFuture = pending.putIfAbsent(player.uuid, future) ?: future

        if (!player.isOnline) {
            pending.remove(player.uuid, future)
            return
        }

        if (pendingFuture === future) {
            player.sendPacket(CodeOfConductPacket(codeOfConduct))
        } else {
            LOGGER.warn(
                "Configuration ran again for {} while its code of conduct was still pending",
                player.username
            )
        }

        try {
            pendingFuture.join()
        } catch (_: CancellationException) {
            LOGGER.info("Player {} left before accepting the code of conduct", player.username)
            player.playerConnection.disconnect()
        }
    }

    fun handleDisconnect(event: PlayerDisconnectEvent) {
        pending.remove(event.player.uuid)?.cancel(true)
    }

    fun handleAcceptPacket(
        @Suppress("unused") packet: ClientAcceptCodeOfConductPacket,
        player: Player
    ) {
        val future = pending.remove(player.uuid)
        if (future == null) {
            LOGGER.warn("Player ${player.username} tried to accept code of conduct but was not in the pending list")
            player.playerConnection.disconnect()
            return
        }
        future.complete(Unit)
    }
}
