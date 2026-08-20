package dev.slne.minestom.lobby.server.player.config

import com.google.inject.Singleton
import net.minestom.server.network.packet.server.configuration.FinishConfigurationPacket

/** Hands the player over to the play phase, the last step of the configuration. */
@Singleton
class JoinWorldTask : ConfigurationTask {

    @Suppress("UnstableApiUsage")
    override fun run(context: ConfigurationContext) {
        val player = context.player

        LobbyConfiguration.stopKeepAlive(player)
        player.setPendingOptions(context.spawningInstance, context.event.isHardcore)
        player.sendPacket(FinishConfigurationPacket())
    }
}
