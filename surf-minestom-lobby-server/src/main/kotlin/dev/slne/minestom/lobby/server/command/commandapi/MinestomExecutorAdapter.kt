package dev.slne.minestom.lobby.server.command.commandapi

import dev.slne.minestom.lobby.api.command.commandapi.CommandAPI
import dev.slne.minestom.lobby.api.command.commandapi.CommandPath
import dev.slne.minestom.lobby.api.command.commandapi.executor.*
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.translation.GlobalTranslator
import net.minestom.server.adventure.MinestomAdventure
import net.minestom.server.command.CommandSender
import net.minestom.server.command.ConsoleSender
import net.minestom.server.command.builder.CommandContext
import net.minestom.server.command.builder.CommandData
import net.minestom.server.command.builder.CommandExecutor
import net.minestom.server.entity.Player

internal val NO_COMPATIBLE_EXECUTOR: Component = Component.text(
    "Dieser Befehl kann von diesem Absendertyp nicht ausgeführt werden.",
    NamedTextColor.RED,
)

internal class MinestomExecutorAdapter {
    fun create(
        path: CommandPath,
        compiledByName: Map<String, CompiledArgument<*>>,
        fixedByName: Map<String, String>,
    ): CommandExecutor = CommandExecutor { sender, context ->
        invokeSync(sender, context.input) {
            val unconsumed = MinestomTrailingInput.unconsumedFrom(context, path, fixedByName)
            if (unconsumed != null) {
                sender.sendTranslated(
                    MinestomTrailingInput.syntaxError(
                        context.input,
                        unconsumed,
                        MinestomTrailingInput.consumedArgument(context, path, fixedByName),
                    ),
                )
                return@invokeSync
            }

            val selected = selectExecutor(sender, path.executors)
            if (selected == null) {
                sender.sendMessage(NO_COMPATIBLE_EXECUTOR)
                return@invokeSync
            }

            val arguments = snapshotArguments(sender, context, path, compiledByName, fixedByName)
            val info = ExecutionInfo(sender, arguments, context.input)
            when (selected) {
                is ExecutorDefinition.Normal -> {
                    selected.executor(info)
                }

                is ExecutorDefinition.Resulting -> {
                    storeResult(context, selected.executor(info))
                }

                is ExecutorDefinition.Suspending -> {
                    launchCommandExecutor(selected, sender, arguments, context.input)
                }
            }
        }
    }

    private fun snapshotArguments(
        sender: CommandSender,
        context: CommandContext,
        path: CommandPath,
        compiledByName: Map<String, CompiledArgument<*>>,
        fixedByName: Map<String, String>,
    ): CommandArguments = CommandArguments.of(
        path.arguments.mapTo(ObjectArrayList(path.arguments.size)) { definition ->
            val fixed = fixedByName[definition.nodeName]
            val compiled = compiledByName[definition.nodeName]
            val present = fixed != null || compiled != null && context.has(definition.nodeName)

            val value = when {
                fixed != null -> fixed
                present -> checkNotNull(compiled).read(sender, context)
                else -> definition.defaultValue?.invoke(sender)
            }

            ParsedArgument(
                name = definition.nodeName,
                value = value,
                raw = when {
                    fixed != null -> fixed
                    present -> context.getRaw(definition.nodeName)
                    else -> null
                },
                present = present,
            )
        },
    )

    private fun selectExecutor(
        sender: CommandSender,
        executors: List<ExecutorDefinition>,
    ): ExecutorDefinition? {
        val preferred = when (sender) {
            is Player -> listOf(ExecutorType.PLAYER, ExecutorType.ANY)
            is ConsoleSender -> listOf(ExecutorType.CONSOLE, ExecutorType.ANY)
            else -> listOf(ExecutorType.ANY)
        }
        return preferred.firstNotNullOfOrNull { type ->
            executors.singleOrNull { executor -> executor.type == type }
        }
    }

    private fun storeResult(context: CommandContext, result: Int) {
        context.returnData = CommandData().set(CommandAPI.RESULT_KEY, result)
    }

    private inline fun invokeSync(
        sender: CommandSender,
        input: String,
        invocation: () -> Unit,
    ) {
        try {
            invocation()
        } catch (failure: Throwable) {
            handleCommandExecutorFailure(sender, input, failure)
        }
    }

    /**
     * Sends [message] to this sender, resolving translation keys for senders that cannot resolve them
     * themselves.
     */
    private fun CommandSender.sendTranslated(message: Component) {
        if (this is Player) {
            sendMessage(message)
        } else {
            sendMessage(GlobalTranslator.render(message, MinestomAdventure.getDefaultLocale()))
        }
    }

}
