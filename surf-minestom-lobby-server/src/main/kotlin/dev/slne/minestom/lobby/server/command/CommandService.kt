package dev.slne.minestom.lobby.server.command

import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.command.CommandRegistrar
import dev.slne.minestom.lobby.api.command.MinestomLampConfigVisitor
import dev.slne.minestom.lobby.api.command.args.LiteralEnum
import dev.slne.minestom.lobby.api.command.selector.EntityTargets
import dev.slne.minestom.lobby.api.command.selector.PlayerTargets
import dev.slne.minestom.lobby.api.player.LobbyPlayer
import dev.slne.minestom.lobby.server.command.args.GameModeArgument
import dev.slne.minestom.lobby.server.command.args.TargetSelectorArgumentTypeFactory
import dev.slne.minestom.lobby.server.command.params.GameModeParameterType
import dev.slne.minestom.lobby.server.command.permission.MinestomCommandPermissionFactory
import dev.slne.minestom.lobby.server.command.permission.MinestomRootCommandPermissionHook
import dev.slne.minestom.lobby.server.lifecycle.LobbyService
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.entity.Entity
import net.minestom.server.entity.GameMode
import revxrsal.commands.LampBuilderVisitor
import revxrsal.commands.minestom.MinestomLamp
import revxrsal.commands.minestom.MinestomLampConfig
import revxrsal.commands.minestom.MinestomStubParameterType
import revxrsal.commands.minestom.actor.ActorFactory
import revxrsal.commands.minestom.actor.MinestomCommandActor
import revxrsal.commands.minestom.argument.ArgumentTypeFactory
import revxrsal.commands.node.ParameterNode

@Singleton
class CommandService @Inject constructor(
    private val registrars: Set<@JvmSuppressWildcards CommandRegistrar>,
    private val lampPermissionFactory: MinestomCommandPermissionFactory,
    private val rootCommandPermissionHook: MinestomRootCommandPermissionHook,
) : LobbyService {

    override suspend fun start() {
        val configBuilder = MinestomLampConfig.builder<MinestomCommandActor>()
            .actorFactory(ActorFactory.defaultFactory())
            .argumentTypes { types ->
                types.addType(GameMode::class.java) { node -> GameModeArgument(node.name()) }
                types.addTypeFactory(TargetSelectorArgumentTypeFactory)
                types.addTypeFactory(LiteralEnumFactory)
            }

        for (registrar in registrars) {
            if (registrar is MinestomLampConfigVisitor) {
                registrar.configure(configBuilder)
            }
        }

        val config = configBuilder.build()

        val lampBuilder = MinestomLamp.builder(config)
            .permissionFactory(lampPermissionFactory)
            .parameterTypes { builder ->
                builder.addParameterType(
                    LobbyPlayer::class.java,
                    MinestomStubParameterType.stubParameterType(),
                )
                builder.addParameterType(
                    PlayerTargets::class.java,
                    MinestomStubParameterType.stubParameterType(),
                )
                builder.addParameterType(
                    EntityTargets::class.java,
                    MinestomStubParameterType.stubParameterType(),
                )
                builder.addParameterType(
                    Entity::class.java,
                    MinestomStubParameterType.stubParameterType(),
                )
                builder.addParameterType(GameMode::class.java, GameModeParameterType())
            }

        for (registrar in registrars) {
            if (registrar is LampBuilderVisitor<*>) {
                @Suppress("UNCHECKED_CAST")
                lampBuilder.accept(registrar as LampBuilderVisitor<MinestomCommandActor>)
            }
        }

        lampBuilder.hooks().onCommandRegistered(rootCommandPermissionHook)

        val lamp = lampBuilder.build()

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
