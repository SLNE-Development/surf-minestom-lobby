package dev.slne.minestom.lobby.server.command.commandapi.brigadier

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.server.command.commandapi.MinestomCommandAPIPlatform
import dev.slne.minestom.lobby.server.mixin.setPacket
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerPacketOutEvent
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket

/**
 * Replaces the command tree Minestom sends with one that also contains the CommandAPI's commands.
 *
 * Minestom builds its packet from its own graph, which no longer holds these commands, so they are
 * appended here from the Brigadier registrations.
 */
@Singleton
class DeclareCommandsListener @Inject constructor() : EventRegistrar {

    override fun register(node: EventNode<Event>) {
        node.addListener(::handlePacketOut)
    }

    @Suppress("UnstableApiUsage")
    private fun handlePacketOut(event: PlayerPacketOutEvent) {
        val original = event.packet as? DeclareCommandsPacket ?: return
        val merger = MinestomCommandAPIPlatform.activeMerger() ?: return

        val merged = merger.merge(original, event.player)
        if (merged === original) return
        event.setPacket(merged)
    }
}
