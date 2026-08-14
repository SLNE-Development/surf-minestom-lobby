package dev.slne.minestom.lobby.server.command.commandapi

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.commandapi.argument.InputShape
import dev.slne.minestom.lobby.api.command.commandapi.suggestion.StringTooltip
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.addListener
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList
import net.minestom.server.MinecraftServer
import net.minestom.server.command.builder.suggestion.Suggestion
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerPacketEvent
import net.minestom.server.listener.TabCompleteListener
import net.minestom.server.network.packet.client.play.ClientTabCompletePacket

@Singleton
class MinestomSuggestionListener @Inject constructor(
    private val ownership: MinestomCommandOwnership,
    private val suggestions: MinestomSuggestionService,
) : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener(::handlePacket)
        node.addListener(::handleDisconnect)
    }

    private fun handlePacket(event: PlayerPacketEvent) {
        val packet = event.packet as? ClientTabCompletePacket ?: return
        val registration = ownership.findInput(packet.text) ?: return
        event.isCancelled = true

        var captured = runCatching {
            CompletionCapture.capture {
                TabCompleteListener.getSuggestion(event.player, packet.text)
            }
        }.getOrElse { failure ->
            MinecraftServer.LOGGER.error(
                "Failed to capture suggestions for input={}, player={}",
                packet.text,
                event.player.uuid,
                failure,
            )
            CapturedCompletion(request = null, nativeSuggestion = null)
        }

        if (packet.text.endsWith(' ')) {
            captured = runCatching {
                val recovered = MinestomTrailingSuggestionRecovery.recover(
                    registration,
                    event.player,
                    packet.text.removePrefix("/"),
                )
                CapturedCompletion(recovered, captured.nativeSuggestion)
            }.getOrElse { failure ->
                MinecraftServer.LOGGER.error(
                    "Failed to recover trailing suggestions for input={}, player={}",
                    packet.text,
                    event.player.uuid,
                    failure,
                )
                CapturedCompletion(request = null, nativeSuggestion = captured.nativeSuggestion)
            }
        }

        val request = captured.request ?: fallbackRequest(packet.text, captured.nativeSuggestion)
        suggestions.submit(event.player, packet.transactionId, request)
    }

    private fun handleDisconnect(event: PlayerDisconnectEvent) {
        suggestions.cancel(event.player)
    }

    private fun fallbackRequest(
        text: String,
        native: Suggestion?,
    ): MinestomSuggestionRequest {
        val slashless = text.removePrefix("/")
        val nativeEntries = native?.entries

        val copiedEntries = if (nativeEntries == null) {
            ObjectList.of()
        } else {
            ObjectArrayList<StringTooltip>(nativeEntries.size).apply {
                nativeEntries.forEach { entry ->
                    add(StringTooltip(entry.entry, entry.tooltip))
                }
            }
        }

        val range = native?.let { suggestion ->
            SuggestionRange(suggestion.start, suggestion.length, current = "")
        } ?: SuggestionCursor.scan(slashless, InputShape.WORD)

        return MinestomSuggestionRequest(
            commandName = slashless.takeWhile { character -> !character.isWhitespace() },
            argumentName = "<native>",
            input = slashless,
            range = range,
            providerDescription = "Minestom native fallback",
            resolve = { copiedEntries },
        )
    }
}
