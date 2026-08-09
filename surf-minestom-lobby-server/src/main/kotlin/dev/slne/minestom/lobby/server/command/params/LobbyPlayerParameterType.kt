package dev.slne.minestom.lobby.server.command.params

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.suggestion.OnlinePlayerSuggestionProvider
import dev.slne.minestom.lobby.api.extension.ConnectionManager
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.api.player.getOnlineLobbyPlayerByUsername
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.minestom.actor.MinestomCommandActor
import revxrsal.commands.minestom.exception.InvalidPlayerException
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.parameter.ParameterType
import revxrsal.commands.stream.MutableStringStream

@Singleton
class LobbyPlayerParameterType : ParameterType<MinestomCommandActor, LobbyPlayer> {
    override fun parse(
        input: MutableStringStream,
        context: ExecutionContext<MinestomCommandActor>
    ): LobbyPlayer {
        val name = input.readString()
        if (name == "@s") return context.actor().requirePlayer() as LobbyPlayer
        val player = ConnectionManager.getOnlineLobbyPlayerByUsername(name)
            ?: throw InvalidPlayerException(name)
        return player
    }

    override fun defaultSuggestions(): SuggestionProvider<MinestomCommandActor> =
        OnlinePlayerSuggestionProvider()
}