package dev.slne.minestom.lobby.server.command.commandapi

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.minestom.lobby.api.command.commandapi.exception.componentOrNull
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerPacketEvent
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket
import net.minestom.server.network.packet.server.play.TabCompletePacket

/**
 * Answers tab completion for CommandAPI-owned commands from the Brigadier dispatcher.
 *
 * Input belonging to another registry is left to Minestom. Brigadier reports both the entries and
 * the range they replace, so the client receives the same replacement span it would get from vanilla.
 */
@Singleton
class MinestomSuggestionListener @Inject constructor(
    private val ownership: MinestomCommandOwnership,
) : EventRegistrar {

    override fun register(node: EventNode<Event>) {
        node.addListener(::handlePacket)
    }

    private fun handlePacket(event: PlayerPacketEvent) {
        val packet = event.packet as? ClientTabCompletePacket ?: return
        if (ownership.findInput(packet.text) == null) return

        event.isCancelled = true

        val dispatcher = MinestomCommandAPIPlatform.activeDispatcher() ?: return
        val input = packet.text.removePrefix("/")
        val parse = dispatcher.parse(input, event.player)
        val offset = packet.text.length - input.length

        dispatcher.getCompletionSuggestions(parse).thenAccept { suggestions ->
            if (!event.player.isOnline) return@thenAccept

            val matches = ObjectArrayList<TabCompletePacket.Match>(suggestions.list.size)
            suggestions.list.forEach { suggestion ->
                matches += TabCompletePacket.Match(
                    suggestion.text,
                    suggestion.tooltip?.componentOrNull(),
                )
            }

            event.player.sendPacket(
                TabCompletePacket(
                    packet.transactionId,
                    suggestions.range.start + offset,
                    suggestions.range.length,
                    matches,
                ),
            )
        }
    }

}
