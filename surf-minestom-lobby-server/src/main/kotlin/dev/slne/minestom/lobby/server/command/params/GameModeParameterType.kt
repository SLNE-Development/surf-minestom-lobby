package dev.slne.minestom.lobby.server.command.params

import net.minestom.server.entity.GameMode
import revxrsal.commands.exception.EnumNotFoundException
import revxrsal.commands.minestom.actor.MinestomCommandActor
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.parameter.ParameterType
import revxrsal.commands.stream.MutableStringStream

class GameModeParameterType : ParameterType<MinestomCommandActor, GameMode> {

    override fun parse(
        input: MutableStringStream,
        context: ExecutionContext<MinestomCommandActor>,
    ): GameMode {
        val name = input.readString()
        return GameMode.entries.find { it.name.equals(name, ignoreCase = true) }
            ?: throw EnumNotFoundException(name, GameMode::class.java)
    }
}
