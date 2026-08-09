package dev.slne.minestom.lobby.server.command.args

import net.minestom.server.command.ArgumentParserType
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.exception.ArgumentSyntaxException
import net.minestom.server.entity.GameMode

class GameModeArgument(id: String) : Argument<GameMode>(id) {

    override fun parse(sender: CommandSender, input: String): GameMode {
        return BY_NAME[input.lowercase()]
            ?: throw ArgumentSyntaxException("Invalid game mode", input, INVALID_GAME_MODE)
    }

    override fun parser(): ArgumentParserType = ArgumentParserType.GAMEMODE

    companion object {
        const val INVALID_GAME_MODE = 1
        private val BY_NAME = GameMode.entries.associateBy { it.name.lowercase() }
    }
}
