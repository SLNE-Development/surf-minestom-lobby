package dev.slne.minestom.lobby.api.command.suggestion

import dev.slne.minestom.lobby.api.extension.ConnectionManager
import dev.slne.minestom.lobby.api.player.onlineLobbyPlayers
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.minestom.actor.MinestomCommandActor
import revxrsal.commands.node.ExecutionContext

class OnlinePlayerSuggestionProvider : SuggestionProvider<MinestomCommandActor> {

    private val delegate = PrefixFilteredSuggestionProvider {
        ConnectionManager.onlineLobbyPlayers.map { it.username }
    }

    override fun getSuggestions(
        context: ExecutionContext<MinestomCommandActor>,
    ): Collection<String> = delegate.getSuggestions(context)
}
