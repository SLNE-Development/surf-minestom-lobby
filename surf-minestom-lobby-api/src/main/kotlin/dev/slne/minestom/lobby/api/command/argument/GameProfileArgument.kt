package dev.slne.minestom.lobby.api.command.argument

import net.minestom.server.command.ArgumentParserType
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.exception.ArgumentSyntaxException

/**
 * A Minecraft `game_profile` argument that keeps the entered profile name or UUID as a string.
 *
 * Parsing performs no Mojang lookup. Consumers can resolve the value against their
 * own player storage, while the client still sees the native game-profile command node.
 */
class GameProfileArgument(id: String) : Argument<String>(id) {
    override fun parse(sender: CommandSender, input: String): String {
        if (input.isEmpty() || input.any(Char::isWhitespace)) {
            throw ArgumentSyntaxException("Invalid game profile", input, INVALID_PROFILE)
        }
        return input
    }

    override fun parser(): ArgumentParserType = ArgumentParserType.GAME_PROFILE

    override fun toString(): String = "GameProfile<$id>"

    private companion object {
        const val INVALID_PROFILE = 1
    }
}
