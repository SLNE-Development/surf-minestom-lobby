package dev.slne.minestom.lobby.server.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.CommandRegistrar
import dev.slne.minestom.lobby.api.command.args.LiteralEnum
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.server.command.args.GameModeArgument
import dev.slne.minestom.lobby.server.command.params.GameModeParameterType
import dev.slne.minestom.lobby.server.command.params.LobbyPlayerParameterType
import dev.slne.minestom.lobby.server.command.permission.MinestomCommandPermissionFactory
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.GameMode
import revxrsal.commands.minestom.MinestomLamp
import revxrsal.commands.minestom.MinestomLampConfig
import revxrsal.commands.minestom.actor.ActorFactory
import revxrsal.commands.minestom.actor.MinestomCommandActor
import revxrsal.commands.minestom.argument.ArgumentTypeFactory
import revxrsal.commands.node.ParameterNode

@Singleton
class CommandService @Inject constructor(
    private val registrars: Set<@JvmSuppressWildcards CommandRegistrar>,
    private val lampPermissionFactory: MinestomCommandPermissionFactory,
    private val lobbyPlayerParamType: LobbyPlayerParameterType,
) : LobbyService {

    override suspend fun start() {
        val config = MinestomLampConfig.builder<MinestomCommandActor>()
            .actorFactory(ActorFactory.defaultFactory())
            .argumentTypes { types ->
                types.addType(GameMode::class.java) { node -> GameModeArgument(node.name()) }
                types.addTypeFactory(LiteralEnumFactory)
            }
            .build()

        val lamp = MinestomLamp.builder(config)
            .permissionFactory(lampPermissionFactory)
            .parameterTypes { builder ->
                builder.addParameterType(LobbyPlayer::class.java, lobbyPlayerParamType)
                builder.addParameterType(GameMode::class.java, GameModeParameterType())
            }
            .build()

        for (registrar in registrars) {
            registrar.register(lamp)
        }
    }

    private object LiteralEnumFactory : ArgumentTypeFactory<MinestomCommandActor> {
        override fun getArgumentType(parameter: ParameterNode<MinestomCommandActor?, *>): Argument<*>? {
            val type = parameter.type()
            val annotation = parameter.annotations().get(LiteralEnum::class.java)
            if (annotation == null || !type.isEnum) return null

            @Suppress("UNCHECKED_CAST")
            return ArgumentType.Enum(
                parameter.name(),
                type as Class<Enum<*>>
            ).setFormat(annotation.format)
        }
    }
}
