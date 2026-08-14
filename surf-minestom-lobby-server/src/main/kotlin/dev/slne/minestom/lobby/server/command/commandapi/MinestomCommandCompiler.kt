package dev.slne.minestom.lobby.server.command.commandapi

import dev.slne.minestom.lobby.api.command.commandapi.CommandDefinition
import dev.slne.minestom.lobby.api.command.commandapi.CommandPath
import dev.slne.minestom.lobby.api.command.commandapi.RegisteredCommand
import dev.slne.minestom.lobby.api.command.commandapi.argument.ArgumentDefinition
import dev.slne.minestom.lobby.api.command.commandapi.argument.ArgumentKind
import dev.slne.minestom.lobby.api.command.commandapi.argument.SuggestionMode
import dev.slne.minestom.lobby.api.command.commandapi.exception.CommandValidationException
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.Argument
import java.util.*

internal data class CompiledRegistration(
    val command: Command,
    val names: Set<String>,
    val registration: RegisteredCommand,
)

internal class MinestomCommandCompiler(
    private val argumentCompiler: MinestomArgumentCompiler = MinestomArgumentCompiler(),
    private val conditions: MinestomConditions = MinestomConditions(),
    private val executorAdapter: MinestomExecutorAdapter = MinestomExecutorAdapter(),
) {
    private val suggestionAdapter = MinestomSuggestionAdapter(conditions)

    fun compile(definition: CommandDefinition, namespace: String?): CompiledRegistration {
        validateSuggestionModes(definition)

        val baseNames = ObjectArrayList<String>(definition.aliases.size + 1)
        baseNames += normalize(definition.name)
        definition.aliases.forEach { alias ->
            baseNames += normalize(alias)
        }

        val names = ObjectLinkedOpenHashSet<String>(
            if (namespace == null) {
                baseNames.size
            } else {
                baseNames.size * 2
            }
        )

        names.addAll(baseNames)
        if (namespace != null) {
            baseNames.forEach { name ->
                names += normalize("$namespace:$name")
            }
        }

        val aliases = ObjectArrayList<String>(names.size - 1)
        val nameIterator = names.iterator()
        val primaryName = nameIterator.next()

        while (nameIterator.hasNext()) {
            aliases += nameIterator.next()
        }

        val command = Command(primaryName, *aliases.toTypedArray())

        val nativePaths = ObjectArrayList<NativePath>(definition.paths.size)
        definition.paths.forEach { path ->
            nativePaths += NativePath(
                path = path,
                remaining = path.arguments,
                fixed = emptyList(),
            )
        }

        compilePaths(command, nativePaths, AccumulatedConditions())

        return CompiledRegistration(
            command = command,
            names = names,
            registration = RegisteredCommand(definition.name, definition.aliases, namespace),
        )
    }

    private fun validateSuggestionModes(definition: CommandDefinition) {
        for (path in definition.paths) {
            for (argument in path.arguments) {
                val suggestionOwner = suggestionOwner(argument)
                if (
                    builtInDefinition(argument).kind == ArgumentKind.EntityType &&
                    (suggestionOwner.suggestions is SuggestionMode.Include<*> ||
                            suggestionOwner.suggestions is SuggestionMode.IncludeSafe<*>)
                ) {
                    throw CommandValidationException(
                        "EntityType argument '${argument.nodeName}' cannot include custom suggestions because " +
                                "Minestom does not expose the native summonable entity set; " +
                                "use built-ins or replace suggestions",
                    )
                }

                if (argument.suggestions == SuggestionMode.BuiltIns) continue

                val kindName = when (builtInDefinition(argument).kind) {
                    is ArgumentKind.Literal -> "Literal"
                    ArgumentKind.Command -> "Command"
                    ArgumentKind.Position -> "Position"
                    ArgumentKind.Position2D -> "Position2D"
                    ArgumentKind.BlockPosition -> "BlockPosition"
                    ArgumentKind.Rotation -> "Rotation"
                    else -> null
                }

                if (kindName != null) {
                    throw CommandValidationException(
                        "$kindName argument '${argument.nodeName}' cannot use custom suggestions",
                    )
                }
            }
        }
    }

    private fun compilePaths(
        command: Command,
        paths: List<NativePath>,
        inheritedConditions: AccumulatedConditions,
    ) {
        val commandPaths = ObjectArrayList<CommandPath>(paths.size)
        paths.forEach { nativePath ->
            commandPaths += nativePath.path
        }

        val nativeConditions = conditions.commonForPaths(
            commandPaths,
            inheritedConditions,
        )

        command.condition = conditions.asNative(nativeConditions)

        val accumulatedConditions = inheritedConditions + nativeConditions
        val literalGroups = Object2ObjectLinkedOpenHashMap<String, MutableList<NativePath>>()

        paths.forEach { nativePath ->
            val first = nativePath.remaining.firstOrNull()
            val literal = first?.kind as? ArgumentKind.Literal

            if (first != null && literal != null && !first.optional) {
                val remaining = ObjectArrayList<ArgumentDefinition<*>>(
                    nativePath.remaining.size - 1,
                )

                for (index in 1 until nativePath.remaining.size) {
                    remaining += nativePath.remaining[index]
                }

                val fixed = ObjectArrayList<FixedArgument>(nativePath.fixed.size + 1)
                fixed.addAll(nativePath.fixed)
                fixed += FixedArgument(first, literal.literal)

                literalGroups.computeIfAbsent(literal.literal) {
                    ObjectArrayList()
                }.add(
                    nativePath.copy(
                        remaining = remaining,
                        fixed = fixed,
                    ),
                )
            } else {
                compileSyntaxes(command, nativePath, accumulatedConditions)
            }
        }

        literalGroups.forEach { (literal, children) ->
            val subcommand = Command(literal)
            compilePaths(subcommand, children, accumulatedConditions)
            command.addSubcommand(subcommand)
        }
    }

    private fun compileSyntaxes(
        command: Command,
        nativePath: NativePath,
        inheritedConditions: AccumulatedConditions,
    ) {
        val arguments = ObjectArrayList<CompiledArgument<*>>(nativePath.remaining.size)
        nativePath.remaining.forEach { definition ->
            arguments += argumentCompiler.compile(definition)
        }

        val fixedByName = Object2ObjectOpenHashMap<String, String>(nativePath.fixed.size)
        nativePath.fixed.forEach { argument ->
            fixedByName[argument.definition.nodeName] = argument.value
        }

        suggestionAdapter.adapt(nativePath.path, arguments, fixedByName)

        for (length in syntaxLengths(nativePath.remaining)) {
            val included = ObjectArrayList<CompiledArgument<*>>(length)
            for (index in 0 until length) {
                included += arguments[index]
            }

            val presentDefinitions = ObjectArrayList<ArgumentDefinition<*>>(
                nativePath.fixed.size + included.size,
            )
            nativePath.fixed.forEach { argument ->
                presentDefinitions += argument.definition
            }
            included.forEach { argument ->
                presentDefinitions += argument.definition
            }

            val includedByName = Object2ObjectOpenHashMap<String, CompiledArgument<*>>(
                included.size,
            )
            included.forEach { argument ->
                includedByName[argument.definition.nodeName] = argument
            }

            val nativeArguments = arrayOfNulls<Argument<*>>(included.size)
            included.forEachIndexed { index, argument ->
                nativeArguments[index] = argument.native
            }

            @Suppress("UNCHECKED_CAST")
            command.addConditionalSyntax(
                conditions.forSyntax(nativePath.path, presentDefinitions, inheritedConditions),
                executorAdapter.create(
                    nativePath.path,
                    includedByName,
                    fixedByName,
                ),
                *(nativeArguments as Array<Argument<*>>),
            )
        }
    }

    private fun syntaxLengths(arguments: List<ArgumentDefinition<*>>): IntRange {
        val firstOptional = arguments.indexOfFirst(ArgumentDefinition<*>::optional)
        return if (firstOptional == -1) arguments.size..arguments.size else firstOptional..arguments.size
    }

    private data class NativePath(
        val path: CommandPath,
        val remaining: List<ArgumentDefinition<*>>,
        val fixed: List<FixedArgument>,
    )

    private data class FixedArgument(
        val definition: ArgumentDefinition<*>,
        val value: String,
    )

    private fun normalize(name: String): String = name.lowercase(Locale.ROOT)
}
