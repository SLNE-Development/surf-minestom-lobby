package dev.slne.minestom.lobby.server.player.config

import com.google.inject.Singleton
import net.minestom.server.network.packet.server.configuration.ResetChatPacket
import net.minestom.server.network.packet.server.configuration.UpdateEnabledFeaturesPacket

/** Sends the feature flags the configuration event collected, before the registry negotiation. */
@Singleton
class EnabledFeaturesTask : ConfigurationTask {

    override fun run(context: ConfigurationContext) {
        val event = context.event

        context.player.sendPacket(
            UpdateEnabledFeaturesPacket(event.featureFlags.map { it.name() })
        )

        if (event.willClearChat()) {
            context.player.sendPacket(ResetChatPacket())
        }
    }
}
