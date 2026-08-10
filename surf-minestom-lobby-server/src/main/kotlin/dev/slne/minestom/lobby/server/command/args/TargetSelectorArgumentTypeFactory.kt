package dev.slne.minestom.lobby.server.command.args

import dev.slne.minestom.lobby.api.command.selector.EntityTargets
import dev.slne.minestom.lobby.api.command.selector.PlayerTargets
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.arguments.ArgumentType
import revxrsal.commands.minestom.actor.MinestomCommandActor
import revxrsal.commands.minestom.argument.ArgumentTypeFactory
import revxrsal.commands.node.ParameterNode

object TargetSelectorArgumentTypeFactory : ArgumentTypeFactory<MinestomCommandActor> {

    override fun getArgumentType(
        parameter: ParameterNode<MinestomCommandActor?, *>,
    ): Argument<*>? = when (parameter.type()) {
        LobbyPlayer::class.java -> ArgumentType.Entity(parameter.name())
            .onlyPlayers(true)
            .singleEntity(true)
            .map { sender, finder -> finder.findFirstPlayer(sender) as? LobbyPlayer }

        PlayerTargets::class.java -> ArgumentType.Entity(parameter.name())
            .onlyPlayers(true)
            .singleEntity(false)
            .map { sender, finder ->
                PlayerTargets(finder.find(sender).filterIsInstance<LobbyPlayer>())
            }

        EntityTargets::class.java -> ArgumentType.Entity(parameter.name())
            .onlyPlayers(false)
            .singleEntity(false)
            .map { sender, finder -> EntityTargets(finder.find(sender)) }

        else -> null
    }
}
