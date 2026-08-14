package dev.slne.minestom.lobby.server.command.commandapi

import dev.slne.minestom.lobby.api.command.commandapi.suggestion.StringTooltip
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import net.minestom.server.command.CommandSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.parser.CommandParser
import net.minestom.server.command.builder.suggestion.Suggestion

/**
 * Recovers callbacks for an empty trailing argument using Minestom's own argument parser.
 *
 * Minestom's packet listener appends a semantic NUL placeholder and its graph parser keeps
 * only one successful path. For overlapping native argument types that can hide another
 * eligible CommandAPI path. This uses the legacy parser API, which remains part of the pinned
 * Minestom version, to ask the native argument parsers about each concrete syntax.
 */
internal object MinestomTrailingSuggestionRecovery {
    fun recover(
        registration: CompiledRegistration,
        sender: CommandSender,
        logicalInput: String,
    ): MinestomSuggestionRequest? {
        require(logicalInput.endsWith(' ')) { "Trailing recovery requires trailing whitespace" }

        val active = activeCommand(registration.command, logicalInput)
        val seenArguments = ReferenceOpenHashSet<Argument<*>>()

        val requests = ObjectArrayList<MinestomSuggestionRequest>(active.command.syntaxes.size)
        active.command.syntaxes.forEach { syntax ->
            val candidate = CommandParser.findEligibleArgument(
                sender,
                active.command,
                active.arguments,
                logicalInput,
                true,
                false,
                { current -> current === syntax },
                { true },
            ) ?: return@forEach

            if (candidate.input().isNotEmpty()) return@forEach
            val argument = candidate.argument()

            if (!seenArguments.add(argument)) return@forEach
            val callback = argument.suggestionCallback ?: return@forEach

            val request = CompletionCapture.capture(inputOverride = logicalInput) {
                val suggestion = Suggestion(logicalInput, logicalInput.length + 1, 0)
                callback.apply(sender, candidate.context(), suggestion)
                suggestion
            }.request ?: error("Failed to capture suggestion callback")

            requests += request
        }

        return merge(requests)
    }

    private fun activeCommand(root: Command, input: String): ActiveCommand {
        val parts = input
            .split(SPACE)
            .dropLastWhile(String::isEmpty)

        var command = root
        var argumentStart = 1

        while (argumentStart < parts.size) {
            val token = parts[argumentStart]
            val child = command.subcommands.firstOrNull { subcommand ->
                Command.isValidName(subcommand, token)
            } ?: break

            command = child
            argumentStart++
        }

        val arguments = arrayOfNulls<String>(parts.size - argumentStart)
        for (index in argumentStart until parts.size) {
            arguments[index - argumentStart] = parts[index]
        }

        @Suppress("UNCHECKED_CAST")
        return ActiveCommand(command, arguments as Array<String>)
    }

    private fun merge(requests: List<MinestomSuggestionRequest>): MinestomSuggestionRequest? {
        val first = requests.firstOrNull() ?: return null
        if (requests.size == 1) return first

        check(requests.all { request -> request.input == first.input && request.range == first.range }) {
            "Eligible trailing suggestion callbacks produced incompatible replacement ranges"
        }

        return first.copy(
            argumentName = requests.joinToString(
                prefix = "<",
                postfix = ">"
            ) { request -> request.argumentName },
            providerDescription = "merged eligible native syntaxes",
            resolve = {
                val resolved = coroutineScope {
                    val deferred = ObjectArrayList<Deferred<List<StringTooltip>>>(requests.size)
                    requests.forEach { request ->
                        deferred += async {
                            request.resolve()
                        }
                    }

                    deferred.awaitAll()
                }

                val unique = Object2ObjectLinkedOpenHashMap<String, StringTooltip>()
                resolved.forEach { entries ->
                    entries.forEach { entry ->
                        unique.putIfAbsent(entry.suggestion, entry)
                    }
                }

                ObjectArrayList(unique.values)
            },
        )
    }

    private data class ActiveCommand(
        val command: Command,
        val arguments: Array<String>,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ActiveCommand) return false

            if (command != other.command) return false
            if (!arguments.contentEquals(other.arguments)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = command.hashCode()
            result = 31 * result + arguments.contentHashCode()
            return result
        }
    }

    private val SPACE = Regex(" ")
}
